/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.BlockModelShaper
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BrewingStandBlock
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.client.ChunkRenderTypeSet
 *  net.minecraftforge.client.event.ModelEvent$ModifyBakingResult
 *  net.minecraftforge.client.model.BakedModelWrapper
 *  net.minecraftforge.client.model.data.ModelData
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.BrewingStation;
import dev.dyslexic.cinematic.client.ClientBrewing;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Scene;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

public final class BrewingBottles
implements BlockEntityRenderer<BrewingStandBlockEntity> {
    static final float SIZE = 0.42f;

    public BrewingBottles(BlockEntityRendererProvider.Context context) {
    }

    public static void hideVanillaBottles(ModelEvent.ModifyBakingResult event) {
        Map models = event.getModels();
        IdentityHashMap<BakedModel, BakedModel> wrapped = new IdentityHashMap<BakedModel, BakedModel>();
        for (BlockState state : Blocks.f_50255_.m_49965_().m_61056_()) {
            ModelResourceLocation key = BlockModelShaper.m_110895_((BlockState)state);
            BakedModel model = (BakedModel)models.get(key);
            if (model == null) continue;
            models.put(key, wrapped.computeIfAbsent(model, NoBottles::new));
        }
    }

    static BlockState withoutBottles(@Nullable BlockState state) {
        if (state == null || !state.m_60713_(Blocks.f_50255_)) {
            return state;
        }
        for (BooleanProperty bottle : BrewingStandBlock.f_50905_) {
            state = (BlockState)state.m_61124_((Property)bottle, (Comparable)Boolean.valueOf(false));
        }
        return state;
    }

    public void render(BrewingStandBlockEntity stand, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!ClientOnly.serverHasMod()) {
            return;
        }
        BlockPos pos = stand.m_58899_();
        Vec3 cam = Minecraft.m_91087_().f_91063_.m_109153_().m_90583_();
        Vec3 origin = Vec3.m_82528_((Vec3i)pos);
        BrewingStation scene = Scene.station(BrewingStation.class);
        boolean mine = scene != null && scene.pos.equals((Object)pos) && scene.menu() != null;
        for (int i = 0; i < 3; ++i) {
            ItemStack stack;
            ItemStack itemStack = stack = mine ? scene.slot(i) : ClientBrewing.item(stand, i);
            if (stack.m_41619_()) continue;
            float grow = mine && scene.hoverSlot == i ? 1.08f : 1.0f;
            BrewingBottles.draw(pose, buffers, origin, cam, stack, BrewingStation.slotPoint(pos, i), grow, BrewingStation.itemScale(i), BrewingStation.itemRotation(i), light);
        }
        if (!mine) {
            ItemStack ingredient = ClientBrewing.item(stand, 3);
            ItemStack fuel = ClientBrewing.item(stand, 4);
            if (!ingredient.m_41619_() || !fuel.m_41619_()) {
                pose.m_85836_();
                pose.m_85837_(-origin.f_82479_ + cam.f_82479_, -origin.f_82480_ + cam.f_82480_, -origin.f_82481_ + cam.f_82481_);
                Direction towards = Direction.m_122366_((double)(cam.f_82479_ - ((double)pos.m_123341_() + 0.5)), (double)0.0, (double)(cam.f_82481_ - ((double)pos.m_123343_() + 0.5)));
                BrewingStation.drawLoose(pose, buffers, cam, pos, towards, ingredient, fuel, light);
                pose.m_85849_();
            }
        }
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 origin, Vec3 cam, ItemStack stack, Vec3 at, float grow, float scale, float rotation, int light) {
        Vec3 toEye = new Vec3(cam.f_82479_ - at.f_82479_, 0.0, cam.f_82481_ - at.f_82481_);
        float yaw = (float)(180.0 - Math.toDegrees(Math.atan2(-toEye.f_82479_, toEye.f_82481_)));
        Vec3 center = at.m_82520_(0.0, 0.18374999426305294 * (double)scale * (double)(grow - 1.0f), 0.0);
        ItemPlacer.drawOriented(pose, buffers, origin, stack, center, ItemPlacer.lying(yaw + rotation, 90.0f), 0.42f * grow * scale, light);
    }

    public boolean shouldRenderOffScreen(BrewingStandBlockEntity stand) {
        return false;
    }

    private static final class NoBottles
    extends BakedModelWrapper<BakedModel> {
        NoBottles(BakedModel model) {
            super(model);
        }

        public List<BakedQuad> m_213637_(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return this.originalModel.m_213637_(ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state, side, rand);
        }

        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            return this.originalModel.getQuads(ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state, side, rand, data, renderType);
        }

        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return this.originalModel.getRenderTypes(ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state, rand, data);
        }

        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            return this.originalModel.getModelData(level, pos, ClientOnly.serverHasMod() ? BrewingBottles.withoutBottles(state) : state, data);
        }
    }
}

