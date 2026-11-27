package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtshop.RtShop;
import com.rtshop.enhance.EnhanceAnvilBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 강화 모루의 빛: 받침에 새긴 룬 띠와 모루 옆면의 작은 보석은 평소에 꺼져 있다가, 사람이 다가오면 천천히 켜지고 멀어지면 천천히 꺼진다.
 * 블록 상태를 바꾸지 않고 이 클라이언트에서만 그 위에 빛을 덧그린다 (서버 부담 없음, 이미 놓인 모루에도 그대로 적용).
 * 빛의 그림: textures/block/enh_glow_side.png(룬), enh_glow_anvil.png(보석) — tools/gen_enhance_anvil.py 가 모델과 같은 자리에 그린다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class EnhanceAnvilGlow {
    private EnhanceAnvilGlow() {
    }

    private static final ResourceLocation SIDE = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_side.png");
    private static final ResourceLocation GEM = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_anvil.png");
    /** 3.64: 빛을 한 겹 더 — 거미 눈처럼 스스로 빛나는 그리기(더하기 섞기)로 덧그린다. 셰이더는 이 그리기를 "빛나는 것"으로 다뤄 번지는 빛을 준다. */
    private static final ResourceLocation SIDE_ADD = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_side_add.png");
    private static final ResourceLocation GEM_ADD = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_anvil_add.png");
    /**
     * 3.75 (W-16, 사용자: Photon 에서 룬과 보석의 불빛이 부자연스럽게 번지고 뚝뚝 끊기며 켜지고 꺼진다): 셰이더용 그림 — 룬 · 보석의 획만.
     * 셰이더가 켜져 있으면 이 한 겹만 그린다. 까닭 (Oculus 1.8 + Photon, 마인크래프트 1.20.1 에서 확인):
     * 둘째 겹(RenderType.eyes)은 셰이더의 "스스로 빛나는 불투명 단계"로 가서 투명도로 흐려지지 못한다 — 켜지는 첫 순간 룬이 까맣게 칠해졌다가
     * 밝기의 세제곱으로 갑자기 환해지고, 꺼질 때도 까만 룬에서 원래 돌로 툭 돌아왔다. 옅은 둘레(알파 55~120)도 불투명한 갈색 테두리가 되어 크게 번졌다.
     * 첫째 겹(RenderType.text)은 셰이더의 반투명 단계(gbuffers_entities_translucent)라 투명도로 부드럽게 섞인다 — 다만 투명도 0.1 아래는 버려지므로
     * 투명도는 0.12 아래로 내리지 않고 그 아래의 어두움은 빛깔(꼭짓점 색)로 낮춘다 ({@link #shaderColor}).
     */
    private static final ResourceLocation SIDE_SH = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_side_sh.png");
    private static final ResourceLocation GEM_SH = new ResourceLocation(RtShop.MODID, "textures/block/enh_glow_anvil_sh.png");
    /** 셰이더에서 쓰는 가장 낮은 투명도 (셰이더는 0.1 아래를 버린다). */
    static final float SH_MIN_ALPHA = 0.12F;
    /** 점검용: 빛을 그린 횟수 (모루 하나에 한 번). */
    public static long drawnForTest;
    /** 점검용: 둘째 겹(스스로 빛나는 그리기)을 그린 횟수 — 셰이더가 켜져 있으면 늘지 않아야 한다. */
    public static long addDrawnForTest;
    /** 점검용: 셰이더용 그림으로 그린 횟수. */
    public static long shaderDrawnForTest;
    /** 점검용: 밝기를 이 값(0~1)에 묶어 두고 그린다 (null 이면 평소대로). */
    public static volatile Float holdForTest;
    /** 이 거리 안으로 오면 켜진다. */
    public static final double NEAR = 5.0;
    private static final float UP = 1F / 40F, DOWN = 1F / 70F;
    private static final int SCAN = 7;

    /** 모루 자리 -> {지금 밝기, 앞 틱의 밝기} (0~1). */
    private static final Map<BlockPos, float[]> GLOW = new HashMap<>();
    private static int tick;

    public static float levelForTest(BlockPos pos) {
        float[] g = GLOW.get(pos);
        return g == null ? -1F : g[0];
    }

    public static int countForTest() {
        return GLOW.size();
    }

    /** 지금 가장 밝은 모루의 밝기 (없으면 -1). */
    public static float brightestForTest() {
        float best = -1F;
        for (float[] g : GLOW.values()) best = Math.max(best, g[0]);
        return best;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            GLOW.clear();
            return;
        }
        if (mc.isPaused()) return;
        if (tick++ % 10 == 0) {
            BlockPos c = mc.player.blockPosition();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int x = -SCAN; x <= SCAN; x++) {
                for (int y = -4; y <= 4; y++) {
                    for (int z = -SCAN; z <= SCAN; z++) {
                        m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                        if (mc.level.getBlockState(m).getBlock() instanceof EnhanceAnvilBlock && !GLOW.containsKey(m)) {
                            GLOW.put(m.immutable(), new float[]{0F, 0F});
                        }
                    }
                }
            }
        }
        Vec3 eye = mc.player.position();
        for (Iterator<Map.Entry<BlockPos, float[]>> it = GLOW.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, float[]> e = it.next();
            BlockPos p = e.getKey();
            float[] g = e.getValue();
            double d2 = eye.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            if (!(mc.level.getBlockState(p).getBlock() instanceof EnhanceAnvilBlock) || (g[0] <= 0F && d2 > 14 * 14)) {
                it.remove();
                continue;
            }
            g[1] = g[0];
            g[0] = d2 <= NEAR * NEAR ? Math.min(1F, g[0] + UP) : Math.max(0F, g[0] - DOWN);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        // 3.64.2: AFTER_TRANSLUCENT_BLOCKS 대신 AFTER_ENTITIES — Embeddium + Oculus 에서 셰이더(Photon 등)를 켜면 반투명 지형 단계에서 그린 것이
        // 보이지 않았다 (사용자: Photon 에서 강화 모루의 빛이 안 들어옴). Cinematic Interactions 의 장면(마법진 · 룬석)이 그리는 단계라 셰이더에서도 보이는 것이 확인된 곳이다.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || GLOW.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Vec3 cp = event.getCamera().getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float time = (mc.level.getGameTime() % 24000L) + event.getPartialTick();
        boolean shader = ShaderState.on();
        Float hold = holdForTest;
        boolean any = false;
        for (Map.Entry<BlockPos, float[]> e : GLOW.entrySet()) {
            float lv = hold != null ? hold : Mth.lerp(event.getPartialTick(), e.getValue()[1], e.getValue()[0]);
            if (lv <= 0.01F) continue;
            BlockPos p = e.getKey();
            BlockState st = mc.level.getBlockState(p);
            if (!(st.getBlock() instanceof EnhanceAnvilBlock)) continue;
            // 부드럽게 켜지고(느리게 시작해 느리게 끝남), 다 켜진 뒤에는 숨 쉬듯 조금 일렁인다
            float ease = lv * lv * (3F - 2F * lv);
            ps.pushPose();
            ps.translate(p.getX() - cp.x, p.getY() - cp.y, p.getZ() - cp.z);
            Matrix4f m = ps.last().pose();
            if (shader) {
                // 셰이더: 일렁임 없이 한 겹만. 빛은 셰이더가 비춘다 — 블록 빛 15, 하늘빛은 그 자리의 것 (동굴 속에서 하늘빛을 받지 않게).
                // 3.76.0: 드러나는 빛 = ease² — 셰이더의 HDR 화면에서는 빛의 양이 조금만 있어도 환해 보여서, 20% 에서 이미 다 켜진 듯했다 (3.75 의 사진).
                drawShader(buf, m, st, ease * ease, LightTexture.pack(15, skyAround(mc, p)));
                ps.popPose();
                drawnForTest++;
                shaderDrawnForTest++;
                any = true;
                continue;
            }
            int a = Mth.clamp(Math.round(255 * ease * (0.86F + 0.14F * Mth.sin(time * 0.07F))), 0, 255);
            VertexConsumer side = buf.getBuffer(RenderType.text(SIDE));
            float e0 = 0.5F / 16F - 0.004F, e1 = 15.5F / 16F + 0.004F, lo = 2F / 16F, hi = 14F / 16F, top = 3F / 16F;
            // 받침 아랫단의 네 옆면 (가운데 판). u 는 밖에서 볼 때 오른쪽으로 늘어난다 — 모델의 면 UV 와 같은 규칙.
            quad(side, m, lo, 0, e0, hi, top, e0, 1 - lo, 1 - hi, a);        // 북
            quad(side, m, hi, 0, e1, lo, top, e1, hi, lo, a);                // 남
            quad(side, m, e0, 0, hi, e0, top, lo, hi, lo, a);                // 서
            quad(side, m, e1, 0, lo, e1, top, hi, 1 - lo, 1 - hi, a);        // 동
            // 모루 옆면(긴 쪽) 가운데의 보석
            VertexConsumer gem = buf.getBuffer(RenderType.text(GEM));
            float g0 = 6.5F / 16F, g1 = 9.5F / 16F, y0 = 12F / 16F, y1 = 14.75F / 16F, n = 3F / 16F - 0.004F, f = 13F / 16F + 0.004F;
            if (EnhanceAnvilBlock.alongZ(st)) {
                gemQuad(gem, m, n, y0, g1, n, y1, g0, g1, g0, a);
                gemQuad(gem, m, f, y0, g0, f, y1, g1, g0, g1, a);
            } else {
                gemQuad(gem, m, g0, y0, n, g1, y1, n, g0, g1, a);
                gemQuad(gem, m, g1, y0, f, g0, y1, f, g1, g0, a);
            }
            // 둘째 겹: 스스로 빛나는 그리기. 같은 자리에서 조금 더 바깥에 (먼저 그린 겹과 겹쳐 깜빡이지 않게).
            int c = Mth.clamp(Math.round(a * 0.75F), 0, 255);
            VertexConsumer sideAdd = buf.getBuffer(RenderType.eyes(SIDE_ADD));
            float d0 = e0 - 0.002F, d1 = e1 + 0.002F;
            lit(sideAdd, m, lo, 0, d0, hi, top, d0, 1 - lo, 1 - hi, c, 0, 0, -1);
            lit(sideAdd, m, hi, 0, d1, lo, top, d1, hi, lo, c, 0, 0, 1);
            lit(sideAdd, m, d0, 0, hi, d0, top, lo, hi, lo, c, -1, 0, 0);
            lit(sideAdd, m, d1, 0, lo, d1, top, hi, 1 - lo, 1 - hi, c, 1, 0, 0);
            VertexConsumer gemAdd = buf.getBuffer(RenderType.eyes(GEM_ADD));
            float n2 = n - 0.002F, f2 = f + 0.002F;
            if (EnhanceAnvilBlock.alongZ(st)) {
                lit(gemAdd, m, n2, y0, g1, n2, y1, g0, g1, g0, c, -1, 0, 0);
                lit(gemAdd, m, f2, y0, g0, f2, y1, g1, g0, g1, c, 1, 0, 0);
            } else {
                lit(gemAdd, m, g0, y0, n2, g1, y1, n2, g0, g1, c, 0, 0, -1);
                lit(gemAdd, m, g1, y0, f2, g0, y1, f2, g1, g0, c, 0, 0, 1);
            }
            ps.popPose();
            drawnForTest++;
            addDrawnForTest++;
            any = true;
        }
        if (any) buf.endBatch();
    }

    /**
     * 셰이더용 빛깔: 켜진 정도 g(0~1) → {투명도, 꼭짓점 밝기} (0~255).
     * 셰이더의 반투명 단계는 (그림 빛깔 × 꼭짓점 색)을 선형 빛으로 바꿔(약 2.2 제곱) 비춘 뒤 투명도를 곱해 섞는다.
     * 그래서 드러나는 빛의 양은 투명도 × 밝기^2.2 이다 — 이것이 g 와 같아지게 하고, 투명도는 0.12 아래로 내리지 않는다
     * (0.1 아래는 셰이더가 버려서, 켜지는 첫 순간 빛이 10% 로 툭 나타났다).
     */
    static int[] shaderColor(float g) {
        g = Mth.clamp(g, 0F, 1F);
        float alpha = Math.max(g, SH_MIN_ALPHA);
        float tint = (float) Math.pow(g / alpha, 1.0 / 2.2);
        return new int[]{Mth.clamp(Math.round(255 * alpha), 0, 255), Mth.clamp(Math.round(255 * tint), 0, 255)};
    }

    /** 셰이더 위에서 드러나는 빛의 양 (투명도 × 밝기^2.2, 0~1) — 점검용. */
    static float shaderShownForTest(float g) {
        int[] c = shaderColor(g);
        return (c[0] / 255F) * (float) Math.pow(c[1] / 255F, 2.2);
    }

    private static void drawShader(MultiBufferSource.BufferSource buf, Matrix4f m, BlockState st, float g, int light) {
        int[] c = shaderColor(g);
        int a = c[0], t = c[1];
        VertexConsumer side = buf.getBuffer(RenderType.text(SIDE_SH));
        float e0 = 0.5F / 16F - 0.004F, e1 = 15.5F / 16F + 0.004F, lo = 2F / 16F, hi = 14F / 16F, top = 3F / 16F;
        quad(side, m, lo, 0, e0, hi, top, e0, 1 - lo, 1 - hi, t, a, light);        // 북
        quad(side, m, hi, 0, e1, lo, top, e1, hi, lo, t, a, light);                // 남
        quad(side, m, e0, 0, hi, e0, top, lo, hi, lo, t, a, light);                // 서
        quad(side, m, e1, 0, lo, e1, top, hi, 1 - lo, 1 - hi, t, a, light);        // 동
        VertexConsumer gem = buf.getBuffer(RenderType.text(GEM_SH));
        float g0 = 6.5F / 16F, g1 = 9.5F / 16F, y0 = 12F / 16F, y1 = 14.75F / 16F, n = 3F / 16F - 0.004F, f = 13F / 16F + 0.004F;
        if (EnhanceAnvilBlock.alongZ(st)) {
            quad(gem, m, n, y0, g1, n, y1, g0, g1, g0, t, a, light);
            quad(gem, m, f, y0, g0, f, y1, g1, g0, g1, t, a, light);
        } else {
            quad(gem, m, g0, y0, n, g1, y1, n, g0, g1, t, a, light);
            quad(gem, m, g1, y0, f, g0, y1, f, g1, g0, t, a, light);
        }
    }

    /** 모루 둘레(네 옆과 위)에서 가장 밝은 하늘빛 (0~15). */
    private static int skyAround(Minecraft mc, BlockPos p) {
        int best = 0;
        for (BlockPos q : new BlockPos[]{p.north(), p.south(), p.west(), p.east(), p.above()}) {
            best = Math.max(best, mc.level.getBrightness(LightLayer.SKY, q));
        }
        return best;
    }

    /** 세로로 선 네모 하나 (아래 왼쪽 → 위 오른쪽; 앞뒤 어느 쪽에서도 보이게 두 번). 세로 v: 아래 = 1, 위 = 1 - 높이. */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float u1, int a) {
        quad(vc, m, x0, y0, z0, x1, y1, z1, u0, u1, 255, a, 0xF000F0);
    }

    /** t = 꼭짓점 밝기(0~255), light = 빛 칸 (LightTexture.pack). */
    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float u1, int t, int a, int light) {
        float v0 = 1F - y0, v1 = 1F - y1;
        put(vc, m, x0, y0, z0, u0, v0, t, a, light);
        put(vc, m, x1, y0, z1, u1, v0, t, a, light);
        put(vc, m, x1, y1, z1, u1, v1, t, a, light);
        put(vc, m, x0, y1, z0, u0, v1, t, a, light);
        put(vc, m, x0, y1, z0, u0, v1, t, a, light);
        put(vc, m, x1, y1, z1, u1, v1, t, a, light);
        put(vc, m, x1, y0, z1, u1, v0, t, a, light);
        put(vc, m, x0, y0, z0, u0, v0, t, a, light);
    }

    /** 스스로 빛나는 네모 (앞뒤 두 번). 밝기는 빛깔로 조절한다 — 더하기 섞기는 투명도를 보지 않는다. */
    private static void lit(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float u1, int c, float nx, float ny, float nz) {
        float v0 = 1F - y0, v1 = 1F - y1;
        litPut(vc, m, x0, y0, z0, u0, v0, c, nx, ny, nz);
        litPut(vc, m, x1, y0, z1, u1, v0, c, nx, ny, nz);
        litPut(vc, m, x1, y1, z1, u1, v1, c, nx, ny, nz);
        litPut(vc, m, x0, y1, z0, u0, v1, c, nx, ny, nz);
        litPut(vc, m, x0, y1, z0, u0, v1, c, nx, ny, nz);
        litPut(vc, m, x1, y1, z1, u1, v1, c, nx, ny, nz);
        litPut(vc, m, x1, y0, z1, u1, v0, c, nx, ny, nz);
        litPut(vc, m, x0, y0, z0, u0, v0, c, nx, ny, nz);
    }

    private static void litPut(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int c, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(c, c, c, 255).uv(u, v).overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(nx, ny, nz).endVertex();
    }

    private static void gemQuad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float u1, int a) {
        quad(vc, m, x0, y0, z0, x1, y1, z1, u0, u1, a);
    }

    private static void put(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int t, int a, int light) {
        vc.vertex(m, x, y, z).color(t, t, t, a).uv(u, v).uv2(light).endVertex();
    }
}
