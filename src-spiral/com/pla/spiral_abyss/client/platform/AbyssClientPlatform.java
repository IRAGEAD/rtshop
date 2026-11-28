/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.injectables.annotations.ExpectPlatform
 *  dev.architectury.injectables.annotations.ExpectPlatform$Transformed
 *  net.minecraft.client.KeyMapping
 */
package com.pla.spiral_abyss.client.platform;

import com.pla.spiral_abyss.client.platform.forge.AbyssClientPlatformImpl;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.KeyMapping;

public final class AbyssClientPlatform {
    @ExpectPlatform
    @ExpectPlatform.Transformed
    public static KeyMapping journalKey() {
        return AbyssClientPlatformImpl.journalKey();
    }
}

