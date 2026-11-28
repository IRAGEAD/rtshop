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

public class ChesedConfig {
    @ConfigValue
    public int chesedMaxHits = 10;
    @ConfigValue
    @Comment(value="Defence from flight")
    public float electrifiedAirCurrentHealthDamagePercent = 33.0f;
    @ConfigValue
    @Comment(value="Immediately kills the player if HP is lower than value")
    public float electrifiedAirInstadeathHP = 4.0f;
    @ConfigValue
    public float electricSphereDamage = 10.0f;
    @ConfigValue
    public float rockfallRayDamage = 10.0f;
    @ConfigValue
    public float kineticFieldRayDamage = 10.0f;
    @ConfigValue
    public float rockfallRockDamage = 5.0f;
    @ConfigValue
    public float earthquakeRayDamage = 5.0f;
    @ConfigValue
    public float eartquakeDamage = 5.0f;
    @ConfigValue
    public float rollAttackDamage = 8.0f;
    @ConfigValue
    public float blockAttackDamage = 10.0f;
    @ConfigValue
    public float rayDamage = 10.0f;
    @ConfigValue
    public float fireTrailDamage = 2.0f;
    @ConfigValue
    public float finalAttackDamagePercentPerMonolith = 30.0f;
    @ConfigValue
    @Comment(value="If health is greater than value, sets the health equal to the value")
    public float healthAfterSecondPhaseTransition = 10.0f;
    @ConfigValue
    @Comment(value="Monolith hp gain when second phase starts")
    public float additionalSecondPhaseMonolithHP = 50.0f;
}

