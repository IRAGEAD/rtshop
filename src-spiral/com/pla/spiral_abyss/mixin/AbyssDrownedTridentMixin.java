/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.monster.Drowned
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 */
package com.pla.spiral_abyss.mixin;

import com.pla.spiral_abyss.platform.AbyssPlatform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value={Drowned.class})
public abstract class AbyssDrownedTridentMixin {
    @ModifyArg(method={"performRangedAttack"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/projectile/ThrownTrident;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"), index=2)
    private ItemStack spiralAbyss$copyHeldTrident(ItemStack vanilla) {
        Drowned drowned = (Drowned)this;
        return AbyssPlatform.data((Entity)drowned).m_128403_("spiral_abyss_run") && drowned.m_21205_().m_150930_(Items.f_42713_) ? drowned.m_21205_().m_41777_() : vanilla;
    }
}

