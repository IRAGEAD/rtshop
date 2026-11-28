/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.Direction
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.model.pipeline.VertexConsumerWrapper
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.Anim;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class ItemPlacer {
    public static final float FLAT_SIZE = 0.205f;
    public static final float BLOCK_SIZE = 0.15f;
    private static final Map<RenderType, RenderType> FADING_TYPES = new IdentityHashMap<RenderType, RenderType>();
    private static final RandomSource RANDOM = RandomSource.m_216327_();

    private ItemPlacer() {
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 base, Direction facing, long seed, float scale, float tilt, int light) {
        ItemPlacer.render(pose, buffers, camera, stack, base, facing, seed, scale, tilt, light, 1.0f);
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 base, Direction facing, long seed, float scale, float tilt, int light, float opacity) {
        ItemPlacer.render(pose, buffers, camera, stack, base, facing, seed, scale, tilt, light, opacity, false);
    }

    public static void renderOne(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 base, Direction facing, long seed, float scale, float tilt, int light) {
        ItemPlacer.render(pose, buffers, camera, stack, base, facing, seed, scale, tilt, light, 1.0f, true);
    }

    private static void render(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 base, Direction facing, long seed, float scale, float tilt, int light, float opacity, boolean one) {
        if (stack.m_41619_() || scale <= 0.001f) {
            return;
        }
        buffers = ItemPlacer.opacity(buffers, opacity);
        Minecraft mc = Minecraft.m_91087_();
        ItemRenderer renderer = mc.m_91291_();
        BakedModel model = renderer.m_174264_(stack, (Level)mc.f_91073_, null, (int)seed);
        boolean block = model.m_7539_();
        int copies = one ? 1 : ItemPlacer.copies(stack.m_41613_(), block);
        RandomSource random = RANDOM;
        random.m_188584_(seed);
        float baseYaw = 180.0f - facing.m_122424_().m_122435_() + (random.m_188501_() - 0.5f) * 16.0f;
        for (int c = 0; c < copies; ++c) {
            float jx = c == 0 ? 0.0f : (random.m_188501_() - 0.5f) * 0.05f;
            float jz = c == 0 ? 0.0f : (random.m_188501_() - 0.5f) * 0.05f;
            float yaw = baseYaw + (c == 0 ? 0.0f : (random.m_188501_() - 0.5f) * 40.0f);
            pose.m_85836_();
            pose.m_85837_(base.f_82479_ - camera.f_82479_ + (double)jx, base.f_82480_ - camera.f_82480_, base.f_82481_ - camera.f_82481_ + (double)jz);
            pose.m_252781_(Axis.f_252436_.m_252977_(yaw));
            if (block) {
                size = 0.15f * scale;
                pose.m_252880_(0.0f, size * 0.5f, 0.0f);
                pose.m_252781_(Axis.f_252436_.m_252977_(180.0f));
                pose.m_252781_(Axis.f_252529_.m_252977_(-tilt * 0.4f));
                pose.m_85841_(size * 2.0f, size * 2.0f, size * 2.0f);
            } else {
                size = 0.205f * scale;
                pose.m_252880_(0.0f, 0.0035f + (float)c * ItemPlacer.flatStep(scale), 0.0f);
                pose.m_252781_(Axis.f_252529_.m_252977_(-90.0f + tilt));
                pose.m_85841_(size, size, size);
            }
            renderer.m_115143_(stack, ItemDisplayContext.FIXED, false, pose, buffers, light, OverlayTexture.f_118083_, model);
            pose.m_85849_();
        }
    }

    static MultiBufferSource opacity(MultiBufferSource buffers, float opacity) {
        if (opacity >= 0.999f) {
            return buffers;
        }
        final float alpha = (float)Anim.clamp01(opacity);
        return type -> new VertexConsumerWrapper(buffers.m_6299_(type.m_110508_() == DefaultVertexFormat.f_85812_ ? FADING_TYPES.computeIfAbsent(type, ItemPlacer::fadingType) : type)){

            public VertexConsumer m_6122_(int r, int g, int b, int a) {
                this.parent.m_6122_(r, g, b, Math.round((float)a * alpha));
                return this;
            }
        };
    }

    private static RenderType fadingType(RenderType original) {
        return new RenderType("cinematic_fading_item", original.m_110508_(), original.m_173186_(), original.m_110507_(), false, true, () -> {
            original.m_110185_();
            if (original.m_110508_() == DefaultVertexFormat.f_85812_) {
                RenderSystem.setShader(GameRenderer::m_172679_);
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask((boolean)false);
        }, () -> {
            original.m_110188_();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.disableBlend();
        }){};
    }

    public static AABB restingBounds(ItemStack stack, Vec3 base) {
        boolean block = ItemPlacer.isBlock(stack);
        double half = block ? 0.09900000393390657 : 0.14554999873042107 + (stack.m_41613_() > 1 ? 0.025 : 0.0);
        double height = block ? (double)0.15f : 0.006 + (double)((float)(ItemPlacer.copies(stack.m_41613_(), false) - 1) * ItemPlacer.flatStep(1.0f));
        return new AABB(base.f_82479_ - half, base.f_82480_, base.f_82481_ - half, base.f_82479_ + half, base.f_82480_ + height, base.f_82481_ + half);
    }

    public static float awayYaw(Direction side) {
        return 180.0f - side.m_122424_().m_122435_();
    }

    public static Quaternionf lying(float yaw, float tilt) {
        return Axis.f_252436_.m_252977_(yaw).mul((Quaternionfc)Axis.f_252529_.m_252977_(-90.0f + tilt));
    }

    public static void drawOriented(PoseStack pose, MultiBufferSource buffers, Vec3 camera, ItemStack stack, Vec3 center, Quaternionf rotation, float size, int light) {
        if (stack.m_41619_() || size <= 0.001f) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        ItemRenderer renderer = mc.m_91291_();
        BakedModel model = renderer.m_174264_(stack, (Level)mc.f_91073_, null, 0);
        pose.m_85836_();
        pose.m_85837_(center.f_82479_ - camera.f_82479_, center.f_82480_ - camera.f_82480_, center.f_82481_ - camera.f_82481_);
        pose.m_252781_(rotation);
        float scale = model.m_7539_() ? size * 2.0f : size;
        pose.m_85841_(scale, scale, scale);
        renderer.m_115143_(stack, ItemDisplayContext.FIXED, false, pose, buffers, light, OverlayTexture.f_118083_, model);
        pose.m_85849_();
    }

    public static boolean isBlock(ItemStack stack) {
        Minecraft mc = Minecraft.m_91087_();
        return mc.m_91291_().m_174264_(stack, (Level)mc.f_91073_, null, 0).m_7539_();
    }

    private static float flatStep(float scale) {
        return 0.205f * scale / 16.0f + 0.0015f;
    }

    private static int copies(int count, boolean block) {
        if (block) {
            return 1;
        }
        return count >= 33 ? 4 : (count >= 17 ? 3 : (count >= 2 ? 2 : 1));
    }
}

