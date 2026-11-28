/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.data_structures.Pair
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer
 *  com.finderfeed.fdlib.systems.bedrock.models.FDModel
 *  com.finderfeed.fdlib.util.math.ComplexEasingFunction
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  com.finderfeed.fdlib.util.rendering.FDEasings
 *  com.finderfeed.fdlib.util.rendering.FDRenderUtil
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector4f
 */
package com.finderfeed.fdbosses.content.entities.geburah;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahLaserAttackPreparator;
import com.finderfeed.fdbosses.content.entities.geburah.sins.attachment.PlayerSin;
import com.finderfeed.fdbosses.init.BossModels;
import com.finderfeed.fdbosses.init.BossRegistries;
import com.finderfeed.fdlib.data_structures.Pair;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.entity.renderer.FDFreeEntityRenderer;
import com.finderfeed.fdlib.systems.bedrock.models.FDModel;
import com.finderfeed.fdlib.util.math.ComplexEasingFunction;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import com.finderfeed.fdlib.util.rendering.FDEasings;
import com.finderfeed.fdlib.util.rendering.FDRenderUtil;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class GeburahRenderer
implements FDFreeEntityRenderer<GeburahEntity> {
    public static final ResourceLocation HALO_EXPLOSION = FDBosses.location("textures/entities/geburah/geburah_halo_explosion.png");
    public static final ResourceLocation HALO = FDBosses.location("textures/entities/geburah/geburah_halo.png");
    public static final ResourceLocation RAY_PREPARE_FRAME = FDBosses.location("textures/entities/geburah/geburah_ray_preparing.png");
    public static final int RAY_ATTACK_PREPARATION_TIME = 30;
    public static final int HAMMER_AMOUNT = 8;
    private static final int IMPACT_TIME = 5;
    private static final int END_PREPARING_SIN_PUNISHMENT = 35;
    private static final ComplexEasingFunction GEBURAH_SIN_PUNISHMENT_EASING = ComplexEasingFunction.builder().addArea(35.0f, FDEasings::linear).addArea(5.0f, FDEasings::linear).build();
    private static FDModel hammerModel;

    public void render(GeburahEntity geburah, float v, float v1, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        if (hammerModel == null) {
            hammerModel = new FDModel(BossModels.JUSTICE_HAMMER.get());
        }
        this.renderLasers(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderSins(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderRayPreparations(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderSinPunishmentAttackEffect(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderStartOperating(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderSecondPhase(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderDeath(geburah, v, v1, poseStack, multiBufferSource, i);
        this.renderRoundAndRoundRayPreparators(geburah, v, v1, poseStack, multiBufferSource, i);
    }

    private void renderRoundAndRoundRayPreparators(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        int tick = geburah.rayAttackPreparationTicker;
        if (tick == -1) {
            return;
        }
        float time = Mth.m_14036_((float)((float)(30 - tick) + pticks), (float)0.0f, (float)30.0f);
        VertexConsumer vertex = src.m_6299_(RenderType.m_110497_((ResourceLocation)RAY_PREPARE_FRAME));
        float frameTime = 10.0f;
        for (Pair<Vec3, Vec3> cannon : geburah.getCannonsPositionAndDirection(pticks)) {
            Vec3 pos = (Vec3)cannon.first;
            Vec3 direction = (Vec3)cannon.second;
            Vec3 offs = pos.m_82546_(geburah.m_20318_(pticks));
            matrices.m_85836_();
            matrices.m_85837_(offs.f_82479_, offs.f_82480_, offs.f_82481_);
            for (int i = 0; i < 6; ++i) {
                int startTime2 = i * 3;
                if (!(time > (float)startTime2) || !(time <= (float)startTime2 + frameTime)) continue;
                float p = FDEasings.easeIn((float)((time - (float)startTime2) / frameTime));
                p *= p;
                QuadRenderer.start((VertexConsumer)vertex).renderBack().offsetOnDirection(-0.25f + p * 0.5f).direction(direction).size(1.0f - p).color(1.0f, 1.0f, 1.0f, p).pose(matrices).render();
            }
            matrices.m_85849_();
        }
    }

    private void renderSecondPhase(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        if (!((Boolean)geburah.m_20088_().m_135370_(GeburahEntity.SECOND_PHASE)).booleanValue()) {
            return;
        }
        float time = (float)geburah.clientSecondPhaseTicker + pticks;
        Vec3 corePos = geburah.getCorePosition().m_82546_(geburah.m_20182_());
        float haloRot = time / 10.0f;
        float haloExplosionTime = 35.0f;
        if (time <= haloExplosionTime) {
            float p = Mth.m_14036_((float)(time / haloExplosionTime), (float)0.0f, (float)1.0f);
            QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110497_((ResourceLocation)HALO_EXPLOSION))).pose(matrices).translate(0.0f, (float)corePos.f_82480_, 0.0f).size(FDEasings.easeOut((float)FDEasings.easeOut((float)p)) * 32.0f).rotationDegrees(FDEasings.easeOut((float)FDEasings.easeOut((float)p)) * 20.0f).color(1.0f, 1.0f, 1.0f, (1.0f - p) * 0.8f).renderBack().render();
            QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110497_((ResourceLocation)HALO))).pose(matrices).direction(new Vec3(0.0, -1.0, 0.0)).translate(0.0f, (float)corePos.f_82480_ + 3.0f * FDEasings.easeOut((float)p), 0.0f).size(FDEasings.easeOut((float)FDEasings.easeOut((float)p)) * 21.0f).rotationDegrees(-haloRot + 20.0f - FDEasings.easeOut((float)p) * 20.0f).color(1.0f, 1.0f, 1.0f, p * 0.75f).renderBack().render();
        } else {
            QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110497_((ResourceLocation)HALO))).pose(matrices).direction(new Vec3(0.0, -1.0, 0.0)).translate(0.0f, (float)corePos.f_82480_ + 3.0f, 0.0f).size(21.0f).rotationDegrees(-haloRot).color(1.0f, 1.0f, 1.0f, 0.75f).renderBack().render();
        }
    }

    private void renderStartOperating(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        if (!((Boolean)geburah.m_20088_().m_135370_(GeburahEntity.OPERATING)).booleanValue()) {
            return;
        }
        float time = (float)geburah.clientOperatingTicks + pticks;
        Vec3 corePos = geburah.getCorePosition().m_82546_(geburah.m_20182_());
        float haloExplosionTime = 35.0f;
        if (time <= haloExplosionTime) {
            float p = Mth.m_14036_((float)(time / haloExplosionTime), (float)0.0f, (float)1.0f);
            QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110497_((ResourceLocation)HALO_EXPLOSION))).pose(matrices).translate(0.0f, (float)corePos.f_82480_, 0.0f).size(FDEasings.easeOut((float)FDEasings.easeOut((float)p)) * 26.0f).rotationDegrees(FDEasings.easeOut((float)FDEasings.easeOut((float)p)) * 20.0f).color(1.0f, 1.0f, 1.0f, (1.0f - p) * 0.8f).renderBack().render();
        }
    }

    private void renderSinPunishmentAttackEffect(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        int tick = geburah.sinPunishmentAttackTicker;
        if (tick == -1) {
            return;
        }
        float time = Mth.m_14036_((float)((float)(40 - tick) + pticks), (float)0.0f, (float)40.0f);
        float maxHammerHeight = 22.0f;
        float hammerScale = 1.0f;
        float hammerHeight = maxHammerHeight;
        float hammerRotation = 360.0f;
        float alpha = 1.0f;
        float localP = GEBURAH_SIN_PUNISHMENT_EASING.apply(time);
        if (time < 35.0f) {
            alpha = localP;
            hammerRotation = 360.0f * FDEasings.easeOut((float)Mth.m_14036_((float)(localP * 1.1f), (float)0.0f, (float)1.0f));
            hammerScale = localP;
        } else {
            hammerHeight = maxHammerHeight * (1.0f - FDEasings.easeIn((float)localP));
        }
        float offset = 18.0f;
        float angle = 45.0f;
        for (int i = 0; i < 8; ++i) {
            matrices.m_85836_();
            matrices.m_252781_(Axis.f_252436_.m_252977_(angle * (float)i));
            matrices.m_252880_(offset, hammerHeight, 0.0f);
            matrices.m_252781_(Axis.f_252436_.m_252977_(hammerRotation + 90.0f));
            matrices.m_252781_(Axis.f_252529_.m_252977_(180.0f));
            matrices.m_85841_(hammerScale, hammerScale, hammerScale);
            hammerModel.render(matrices, src.m_6299_(RenderType.m_110502_()), 0xF000F0, OverlayTexture.f_118083_, 0.3f, 0.7f, 1.0f, alpha * 0.75f);
            matrices.m_85849_();
        }
    }

    private void renderRayPreparations(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        GeburahLaserAttackPreparator attackPreparator = geburah.laserAttackPreparator;
        if (attackPreparator.time != -1) {
            List<Pair<Vec3, Vec3>> cannons = geburah.getCannonsPositionAndDirection(pticks);
            float time = (float)(attackPreparator.time - attackPreparator.currentTime) + pticks;
            float centerOffset = 4.75f;
            float length = 32.0f - centerOffset;
            int fadeIn = 10;
            float stayTime = attackPreparator.time - fadeIn - 10;
            int fadeOut = 10;
            ComplexEasingFunction lengthEasingFunction = ComplexEasingFunction.builder().addArea((float)fadeIn, FDEasings::easeOut).addArea(stayTime + (float)fadeOut, FDEasings::one).build();
            ComplexEasingFunction upperLengthEasingFunction = ComplexEasingFunction.builder().addArea((float)fadeIn + stayTime, FDEasings::linear).addArea((float)fadeOut, FDEasings::one).build();
            ComplexEasingFunction alphaEasingFunction = ComplexEasingFunction.builder().addArea((float)fadeIn, FDEasings::easeOut).addArea(stayTime, FDEasings::one).addArea((float)fadeOut, FDEasings::reversedEaseOut).build();
            VertexConsumer vertex = src.m_6299_(RenderType.m_110502_());
            float l = length * lengthEasingFunction.apply(time);
            float l2 = length * upperLengthEasingFunction.apply(time);
            float alpha = alphaEasingFunction.apply(time);
            float wsize = 0.5f;
            for (Pair<Vec3, Vec3> pair : cannons) {
                matrices.m_85836_();
                Vec3 direction = (Vec3)pair.second;
                matrices.m_85837_(direction.f_82479_ * (double)centerOffset, (double)0.03f, direction.f_82481_ * (double)centerOffset);
                FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)direction);
                Matrix4f mat = matrices.m_85850_().m_252922_();
                vertex.m_252986_(mat, -wsize, l2, 0.0f).m_85950_(0.3f, 0.5f, 1.0f, 0.25f * alpha).m_5752_();
                vertex.m_252986_(mat, -wsize, l, 0.0f).m_85950_(0.3f, 0.5f, 1.0f, 0.25f * alpha).m_5752_();
                vertex.m_252986_(mat, wsize, l, 0.0f).m_85950_(0.3f, 0.5f, 1.0f, 0.25f * alpha).m_5752_();
                vertex.m_252986_(mat, wsize, l2, 0.0f).m_85950_(0.3f, 0.5f, 1.0f, 0.25f * alpha).m_5752_();
                vertex.m_252986_(mat, -wsize, 0.0f, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.5f * alpha).m_5752_();
                vertex.m_252986_(mat, -wsize, l2, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.5f * alpha).m_5752_();
                vertex.m_252986_(mat, wsize, l2, 0.0f).m_85950_(0.3f, 1.0f, 1.0f, 0.5f * alpha).m_5752_();
                vertex.m_252986_(mat, wsize, 0.0f, 0.0f).m_85950_(0.5f, 1.0f, 1.0f, 0.5f * alpha).m_5752_();
                matrices.m_85849_();
            }
        }
    }

    private void renderSins(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        List playerSins = (List)geburah.m_20088_().m_135370_(GeburahEntity.ACTIVE_SINS);
        if (playerSins.isEmpty()) {
            this.renderSinsAtScreen(null, 1, geburah, pticks, matrices, src);
            this.renderSinsAtScreen(null, 2, geburah, pticks, matrices, src);
            this.renderSinsAtScreen(null, 3, geburah, pticks, matrices, src);
            this.renderSinsAtScreen(null, 4, geburah, pticks, matrices, src);
        } else {
            for (int i = 0; i < 4; ++i) {
                int screenId = i + 1;
                this.renderSinsAtScreen(playerSins, screenId, geburah, pticks, matrices, src);
            }
        }
    }

    private void renderSinsAtScreen(List<PlayerSin> sins, int screenId, GeburahEntity geburah, float pticks, PoseStack matrices, MultiBufferSource src) {
        matrices.m_85836_();
        float time = (float)geburah.clientOperatingTicks + pticks;
        float screenStartup = 11.0f;
        float alpha = 0.0f;
        if (((Boolean)geburah.m_20088_().m_135370_(GeburahEntity.OPERATING)).booleanValue()) {
            alpha = time <= screenStartup ? ((float)Math.sin(time / screenStartup * (float)Math.PI * 8.0f) + 1.0f) / 2.0f * 0.75f + 0.25f : 1.0f;
        }
        Matrix4f tv_1 = geburah.getModelPartTransformation("tv_1_" + screenId, GeburahEntity.getClientModel(), pticks);
        matrices.m_252931_(tv_1);
        VertexConsumer vrtx = time <= screenStartup ? src.m_6299_(RenderType.m_110473_((ResourceLocation)FDBosses.location("textures/entities/geburah/screen_sin/base_screen.png"))) : src.m_6299_(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/screen_sin/base_screen.png")));
        QuadRenderer.start((VertexConsumer)vrtx).pose(matrices).sizeY(0.562f).sizeX(1.185f).light(0xF000F0).offsetOnDirection(0.01f).color(1.0f, 1.0f, 1.0f, alpha).direction(new Vec3(-1.0, 0.0, 0.0)).render();
        if (sins != null) {
            float sinSize = 0.5f;
            int size = sins.size();
            float startOffset = 0.0f;
            startOffset = size % 2 == 0 ? sinSize + ((float)size / 2.0f - 1.0f) * sinSize * 2.0f : (float)(size / 2) * sinSize;
            for (int i = 0; i < size; ++i) {
                PlayerSin sin = sins.get(i);
                ResourceLocation key = BossRegistries.PLAYER_SINS.get().getKey((Object)sin);
                String idStart = key.m_135815_();
                QuadRenderer.start((VertexConsumer)src.m_6299_(RenderType.m_110452_((ResourceLocation)FDBosses.location("textures/entities/geburah/screen_sin/sin_" + idStart + ".png")))).pose(matrices).size(sinSize).light(0xF000F0).offsetOnDirection(0.011f).translate(0.0f, 0.0f, -startOffset + sinSize * (float)i * 2.0f).color(1.0f, 1.0f, 1.0f, 1.0f).direction(new Vec3(-1.0, 0.0, 0.0)).render();
            }
        }
        matrices.m_85849_();
    }

    private void renderLasers(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        float alpha = geburah.getLaserVisualAlpha(pticks);
        if (alpha == 0.0f) {
            return;
        }
        Camera camera = Minecraft.m_91087_().f_91063_.m_109153_();
        VertexConsumer vertex = src.m_6299_(RenderType.m_110502_());
        matrices.m_85836_();
        float r = 0.3f;
        float g = 0.7f;
        float b = 1.0f;
        for (Pair<Vec3, Vec3> pair : geburah.getCannonsPositionAndDirection(pticks)) {
            matrices.m_85836_();
            Vec3 dir = (Vec3)pair.second;
            Vec3 pos = (Vec3)pair.first;
            Vec3 posEnd = pos.m_82549_(dir.m_82490_(27.0));
            ClipContext clipContext = new ClipContext(pos, posEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null);
            BlockHitResult res = geburah.m_9236_().m_45547_(clipContext);
            Vec3 location = res.m_82450_();
            double len = location.m_82554_(pos);
            Vec3 offsetFromOrigin = pos.m_82546_(geburah.m_20182_());
            matrices.m_85837_(offsetFromOrigin.f_82479_, offsetFromOrigin.f_82480_, offsetFromOrigin.f_82481_);
            FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)dir);
            Vec3 n = FDMathUtil.getNormalVectorFromLineToPoint((Vec3)pos, (Vec3)pos.m_82549_(dir.m_82542_(len, len, len)), (Vec3)camera.m_90583_());
            Matrix4f mt2 = new Matrix4f();
            FDRenderUtil.applyMovementMatrixRotations((Matrix4f)mt2, (Vec3)dir);
            Vector4f up = new Vector4f(0.0f, 0.0f, 1.0f, 1.0f);
            mt2.transform(up);
            Vector4f left = new Vector4f(1.0f, 0.0f, 0.0f, 1.0f);
            mt2.transform(left);
            Vec3 vup = new Vec3((double)(up.x / up.w), (double)(up.y / up.w), (double)(up.z / up.w));
            Vec3 vleft = new Vec3((double)left.x, (double)left.y, (double)left.z);
            float betweenVectors = (float)FDMathUtil.angleBetweenVectors((Vec3)n, (Vec3)vup);
            if (vleft.m_82526_(n) > 0.0) {
                matrices.m_252781_(Axis.f_252436_.m_252961_(betweenVectors));
            } else {
                matrices.m_252781_(Axis.f_252436_.m_252961_(-betweenVectors));
            }
            float time = ((float)geburah.f_19797_ + pticks) * 50.0f;
            float p = (float)(Math.sin(time) + 1.0) / 2.0f;
            p = p * 0.4f + 0.6f;
            float w = 0.3f * p;
            Matrix4f mat = matrices.m_85850_().m_252922_();
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(r, g, b, alpha).m_5752_();
            vertex.m_252986_(mat, w, 0.0f, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(mat, w, (float)len, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(r, g, b, alpha).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(r, g, b, alpha).m_5752_();
            vertex.m_252986_(mat, -w, (float)len, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(mat, -w, 0.0f, 0.0f).m_85950_(r, g, b, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(r, g, b, alpha).m_5752_();
            matrices.m_252880_(0.0f, 0.0f, 0.005f);
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
            vertex.m_252986_(mat, w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
            vertex.m_252986_(mat, w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
            vertex.m_252986_(mat, 0.0f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
            vertex.m_252986_(mat, -w * 0.15f, (float)len, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
            vertex.m_252986_(mat, -w * 0.15f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, 0.0f).m_5752_();
            vertex.m_252986_(mat, 0.0f, 0.0f, 0.0f).m_85950_(1.0f, 1.0f, 1.0f, alpha).m_5752_();
            matrices.m_85849_();
        }
        matrices.m_85849_();
    }

    private void renderDeath(GeburahEntity geburah, float yaw, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        int i;
        if (!geburah.m_21224_()) {
            return;
        }
        int tick = geburah.f_20919_;
        int c = 8;
        int c2 = 4;
        float angle = (float)Math.PI * 2 / (float)c;
        float angle2 = (float)Math.PI * 2 / (float)c2;
        for (i = 0; i < c; ++i) {
            this.renderDeathHammer(tick, i * 10, 130, 60, angle * (float)i + angle / 2.0f, geburah, 3.0f, pticks, matrices, src, light);
        }
        for (i = 0; i < c2; ++i) {
            this.renderDeathHammer(tick, i * 20, 130, 60, angle2 * (float)i + angle2 / 2.0f, geburah, 13.0f, pticks, matrices, src, light);
        }
    }

    private void renderDeathHammer(int currentTick, int appearTick, int strikeTick, int hammerPrepareTime, float angle, GeburahEntity geburah, float originOffset, float pticks, PoseStack matrices, MultiBufferSource src, int light) {
        float tick = Mth.m_14036_((float)((float)(currentTick - appearTick) + pticks), (float)0.0f, (float)2.1474836E9f);
        Vec3 v = new Vec3(1.0, 0.0, 0.0).m_82524_(angle);
        ComplexEasingFunction hammerEasing = ComplexEasingFunction.builder().addArea((float)hammerPrepareTime, FDEasings::easeOut).addArea((float)(strikeTick - hammerPrepareTime - appearTick), FDEasings::one).addArea(7.0f, FDEasings::reversedEaseInOut).build();
        ComplexEasingFunction hammerAlpha = ComplexEasingFunction.builder().addArea((float)hammerPrepareTime, FDEasings::easeIn).build();
        float s = (float)Math.sin(tick / 10.0f) * 0.25f;
        matrices.m_85836_();
        matrices.m_252880_(0.0f, originOffset, 0.0f);
        Vec3 dir = v.m_82520_(0.0, -0.5, 0.0);
        FDRenderUtil.applyMovementMatrixRotations((PoseStack)matrices, (Vec3)dir);
        float offset = hammerEasing.apply(tick);
        matrices.m_252880_(0.0f, -offset * 20.0f + s, 0.0f);
        matrices.m_252781_(Axis.f_252436_.m_252977_(offset * 360.0f));
        matrices.m_85841_(0.75f, 0.75f, 0.75f);
        float alpha = hammerAlpha.apply(tick);
        hammerModel.render(matrices, src.m_6299_(RenderType.m_110502_()), 0xF000F0, OverlayTexture.f_118083_, 0.3f, 0.7f, 1.0f, alpha * 0.75f);
        matrices.m_85849_();
    }
}

