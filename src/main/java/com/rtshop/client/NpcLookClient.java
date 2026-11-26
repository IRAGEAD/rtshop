package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * NPC 외형 그리기 (3.61, 시안 C): 서버가 알려 준 NPC(엔티티 번호 → 모습 이름)를 주민 대신 그 인물의 모습으로 그린다.
 * 모습은 그림을 입힌 네모들의 묶음이다 (네모 = assets/rtshop/npc_looks/&lt;이름&gt;.bin, 빛깔 = textures/npc_looks/&lt;이름&gt;.png, 형식은 tools/gen_npc_looks.py 머리말).
 * 3.63: 빛깔을 그림에 구워 넣어 네모를 인물당 2천여 개 → 2~3백 개로 줄였고, 카메라 반대쪽을 보는 네모는 넘기지 않는다
 * (3.61 · 3.62 에서는 NPC 가 모인 곳에서 프레임이 크게 떨어졌다).
 * 몸은 엔티티가 보는 쪽으로 서고, 머리(얼굴·머리카락·수염·모자)는 8칸 안의 플레이어를 천천히 바라본다. 이름표는 예전처럼 머리 위에.
 * client.json 의 npcLooks 를 false 로 하면 예전의 주민 모습.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class NpcLookClient {
    private NpcLookClient() {
    }

    /** 네모 하나 = 꼭짓점 넷(12) + 방향(1) + 그림 속 자리 넷(8). */
    private static final int STRIDE = 21;
    private static final float[][] NORMALS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    /** 반 픽셀 한 칸의 크기: 사람 키 32픽셀 = 1.875칸. */
    private static final float UNIT = 0.9375F / 32F;
    /** 머리가 도는 축의 높이 (반 픽셀): 목. */
    private static final float NECK = 48F;

    private static final class Model {
        float[] body = new float[0], head = new float[0];
        float top;
        ResourceLocation texture;
    }

    private static final Model MISSING = new Model();
    private static final Map<Integer, String> LOOKS = new HashMap<>();
    private static final Map<String, Model> MODELS = new HashMap<>();
    private static final Map<Integer, float[]> HEADS = new HashMap<>();
    /** 점검용: 지금까지 사람 모습으로 그린 횟수. */
    public static long drawnForTest;
    /** 점검용: 마지막으로 그린 NPC 하나에서 실제로 넘긴 네모 수 / 카메라 반대쪽이라 넘기지 않은 네모 수. */
    public static int lastQuadsForTest, lastCulledForTest;

    /** 서버가 보낸 것: {"reset": true} 또는 {"looks": {"엔티티 번호": "모습 이름"}}. */
    public static void receive(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("reset") && o.get("reset").getAsBoolean()) {
                LOOKS.clear();
                HEADS.clear();
            }
            if (o.has("looks")) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("looks").entrySet()) {
                    LOOKS.put(Integer.parseInt(e.getKey()), e.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static int knownForTest() {
        return LOOKS.size();
    }

    /** 모습 파일을 읽어 그릴 수 있는 꼴로 바꾼다. 없거나 깨졌으면 null (그 NPC 는 주민 모습으로 남는다). */
    public static int quadsForTest(String look) {
        Model m = model(look);
        return m == null ? -1 : (m.body.length + m.head.length) / STRIDE;
    }

    private static Model model(String look) {
        Model m = MODELS.get(look);
        if (m == null) {
            m = load(look);
            MODELS.put(look, m == null ? MISSING : m);
        }
        return m == MISSING ? null : m;
    }

    private static Model load(String look) {
        try {
            ResourceLocation rl = ResourceLocation.tryParse("rtshop:npc_looks/" + look + ".bin");
            if (rl == null) return null;
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
            if (res.isEmpty()) return null;
            byte[] b;
            try (InputStream in = res.get().open()) {
                b = in.readAllBytes();
            }
            if (b.length < 13 || b[0] != 'R' || b[1] != 'T' || b[2] != 'N' || b[3] != 'L' || b[4] != 2) return null;
            int n = ((b[5] & 0xFF) << 24) | ((b[6] & 0xFF) << 16) | ((b[7] & 0xFF) << 8) | (b[8] & 0xFF);
            float tw = ((b[9] & 0xFF) << 8) | (b[10] & 0xFF), th = ((b[11] & 0xFF) << 8) | (b[12] & 0xFF);
            if (n < 0 || n > 20000 || tw <= 0 || th <= 0 || b.length < 13 + n * 11) return null;
            ResourceLocation tex = ResourceLocation.tryParse("rtshop:textures/npc_looks/" + look + ".png");
            if (tex == null || Minecraft.getInstance().getResourceManager().getResource(tex).isEmpty()) return null;
            int heads = 0;
            for (int i = 0; i < n; i++) if (b[13 + i * 11] == 1) heads++;
            Model m = new Model();
            m.texture = tex;
            m.body = new float[(n - heads) * STRIDE];
            m.head = new float[heads * STRIDE];
            int ib = 0, ih = 0;
            for (int i = 0; i < n; i++) {
                int o = 13 + i * 11;
                boolean head = b[o] == 1;
                int dir = Math.max(0, Math.min(5, b[o + 1]));
                float p = b[o + 2], a0 = b[o + 3], b0 = b[o + 4], a1 = b[o + 5], b1 = b[o + 6];
                float u = ((b[o + 7] & 0xFF) << 8) | (b[o + 8] & 0xFF), v = ((b[o + 9] & 0xFF) << 8) | (b[o + 10] & 0xFF);
                float[] q = head ? m.head : m.body;
                int at = head ? ih : ib;
                if (dir < 2) {            // ±x: a = y, b = z
                    put(q, at, p, a0, b0, p, a1, b0, p, a1, b1, p, a0, b1);
                    m.top = Math.max(m.top, a1);
                } else if (dir < 4) {     // ±y: a = x, b = z
                    put(q, at, a0, p, b0, a1, p, b0, a1, p, b1, a0, p, b1);
                    m.top = Math.max(m.top, p);
                } else {                  // ±z: a = x, b = y
                    put(q, at, a0, b0, p, a1, b0, p, a1, b1, p, a0, b1, p);
                    m.top = Math.max(m.top, b1);
                }
                q[at + 12] = dir;
                float u0 = u / tw, v0 = v / th, u1 = (u + (a1 - a0)) / tw, v1 = (v + (b1 - b0)) / th;
                q[at + 13] = u0;
                q[at + 14] = v0;
                q[at + 15] = u1;
                q[at + 16] = v0;
                q[at + 17] = u1;
                q[at + 18] = v1;
                q[at + 19] = u0;
                q[at + 20] = v1;
                if (head) ih += STRIDE;
                else ib += STRIDE;
            }
            return m;
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] NPC 모습 파일을 읽지 못했습니다: {} ({})", look, t.toString());
            return null;
        }
    }

    private static void put(float[] q, int at, float... v) {
        System.arraycopy(v, 0, q, at, 12);
    }

    private static final org.joml.Vector3f TMP = new org.joml.Vector3f();
    private static final float[][] VIEW_NORMALS = new float[6][3];

    /** 네모들을 넘긴다. 카메라 반대쪽을 보는 네모(카메라에서 그 면의 앞이 보이지 않는 것)는 건너뛴다. 넘긴 수를 돌려준다. */
    private static int emit(float[] q, PoseStack ps, VertexConsumer vc, int light) {
        PoseStack.Pose last = ps.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();
        for (int d = 0; d < 6; d++) {
            normal.transform(NORMALS[d][0], NORMALS[d][1], NORMALS[d][2], TMP);
            VIEW_NORMALS[d][0] = TMP.x;
            VIEW_NORMALS[d][1] = TMP.y;
            VIEW_NORMALS[d][2] = TMP.z;
        }
        int sent = 0;
        for (int i = 0; i + STRIDE <= q.length; i += STRIDE) {
            int d = (int) q[i + 12];
            float[] vn = VIEW_NORMALS[d];
            // 이 자리의 좌표는 카메라가 원점이다: 면 위의 한 점과 법선의 곱이 0 이상이면 뒷면이 보이는 것
            pose.transformPosition(q[i], q[i + 1], q[i + 2], TMP);
            if (TMP.x * vn[0] + TMP.y * vn[1] + TMP.z * vn[2] >= 0F) continue;
            float nx = NORMALS[d][0], ny = NORMALS[d][1], nz = NORMALS[d][2];
            for (int v = 0; v < 4; v++) {
                int k = i + v * 3, t = i + 13 + v * 2;
                vc.vertex(pose, q[k], q[k + 1], q[k + 2]).color(255, 255, 255, 255).uv(q[t], q[t + 1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
            }
            sent++;
        }
        return sent;
    }

    /** 머리가 몸에 대해 돌아갈 각 (좌우, 위아래): 8칸 안의 플레이어를 천천히 바라보고, 멀어지거나 등 뒤로 가면 앞을 본다. */
    private static float[] headAngles(LivingEntity e, float bodyYaw) {
        float[] cur = HEADS.computeIfAbsent(e.getId(), k -> new float[2]);
        float wantYaw = 0F, wantPitch = 0F;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            double dx = mc.player.getX() - e.getX(), dz = mc.player.getZ() - e.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat < 8.0 && flat > 0.3) {
                float rel = Mth.wrapDegrees((float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90F - bodyYaw);
                if (Math.abs(rel) <= 80F) {
                    wantYaw = Mth.clamp(rel, -40F, 40F);
                    double dy = mc.player.getEyeY() - (e.getY() + 1.62);
                    wantPitch = Mth.clamp((float) -(Mth.atan2(dy, flat) * (180.0 / Math.PI)), -20F, 20F);
                }
            }
        }
        cur[0] += (wantYaw - cur[0]) * 0.08F;
        cur[1] += (wantPitch - cur[1]) * 0.08F;
        return cur;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        if (LOOKS.isEmpty()) return;
        LivingEntity e = event.getEntity();
        String look = LOOKS.get(e.getId());
        if (look == null || e.isInvisible() || !ClientConfig.get().npcLooks) return;
        Model m = model(look);
        if (m == null) return;
        event.setCanceled(true);
        try {
            PoseStack ps = event.getPoseStack();
            float pt = event.getPartialTick();
            int light = event.getPackedLight();
            MultiBufferSource buf = event.getMultiBufferSource();
            float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            ps.scale(UNIT, UNIT, UNIT);
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(m.texture));
            int sent = emit(m.body, ps, vc, light);
            float[] h = headAngles(e, bodyYaw);
            ps.translate(0F, NECK, 0F);
            ps.mulPose(Axis.YP.rotationDegrees(-h[0]));
            ps.mulPose(Axis.XP.rotationDegrees(h[1]));
            ps.translate(0F, -NECK, 0F);
            sent += emit(m.head, ps, vc, light);
            ps.popPose();
            drawnForTest++;
            lastQuadsForTest = sent;
            lastCulledForTest = (m.body.length + m.head.length) / STRIDE - sent;
            nameTag(e, ps, buf, light, Math.max(e.getBbHeight() + 0.5F, m.top * UNIT + 0.35F));
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] npc look", t);
        }
    }

    /** 머리 위의 이름표 (주민을 그리지 않으니 직접 그린다. 바닐라의 이름표와 같은 모습). */
    private static void nameTag(LivingEntity e, PoseStack ps, MultiBufferSource buf, int light, float height) {
        Minecraft mc = Minecraft.getInstance();
        if (!Minecraft.renderNames()) return;
        boolean show = e.isCustomNameVisible() || (e.hasCustomName() && e == mc.getEntityRenderDispatcher().crosshairPickEntity);
        if (!show || mc.getEntityRenderDispatcher().distanceToSqr(e) > 4096.0) return;
        Component name = e.getDisplayName();
        boolean through = !e.isDiscrete();
        ps.pushPose();
        ps.translate(0F, height, 0F);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f pose = ps.last().pose();
        int back = (int) (mc.options.getBackgroundOpacity(0.25F) * 255F) << 24;
        Font font = mc.font;
        float x = -font.width(name) / 2F;
        font.drawInBatch(name, x, 0F, 0x20FFFFFF, false, pose, buf, through ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, back, light);
        if (through) font.drawInBatch(name, x, 0F, 0xFFFFFFFF, false, pose, buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LOOKS.clear();
        HEADS.clear();
    }
}
