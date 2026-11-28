/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.architectury.registry.registries.DeferredRegister
 *  dev.architectury.registry.registries.RegistrySupplier
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 */
package com.pla.spiral_abyss.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class SoundInit {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create((String)"spiral_abyss", (ResourceKey)Registries.f_256840_);
    public static final RegistrySupplier<SoundEvent> PORTAL_ENTRY = SOUNDS.register("portal.entry", () -> SoundEvent.m_262824_((ResourceLocation)new ResourceLocation("spiral_abyss", "portal.entry")));

    private SoundInit() {
    }
}

