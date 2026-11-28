/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.CraftingMenu
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.dyslexic.cinematic.mixin;

import dev.dyslexic.cinematic.table.TableActions;
import java.util.OptionalInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ServerPlayer.class})
public abstract class ServerPlayerMixin {
    @Inject(method={"openMenu"}, at={@At(value="RETURN")})
    private void cinematic$openedMenu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> result) {
        AbstractContainerMenu abstractContainerMenu;
        ServerPlayer player = (ServerPlayer)this;
        if (((OptionalInt)result.getReturnValue()).isPresent() && (abstractContainerMenu = player.f_36096_) instanceof CraftingMenu) {
            CraftingMenu menu = (CraftingMenu)abstractContainerMenu;
            TableActions.intoWindow(player, menu);
        }
    }
}

