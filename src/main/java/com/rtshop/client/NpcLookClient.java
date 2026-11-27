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
 * 몸은 엔티티가 보는 쪽으로 서고, 머리(얼굴·머리카락·수염·모자)는 8칸 안의 플레이어를 천천히 바라본다. 이름표는 예전처럼 머리 위에
 * (3.64: 가까울 때만 — client.json 의 npcNameDistance, 기본 16칸).
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
    /** 3.71 팔다리가 도는 축 (반 픽셀): 어깨(옆으로 12, 높이 44) · 엉덩이(옆으로 4, 높이 24). */
    private static final float ARM_X = 12F, ARM_Y = 44F, LEG_X = 4F, LEG_Y = 24F;

    private static final class Model {
        float[] body = new float[0], head = new float[0];
        /** 3.71: 부위 2 · 3 (팔 +x · -x), 4 · 5 (다리 +x · -x). 치마 · 긴 겉옷을 입은 인물은 다리가 없다(몸에 붙음). */
        float[][] limbs = {new float[0], new float[0], new float[0], new float[0]};
        float top;
        ResourceLocation texture;

        boolean legs() {
            return limbs[2].length > 0 || limbs[3].length > 0;
        }
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
        if (m == null) return -1;
        int n = m.body.length + m.head.length;
        for (float[] l : m.limbs) n += l.length;
        return n / STRIDE;
    }

    /** 점검용: 부위별 네모 수 [몸, 머리, 팔+, 팔-, 다리+, 다리-]. */
    public static int[] partsForTest(String look) {
        Model m = model(look);
        if (m == null) return new int[0];
        return new int[]{m.body.length / STRIDE, m.head.length / STRIDE, m.limbs[0].length / STRIDE, m.limbs[1].length / STRIDE, m.limbs[2].length / STRIDE, m.limbs[3].length / STRIDE};
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
            // 부위: 0 몸 · 1 머리 · 2~5 팔다리 (3.71, 그 밖의 값은 몸)
            int[] count = new int[6];
            for (int i = 0; i < n; i++) {
                int part = b[13 + i * 11];
                count[part >= 0 && part < 6 ? part : 0]++;
            }
            Model m = new Model();
            m.texture = tex;
            m.body = new float[count[0] * STRIDE];
            m.head = new float[count[1] * STRIDE];
            for (int k = 0; k < 4; k++) m.limbs[k] = new float[count[k + 2] * STRIDE];
            int[] at = new int[6];
            for (int i = 0; i < n; i++) {
                int o = 13 + i * 11;
                int part = b[o] >= 0 && b[o] < 6 ? b[o] : 0;
                boolean head = part == 1;
                int dir = Math.max(0, Math.min(5, b[o + 1]));
                float p = b[o + 2], a0 = b[o + 3], b0 = b[o + 4], a1 = b[o + 5], b1 = b[o + 6];
                float u = ((b[o + 7] & 0xFF) << 8) | (b[o + 8] & 0xFF), v = ((b[o + 9] & 0xFF) << 8) | (b[o + 10] & 0xFF);
                float[] q = part == 0 ? m.body : head ? m.head : m.limbs[part - 2];
                int w = at[part];
                if (dir < 2) {            // ±x: a = y, b = z
                    put(q, w, p, a0, b0, p, a1, b0, p, a1, b1, p, a0, b1);
                    m.top = Math.max(m.top, a1);
                } else if (dir < 4) {     // ±y: a = x, b = z
                    put(q, w, a0, p, b0, a1, p, b0, a1, p, b1, a0, p, b1);
                    m.top = Math.max(m.top, p);
                } else {                  // ±z: a = x, b = y
                    put(q, w, a0, b0, p, a1, b0, p, a1, b1, p, a0, b1, p);
                    m.top = Math.max(m.top, b1);
                }
                q[w + 12] = dir;
                float u0 = u / tw, v0 = v / th, u1 = (u + (a1 - a0)) / tw, v1 = (v + (b1 - b0)) / th;
                q[w + 13] = u0;
                q[w + 14] = v0;
                q[w + 15] = u1;
                q[w + 16] = v0;
                q[w + 17] = u1;
                q[w + 18] = v1;
                q[w + 19] = u0;
                q[w + 20] = v1;
                at[part] += STRIDE;
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

    private static java.lang.reflect.Method shadowPass;
    private static boolean shadowPassLooked;

    /**
     * 뒷면 건너뛰기를 해도 되는가: "카메라가 원점인 원근 화면"일 때만 맞는 셈이라,
     * 직교 투영(화면 속에 그리는 엔티티 등)이거나 셰이더(Oculus · Iris)가 그림자를 그리는 중이면 건너뛰지 않는다.
     */
    private static boolean mayCull() {
        try {
            if (com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrix().m23() == 0F) return false;
            if (!shadowPassLooked) {
                shadowPassLooked = true;
                for (String cn : new String[]{"net.irisshaders.iris.shadows.ShadowRenderingState", "net.coderbot.iris.shadows.ShadowRenderingState"}) {
                    try {
                        shadowPass = Class.forName(cn).getMethod("areShadowsCurrentlyBeingRendered");
                        break;
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (shadowPass != null && Boolean.TRUE.equals(shadowPass.invoke(null))) return false;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 네모들을 넘긴다. cull 이면 카메라 반대쪽을 보는 네모(카메라에서 그 면의 앞이 보이지 않는 것)는 건너뛴다. 넘긴 수를 돌려준다. */
    private static int emit(float[] q, PoseStack ps, VertexConsumer vc, int light, boolean cull) {
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
            if (cull) pose.transformPosition(q[i], q[i + 1], q[i + 2], TMP);
            if (cull && TMP.x * vn[0] + TMP.y * vn[1] + TMP.z * vn[2] >= 0F) continue;
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

    /**
     * 3.71 팔다리 각 (라디안): [팔+ 앞뒤, 팔- 앞뒤, 팔 옆으로, 다리+ 앞뒤, 다리- 앞뒤, 몸 오르내림(반 픽셀), 몸 좌우 기울기(도)].
     * 걸을 때는 팔과 반대쪽 다리가 함께 앞으로 (다리가 없는 인물 — 치마 · 긴 겉옷 — 은 몸이 살짝 오르내리며 흔들림),
     * 서 있을 때는 팔이 숨 쉬듯 조금 흔들린다.
     */
    public static float[] pose(float walkPos, float walkSpeed, float age, boolean legs) {
        float spd = Mth.clamp(walkSpeed, 0F, 1F);
        float swing = Mth.cos(walkPos * 0.6662F) * spd;
        float idleX = Mth.sin(age * 0.067F) * 0.05F, idleZ = Mth.cos(age * 0.09F) * 0.04F + 0.05F;
        float arm = swing * 0.75F;
        float leg = legs ? swing * 0.9F : 0F;
        float bob = legs ? 0F : Math.abs(Mth.sin(walkPos * 0.6662F)) * 1.2F * spd;
        float tilt = legs ? 0F : Mth.sin(walkPos * 0.6662F) * 2.5F * spd;
        return new float[]{-arm + idleX, arm - idleX, idleZ, leg, -leg, bob, tilt};
    }

    /** 점검용: 마지막으로 그린 인물의 자세. */
    public static float[] lastPoseForTest = new float[7];

    private static int limb(float[] q, PoseStack ps, VertexConsumer vc, int light, boolean cull, float px, float py, float xRot, float zRot) {
        if (q.length == 0) return 0;
        ps.pushPose();
        ps.translate(px, py, 0F);
        if (zRot != 0F) ps.mulPose(Axis.ZP.rotation(zRot));
        if (xRot != 0F) ps.mulPose(Axis.XP.rotation(xRot));
        ps.translate(-px, -py, 0F);
        int n = emit(q, ps, vc, light, cull);
        ps.popPose();
        return n;
    }

    /** 동행처럼 스스로 움직이는 인물은 그 엔티티가 보는 쪽으로 머리를 돌린다 (마을 NPC 는 가까운 플레이어를 바라봄). */
    private static float[] ownHead(LivingEntity e, float bodyYaw, float pt) {
        float yaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot) - bodyYaw), -50F, 50F);
        float pitch = Mth.clamp(Mth.lerp(pt, e.xRotO, e.getXRot()), -30F, 30F);
        return new float[]{yaw, pitch};
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity e = event.getEntity();
        boolean comp = e instanceof com.rtshop.story.CompanionEntity;
        if (LOOKS.isEmpty() && !comp) return;
        String look = LOOKS.get(e.getId());
        // 3.75 (W-2): 동행은 엔티티에 실린 모습을 먼저 (서버의 모습 목록이 닿기 전에도)
        if (comp && !((com.rtshop.story.CompanionEntity) e).look().isEmpty()) look = ((com.rtshop.story.CompanionEntity) e).look();
        if (ABSENT.equals(look)) {   // 3.77: 이야기 속에서 자리를 비운 사람 — 그리지 않는다 (이름표도)
            event.setCanceled(true);
            absentSkippedForTest++;
            return;
        }
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
            boolean companion = e instanceof com.rtshop.story.CompanionEntity;
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
            if (companion && ((com.rtshop.story.CompanionEntity) e).isDowned()) {   // 쓰러졌을 때는 몸을 숙인다 (CompanionRenderer 와 같게)
                ps.translate(0, -0.25, 0);
                ps.mulPose(Axis.XP.rotationDegrees(28F));
            }
            ps.scale(UNIT, UNIT, UNIT);
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(m.texture));
            boolean cull = mayCull();
            float[] pose = pose(e.walkAnimation.position(pt), e.walkAnimation.speed(pt), e.tickCount + pt, m.legs());
            lastPoseForTest = pose;
            if (pose[5] != 0F) ps.translate(0F, pose[5], 0F);
            if (pose[6] != 0F) ps.mulPose(Axis.ZP.rotationDegrees(pose[6]));
            int sent = emit(m.body, ps, vc, light, cull);
            sent += limb(m.limbs[0], ps, vc, light, cull, ARM_X, ARM_Y, pose[0], pose[2]);
            sent += limb(m.limbs[1], ps, vc, light, cull, -ARM_X, ARM_Y, pose[1], -pose[2]);
            sent += limb(m.limbs[2], ps, vc, light, cull, LEG_X, LEG_Y, pose[3], 0F);
            sent += limb(m.limbs[3], ps, vc, light, cull, -LEG_X, LEG_Y, pose[4], 0F);
            float[] h = companion ? ownHead(e, bodyYaw, pt) : headAngles(e, bodyYaw);
            ps.translate(0F, NECK, 0F);
            ps.mulPose(Axis.YP.rotationDegrees(-h[0]));
            ps.mulPose(Axis.XP.rotationDegrees(h[1]));
            ps.translate(0F, -NECK, 0F);
            sent += emit(m.head, ps, vc, light, cull);
            ps.popPose();
            drawnForTest++;
            if (cull) {   // 그림자 그리기처럼 건너뛰지 않는 때의 수는 점검 기록에 섞지 않는다
                int all = m.body.length + m.head.length;
                for (float[] l : m.limbs) all += l.length;
                lastQuadsForTest = sent;
                lastCulledForTest = all / STRIDE - sent;
            }
            if (companion) companionsDrawnForTest++;
            nameTag(e, ps, buf, light, Math.max(e.getBbHeight() + 0.5F, m.top * UNIT + 0.35F));
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] npc look", t);
        }
    }

    /** 점검용: 동행 NPC 를 인물 모습으로 그린 횟수. */
    public static long companionsDrawnForTest;

    /** 3.77: 서버가 「자리를 비웠다」고 알린 NPC 의 모습 이름 (NpcLooks.ABSENT 와 같다). */
    public static final String ABSENT = "_absent";
    /** 점검용: 자리를 비워 그리지 않은 횟수. */
    public static long absentSkippedForTest;

    /** 이 엔티티를 자리를 비운 것으로 받았는가. */
    public static boolean absent(int entityId) {
        return ABSENT.equals(LOOKS.get(entityId));
    }

    /** 점검용: 이 엔티티에 모습이 정해졌는가. */
    public static String lookOfForTest(int entityId) {
        return LOOKS.getOrDefault(entityId, "");
    }

    /** 점검용: 이름표를 그린 횟수 / 멀어서 그리지 않은 횟수. */
    public static long namesDrawnForTest, namesSkippedForTest;

    /**
     * 이 거리(제곱)에서 NPC 의 이름표를 그리는가 (3.64). max = client.json 의 npcNameDistance (칸).
     * 0 이하면 예전처럼 64칸까지 그린다. 이름표는 글자마다 네모를 두 번(벽 너머용 · 보통) 그려서, NPC 가 모인 곳에서는 먼 이름표까지 다 그리면 프레임이 깎인다.
     */
    public static boolean nameShownAt(double distSq, int max) {
        if (distSq > 4096.0) return false;
        return max <= 0 || distSq <= (double) max * max;
    }

    /** 주민 모습으로 그려지는 RT Shop NPC(npcLooks 를 끈 때)의 이름표에도 같은 거리 규칙을 쓴다. */
    @SubscribeEvent
    public static void onVanillaNameTag(net.minecraftforge.client.event.RenderNameTagEvent event) {
        if (LOOKS.isEmpty() || !LOOKS.containsKey(event.getEntity().getId())) return;
        if (absent(event.getEntity().getId())) {   // 자리를 비운 사람의 이름표도 없다
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (!nameShownAt(mc.getEntityRenderDispatcher().distanceToSqr(event.getEntity()), ClientConfig.get().npcNameDistance)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                namesSkippedForTest++;
            }
        } catch (Throwable ignored) {
        }
    }

    /** 머리 위의 이름표 (주민을 그리지 않으니 직접 그린다. 바닐라의 이름표와 같은 모습). */
    private static void nameTag(LivingEntity e, PoseStack ps, MultiBufferSource buf, int light, float height) {
        Minecraft mc = Minecraft.getInstance();
        if (!Minecraft.renderNames()) return;
        boolean show = e.isCustomNameVisible() || (e.hasCustomName() && e == mc.getEntityRenderDispatcher().crosshairPickEntity);
        if (!show || !nameShownAt(mc.getEntityRenderDispatcher().distanceToSqr(e), ClientConfig.get().npcNameDistance)) {
            if (show) namesSkippedForTest++;
            return;
        }
        namesDrawnForTest++;
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
