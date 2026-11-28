/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.LecternBlock
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.entity.LecternBlockEntity
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.RemainingLayouts;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

public final class StationDisplayLayouts {
    private StationDisplayLayouts() {
    }

    public static boolean campfire(CampfireBlockEntity fire, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!ClientOnly.serverHasMod()) {
            return false;
        }
        SpatialAppearance layout = SpatialLayouts.current(StationKind.CAMPFIRE);
        if (layout.equals(SpatialAppearance.VANILLA)) {
            return false;
        }
        BlockPos pos = fire.m_58899_();
        Direction facing = (Direction)fire.m_58900_().m_61143_((Property)CampfireBlock.f_51230_);
        SpatialAppearance.Frame frame = RemainingLayouts.frame(StationKind.CAMPFIRE, pos, facing);
        for (int i = 0; i < fire.m_59065_().size(); ++i) {
            ItemStack food = (ItemStack)fire.m_59065_().get(i);
            if (food.m_41619_()) continue;
            Direction turn = Direction.m_122407_((int)((i + facing.m_122416_()) % 4));
            ItemPlacer.drawOriented(pose, buffers, Vec3.m_82528_((Vec3i)pos), food, layout.point(frame, i), Axis.f_252436_.m_252977_(-turn.m_122435_() - (float)layout.rotation(i)).mul((Quaternionfc)Axis.f_252529_.m_252977_(90.0f)), 0.375f * layout.scale(i), light);
        }
        return true;
    }

    public static boolean customLectern() {
        return !SpatialLayouts.current(StationKind.LECTERN).equals(SpatialAppearance.VANILLA);
    }

    public static void lectern(LecternBlockEntity lectern, PoseStack pose) {
        BlockPos pos = lectern.m_58899_();
        SpatialAppearance.Frame frame = RemainingLayouts.frame(StationKind.LECTERN, pos, (Direction)lectern.m_58900_().m_61143_((Property)LecternBlock.f_54465_));
        SpatialAppearance layout = SpatialLayouts.current(StationKind.LECTERN);
        Vec3 from = frame.base(0).m_82546_(Vec3.m_82528_((Vec3i)pos));
        Vec3 to = layout.point(frame, 0).m_82546_(Vec3.m_82528_((Vec3i)pos));
        Vec3 normal = frame.normal();
        pose.m_85837_(to.f_82479_, to.f_82480_, to.f_82481_);
        pose.m_252781_(new Quaternionf().rotationAxis((float)Math.toRadians(layout.rotation(0)), (float)normal.f_82479_, (float)normal.f_82480_, (float)normal.f_82481_));
        float scale = layout.scale(0);
        pose.m_85841_(scale, scale, scale);
        pose.m_85837_(-from.f_82479_, -from.f_82480_, -from.f_82481_);
    }
}

