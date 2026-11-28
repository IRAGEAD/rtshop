/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.config.Comment
 *  com.finderfeed.fdlib.systems.config.ConfigValue
 */
package com.finderfeed.fdbosses.config;

import com.finderfeed.fdlib.systems.config.Comment;
import com.finderfeed.fdlib.systems.config.ConfigValue;

public class ItemConfig {
    @ConfigValue
    public float lightningStrikeDamagePercent = 100.0f;
    @ConfigValue
    public float chanceToSummonLightningStrike = 33.0f;
    @ConfigValue
    @Comment(value="Time in ticks")
    public int lightningStrikeShockDuration = 200;
    @ConfigValue
    public float playerMalkuthFireballToolDamagePercent = 200.0f;
    @ConfigValue
    public int playerMalkuthFireballAbilityCooldown = 100;
    @ConfigValue
    public int justiceCoreDamageReduction = 15;
    @ConfigValue
    public int phaseSphereUseDuration = 200;
    @ConfigValue
    public int phaseSphereCooldown = 100;
    @ConfigValue
    public int iceFireGauntletCooldown = 60;
    @ConfigValue
    public int iceFireGauntletCrushDamage = 15;
    @ConfigValue
    public int divineGearAttackCooldown = 60;
    @ConfigValue
    public int divineGearDamage = 25;
    @ConfigValue
    public int divineGearLifetime = 500;
    @ConfigValue
    public int divineGearCharges = 3;
    @ConfigValue
    public int divineGearChargeReplenishTime = 500;
    @ConfigValue
    public float divineGearAttackRadius = 25.0f;
}

