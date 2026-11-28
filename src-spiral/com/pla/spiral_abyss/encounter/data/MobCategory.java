/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter.data;

public enum MobCategory {
    FODDER_MOB("fodder_mob"),
    COMMON_MOB("common_mob"),
    CHAMPION_MOB("champion_mob"),
    MINI_BOSS("mini_boss"),
    BOSS("boss");

    private final String key;

    private MobCategory(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }
}

