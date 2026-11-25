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
    static final ResourceLocation ORB = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_orb.png");
    static final ResourceLocation GLOW = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_glow.png");
    static final ResourceLocation WHITE = new ResourceLocation(RtShop.MODID, "textures/entity/white.png");
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
        if (k < 0.5F) {
            VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucent(GLOW));
            for (int i = 0; i < 4; i++) {
                float a = time * 0.012F + i * 1.5708F;
                float tw = 0.5F + 0.5F * Mth.sin(time * 0.11F + i * 2.3F);
                ps.pushPose();
                ps.translate(Mth.cos(a) * 0.52F, 0.16F * Mth.sin(time * 0.03F + i * 1.9F) + (i % 2 == 0 ? 0.22F : -0.05F), Mth.sin(a) * 0.52F);
                ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
                quad(ps, glow, 0.035F + 0.02F * tw, (0.35F + 0.65F * tw) * (1F - k * 2F));
                ps.popPose();
            }
        }
        // 구슬 (카메라를 본다)
        ps.pushPose();
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        float s = Mth.lerp(k, 0.20F, 0.12F) + flare * 0.03F * (1F - 0.5F * k);
        quad(ps, buf.getBuffer(RenderType.entityTranslucent(GLOW)), s * 2.8F, 0.55F + flare * 0.45F);
        ps.translate(0, 0, 0.01);
        quad(ps, buf.getBuffer(RenderType.entityTranslucent(ORB)), s, 1F);
        ps.popPose();
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
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int a) {
        vc.vertex(m, x, y, 0).color(255, 255, 255, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
                .normal(n, 0, 0, 1).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(ArcaneOrbBlockEntity be) {
        return true;
    }
}
