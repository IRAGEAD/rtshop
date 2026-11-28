/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$DoubleValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.pla.spiral_abyss.config;

import java.util.EnumMap;
import java.util.Locale;
import net.minecraftforge.common.ForgeConfigSpec;

public final class AbyssClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue INVENTORY_BUTTON_X;
    public static final ForgeConfigSpec.IntValue INVENTORY_BUTTON_Y;
    public static final double MIN_HUD_SCALE = 0.5;
    public static final double MAX_HUD_SCALE = 2.5;
    private static final EnumMap<HudGroup, HudSettings> HUD;

    public static HudSettings hud(HudGroup group) {
        return HUD.get((Object)group);
    }

    private AbyssClientConfig() {
    }

    static {
        HUD = new EnumMap(HudGroup.class);
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(new String[]{"Position of the Abyss Menu button in survival and creative inventories.", "Coordinates use GUI-scaled pixels from the top-left corner of the screen.", "Changes apply immediately after the config reloads, including while the inventory is open.", "Positions are kept within the visible screen."}).push("inventory_button");
        INVENTORY_BUTTON_X = builder.comment("Horizontal position. Increase to move right.").defineInRange("x", 8, 0, Integer.MAX_VALUE);
        INVENTORY_BUTTON_Y = builder.comment("Vertical position. Increase to move down.").defineInRange("y", 8, 0, Integer.MAX_VALUE);
        builder.pop();
        builder.comment(new String[]{"Local Abyss overlay positions and sizes; never sent to the server.", "Anchors use the free screen space after accounting for each group's size,", "so edge placements remain visible after resizing or changing GUI scale."}).push("abyss_overlay");
        for (HudGroup group : HudGroup.values()) {
            builder.push(group.name().toLowerCase(Locale.ROOT));
            HUD.put(group, new HudSettings(builder));
            builder.pop();
        }
        builder.pop();
        SPEC = builder.build();
    }

    public static final class HudSettings {
        public final ForgeConfigSpec.DoubleValue x;
        public final ForgeConfigSpec.DoubleValue y;
        public final ForgeConfigSpec.DoubleValue scale;

        private HudSettings(ForgeConfigSpec.Builder builder) {
            this.x = builder.comment("Horizontal anchor: 0 is left, 1 is right, -1 retains the default placement.").defineInRange("x", -1.0, -1.0, 1.0);
            this.y = builder.comment("Vertical anchor: 0 is top, 1 is bottom, -1 retains the default placement.").defineInRange("y", -1.0, -1.0, 1.0);
            this.scale = builder.comment("Size multiplier relative to the original HUD.").defineInRange("scale", 1.0, 0.5, 2.5);
        }

        public void reset() {
            this.x.set((Object)((Double)this.x.getDefault()));
            this.y.set((Object)((Double)this.y.getDefault()));
            this.scale.set((Object)((Double)this.scale.getDefault()));
        }
    }

    public static enum HudGroup {
        TIMER,
        CHALLENGES,
        REJECTION;

    }
}

