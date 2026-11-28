/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dyslexic.cinematic.client.CampfireStation;
import dev.dyslexic.cinematic.client.ClientStation;
import dev.dyslexic.cinematic.client.EnchantStation;
import dev.dyslexic.cinematic.client.GrindstoneStation;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.LecternStation;
import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.SpatialAppearance;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import dev.dyslexic.cinematic.client.SpatialTarget;
import dev.dyslexic.cinematic.client.StonecutterStation;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

final class RemainingLayouts {
    private RemainingLayouts() {
    }

    static boolean supports(StationKind kind) {
        return switch (kind) {
            case StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.STONECUTTER, StationKind.CAMPFIRE, StationKind.LECTERN, StationKind.BEACON, StationKind.JUKEBOX -> true;
            default -> false;
        };
    }

    static boolean billboard(StationKind kind, int slot) {
        return kind == StationKind.BEACON && slot > 0 || kind == StationKind.ANVIL && slot == 2 || kind == StationKind.STONECUTTER && slot == 2;
    }

    static SpatialAppearance.Frame frame(StationKind kind, BlockPos pos, Direction side) {
        ClientLevel level = Minecraft.m_91087_().f_91073_;
        if (kind == StationKind.CAMPFIRE) {
            Direction facing = level != null && level.m_8055_(pos).m_61138_((Property)CampfireBlock.f_51230_) ? (Direction)level.m_8055_(pos).m_61143_((Property)CampfireBlock.f_51230_) : Direction.NORTH;
            return new SpatialAppearance.Frame(kind, pos, facing, Direction.UP, Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, 0.44921875, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), new Vec3(0.0, 1.0, 0.0));
        }
        if (kind == StationKind.LECTERN) {
            Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
            Vec3 across = ShelfBooks.right(side);
            double angle = Math.toRadians(22.5);
            Vec3 slope = new Vec3(0.0, Math.sin(angle), 0.0).m_82546_(out.m_82490_(Math.cos(angle)));
            Vec3 normal = new Vec3(0.0, Math.cos(angle), 0.0).m_82549_(out.m_82490_(Math.sin(angle)));
            Vec3 origin = Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, 0.9975, 0.0).m_82549_(out.m_82490_(0.104375)).m_82549_(normal.m_82490_(0.012));
            return new SpatialAppearance.Frame(kind, pos, side, Direction.UP, origin, across, slope, normal);
        }
        Vec3 top = Vec3.m_82539_((Vec3i)pos).m_82520_(0.0, kind == StationKind.STONECUTTER ? 0.5625 : 1.0, 0.0);
        if (kind == StationKind.GRINDSTONE) {
            top = GrindstoneStation.wheelCenter(pos, level == null ? Blocks.f_50623_.m_49966_() : level.m_8055_(pos)).m_82520_(0.0, 0.375, 0.0);
        }
        return RemainingLayouts.frameAt(kind, top, side);
    }

    static SpatialAppearance.Frame frameAt(StationKind kind, Vec3 top, Direction side) {
        Vec3 out = Vec3.m_82528_((Vec3i)side.m_122436_());
        return new SpatialAppearance.Frame(kind, BlockPos.m_274446_((Position)top.m_82520_(0.0, -0.01, 0.0)), side, Direction.UP, top, ShelfBooks.right(side), kind == StationKind.BEACON ? new Vec3(0.0, 1.0, 0.0) : out, kind == StationKind.BEACON ? out : new Vec3(0.0, 1.0, 0.0));
    }

    static Vec3 base(SpatialAppearance.Frame f, int slot) {
        Vec3 at = f.origin();
        Vec3 right = f.across();
        Vec3 out = Vec3.m_82528_((Vec3i)f.side().m_122436_());
        return switch (f.kind()) {
            case StationKind.ANVIL -> {
                if (slot == 0) {
                    yield at.m_82549_(right.m_82490_(-0.02));
                }
                if (slot == 1) {
                    yield at.m_82549_(right.m_82490_(0.07)).m_82549_(out.m_82490_(-0.03)).m_82520_(0.0, 0.012, 0.0);
                }
                yield at.m_82549_(out.m_82490_(0.02)).m_82520_(0.0, 0.2, 0.0);
            }
            case StationKind.GRINDSTONE -> {
                if (slot < 2) {
                    yield at.m_82549_(right.m_82490_(slot == 0 ? 0.1 : -0.1));
                }
                yield at.m_82549_(out.m_82490_(0.04)).m_82520_(0.0, 0.03, 0.0);
            }
            case StationKind.STONECUTTER -> {
                if (slot == 0) {
                    yield at.m_82549_(right.m_82490_(-0.25)).m_82549_(out.m_82490_(0.24));
                }
                if (slot == 1) {
                    yield at;
                }
                yield at.m_82549_(out.m_82490_(-0.06)).m_82520_(0.0, 0.29, 0.0);
            }
            case StationKind.CAMPFIRE -> CampfireStation.vanillaSpot(f.pos(), f.side(), slot);
            case StationKind.LECTERN, StationKind.JUKEBOX -> at;
            case StationKind.BEACON -> {
                if (slot == 0) {
                    yield at.m_82549_(out.m_82490_(0.27)).m_82520_(0.0, 0.14, 0.0);
                }
                int i = slot - 1;
                double x = i < 4 ? (i % 2 == 0 ? -0.8 : -0.45) : (i == 4 ? -0.62 : 0.6);
                double y = i < 5 ? 0.2 + 0.31 * (double)(i / 2) : 0.36 + 0.31 * (double)(i - 5);
                yield at.m_82549_(out.m_82490_(0.14 - 0.08 * Math.abs(x))).m_82549_(right.m_82490_(x)).m_82520_(0.0, y, 0.0);
            }
            default -> throw new IllegalArgumentException("No station anchors: " + String.valueOf((Object)f.kind()));
        };
    }

    static double[] half(StationKind kind, int slot) {
        double[] dArray;
        switch (kind) {
            case LECTERN: {
                double[] dArray2 = new double[2];
                dArray2[0] = 0.447;
                dArray = dArray2;
                dArray2[1] = 0.280381679389313;
                break;
            }
            case JUKEBOX: {
                double[] dArray3 = new double[2];
                dArray3[0] = 0.25;
                dArray = dArray3;
                dArray3[1] = 0.25;
                break;
            }
            case ANVIL: {
                if (slot == 2) {
                    double[] dArray4 = new double[2];
                    dArray4[0] = 0.3;
                    dArray = dArray4;
                    dArray4[1] = 0.08;
                    break;
                }
                double[] dArray5 = new double[2];
                dArray5[0] = 0.2;
                dArray = dArray5;
                dArray5[1] = 0.14;
                break;
            }
            case STONECUTTER: {
                if (slot == 2) {
                    double[] dArray6 = new double[2];
                    dArray6[0] = 0.72;
                    dArray = dArray6;
                    dArray6[1] = 0.28;
                    break;
                }
                double[] dArray7 = new double[2];
                dArray7[0] = 0.15;
                dArray = dArray7;
                dArray7[1] = 0.15;
                break;
            }
            default: {
                double[] dArray8 = new double[2];
                dArray8[0] = 0.13;
                dArray = dArray8;
                dArray8[1] = 0.13;
            }
        }
        return dArray;
    }

    static Vec3 point(StationKind kind, Vec3 top, Direction side, int slot) {
        return SpatialLayouts.current(kind).point(RemainingLayouts.frameAt(kind, top, side), slot);
    }

    static Vec3 transform(StationKind kind, Vec3 top, Direction side, int slot, Vec3 point) {
        SpatialAppearance.Frame frame = RemainingLayouts.frameAt(kind, top, side);
        SpatialAppearance layout = SpatialLayouts.current(kind);
        Vec3 delta = point.m_82546_(frame.base(slot));
        return layout.point(frame, slot).m_82549_(layout.axis(frame, slot, true).m_82490_(delta.m_82526_(frame.across()) * layout.group().width() * layout.region(slot).width())).m_82549_(layout.axis(frame, slot, false).m_82490_(delta.m_82526_(frame.up()) * layout.group().depth() * layout.region(slot).depth())).m_82549_(frame.normal().m_82490_(delta.m_82526_(frame.normal()) * (double)layout.scale(slot)));
    }

    static float scale(StationKind kind, int slot) {
        return SpatialLayouts.current(kind).scale(slot);
    }

    static float rotation(StationKind kind, int slot) {
        return (float)SpatialLayouts.current(kind).rotation(slot) * (float)(kind == StationKind.LECTERN || kind == StationKind.BEACON ? 1 : -1);
    }

    static List<Vec3> samples(SpatialTarget target, List<Vec3> old) {
        if (target.geometry().equals(SpatialAppearance.VANILLA)) {
            return old;
        }
        ArrayList<Vec3> samples = new ArrayList<Vec3>(old);
        int count = switch (target.layoutStation().kind()) {
            case StationKind.ANVIL, StationKind.GRINDSTONE -> 2;
            case StationKind.STONECUTTER, StationKind.BEACON -> 1;
            default -> SpatialAppearance.count(target.layoutStation().kind());
        };
        for (int i = 0; i < count; ++i) {
            samples.add(target.layoutPoint(i).m_82549_(target.layoutFrame().normal().m_82490_(0.04)));
        }
        return samples;
    }

    static ItemStack item(ClientStation station, int slot) {
        BlockEntity blockEntity;
        if (station instanceof CampfireStation && Minecraft.m_91087_().f_91073_ != null && (blockEntity = Minecraft.m_91087_().f_91073_.m_7702_(station.pos)) instanceof CampfireBlockEntity) {
            CampfireBlockEntity fire = (CampfireBlockEntity)blockEntity;
            return (ItemStack)fire.m_59065_().get(slot);
        }
        if (station instanceof LecternStation) {
            return new ItemStack((ItemLike)Items.f_42615_);
        }
        if (station instanceof MenuStation) {
            int inputs;
            MenuStation menu = (MenuStation)station;
            int n = inputs = station.kind() == StationKind.ANVIL || station.kind() == StationKind.GRINDSTONE ? 2 : 1;
            if (slot < inputs) {
                return menu.slot(slot);
            }
            if (station.kind() == StationKind.BEACON && slot < 7) {
                return new ItemStack((ItemLike)Items.f_42686_);
            }
        }
        return ItemStack.f_41583_;
    }

    static void ghost(SpatialTarget target, PoseStack pose, MultiBufferSource buffers, Vec3 camera, int slot, int light) {
        ClientStation station = target.layoutStation();
        StationKind kind = station.kind();
        ItemStack example = new ItemStack((ItemLike)(switch (kind) {
            case StationKind.ANVIL -> {
                if (slot == 0) {
                    yield Items.f_42385_;
                }
                if (slot == 1) {
                    yield Items.f_42690_;
                }
                yield Items.f_42656_;
            }
            case StationKind.GRINDSTONE -> {
                if (slot < 2) {
                    yield Items.f_42383_;
                }
                yield Items.f_42484_;
            }
            case StationKind.STONECUTTER -> {
                if (slot == 0) {
                    yield Items.f_41905_;
                }
                if (slot == 1) {
                    yield Items.f_41922_;
                }
                yield Items.f_42371_;
            }
            case StationKind.CAMPFIRE -> Items.f_42579_;
            case StationKind.LECTERN -> Items.f_42615_;
            case StationKind.BEACON -> {
                if (slot == 0) {
                    yield Items.f_42417_;
                }
                yield Items.f_42686_;
            }
            case StationKind.JUKEBOX -> Items.f_42752_;
            default -> Items.f_42516_;
        }));
        float scale = target.geometry().scale(slot);
        float rotation = (float)target.geometry().rotation(slot);
        if (kind == StationKind.ANVIL && slot < 2) {
            ItemPlacer.drawOriented(pose, buffers, camera, example, target.layoutPoint(slot).m_82520_(0.0, 0.004, 0.0), ItemPlacer.lying(ItemPlacer.awayYaw(station.sideDirection()) + 45.0f + (float)(slot == 1 ? 70 : 0) + RemainingLayouts.rotation(kind, slot), 0.0f), (slot == 0 ? 0.4f : 0.3f) * scale, light);
        } else if (kind == StationKind.GRINDSTONE && slot < 2) {
            GrindstoneStation.drawLying(pose, buffers, camera, example, target.layoutPoint(slot), station.sideDirection(), slot, 1.0f, light);
        } else if (kind == StationKind.STONECUTTER && slot == 0) {
            StonecutterStation.drawStack(pose, buffers, camera, example, target.layoutPoint(slot), station.sideDirection(), light);
        } else if (kind == StationKind.STONECUTTER) {
            float size = (slot == 1 ? 0.2f : 0.12750001f) * scale;
            ItemPlacer.drawOriented(pose, buffers, camera, example, target.layoutPoint(slot).m_82520_(0.0, slot == 1 ? (double)(size / 2.0f) : 0.0, 0.0), Axis.f_252436_.m_252977_(ItemPlacer.awayYaw(station.sideDirection()) + RemainingLayouts.rotation(kind, slot)), size, light);
        } else if (kind == StationKind.BEACON && slot == 0) {
            EnchantStation.drawFloating(pose, buffers, camera, example, target.layoutPoint(slot), (float)(Scene.clock() * 50.0), 0.26f * scale);
        } else if (kind == StationKind.BEACON || kind == StationKind.ANVIL && slot == 2) {
            Quaternionf cameraRotation = new Quaternionf((Quaternionfc)Minecraft.m_91087_().f_91063_.m_109153_().m_253121_()).mul((Quaternionfc)Axis.f_252436_.m_252977_(180.0f));
            cameraRotation.mul((Quaternionfc)Axis.f_252403_.m_252977_(rotation));
            ItemPlacer.drawOriented(pose, buffers, camera, example, target.layoutPoint(slot), cameraRotation, 0.16f * scale, light);
        } else if (kind == StationKind.CAMPFIRE) {
            Direction direction = Direction.m_122407_((int)((slot + target.layoutFrame().side().m_122416_()) % 4));
            ItemPlacer.drawOriented(pose, buffers, camera, example, target.layoutPoint(slot), Axis.f_252436_.m_252977_(-direction.m_122435_() - rotation).mul((Quaternionfc)Axis.f_252529_.m_252977_(90.0f)), 0.375f * scale, light);
        } else {
            ItemPlacer.drawOriented(pose, buffers, camera, example, target.layoutPoint(slot).m_82520_(0.0, kind == StationKind.JUKEBOX ? 0.2 * (double)scale : 0.015, 0.0), kind == StationKind.JUKEBOX ? Axis.f_252436_.m_252977_(-station.sideDirection().m_122435_() + RemainingLayouts.rotation(kind, slot)) : ItemPlacer.lying(ItemPlacer.awayYaw(station.sideDirection()) + rotation, 0.0f), (kind == StationKind.JUKEBOX ? 0.6f : 0.25f) * scale, light);
        }
    }
}

