/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.FDRegistries
 *  com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarFactory
 *  net.minecraft.resources.ResourceKey
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.RegistryObject
 */
package com.finderfeed.fdbosses.init;

import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBar;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahBossBar;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthBossBar;
import com.finderfeed.fdlib.systems.FDRegistries;
import com.finderfeed.fdlib.systems.hud.bossbars.FDBossBarFactory;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class BossBars {
    public static final DeferredRegister<FDBossBarFactory<?>> BOSS_BARS = DeferredRegister.create((ResourceKey)FDRegistries.FD_BOSS_BARS, (String)"fdbosses");
    public static final RegistryObject<FDBossBarFactory<?>> CHESED_BOSS_BAR = BOSS_BARS.register("chesed_boss_bar", () -> ChesedBossBar::new);
    public static final RegistryObject<FDBossBarFactory<?>> MALKUTH_BOSS_BAR = BOSS_BARS.register("malkuth_boss_bar", () -> MalkuthBossBar::new);
    public static final RegistryObject<FDBossBarFactory<?>> GEBURAG_BOSS_BAR = BOSS_BARS.register("geburah_boss_bar", () -> GeburahBossBar::new);
}

