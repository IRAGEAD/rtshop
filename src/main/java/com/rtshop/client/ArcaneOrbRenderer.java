package com.rtshop.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rtshop.RtShop;
import com.rtshop.magic.ArcaneOrbBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 별빛 수정 구체 「별빛 천구」: 받침 위에 떠서 위아래로 흔들리는 푸른 구슬(늘 카메라를 보는 그림)과 빛무리,
 * 그 둘레를 서로 다른 축으로 도는 금·은 고리 세 겹(혼천의의 띠, 두께가 있는 입체), 둘레에서 반짝이는 작은 별빛.
 * 플레이어가 제단을 쓰면 구슬이 밝아지고 고리가 빨라진다.
 */
public class ArcaneOrbRenderer implements BlockEntityRenderer<ArcaneOrbBlockEntity> {
    static final ResourceLocation WHITE = new ResourceLocation(RtShop.MODID, "textures/entity/white.png");
    /** 구슬의 색 (3.64): client.json 의 altarOrbColor. 그림은 tools/gen_magic_art.py 의 ORB_COLORS 가 색마다 한 벌(구슬 · 짙은 빛 · 옅은 빛무리)씩 만든다. */
    public static final java.util.List<String> COLORS = java.util.List.of("blue", "ice", "gold", "violet", "jade", "rose");
    private static final java.util.Map<String, ResourceLocation> TEX = new java.util.HashMap<>();
    /** 점검용: 구슬을 그린 횟수 (받침 위의 구슬 / 장면 속의 구슬). */
    public static long orbDrawnForTest, sceneOrbDrawnForTest;

    public static String color() {
        String c = ClientConfig.get().altarOrbColor;
        return c != null && COLORS.contains(c) ? c : ClientConfig.DEFAULT_ORB;   // 모르는 이름이면 기본 색 (3.64.1: 달빛 은백)
    }

    /** part: "" 구슬 · "_rim" 구슬 둘레의 빛 (더하기 섞기용, 3.64.2). */
    public static ResourceLocation tex(String part) {
        return TEX.computeIfAbsent(color() + part, k -> new ResourceLocation(RtShop.MODID, "textures/entity/orb/" + k + ".png"));
    }

    /** 구슬 둘레 빛의 크기 (구슬 반지름의 배수, 3.64.2). 받침 위에서는 0.20 × 1.7 = 0.34 로 가장 안쪽 고리(0.35) 안에 머문다. */
    public static final float RIM = 1.7F;

    /** 숨 쉬듯 밝아졌다 어두워지는 정도 (0~1). */
    public static float breath(float time) {
        return 0.5F + 0.5F * Mth.sin(time * 0.07F);
    }
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int SEGMENTS = 40;
    /** 고리 색: 금 · 밝은 금 · 은. */
    static final int GOLD = 0xE8BE68, PALE_GOLD = 0xF6D68C, SILVER = 0xC4CEE6;

    public ArcaneOrbRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ArcaneOrbBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        float time = (be.getLevel().getGameTime() % 24000L) + partial;
        float flare = MagicClient.flare(be.getBlockPos());
        float speed = 1F + flare * 5F;
        // Cinematic 장면 동안에는 구슬과 고리가 작게 모인다 (둘레에 룬석과 소환진이 놓일 자리를 낸다)
        float k = MagicClient.sceneK(be.getBlockPos());
        float bob = Mth.sin(time * 0.08F) * 0.04F * (1F - k);
        float shrink = Mth.lerp(k, 1F, 0.40F);
        ps.pushPose();
        ps.translate(0.5, 1.2 + bob, 0.5);
        // 고리 세 겹: 선 금빛 고리(자오환)는 제자리에서 천천히 돌고, 안쪽 둘은 기울어진 채 서로 반대로 돈다
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(time * 0.9F * speed));
        ps.mulPose(Axis.XP.rotationDegrees(90F));
        band(ps, solid, 0.45F * shrink, 0.040F * shrink, 0.022F * shrink, GOLD);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-time * 1.5F * speed));
        ps.mulPose(Axis.ZP.rotationDegrees(32F));
        ps.mulPose(Axis.XP.rotationDegrees(time * 0.7F * speed));
        band(ps, solid, 0.40F * shrink, 0.030F * shrink, 0.018F * shrink, PALE_GOLD);
        ps.popPose();
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(time * 1.2F * speed));
        ps.mulPose(Axis.ZP.rotationDegrees(-48F));
        ps.mulPose(Axis.YP.rotationDegrees(time * 0.5F * speed));
        band(ps, solid, 0.35F * shrink, 0.022F * shrink, 0.014F * shrink, SILVER);
        ps.popPose();
        // 둘레의 작은 별빛 (장면 동안에는 숨긴다)
        // 3.64: 카메라를 보는 그림은 게임 카메라의 방향을 그대로 쓰고 앞뒤 두 면으로 그린다
        // (전에는 그림의 앞면이 카메라 반대쪽을 향해서 구슬 · 빛무리 · 별빛이 하나도 보이지 않았다).
        // 3.64.2: 빛(별빛 · 구슬 둘레)은 더하기 섞기(RenderType.eyes: 깊이를 쓰지 않음)로 — 알파 섞기 빛은 Photon 같은 셰이더에서
        // 탁한 유리처럼 보여 뒤의 고리 · 무늬를 덮었고, 깊이를 써서 뒤에 그려지는 것을 가렸다. 넓게 퍼지던 빛무리는 없애고 빛은 구슬 바로 둘레에만 둔다.
        org.joml.Quaternionf face = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        if (k < 0.5F) {
            VertexConsumer glow = buf.getBuffer(RenderType.eyes(tex("_rim")));
            for (int i = 0; i < 4; i++) {
                float a = time * 0.012F + i * 1.5708F;
                float tw = 0.5F + 0.5F * Mth.sin(time * 0.11F + i * 2.3F);
                ps.pushPose();
                ps.translate(Mth.cos(a) * 0.52F, 0.16F * Mth.sin(time * 0.03F + i * 1.9F) + (i % 2 == 0 ? 0.22F : -0.05F), Mth.sin(a) * 0.52F);
                ps.mulPose(face);
                glowQuad(ps, glow, 0.035F + 0.02F * tw, (0.35F + 0.65F * tw) * (1F - k * 2F));
                ps.popPose();
            }
        }
        // 구슬 (카메라를 본다). Cinematic 장면이 펼쳐지면 장면 쪽(CinematicOrbStation)이 같은 구슬을 그리므로 여기서는 옅어진다.
        float show = 1F - k;
        if (show > 0.02F) {
            ps.pushPose();
            ps.mulPose(face);
            float s = Mth.lerp(k, 0.20F, 0.12F) + flare * 0.03F * (1F - 0.5F * k);
            float br = breath(time);
            // 둘레의 빛(가장 안쪽 은빛 고리 0.35 안쪽까지만) → 그 위에 구슬. 구슬은 빛·그림자를 받지 않는 그림 (어두운 곳이나 셰이더에서 가장자리가 검게 보이지 않게)
            glowQuad(ps, buf.getBuffer(RenderType.eyes(tex("_rim"))), s * RIM, (0.55F + 0.15F * br + flare * 0.30F) * show);
            quad(ps, buf.getBuffer(RenderType.text(tex(""))), s * (1F + 0.03F * br), show);
            ps.popPose();
            orbDrawnForTest++;
        }
        ps.popPose();
    }

    /**
     * 두께가 있는 고리: 가운데를 지나는 Y 축 둘레의 띠 (반지름 r, 띠 너비 w = Y 방향, 두께 th = 안쪽으로).
     * 바깥면·안쪽면·위아래 면을 조각조각 그린다.
     */
    private static void band(PoseStack ps, VertexConsumer vc, float r, float w, float th, int rgb) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float h = w / 2F, ri = r - th;
        int cr = rgb >> 16 & 255, cg = rgb >> 8 & 255, cb = rgb & 255;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = (float) (i * Math.PI * 2 / SEGMENTS), a1 = (float) ((i + 1) * Math.PI * 2 / SEGMENTS);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float nx = Mth.cos((a0 + a1) / 2F), nz = Mth.sin((a0 + a1) / 2F);
            // 바깥면
            v(vc, m, n, r * c0, -h, r * s0, cr, cg, cb, nx, 0, nz);
            v(vc, m, n, r * c1, -h, r * s1, cr, cg, cb, nx, 0, nz);
            v(vc, m, n, r * c1, h, r * s1, cr, cg, cb, nx, 0, nz);
            v(vc, m, n, r * c0, h, r * s0, cr, cg, cb, nx, 0, nz);
            // 안쪽면 (조금 어둡게)
            int dr = cr * 7 / 10, dg = cg * 7 / 10, db = cb * 7 / 10;
            v(vc, m, n, ri * c1, -h, ri * s1, dr, dg, db, -nx, 0, -nz);
            v(vc, m, n, ri * c0, -h, ri * s0, dr, dg, db, -nx, 0, -nz);
            v(vc, m, n, ri * c0, h, ri * s0, dr, dg, db, -nx, 0, -nz);
            v(vc, m, n, ri * c1, h, ri * s1, dr, dg, db, -nx, 0, -nz);
            // 위 · 아래 면
            v(vc, m, n, ri * c0, h, ri * s0, cr, cg, cb, 0, 1, 0);
            v(vc, m, n, r * c0, h, r * s0, cr, cg, cb, 0, 1, 0);
            v(vc, m, n, r * c1, h, r * s1, cr, cg, cb, 0, 1, 0);
            v(vc, m, n, ri * c1, h, ri * s1, cr, cg, cb, 0, 1, 0);
            v(vc, m, n, r * c0, -h, r * s0, cr, cg, cb, 0, -1, 0);
            v(vc, m, n, ri * c0, -h, ri * s0, cr, cg, cb, 0, -1, 0);
            v(vc, m, n, ri * c1, -h, ri * s1, cr, cg, cb, 0, -1, 0);
            v(vc, m, n, r * c1, -h, r * s1, cr, cg, cb, 0, -1, 0);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, int r, int g, int b, float nx, float ny, float nz) {
        vc.vertex(m, x, y, z).color(r, g, b, 255).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
                .normal(n, nx, ny, nz).endVertex();
    }

    private static void quad(PoseStack ps, VertexConsumer vc, float half, float alpha) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int a = Mth.clamp((int) (alpha * 255), 0, 255);
        vertex(vc, m, n, -half, -half, 0, 1, a);
        vertex(vc, m, n, half, -half, 1, 1, a);
        vertex(vc, m, n, half, half, 1, 0, a);
        vertex(vc, m, n, -half, half, 0, 0, a);
        // 뒷면 (같은 네모를 반대 순서로): 어느 쪽에서 보아도 한 면은 앞이다
        vertex(vc, m, n, -half, half, 0, 0, a);
        vertex(vc, m, n, half, half, 1, 0, a);
        vertex(vc, m, n, half, -half, 1, 1, a);
        vertex(vc, m, n, -half, -half, 0, 1, a);
    }

    /**
     * 더하기 섞기로 그리는 빛 한 장 (RenderType.eyes 의 꼭짓점 꼴). 세기는 꼭짓점 빛깔로 — 더하기 섞기는 투명도를 보지 않는다.
     * 앞뒤 두 면 (그 그리기는 뒷면을 그리지 않는다).
     */
    static void glowQuad(PoseStack ps, VertexConsumer vc, float half, float strength) {
        int c = Mth.clamp(Math.round(strength * 255F), 0, 255);
        if (c <= 0) return;
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        float[][] p = {{-half, -half, 0F, 1F}, {half, -half, 1F, 1F}, {half, half, 1F, 0F}, {-half, half, 0F, 0F}};
        for (int[] order : BOTH_WAYS) {
            for (int i : order) {
                vc.vertex(m, p[i][0], p[i][1], 0F).color(c, c, c, 255).uv(p[i][2], p[i][3]).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(FULL_BRIGHT).normal(n, 0F, 0F, 1F).endVertex();
            }
        }
    }

    static final int[][] BOTH_WAYS = {{0, 1, 2, 3}, {3, 2, 1, 0}};

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int a) {
        vc.vertex(m, x, y, 0).color(255, 255, 255, a).uv(u, v).uv2(FULL_BRIGHT).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(ArcaneOrbBlockEntity be) {
        return true;
    }
}
