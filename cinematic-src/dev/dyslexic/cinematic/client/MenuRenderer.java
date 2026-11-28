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
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.AnvilStation;
import dev.dyslexic.cinematic.client.BeaconStation;
import dev.dyslexic.cinematic.client.CartographyStation;
import dev.dyslexic.cinematic.client.ClientMenus;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.LoomStation;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SmithingStation;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MenuRenderer {
    private static final double MAX_DISTANCE_SQR = 2304.0;

    private MenuRenderer() {
    }

    public static void renderAll(PoseStack pose, MultiBufferSource buffers, Camera camera, Frustum frustum, ClientLevel level) {
        MenuStation station;
        ClientStation clientStation;
        MenuStation mine;
        Vec3 cam = camera.m_90583_();
        MenuStation menuStation = mine = Scene.active() && (clientStation = Scene.station) instanceof MenuStation ? (station = (MenuStation)clientStation) : null;
        if (mine != null) {
            mine.frame();
            mine.renderScene(pose, buffers, camera, LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)mine.lightPos()));
        }
        double clock = Scene.clock();
        double dt = Scene.frameDt;
        Iterator<Map.Entry<BlockPos, ClientMenus.Entry>> it = ClientMenus.BLOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, ClientMenus.Entry> e = it.next();
            BlockPos pos = e.getKey();
            ClientMenus.Entry entry = e.getValue();
            BlockState state = level.m_8055_(pos);
            if (entry.kind == null || !entry.kind.matches(state)) {
                it.remove();
                continue;
            }
            if (mine != null && mine.pos.equals((Object)pos) || pos.m_203193_((Position)cam) > 2304.0) continue;
            double age = clock - entry.fxAt;
            double move = clock - entry.moveAt;
            MenuRenderer.onlookerFx(pose, buffers, camera, pos, state, entry, age, move, dt);
            if (!frustum.m_113029_(new AABB(pos).m_82400_(1.5))) continue;
            int light = LevelRenderer.m_109541_((BlockAndTintGetter)level, (BlockPos)(entry.kind == StationKind.GRINDSTONE ? pos : pos.m_7494_()));
            switch (entry.kind) {
                case ANVIL: {
                    if (move < 2.05) {
                        AnvilStation.drawOnlooker(pose, buffers, camera, pos, entry, move, light);
                        break;
                    }
                    AnvilStation.drawContents(pose, buffers, cam, pos, entry.side, entry.items, light);
                    break;
                }
                case GRINDSTONE: {
                    if (entry.spin > 0.01) {
                        GrindstoneStation.drawRim(pose, buffers, cam, GrindstoneStation.wheelCenter(pos, state), entry.side, entry.run, light);
                    }
                    if (move < 2.4) {
                        GrindstoneStation.drawOnlooker(pose, buffers, camera, pos, state, entry, move, light);
                        break;
                    }
                    GrindstoneStation.drawContents(pose, buffers, cam, pos, state, entry.side, entry.items, light);
                    break;
                }
                case SMITHING: {
                    SmithingStation.drawContents(pose, buffers, cam, pos, entry.side, entry.items, light);
                    break;
                }
                case STONECUTTER: {
                    double d = entry.moveToBag ? 1.5 : 1.05;
                    if (move < d) {
                        StonecutterStation.drawOnlooker(pose, buffers, camera, pos, entry, move, light);
                        break;
                    }
                    StonecutterStation.drawContents(pose, buffers, cam, pos, entry.side, entry.items, light);
                    break;
                }
                case LOOM: {
                    LoomStation.drawContents(pose, buffers, cam, pos, entry.side, entry.items, light);
                    break;
                }
                case CARTOGRAPHY: {
                    CartographyStation.drawContents(pose, buffers, cam, pos, entry.side, entry.items, light);
                    break;
                }
                case BEACON: {
                    BeaconStation.drawContents(pose, buffers, camera, pos, entry.side, entry.items);
                    break;
                }
            }
        }
    }

    private static void onlookerFx(PoseStack pose, MultiBufferSource buffers, Camera camera, BlockPos pos, BlockState state, ClientMenus.Entry entry, double age, double move, double dt) {
        switch (entry.kind) {
            case ANVIL: {
                AnvilStation.onlookerFx(pos, entry, move, dt);
                break;
            }
            case GRINDSTONE: {
                GrindstoneStation.onlookerFx(pos, state, entry, move, dt);
                break;
            }
            case SMITHING: {
                SmithingStation.onlookerFx(pos, entry.side, age, dt);
                break;
            }
            case STONECUTTER: {
                StonecutterStation.onlookerFx(pos, entry, move, dt);
                break;
            }
            case BEACON: {
                BeaconStation.onlookerFx(pose, buffers, camera, pos, age, dt);
                break;
            }
        }
    }
}

