/*
 * Decompiled with CFR 0.152.
 */
package com.pla.spiral_abyss.encounter.data;

import java.util.Locale;

public enum CombatRole {
    ANY,
    MELEE,
    RANGED;


    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}

