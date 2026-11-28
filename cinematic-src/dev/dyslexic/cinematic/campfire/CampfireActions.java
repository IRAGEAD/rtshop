/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.stats.Stats
 *  net.minecraft.world.Container
 *  net.minecraft.world.Containers
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.CampfireCookingRecipe
 *  net.minecraft.world.item.crafting.RecipeManager
 *  net.minecraft.world.item.crafting.RecipeManager$CachedCheck
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.gameevent.GameEvent
 *  net.minecraft.world.level.gameevent.GameEvent$Context
 */
package dev.dyslexic.cinematic.campfire;

import dev.dyslexic.cinematic.net.CampfireProgressPayload;
import dev.dyslexic.cinematic.net.Network;
import dev.dyslexic.cinematic.net.StationActionPayload;
import dev.dyslexic.cinematic.station.Sessions;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;

public final class CampfireActions {
    public static final int SPOTS = 4;
    private static final WeakHashMap<CampfireBlockEntity, CookingWatch> WATCHES = new WeakHashMap();

    private CampfireActions() {
    }

    public static void handle(ServerPlayer player, ServerLevel level, Sessions.Session session, StationActionPayload action) {
        BlockEntity blockEntity;
        int spot = action.index();
        if (action.action() != StationActionPayload.Action.CLICK || spot < 0 || spot >= 4 || !((blockEntity = level.m_7702_(session.pos())) instanceof CampfireBlockEntity)) {
            return;
        }
        CampfireBlockEntity fire = (CampfireBlockEntity)blockEntity;
        InventoryMenu menu = player.f_36095_;
        ItemStack carried = menu.m_142621_();
        ItemStack there = (ItemStack)fire.m_59065_().get(spot);
        if (!carried.m_41619_()) {
            Optional recipe = fire.m_59051_(carried);
            if (!there.m_41619_() || recipe.isEmpty()) {
                return;
            }
            fire.f_59044_[spot] = ((CampfireCookingRecipe)recipe.get()).m_43753_();
            fire.f_59043_[spot] = 0;
            fire.m_59065_().set(spot, (Object)(player.m_150110_().f_35937_ ? carried.m_255036_(1) : carried.m_41620_(1)));
            level.m_220407_(GameEvent.f_157792_, session.pos(), GameEvent.Context.m_223719_((Entity)player, (BlockState)fire.m_58900_()));
            player.m_36220_(Stats.f_12975_);
        } else {
            if (there.m_41619_()) {
                return;
            }
            fire.m_59065_().set(spot, (Object)ItemStack.f_41583_);
            fire.f_59043_[spot] = 0;
            menu.m_142503_(there);
        }
        fire.m_6596_();
        CookingWatch watch = WATCHES.get(fire);
        if (watch != null) {
            watch.reset(spot);
        }
        BlockState state = fire.m_58900_();
        level.m_7260_(session.pos(), state, state, 3);
        menu.m_38946_();
        CampfireActions.sendProgress(player, session);
    }

    public static void quickMove(ServerPlayer player, ServerLevel level, Sessions.Session session, int inventorySlot) {
        BlockEntity blockEntity = level.m_7702_(session.pos());
        if (!(blockEntity instanceof CampfireBlockEntity)) {
            return;
        }
        CampfireBlockEntity fire = (CampfireBlockEntity)blockEntity;
        ItemStack source = player.m_150109_().m_8020_(inventorySlot);
        Optional recipe = level.m_7465_().m_44015_(RecipeType.f_44111_, (Container)new SimpleContainer(new ItemStack[]{source}), (Level)level);
        if (recipe.isEmpty()) {
            return;
        }
        int placed = 0;
        for (int spot = 0; spot < 4 && !source.m_41619_(); ++spot) {
            if (!((ItemStack)fire.m_59065_().get(spot)).m_41619_()) continue;
            fire.f_59044_[spot] = ((CampfireCookingRecipe)recipe.get()).m_43753_();
            fire.f_59043_[spot] = 0;
            fire.m_59065_().set(spot, (Object)source.m_41620_(1));
            CookingWatch watch = WATCHES.get(fire);
            if (watch != null) {
                watch.reset(spot);
            }
            ++placed;
        }
        if (placed == 0) {
            return;
        }
        player.m_150109_().m_6596_();
        level.m_220407_(GameEvent.f_157792_, session.pos(), GameEvent.Context.m_223719_((Entity)player, (BlockState)fire.m_58900_()));
        player.m_36220_(Stats.f_12975_);
        fire.m_6596_();
        BlockState state = fire.m_58900_();
        level.m_7260_(session.pos(), state, state, 3);
        player.f_36095_.m_38946_();
        CampfireActions.sendProgress(player, session);
    }

    public static void tick(ServerLevel level, Sessions.Session session) {
        BlockEntity blockEntity = level.m_7702_(session.pos());
        if (!(blockEntity instanceof CampfireBlockEntity)) {
            return;
        }
        CampfireBlockEntity fire = (CampfireBlockEntity)blockEntity;
        BlockState state = level.m_8055_(session.pos());
        if (!(state.m_60734_() instanceof CampfireBlock) || !((Boolean)state.m_61143_((Property)CampfireBlock.f_51227_)).booleanValue()) {
            return;
        }
        CookingWatch watch = WATCHES.computeIfAbsent(fire, unused -> new CookingWatch());
        long now = level.m_46467_();
        if (watch.lastTick == now) {
            return;
        }
        boolean consecutive = watch.lastTick == now - 1L;
        watch.lastTick = now;
        boolean changed = false;
        for (int i = 0; i < 4; ++i) {
            Optional recipe;
            ItemStack item = (ItemStack)fire.m_59065_().get(i);
            if (item.m_41619_()) {
                watch.reset(i);
                continue;
            }
            if (fire.f_59044_[i] <= 0 && (recipe = level.m_7465_().m_44015_(RecipeType.f_44111_, (Container)new SimpleContainer(new ItemStack[]{item}), (Level)level)).isPresent()) {
                fire.f_59044_[i] = ((CampfireCookingRecipe)recipe.get()).m_43753_();
                fire.f_59043_[i] = 0;
                watch.reset(i);
                changed = true;
            }
            if (!consecutive || watch.previous[i] != fire.f_59043_[i]) {
                watch.stalled[i] = 0;
            } else {
                int n = i;
                watch.stalled[n] = watch.stalled[n] + 1;
            }
            watch.previous[i] = fire.f_59043_[i];
            if (fire.f_59044_[i] <= 0 || watch.stalled[i] < 5) continue;
            CampfireActions.advanceStalledSlot(level, session.pos(), state, fire, watch, i);
            watch.previous[i] = fire.f_59043_[i];
            changed = true;
        }
        if (changed) {
            fire.m_6596_();
        }
    }

    private static void advanceStalledSlot(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity fire, CookingWatch watch, int spot) {
        SimpleContainer input;
        ItemStack result;
        ItemStack item = (ItemStack)fire.m_59065_().get(spot);
        int n = spot;
        fire.f_59043_[n] = fire.f_59043_[n] + 1;
        if (fire.f_59043_[spot] >= fire.f_59044_[spot] && (result = watch.recipes.m_213657_((Container)(input = new SimpleContainer(new ItemStack[]{item})), (Level)level).map(arg_0 -> CampfireActions.lambda$advanceStalledSlot$1((Container)input, level, arg_0)).orElse(item)).m_246617_(level.m_246046_())) {
            Containers.m_18992_((Level)level, (double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_(), (ItemStack)result);
            fire.m_59065_().set(spot, (Object)ItemStack.f_41583_);
            level.m_7260_(pos, state, state, 3);
            level.m_220407_(GameEvent.f_157792_, pos, GameEvent.Context.m_223722_((BlockState)state));
            watch.reset(spot);
        }
    }

    public static void sendProgress(ServerPlayer player, Sessions.Session session) {
        BlockEntity blockEntity = player.m_284548_().m_7702_(session.pos());
        if (!(blockEntity instanceof CampfireBlockEntity)) {
            return;
        }
        CampfireBlockEntity fire = (CampfireBlockEntity)blockEntity;
        int[] progress = new int[4];
        int[] total = new int[4];
        for (int i = 0; i < 4; ++i) {
            if (((ItemStack)fire.m_59065_().get(i)).m_41619_()) continue;
            progress[i] = fire.f_59043_[i];
            total[i] = fire.f_59044_[i];
        }
        Network.sendToPlayer(player, new CampfireProgressPayload(session.pos(), (Boolean)fire.m_58900_().m_61143_((Property)CampfireBlock.f_51227_), progress, total));
    }

    private static /* synthetic */ ItemStack lambda$advanceStalledSlot$1(Container input, ServerLevel level, CampfireCookingRecipe recipe) {
        return recipe.m_5874_(input, level.m_9598_());
    }

    private static final class CookingWatch {
        final int[] previous = new int[]{-1, -1, -1, -1};
        final int[] stalled = new int[4];
        final RecipeManager.CachedCheck<Container, CampfireCookingRecipe> recipes = RecipeManager.m_220267_((RecipeType)RecipeType.f_44111_);
        long lastTick = Long.MIN_VALUE;

        private CookingWatch() {
        }

        void reset(int spot) {
            this.previous[spot] = -1;
            this.stalled[spot] = 0;
        }
    }
}

