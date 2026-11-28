/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance$Attenuation
 *  net.minecraft.client.resources.sounds.TickableSoundInstance
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.entity.LivingEntity
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.client.sound;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.internals.casting.SpellCaster;
import org.jetbrains.annotations.Nullable;

public class SpellCastingSound
extends SimpleSoundInstance
implements SoundInstance,
TickableSoundInstance {
    private LivingEntity emitter;
    private boolean done;
    @Nullable
    public Listener listener;

    public SpellCastingSound(LivingEntity emitter, ResourceLocation id, float volume, float pitch) {
        super(id, SoundSource.PLAYERS, volume, pitch, SoundInstance.m_235150_(), true, 0, SoundInstance.Attenuation.LINEAR, emitter.m_20185_(), emitter.m_20186_(), emitter.m_20189_(), false);
        this.emitter = emitter;
    }

    private boolean isEmitterCasting() {
        SpellCaster.Entity caster;
        LivingEntity livingEntity;
        return this.emitter != null && this.emitter.m_6084_() && (livingEntity = this.emitter) instanceof SpellCaster.Entity && (caster = (SpellCaster.Entity)livingEntity).isCastingSpell();
    }

    public boolean m_7801_() {
        return this.done;
    }

    protected final void setDone() {
        this.done = true;
        this.f_119578_ = false;
        this.f_119573_ = 0.0f;
        if (this.listener != null) {
            this.listener.onSpellCastingSoundDone();
        }
    }

    public void m_7788_() {
        if (!this.isEmitterCasting()) {
            this.setDone();
        }
    }

    public double m_7772_() {
        return this.emitter.m_20185_();
    }

    public double m_7780_() {
        return this.emitter.m_20186_();
    }

    public double m_7778_() {
        return this.emitter.m_20189_();
    }

    public static interface Listener {
        public void onSpellCastingSoundDone();
    }
}

