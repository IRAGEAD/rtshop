/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss;

import com.pla.spiral_abyss.config.GearConfig;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.init.CreativeTabInit;
import com.pla.spiral_abyss.init.EntityInit;
import com.pla.spiral_abyss.init.ItemInit;
import com.pla.spiral_abyss.init.MenuInit;
import com.pla.spiral_abyss.init.SoundInit;
import com.pla.spiral_abyss.network.AbyssNetwork;

public final class SpiralAbyss {
    public static final String MOD_ID = "spiral_abyss";

    public static void init() {
        GearConfig.prepare();
        EntityInit.ENTITY_TYPES.register();
        BlockInit.BLOCKS.register();
        BlockEntityInit.BLOCK_ENTITIES.register();
        ItemInit.ITEMS.register();
        CreativeTabInit.TABS.register();
        SoundInit.SOUNDS.register();
        MenuInit.MENUS.register();
        AbyssNetwork.register();
    }
}

