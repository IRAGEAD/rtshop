/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.FurnaceAppearance;
import dev.dyslexic.cinematic.client.FurnaceLayouts;
import dev.dyslexic.cinematic.client.FurnaceStation;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.furnace.FurnaceLayout;
import dev.dyslexic.cinematic.furnace.FurnaceZones;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class FurnaceRenderer {
    private static final double MAX_DISTANCE_SQR = 2304.0;
    private static final int[] FUEL_GLOW = new int[]{1};
    private static final int[] INPUT_GLOW = new int[]{0};
    private static final int[] COOKING_GLOW = new int[]{0, 2};
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private FurnaceRenderer() {
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Frustum frustum, ClientLevel level) {
        ClientFurnaces.Entry entry;
        FurnaceStation mine = Scene.station(FurnaceStation.class);
        for (Map.Entry<BlockPos, ClientFurnaces.Entry> e : ClientFurnaces.FURNACES.entrySet()) {
            FurnaceLayout layout;
            BlockPos pos = e.getKey();
            if (pos.m_203193_((Position)cam) > 2304.0) continue;
            BlockState state = level.m_8055_(pos);
            StationKind kind = StationKind.of(state);
            FurnaceLayout furnaceLayout = layout = kind == null ? null : FurnaceLayout.of(kind);
            if (layout == null) continue;
            FurnaceAppearance appearance = FurnaceLayouts.current(kind);
            if (!frustum.m_113029_(new AABB(pos).m_82400_(appearance.equals(FurnaceAppearance.VANILLA) ? 0.2 : 0.6))) continue;
            Direction facing = (Direction)state.m_61143_((Property)AbstractFurnaceBlock.f_48683_);
            Vec3 toCamera = cam.m_82546_(FurnaceZones.point(pos, facing, 0.5, 0.5, 0.0));
            boolean frontSeen = toCamera.m_82526_(Vec3.m_82528_((Vec3i)facing.m_122436_())) > 0.0;
            boolean topSeen = cam.f_82480_ > (double)(pos.m_123342_() + 1);
            boolean lit = (Boolean)state.m_61143_((Property)AbstractFurnaceBlock.f_48684_);
            int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)pos.m_121945_(facing));
            int inside = lit ? LightTexture.m_109885_((int)15, (int)LightTexture.m_109894_((int)light)) : light;
            ClientFurnaces.Entry entry2 = e.getValue();
            for (FurnaceLayout.Bed bed : layout.beds) {
                if (!(bed.face() == FurnaceZones.Face.FRONT ? frontSeen : topSeen)) continue;
                FurnaceRenderer.drawBed(pose, buffers, cam, pos, facing, entry2, layout, appearance, bed, bed.slot() == 2 ? light : inside);
            }
            if (!lit) continue;
            double heat = mine != null && mine.pos.equals((Object)pos) ? mine.cook() : 0.5;
            FurnaceRenderer.drawGlow(pose, buffers, cam, pos, facing, layout, appearance, heat, frontSeen, topSeen);
        }
        if (mine != null && Scene.clientOnly() && mine.stillThere((Level)level) && (entry = ClientFurnaces.get(mine.pos)) != null) {
            BlockState state = level.m_8055_(mine.pos);
            Direction facing = (Direction)state.m_61143_((Property)AbstractFurnaceBlock.f_48683_);
            FurnaceLayout layout = mine.layout;
            FurnaceAppearance appearance = FurnaceLayouts.current(mine.kind());
            Vec3 toCamera = cam.m_82546_(FurnaceZones.point(mine.pos, facing, 0.5, 0.5, 0.0));
            boolean frontSeen = toCamera.m_82526_(Vec3.m_82528_((Vec3i)facing.m_122436_())) > 0.0;
            boolean topSeen = cam.f_82480_ > (double)(mine.pos.m_123342_() + 1);
            int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.pos.m_121945_(facing));
            int inside = (Boolean)state.m_61143_((Property)AbstractFurnaceBlock.f_48684_) != false ? LightTexture.m_109885_((int)15, (int)LightTexture.m_109894_((int)light)) : light;
            for (FurnaceLayout.Bed bed : layout.beds) {
                if (!(bed.face() == FurnaceZones.Face.FRONT ? frontSeen : topSeen)) continue;
                FurnaceRenderer.drawBed(pose, buffers, cam, mine.pos, facing, entry, layout, appearance, bed, bed.slot() == 2 ? light : inside);
            }
            if (((Boolean)state.m_61143_((Property)AbstractFurnaceBlock.f_48684_)).booleanValue()) {
                FurnaceRenderer.drawGlow(pose, buffers, cam, mine.pos, facing, layout, appearance, mine.cook(), frontSeen, topSeen);
            }
        }
    }

    private static void drawBed(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction facing, ClientFurnaces.Entry entry, FurnaceLayout layout, FurnaceAppearance appearance, FurnaceLayout.Bed bed, int light) {
        ItemStack stack = entry.items[bed.slot()];
        if (stack.m_41619_()) {
            return;
        }
        int copies = Math.min(bed.maxCopies(), stack.m_41613_() >= 32 ? 4 : (stack.m_41613_() >= 9 ? 3 : (stack.m_41613_() >= 2 ? 2 : 1)));
        double landed = Scene.clock() - entry.landedAt[bed.slot()];
        double slide = landed >= 0.0 && landed < 0.3 ? 1.0 - Anim.outCubic(landed / 0.3) : 0.0;
        long seed = pos.m_121878_() * 131L + (long)bed.slot() * 977L;
        for (int c = 0; c < copies; ++c) {
            Vec3 base;
            double u = copies == 1 ? (bed.u0() + bed.u1()) / 2.0 : bed.u0() + (bed.u1() - bed.u0()) * ((double)c + 0.5) / (double)copies;
            double wobble = (double)(seed >> c * 5 & 7L) / 7.0 - 0.5;
            if (bed.face() == FurnaceZones.Face.FRONT) {
                double out = -bed.depth() + wobble * 0.02 + 0.06 * slide;
                base = appearance.point(layout, pos, facing, bed.slot(), u, bed.floor() + 0.002 + (double)c * 0.0015, out, true);
            } else {
                double v = bed.floor() + wobble * 0.08;
                base = appearance.point(layout, pos, facing, bed.slot(), u, v, -bed.depth() + 0.06 * slide, true);
            }
            ItemPlacer.renderOne(pose, buffers, cam, stack, base, facing, seed + (long)c * 31L, 0.95f * (float)appearance.region(bed.slot()).itemScale(), bed.tilt(), light);
        }
    }

    private static void drawGlow(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, Direction facing, FurnaceLayout layout, FurnaceAppearance appearance, double heat, boolean frontSeen, boolean topSeen) {
        double t = Scene.clock() + (double)(pos.m_121878_() & 0xFFL) * 0.37;
        double flicker = 0.82 + 0.1 * Math.sin(t * 11.3) + 0.08 * Math.sin(t * 23.1 + 1.3);
        VertexConsumer glow = buffers.m_6299_(Fx.flatGlow());
        for (FurnaceLayout.Glow g : layout.glows) {
            int[] regions;
            if (g.face() != FurnaceZones.Face.FRONT ? !topSeen : !frontSeen) continue;
            for (int region : regions = g.fire() ? FUEL_GLOW : (g.face() == FurnaceZones.Face.TOP || appearance.equals(FurnaceAppearance.VANILLA) ? INPUT_GLOW : COOKING_GLOW)) {
                double strength;
                double[] rect = appearance.glowRect(layout, g, region);
                if (rect == null) continue;
                double out = 0.003 + appearance.region(region).out() / 16.0;
                if (g.fire()) {
                    strength = 120.0 * flicker;
                    FurnaceStation.glowQuad(pose, glow, cam, pos, facing, g.face(), rect, out, FurnaceRenderer.rgb(strength, 0.42, 0.1), FurnaceRenderer.rgb(strength * 0.25, 0.35, 0.08));
                    continue;
                }
                if (g.face() == FurnaceZones.Face.TOP) {
                    strength = (130.0 + 90.0 * heat) * flicker;
                    Vec3 center = FurnaceZones.top(pos, facing, (rect[0] + rect[1]) / 2.0, (rect[2] + rect[3]) / 2.0, out);
                    FurnaceRenderer.radial(pose, glow, cam, center, (rect[1] - rect[0]) * 0.72, FurnaceRenderer.rgb(strength, 0.45, 0.12));
                    continue;
                }
                strength = (35.0 + 70.0 * heat) * flicker;
                FurnaceStation.glowQuad(pose, glow, cam, pos, facing, g.face(), rect, out, FurnaceRenderer.rgb(strength, 0.5, 0.16), FurnaceRenderer.rgb(strength * 0.08, 0.5, 0.16));
            }
        }
    }

    private static void radial(PoseStack pose, VertexConsumer buffer, Vec3 cam, Vec3 center, double radius, int[] rgb) {
        Matrix4f m = pose.m_85850_().m_252922_();
        int segments = 24;
        float cx = (float)(center.f_82479_ - cam.f_82479_);
        float cy = (float)(center.f_82480_ - cam.f_82480_);
        float cz = (float)(center.f_82481_ - cam.f_82481_);
        for (int i = 0; i < segments; ++i) {
            double a0 = (double)i * Math.PI * 2.0 / (double)segments;
            double a1 = (double)(i + 1) * Math.PI * 2.0 / (double)segments;
            float x0 = cx + (float)(Math.cos(a0) * radius);
            float z0 = cz + (float)(Math.sin(a0) * radius);
            float x1 = cx + (float)(Math.cos(a1) * radius);
            float z1 = cz + (float)(Math.sin(a1) * radius);
            Fx.glowVertex(buffer, m, cx, cy, cz, rgb[0], rgb[1], rgb[2], UP);
            Fx.glowVertex(buffer, m, x0, cy, z0, 0, 0, 0, UP);
            Fx.glowVertex(buffer, m, x1, cy, z1, 0, 0, 0, UP);
            Fx.glowVertex(buffer, m, cx, cy, cz, rgb[0], rgb[1], rgb[2], UP);
            Fx.glowVertex(buffer, m, cx, cy, cz, rgb[0], rgb[1], rgb[2], UP);
            Fx.glowVertex(buffer, m, x1, cy, z1, 0, 0, 0, UP);
            Fx.glowVertex(buffer, m, x0, cy, z0, 0, 0, 0, UP);
            Fx.glowVertex(buffer, m, cx, cy, cz, rgb[0], rgb[1], rgb[2], UP);
        }
    }

    private static int[] rgb(double red, double green, double blue) {
        return new int[]{(int)red, (int)(red * green), (int)(red * blue)};
    }
}

