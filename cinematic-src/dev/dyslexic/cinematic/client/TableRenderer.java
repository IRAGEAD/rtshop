/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Position
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.TableLayout;
import dev.dyslexic.cinematic.client.TableLayouts;
import dev.dyslexic.cinematic.client.TableStation;
import dev.dyslexic.cinematic.table.TableGrid;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TableRenderer {
    private static final double MAX_DISTANCE_SQR = 4096.0;

    private TableRenderer() {
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Frustum frustum, ClientLevel level) {
        TableStation mine = Scene.station(TableStation.class);
        if (mine != null) {
            mine.updateOcclusion(cam);
        }
        for (Map.Entry<BlockPos, ClientTables.Entry> e : ClientTables.TABLES.entrySet()) {
            BlockPos pos = e.getKey();
            if (pos.m_203193_((Position)cam) > 4096.0 || !frustum.m_113029_(new AABB(pos).m_82363_(0.0, 0.4, 0.0)) || !TableGrid.isTable(level.m_8055_(pos))) continue;
            TableStation station = mine != null && mine.pos.equals((Object)pos) ? mine : null;
            TableRenderer.draw(pose, buffers, cam, pos, e.getValue(), station, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)pos.m_7494_()), false);
        }
        if (mine != null && Scene.clientOnly() && ClientTables.get(mine.pos) != null && frustum.m_113029_(new AABB(mine.pos).m_82363_(0.0, 0.4, 0.0))) {
            TableRenderer.draw(pose, buffers, cam, mine.pos, ClientTables.get(mine.pos), mine, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.pos.m_7494_()), false);
        }
        if (mine != null && mine.craftItems != null && ClientTables.get(mine.pos) == null && TableRenderer.gathering(mine)) {
            TableRenderer.draw(pose, buffers, cam, mine.pos, null, mine, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.pos.m_7494_()), false);
        }
        if (mine != null) {
            mine.drawHighlight(pose, buffers, cam);
            if (mine.hasFadedItems()) {
                MultiBufferSource.BufferSource source;
                if (buffers instanceof MultiBufferSource.BufferSource) {
                    source = (MultiBufferSource.BufferSource)buffers;
                    source.m_109911_();
                }
                TableRenderer.draw(pose, buffers, cam, mine.pos, ClientTables.get(mine.pos), mine, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.pos.m_7494_()), true);
                if (buffers instanceof MultiBufferSource.BufferSource) {
                    source = (MultiBufferSource.BufferSource)buffers;
                    source.m_109911_();
                }
            }
        }
    }

    private static boolean gathering(TableStation station) {
        double craft = station.craftProgress();
        return craft >= 0.0 && craft < 0.4 && station.craftItems != null;
    }

    private static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 cam, BlockPos pos, ClientTables.Entry entry, TableStation station, int light, boolean translucent) {
        boolean gathering;
        double now = Scene.clock();
        double craft = station == null ? -1.0 : station.craftProgress();
        double midpoint = 0.4;
        boolean bl = gathering = station != null && TableRenderer.gathering(station);
        double squeeze = craft < 0.0 ? 0.0 : (craft < midpoint ? Anim.inOutSine(craft / midpoint) : 1.0 - Anim.outCubic((craft - midpoint) / (1.0 - midpoint)));
        TableLayout layout = TableLayouts.current();
        Vec3 center = layout.center(pos);
        for (int i = 0; i < 9; ++i) {
            double landed;
            float opacity;
            ItemStack stack;
            ItemStack itemStack = gathering ? station.craftItems[i] : (stack = entry == null ? ItemStack.f_41583_ : entry.items[i]);
            if (stack.m_41619_() || (opacity = station == null ? 1.0f : station.itemOpacity(i)) < 0.999f != translucent) continue;
            byte facing = gathering ? station.craftFacing[i] : entry.facing[i];
            Vec3 base = layout.cell(pos, i);
            float scale = 1.0f;
            if (squeeze > 0.0) {
                base = Anim.lerp(base, center, squeeze * 0.55).m_82520_(0.0, 0.05 * squeeze, 0.0);
                scale *= (float)(1.0 - 0.35 * squeeze);
            }
            if (!gathering && entry != null && (landed = now - entry.landedAt[i]) >= 0.0 && landed < 0.25) {
                double k = landed / 0.25;
                base = base.m_82520_(0.0, 0.035 * (1.0 - Anim.outCubic(k)), 0.0);
                scale *= (float)(0.92 + 0.08 * Anim.outBack(k, 2.2));
            }
            long seed = pos.m_121878_() * 31L + (long)i * 7919L;
            layout.renderItem(pose, buffers, cam, stack, base, Direction.m_122407_((int)facing), seed, scale, 0.0f, light, opacity, false);
        }
    }
}

