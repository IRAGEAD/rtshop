/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.FDRegistries
 *  com.finderfeed.fdlib.systems.config.JsonConfig
 *  net.minecraft.resources.ResourceKey
 *  net.minecraftforge.registries.DeferredRegister
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.config.BossClientConfig;
import com.finderfeed.fdbosses.config.BossConfig;
import com.finderfeed.fdlib.systems.FDRegistries;
import com.finderfeed.fdlib.systems.config.JsonConfig;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;

public class BossConfigs {
    public static final DeferredRegister<JsonConfig> CONFIGS = DeferredRegister.create((ResourceKey)FDRegistries.CONFIGS_KEY, (String)"fdbosses");
    public static final Supplier<BossConfig> BOSS_CONFIG = CONFIGS.register("bosses", BossConfig::new);
    public static final Supplier<BossClientConfig> BOSS_CONFIG_CLIENT = CONFIGS.register("bosses_clientside", BossClientConfig::new);
}

