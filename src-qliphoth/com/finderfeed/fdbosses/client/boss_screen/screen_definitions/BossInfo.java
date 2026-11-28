/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package com.finderfeed.fdbosses.client.boss_screen.screen_definitions;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class BossInfo {
    private Either<ResourceLocation, ItemStack> infoIcon;
    private Component infoName;
    private Component infoStats;
    private Component infoDescription;

    public BossInfo(ResourceLocation skillIcon, Component skillName, Component skillStats, Component skillDescription) {
        this.infoIcon = Either.left((Object)skillIcon);
        this.infoName = skillName;
        this.infoStats = skillStats;
        this.infoDescription = skillDescription;
    }

    public BossInfo(ItemStack skillIcon, Component skillStats, Component skillDescription) {
        this.infoIcon = Either.right((Object)skillIcon);
        this.infoName = skillIcon.m_41786_();
        this.infoStats = skillStats;
        this.infoDescription = skillDescription;
    }

    public Component getInfoStats() {
        return this.infoStats;
    }

    public Component getInfoDescription() {
        return this.infoDescription;
    }

    public Component getInfoName() {
        return this.infoName;
    }

    public Either<ResourceLocation, ItemStack> getInfoIcon() {
        return this.infoIcon;
    }
}

