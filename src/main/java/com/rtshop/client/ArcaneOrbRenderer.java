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
 * 별빛 수정 구체: 받침 위에 떠서 위아래로 흔들리는 자수정 구슬(늘 카메라를 보는 그림)과 빛무리,
 * 서로 반대로 도는 룬 고리 두 개. 플레이어가 구체를 쓰면 구슬이 밝아지고 고리가 빨라진다.
 */
public class ArcaneOrbRenderer implements BlockEntityRenderer<ArcaneOrbBlockEntity> {
    static final ResourceLocation ORB = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_orb.png");
    static final ResourceLocation GLOW = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_glow.png");
    static final ResourceLocation RING_OUT = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_ring_outer.png");
    static final ResourceLocation RING_IN = new ResourceLocation(RtShop.MODID, "textures/entity/arcane_ring_inner.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public ArcaneOrbRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ArcaneOrbBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) return;
        float time = (be.getLevel().getGameTime() % 24000L) + partial;
        float flare = MagicClient.flare(be.getBlockPos());
        float speed = 1F + flare * 5F;
        float bob = Mth.sin(time * 0.08F) * 0.05F;
        ps.pushPose();
        ps.translate(0.5, 1.2 + bob, 0.5);
        // 고리 (수평으로 눕혀 서로 반대로 돈다, 조금 기울임)
        ring(ps, buf, RING_OUT, 0.95F, time * 1.2F * speed, 12F, 0.9F + flare * 0.1F);
        ring(ps, buf, RING_IN, 0.62F, -time * 2.1F * speed, -18F, 0.85F + flare * 0.15F);
        // 구슬 (카메라를 본다)
        ps.pushPose();
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        float s = 0.34F + flare * 0.04F;
        quad(ps, buf.getBuffer(RenderType.entityTranslucent(GLOW)), s * 2.6F, 0.55F + flare * 0.45F);
        ps.translate(0, 0, 0.01);
        quad(ps, buf.getBuffer(RenderType.entityTranslucent(ORB)), s, 1F);
        ps.popPose();
        ps.popPose();
    }

    private static void ring(PoseStack ps, MultiBufferSource buf, ResourceLocation tex, float size, float rotDeg, float tilt, float alpha) {
        ps.pushPose();
        ps.mulPose(Axis.XP.rotationDegrees(90F + tilt));
        ps.mulPose(Axis.ZP.rotationDegrees(rotDeg));
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(tex));
        quad(ps, vc, size, alpha);
        // 뒷면도 보이게
        ps.mulPose(Axis.YP.rotationDegrees(180F));
        quad(ps, vc, size, alpha);
        ps.popPose();
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
