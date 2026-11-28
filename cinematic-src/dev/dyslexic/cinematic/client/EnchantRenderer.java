/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Camera
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.ClientEnchants;
import dev.dyslexic.cinematic.client.ClientPayloads;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EnchantRenderer {
    private static final double MAX_DISTANCE_SQR = 2304.0;
    private static final Set<BlockPos> DRAWN = new HashSet<BlockPos>();

    private EnchantRenderer() {
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Camera camera, Frustum frustum, ClientLevel level) {
        Vec3 cam = camera.m_90583_();
        EnchantStation mine = Scene.station(EnchantStation.class);
        if (mine != null) {
            mine.renderScene(pose, buffers, camera, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.pos.m_7494_()));
        }
        Set<BlockPos> drawn = DRAWN;
        drawn.clear();
        double clock = Scene.clock();
        for (Map.Entry<BlockPos, ClientEnchants.Entry> e : ClientEnchants.TABLES.entrySet()) {
            BlockPos pos = e.getKey();
            if (mine != null && mine.pos.equals((Object)pos) || pos.m_203193_((Position)cam) > 2304.0 || !level.m_8055_(pos).m_60713_(Blocks.f_50201_) || !frustum.m_113029_(EnchantRenderer.visibleBox(pos))) continue;
            drawn.add(pos);
            ClientEnchants.Entry entry = e.getValue();
            double flash = clock - entry.flashAt;
            double boost = flash >= 0.0 && flash < 1.2 ? 1.2 * (1.0 - flash / 1.2) : 0.0;
            EnchantRenderer.drawCircle(pose, buffers, cam, pos, 0.28 + boost, clock, entry.item.m_41619_() || entry.lapis > 0);
            Vec3 at = EnchantStation.itemPosition(pos, entry.side, clock);
            if (!entry.item.m_41619_()) {
                EnchantStation.drawFloating(pose, buffers, cam, entry.item, at, (float)(clock * 40.0 + SpatialLayouts.current(StationKind.ENCHANTING).rotation(0)), 0.34f * SpatialLayouts.current(StationKind.ENCHANTING).scale(0));
            }
            if (entry.lapis > 0) {
                EnchantStation.drawLapis(pose, buffers, cam, EnchantStation.lapisPosition(pos, entry.side), entry.lapis, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)pos.m_7494_()), SpatialLayouts.current(StationKind.ENCHANTING).scale(1), (float)(-SpatialLayouts.current(StationKind.ENCHANTING).rotation(1)));
            }
            if (!(boost > 0.0)) continue;
            float b = (float)boost;
            EnchantFx.beam(pose, buffers, cam, new Vec3(at.f_82479_, EnchantFx.top((BlockPos)pos).f_82480_, at.f_82481_), 1.9, 0.6, clock * 0.9, 0.7f * b, 0.5f * b, 1.0f * b);
        }
        for (ClientPayloads.AtStation at : ClientPayloads.AT_STATION.values()) {
            if (at.kind() != StationKind.ENCHANTING || drawn.contains(at.pos()) || mine != null && mine.pos.equals((Object)at.pos()) || !(at.pos().m_203193_((Position)cam) < 2304.0) || !frustum.m_113029_(EnchantRenderer.visibleBox(at.pos()))) continue;
            drawn.add(at.pos());
            EnchantRenderer.drawCircle(pose, buffers, cam, at.pos(), 0.28, clock, true);
        }
    }

    private static AABB visibleBox(BlockPos pos) {
        return new AABB(pos).m_82400_(0.5).m_82363_(0.0, 2.0, 0.0);
    }

    private static void drawCircle(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, double glow, double clock, boolean ready) {
        float g = (float)glow;
        Vec3 top = EnchantFx.top(pos);
        EnchantFx.CirclePalette palette = ready ? EnchantFx.CirclePalette.VIOLET : EnchantFx.CirclePalette.RED;
        EnchantFx.circle(pose, buffers, cam, top.m_82520_(0.0, 0.003, 0.0), 0.47, clock * 0.25, palette, 0.85f * g);
        EnchantFx.circle(pose, buffers, cam, top.m_82520_(0.0, 0.004, 0.0), 0.25, -clock * 0.42, palette, 0.55f * g);
    }
}

