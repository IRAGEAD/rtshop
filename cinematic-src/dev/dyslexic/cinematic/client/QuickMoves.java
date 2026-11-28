/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.AbstractFurnaceMenu
 *  net.minecraft.world.inventory.BlastFurnaceMenu
 *  net.minecraft.world.inventory.CartographyTableMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.EnchantmentMenu
 *  net.minecraft.world.inventory.GrindstoneMenu
 *  net.minecraft.world.inventory.LoomMenu
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.inventory.SmokerMenu
 *  net.minecraft.world.inventory.StonecutterMenu
 *  net.minecraft.world.item.BannerItem
 *  net.minecraft.world.item.BannerPatternItem
 *  net.minecraft.world.item.DyeItem
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.common.ForgeHooks
 */
package dev.dyslexic.cinematic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

final class QuickMoves {
    private QuickMoves() {
    }

    static boolean entersStation(AbstractContainerMenu menu, int index, Level level) {
        if (index < 0 || index >= menu.f_38839_.size()) {
            return false;
        }
        ItemStack stack = menu.m_38853_(index).m_7993_();
        if (stack.m_41619_()) {
            return false;
        }
        if (menu instanceof AbstractFurnaceMenu) {
            RecipeType type;
            RecipeType recipeType = menu instanceof SmokerMenu ? RecipeType.f_44110_ : (type = menu instanceof BlastFurnaceMenu ? RecipeType.f_44109_ : RecipeType.f_44108_);
            if (QuickMoves.smelts(menu, stack, level)) {
                return QuickMoves.fits(menu, 0, stack);
            }
            return ForgeHooks.getBurnTime((ItemStack)stack, (RecipeType)type) > 0 && QuickMoves.fits(menu, 1, stack);
        }
        if (menu instanceof StonecutterMenu) {
            return level.m_7465_().m_44015_(RecipeType.f_44112_, (Container)new SimpleContainer(new ItemStack[]{stack}), level).isPresent() && QuickMoves.fits(menu, 0, stack);
        }
        if (menu instanceof LoomMenu) {
            return stack.m_41720_() instanceof BannerItem ? QuickMoves.fits(menu, 0, stack) : (stack.m_41720_() instanceof DyeItem ? QuickMoves.fits(menu, 1, stack) : stack.m_41720_() instanceof BannerPatternItem && QuickMoves.fits(menu, 2, stack));
        }
        if (menu instanceof CartographyTableMenu) {
            return stack.m_150930_(Items.f_42573_) ? QuickMoves.fits(menu, 0, stack) : (stack.m_150930_(Items.f_42516_) || stack.m_150930_(Items.f_42676_) || stack.m_150930_(Items.f_42027_)) && QuickMoves.fits(menu, 1, stack);
        }
        if (menu instanceof GrindstoneMenu) {
            return !(menu.m_38853_(0).m_6657_() && menu.m_38853_(1).m_6657_() || !QuickMoves.fits(menu, 0, stack) && !QuickMoves.fits(menu, 1, stack));
        }
        if (menu instanceof EnchantmentMenu) {
            return stack.m_150930_(Items.f_42534_) ? QuickMoves.fits(menu, 1, stack) : !menu.m_38853_(0).m_6657_() && menu.m_38853_(0).m_5857_(stack);
        }
        for (int i = 0; i < menu.f_38839_.size(); ++i) {
            if (((Slot)menu.f_38839_.get((int)i)).f_40218_ == Minecraft.m_91087_().f_91074_.m_150109_() || !QuickMoves.fits(menu, i, stack)) continue;
            return true;
        }
        return false;
    }

    static void sendVanilla(Minecraft mc, AbstractContainerMenu menu, int index) {
        if (index < 0 || index >= menu.f_38839_.size() || mc.f_91072_ == null || mc.f_91074_ == null) {
            return;
        }
        if (QuickMoves.entersStation(menu, index, (Level)mc.f_91073_)) {
            mc.f_91072_.m_171799_(menu.f_38840_, index, 0, ClickType.QUICK_MOVE, (Player)mc.f_91074_);
        }
    }

    private static boolean smelts(AbstractContainerMenu menu, ItemStack stack, Level level) {
        RecipeType type;
        RecipeType cooking = type = menu instanceof SmokerMenu ? RecipeType.f_44110_ : (menu instanceof BlastFurnaceMenu ? RecipeType.f_44109_ : RecipeType.f_44108_);
        return level.m_7465_().m_44015_(cooking, (Container)new SimpleContainer(new ItemStack[]{stack}), level).isPresent();
    }

    private static boolean fits(AbstractContainerMenu menu, int index, ItemStack stack) {
        Slot slot = menu.m_38853_(index);
        if (!slot.m_5857_(stack)) {
            return false;
        }
        ItemStack there = slot.m_7993_();
        return there.m_41619_() || ItemStack.m_150942_((ItemStack)there, (ItemStack)stack) && there.m_41613_() < slot.m_5866_(stack);
    }
}

