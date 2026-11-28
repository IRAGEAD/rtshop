/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlock;
import com.finderfeed.fdbosses.content.entities.geburah.respawn_point_setter_block.GeburahRespiteBlockEntity;
import com.finderfeed.fdlib.util.rendering.renderers.QuadRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public class GeburahRespiteBlockEntityRenderer
implements BlockEntityRenderer<GeburahRespiteBlockEntity> {
    public static final ResourceLocation RESOURCE_LOCATION = FDBosses.location("textures/misc/geburah_respawn_point_setter.png");

    public GeburahRespiteBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    public void render(GeburahRespiteBlockEntity tile, float partialTicks, PoseStack matrices, MultiBufferSource src, int light, int overlay) {
        VertexConsumer vertexConsumer = src.m_6299_(RenderType.m_110488_((ResourceLocation)RESOURCE_LOCATION));
        BlockState state = tile.m_58900_();
        Direction direction = (Direction)state.m_61143_((Property)GeburahRespiteBlock.f_54117_);
        float time = (float)tile.m_58904_().m_46467_() + partialTicks;
        matrices.m_85836_();
        matrices.m_85837_(0.5, 0.5, 0.5);
        QuadRenderer.start((VertexConsumer)vertexConsumer).direction(new Vec3((double)(-direction.m_122429_()), (double)(-direction.m_122430_()), (double)(-direction.m_122431_()))).offsetOnDirection(1.0f).rotationDegrees(time).pose(matrices).renderBack().size(1.1f).render();
        QuadRenderer.start((VertexConsumer)vertexConsumer).direction(new Vec3((double)(-direction.m_122429_()), (double)(-direction.m_122430_()), (double)(-direction.m_122431_()))).offsetOnDirection(0.75f).rotationDegrees(-time).pose(matrices).renderBack().size(0.6f).render();
        matrices.m_85849_();
    }
}

