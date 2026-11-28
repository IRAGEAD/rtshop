/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.pla.spiral_abyss.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public final class AbyssConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue NO_DROP_REWARD_CHESTS;
    public static final ForgeConfigSpec.BooleanValue MIX_MOBS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOB_BLACKLIST;
    public static final ForgeConfigSpec.IntValue RESET_DAYS;
    public static final ForgeConfigSpec.BooleanValue BROADCAST_RARE_LOOT;
    public static final ForgeConfigSpec.DoubleValue RARE_REWARD_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ABYSSAL_REJECTION;

    private AbyssConfig() {
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        NO_DROP_REWARD_CHESTS = builder.comment(new String[]{"Open normal reward chests as a shared loot container instead of dropping items.", "Does not affect Endless experience chests. Opened payouts retain their delivery mode."}).define("no_drop_reward_chests", false);
        ABYSSAL_REJECTION = builder.comment(new String[]{"Reduce a player's damage to floor 5/6 bosses by 60% until they defeat that species outside the Abyss.", "Preserves normal boss progression. Disable for packs that intentionally allow Abyss-first boss encounters."}).define("abyssal_rejection", true);
        RESET_DAYS = builder.comment(new String[]{"Server-wide reset interval in real-world days, including server downtime.", "0 disables automatic resets. Personal resets never change this schedule or the cycle's mob choices."}).defineInRange("reset_days", 20, 0, 36500);
        MIX_MOBS = builder.comment(new String[]{"False: pick one entity ID per category per wave and repeat it for the count.", "True: pick an entity ID independently for every spawn, including across mods."}).define("mix_mobs", false);
        MOB_BLACKLIST = builder.comment(new String[]{"Entity IDs excluded from all Abyss wave selections, including bosses and mini-bosses in normal and Endless modes.", "Example: [\"minecraft:wither\", \"annoyingvillagers:herobrine_clone\"]. Empty allows all configured mobs."}).defineListAllowEmpty(List.of("mob_blacklist"), () -> List.of(), value -> {
            String id;
            return value instanceof String && ResourceLocation.m_135820_((String)(id = (String)value)) != null && id.equals(ResourceLocation.m_135820_((String)id).toString());
        });
        BROADCAST_RARE_LOOT = builder.comment("Announce successful pickups of rare gold-trail Abyss rewards to every online player.").define("broadcast_rare_loot", true);
        RARE_REWARD_CHANCE = builder.comment("Base rare reward probability per eligible player and mob-linked item roll in a Golden Chest.").defineInRange("rare_reward_chance", 0.05, 0.0, 1.0);
        SPEC = builder.build();
    }
}

