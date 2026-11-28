/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BrewingStandBlockEntity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.Cinematic;
import dev.dyslexic.cinematic.client.ClientBrewing;
import dev.dyslexic.cinematic.client.ClientFurnaces;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.ClientTables;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.SceneScreen;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ServerSmoke {
    private static final boolean ENABLED = Boolean.getBoolean("cinematic.serverSmoke");
    private static final boolean RECONNECT = Boolean.getBoolean("cinematic.serverSmoke.reconnect");
    private static final BlockPos TABLE = new BlockPos(0, -60, 0);
    private static final BlockPos FURNACE = new BlockPos(2, -60, 0);
    private static final BlockPos BREWING = new BlockPos(-2, -60, 0);
    private static int stage = 1;
    private static int ticks;

    private ServerSmoke() {
    }

    public static void tick() {
        if (!ENABLED || stage == 99 || stage == -1) {
            return;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null || mc.f_91072_ == null) {
            return;
        }
        if (++ticks > 900) {
            ServerSmoke.fail("timed out at stage " + stage + ", phase " + String.valueOf((Object)Scene.phase));
            return;
        }
        if (RECONNECT) {
            boolean back;
            ClientTables.Entry entry = ClientTables.get(TABLE);
            boolean bl = back = entry != null && Arrays.stream(entry.items).anyMatch(s -> s.m_150930_(Items.f_42647_) && s.m_41613_() == 16);
            if (back && ClientOnly.serverHasMod()) {
                Cinematic.LOGGER.info("[ServerSmoke] PASS: the table's 16 planks came back after the server was stopped and started (world save)");
                stage = 99;
                mc.m_91395_();
            }
            return;
        }
        switch (stage) {
            case 1: {
                if (!(StationKind.TABLE.matches(mc.f_91073_.m_8055_(TABLE)) && StationKind.FURNACE.matches(mc.f_91073_.m_8055_(FURNACE)) && mc.f_91073_.m_7702_(BREWING) instanceof BrewingStandBlockEntity && mc.f_91074_.m_150109_().m_8020_(12).m_150930_(Items.f_42589_) && !(mc.f_91074_.m_20182_().f_82481_ < 2.0))) {
                    return;
                }
                if (!ClientOnly.serverHasMod()) {
                    ServerSmoke.fail("the server should have the mod");
                    return;
                }
                ServerSmoke.use(mc, TABLE, new Vec3(0.5, 1.0, 0.5), Direction.UP);
                ServerSmoke.next();
                break;
            }
            case 2: {
                if (!ServerSmoke.active(StationKind.TABLE) || ticks < 25) {
                    return;
                }
                ServerSmoke.click(mc, 9);
                ServerSmoke.next();
                break;
            }
            case 3: {
                boolean laid;
                ClientTables.Entry entry = ClientTables.get(TABLE);
                boolean bl = laid = entry != null && Arrays.stream(entry.items).anyMatch(s -> s.m_150930_(Items.f_42647_) && s.m_41613_() == 16);
                if (!laid || !mc.f_91074_.m_150109_().m_8020_(9).m_41619_()) {
                    return;
                }
                Cinematic.LOGGER.info("[ServerSmoke] shift-click laid 16 planks on the table, the server sent them back");
                Scene.leave();
                ServerSmoke.next();
                break;
            }
            case 4: {
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) {
                    return;
                }
                ServerSmoke.use(mc, FURNACE, new Vec3(0.5, 0.6, 1.0), Direction.SOUTH);
                ServerSmoke.next();
                break;
            }
            case 5: {
                if (!ServerSmoke.active(StationKind.FURNACE) || ticks < 25) {
                    return;
                }
                ServerSmoke.click(mc, 10);
                ServerSmoke.click(mc, 11);
                ServerSmoke.next();
                break;
            }
            case 6: {
                boolean dirt;
                boolean ore = ClientFurnaces.item(FURNACE, 0).m_150930_(Items.f_151050_) && ClientFurnaces.item(FURNACE, 0).m_41613_() == 5;
                boolean bl = dirt = mc.f_91074_.m_150109_().m_8020_(11).m_150930_(Items.f_42329_) && mc.f_91074_.m_150109_().m_8020_(11).m_41613_() == 3;
                if (!ore || !mc.f_91074_.m_150109_().m_8020_(10).m_41619_()) {
                    return;
                }
                if (!dirt) {
                    ServerSmoke.fail("the dirt left its slot");
                    return;
                }
                Cinematic.LOGGER.info("[ServerSmoke] furnace: raw iron in at the top, dirt left where it was");
                Scene.leave();
                ServerSmoke.next();
                break;
            }
            case 7: {
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) {
                    return;
                }
                ServerSmoke.use(mc, BREWING, new Vec3(0.5, 1.0, 0.5), Direction.UP);
                ServerSmoke.next();
                break;
            }
            case 8: {
                if (!ServerSmoke.active(StationKind.BREWING) || ticks < 25) {
                    return;
                }
                ServerSmoke.click(mc, 12);
                ServerSmoke.next();
                break;
            }
            case 9: {
                BlockEntity dirt = mc.f_91073_.m_7702_(BREWING);
                if (!(dirt instanceof BrewingStandBlockEntity)) {
                    return;
                }
                BrewingStandBlockEntity stand = (BrewingStandBlockEntity)dirt;
                boolean shown = false;
                for (int i = 0; i < 3; ++i) {
                    shown |= ClientBrewing.item(stand, i).m_150930_(Items.f_42589_);
                }
                if (!shown || !mc.f_91074_.m_150109_().m_8020_(12).m_41619_()) {
                    return;
                }
                Cinematic.LOGGER.info("[ServerSmoke] brewing stand: the bottle went onto a foot and its contents packet arrived");
                Scene.leave();
                ServerSmoke.next();
                break;
            }
            case 10: {
                if (Scene.phase != Scene.Phase.NONE || ticks < 30) {
                    return;
                }
                Cinematic.LOGGER.info("[ServerSmoke] PASS: dedicated server with the mod, shift-clicks into table, furnace and brewing stand");
                stage = 99;
                mc.m_91395_();
                break;
            }
        }
    }

    private static boolean active(StationKind kind) {
        return Scene.phase == Scene.Phase.ACTIVE && Scene.station != null && Scene.station.kind() == kind && !Scene.clientOnly() && Minecraft.m_91087_().f_91080_ instanceof SceneScreen;
    }

    private static void use(Minecraft mc, BlockPos pos, Vec3 local, Direction face) {
        mc.f_91072_.m_233732_(mc.f_91074_, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.m_82528_((Vec3i)pos).m_82549_(local), face, pos, false));
    }

    private static void click(Minecraft mc, int inventorySlot) {
        Screen screen = mc.f_91080_;
        if (screen instanceof SceneScreen) {
            SceneScreen screen2 = (SceneScreen)screen;
            screen2.panelClick(inventorySlot < 9 ? 36 + inventorySlot : inventorySlot, 0, true);
        }
    }

    private static void next() {
        ++stage;
        ticks = 0;
    }

    private static void fail(String reason) {
        Cinematic.LOGGER.error("[ServerSmoke] FAIL: {}", (Object)reason);
        stage = -1;
        Minecraft.m_91087_().m_91395_();
    }
}

