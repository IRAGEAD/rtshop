/*
 * Decompiled with CFR 0.152.
 */
package com.finderfeed.fdbosses.content.data_components;

import com.finderfeed.fdbosses.content.items.CoreItem;
import com.finderfeed.fdbosses.init.BossItems;
import java.util.function.Supplier;

public class ItemCoreDataComponent {

    public static enum CoreType {
        LIGHTNING(BossItems.LIGHTNING_CORE, 0x11FFFF),
        FIRE_AND_ICE(BossItems.FIRE_AND_ICE_CORE, 0xFFAA11),
        JUSTICE_CORE(BossItems.JUSTICE_CORE, 0x11AAFF);

        private Supplier<CoreItem> item;
        private int textColor;

        private CoreType(Supplier<CoreItem> item, int textColor) {
            this.item = item;
            this.textColor = textColor;
        }

        public int getTextColor() {
            return this.textColor;
        }

        public CoreItem getItem() {
            return this.item.get();
        }
    }
}

