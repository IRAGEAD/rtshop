/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.AbstractCookingRecipe
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraftforge.common.ForgeHooks
 *  net.minecraftforge.event.ForgeEventFactory
 */
package dev.dyslexic.cinematic.furnace;

import dev.dyslexic.cinematic.furnace.FurnaceWatch;
import dev.dyslexic.cinematic.net.FurnaceStatePayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.RecipeChoices;
import dev.dyslexic.cinematic.station.Sessions;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

public final class FurnaceActions {
    private FurnaceActions() {
    }

    public static void handle(ServerPlayer player, ServerLevel level, Sessions.Session session, StationActionPayload action) {
        Outcome outcome;
        if (action.action() != StationActionPayload.Action.CLICK || action.index() < 0 || action.index() > 2) {
            return;
        }
        BlockEntity blockEntity = level.m_7702_(session.pos());
        if (!(blockEntity instanceof AbstractFurnaceBlockEntity)) {
            return;
        }
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)blockEntity;
        Outcome outcome2 = outcome = action.index() == 2 ? FurnaceActions.takeResult(player, level, furnace, action.button(), action.shift()) : FurnaceActions.load(player, furnace, action.index(), action.button(), action.shift());
        if (outcome == Outcome.NONE) {
            return;
        }
        BlockPos pos = session.pos();
        if (outcome != Outcome.REFUSED) {
            furnace.m_6596_();
            FurnaceWatch.sync(level, pos, furnace);
            FurnaceActions.sendState(player, session);
        }
        player.f_36095_.m_38946_();
        float pitch = 0.9f + level.f_46441_.m_188501_() * 0.2f;
        switch (outcome) {
            case PLACED: {
                level.m_5594_(null, pos, action.index() == 1 ? SoundEvents.f_12446_ : SoundEvents.f_144110_, SoundSource.PLAYERS, 0.4f, pitch * 1.2f);
                break;
            }
            case TAKEN: {
                level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.PLAYERS, 0.2f, pitch * 1.4f);
                break;
            }
            default: {
                level.m_5594_(null, pos, SoundEvents.f_12443_, SoundSource.PLAYERS, 0.3f, 0.6f);
            }
        }
    }

    private static Outcome load(ServerPlayer player, AbstractFurnaceBlockEntity furnace, int slot, int button, boolean shift) {
        InventoryMenu menu = player.f_36095_;
        ItemStack carried = menu.m_142621_();
        ItemStack inside = furnace.m_8020_(slot);
        Outcome outcome = Outcome.NONE;
        if (shift && button == 0) {
            if (!inside.m_41619_()) {
                ItemStack moving = inside.m_41777_();
                player.m_150109_().m_36054_(moving);
                if (moving.m_41613_() != inside.m_41613_()) {
                    furnace.m_6836_(slot, moving.m_41619_() ? ItemStack.f_41583_ : moving);
                    outcome = Outcome.TAKEN;
                }
            }
        } else if (carried.m_41619_()) {
            if (!inside.m_41619_()) {
                int take = button == 0 ? inside.m_41613_() : (inside.m_41613_() + 1) / 2;
                menu.m_142503_(furnace.m_7407_(slot, take));
                outcome = Outcome.TAKEN;
            }
        } else if (!furnace.m_7013_(slot, carried)) {
            outcome = Outcome.REFUSED;
        } else if (inside.m_41619_()) {
            int count = button == 0 ? Math.min(carried.m_41613_(), Math.min(furnace.m_6893_(), carried.m_41741_())) : 1;
            furnace.m_6836_(slot, carried.m_41620_(count));
            outcome = Outcome.PLACED;
        } else if (ItemStack.m_150942_((ItemStack)carried, (ItemStack)inside)) {
            int room;
            int move = Math.min(button == 0 ? carried.m_41613_() : 1, room = Math.min(furnace.m_6893_(), inside.m_41741_()) - inside.m_41613_());
            if (move > 0) {
                inside.m_41769_(move);
                carried.m_41774_(move);
                outcome = Outcome.PLACED;
            }
        } else {
            furnace.m_6836_(slot, carried);
            menu.m_142503_(inside);
            outcome = Outcome.PLACED;
        }
        if (menu.m_142621_().m_41619_()) {
            menu.m_142503_(ItemStack.f_41583_);
        }
        return outcome;
    }

    public static void quickMove(ServerPlayer player, ServerLevel level, Sessions.Session session, int inventorySlot) {
        int move;
        BlockEntity blockEntity = level.m_7702_(session.pos());
        if (!(blockEntity instanceof AbstractFurnaceBlockEntity)) {
            return;
        }
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)blockEntity;
        ItemStack source = player.m_150109_().m_8020_(inventorySlot);
        RecipeType<? extends AbstractCookingRecipe> type = FurnaceActions.recipeType(session.kind());
        int slot = level.m_7465_().m_44015_(type, (Container)new SimpleContainer(new ItemStack[]{source}), (Level)level).isPresent() ? 0 : (ForgeHooks.getBurnTime((ItemStack)source, type) > 0 ? 1 : -1);
        BlockPos pos = session.pos();
        if (slot < 0 || !furnace.m_7013_(slot, source)) {
            level.m_5594_(null, pos, SoundEvents.f_12443_, SoundSource.PLAYERS, 0.3f, 0.6f);
            return;
        }
        ItemStack inside = furnace.m_8020_(slot);
        int limit = Math.min(furnace.m_6893_(), source.m_41741_());
        if (inside.m_41619_()) {
            move = Math.min(source.m_41613_(), limit);
            furnace.m_6836_(slot, source.m_41620_(move));
        } else if (ItemStack.m_150942_((ItemStack)source, (ItemStack)inside)) {
            move = Math.max(0, Math.min(source.m_41613_(), Math.min(limit, inside.m_41741_()) - inside.m_41613_()));
            inside.m_41769_(move);
            source.m_41774_(move);
        } else {
            move = 0;
        }
        if (move <= 0) {
            level.m_5594_(null, pos, SoundEvents.f_12443_, SoundSource.PLAYERS, 0.3f, 0.6f);
            return;
        }
        player.m_150109_().m_6596_();
        furnace.m_6596_();
        FurnaceWatch.sync(level, pos, furnace);
        FurnaceActions.sendState(player, session);
        player.f_36095_.m_38946_();
        float pitch = 0.9f + level.f_46441_.m_188501_() * 0.2f;
        level.m_5594_(null, pos, slot == 1 ? SoundEvents.f_12446_ : SoundEvents.f_144110_, SoundSource.PLAYERS, 0.4f, pitch * 1.2f);
    }

    private static RecipeType<? extends AbstractCookingRecipe> recipeType(StationKind kind) {
        return switch (kind) {
            case StationKind.SMOKER -> RecipeType.f_44110_;
            case StationKind.BLAST_FURNACE -> RecipeType.f_44109_;
            default -> RecipeType.f_44108_;
        };
    }

    private static Outcome takeResult(ServerPlayer player, ServerLevel level, AbstractFurnaceBlockEntity furnace, int button, boolean shift) {
        ItemStack taken;
        InventoryMenu menu = player.f_36095_;
        ItemStack carried = menu.m_142621_();
        ItemStack inside = furnace.m_8020_(2);
        if (inside.m_41619_()) {
            return carried.m_41619_() ? Outcome.NONE : Outcome.REFUSED;
        }
        if (shift) {
            ItemStack moving = inside.m_41777_();
            player.m_150109_().m_36054_(moving);
            int moved = inside.m_41613_() - moving.m_41613_();
            if (moved <= 0) {
                return Outcome.NONE;
            }
            taken = furnace.m_7407_(2, moved);
        } else if (carried.m_41619_()) {
            taken = furnace.m_7407_(2, button == 0 ? inside.m_41613_() : (inside.m_41613_() + 1) / 2);
            menu.m_142503_(taken.m_41777_());
        } else if (ItemStack.m_150942_((ItemStack)carried, (ItemStack)inside)) {
            int move = Math.min(inside.m_41613_(), carried.m_41741_() - carried.m_41613_());
            if (move <= 0) {
                return Outcome.REFUSED;
            }
            taken = furnace.m_7407_(2, move);
            carried.m_41769_(move);
        } else {
            return Outcome.REFUSED;
        }
        taken.m_41678_((Level)level, (Player)player, taken.m_41613_());
        furnace.m_155003_(player);
        ForgeEventFactory.firePlayerSmeltedEvent((Player)player, (ItemStack)taken);
        return Outcome.TAKEN;
    }

    public static void sendState(ServerPlayer player, Sessions.Session session) {
        BlockEntity blockEntity = player.m_9236_().m_7702_(session.pos());
        if (blockEntity instanceof AbstractFurnaceBlockEntity) {
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)blockEntity;
            Network.sendToPlayer(player, new FurnaceStatePayload(session.pos(), furnace.f_58316_, furnace.f_58317_, furnace.f_58318_, furnace.f_58319_, RecipeChoices.furnaceChoice(furnace, session.kind())));
        }
    }

    private static enum Outcome {
        NONE,
        PLACED,
        TAKEN,
        REFUSED;

    }
}

