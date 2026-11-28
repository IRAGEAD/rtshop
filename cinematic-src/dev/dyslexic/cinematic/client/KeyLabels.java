/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.network.chat.Component
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

final class KeyLabels {
    private KeyLabels() {
    }

    static Component of(KeyMapping mapping) {
        InputConstants.Key key = mapping.getKey();
        int code = key.m_84873_();
        if (key.m_84868_() == InputConstants.Type.KEYSYM && code >= 65 && code <= 90) {
            return Component.m_237113_((String)String.valueOf((char)code));
        }
        return mapping.m_90863_();
    }
}

