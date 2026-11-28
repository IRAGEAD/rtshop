/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.config.Comment
 *  com.finderfeed.fdlib.systems.config.ConfigValue
 *  com.finderfeed.fdlib.systems.config.ReflectiveJsonConfig
 */
package com.finderfeed.fdbosses.config;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdlib.systems.config.Comment;
import com.finderfeed.fdlib.systems.config.ConfigValue;
import com.finderfeed.fdlib.systems.config.ReflectiveJsonConfig;

public class BossClientConfig
extends ReflectiveJsonConfig {
    @ConfigValue
    @Comment(value="Decreases the amount of particles that some attacks spawn")
    public boolean lessParticles = false;
    @ConfigValue
    @Comment(value="Disable/Enable phase sphere visual effect")
    public boolean phaseSphereVisualEffect = true;

    public BossClientConfig() {
        super(FDBosses.location("bosses_clientside"));
    }

    public boolean isClientside() {
        return true;
    }
}

