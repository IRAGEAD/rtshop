/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public abstract class ArenaInteractable
extends Mob {
    private UUID arenaHost;

    protected ArenaInteractable(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.m_20242_(true);
        this.m_21530_();
    }

    public UUID arenaHost() {
        return this.arenaHost;
    }

    public void setArenaHost(UUID host) {
        this.arenaHost = host;
    }

    public void m_8119_() {
        this.m_20256_(Vec3.f_82478_);
        super.m_8119_();
        this.m_20256_(Vec3.f_82478_);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        if (this.arenaHost != null) {
            tag.m_128362_("ArenaHost", this.arenaHost);
        }
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.arenaHost = tag.m_128403_("ArenaHost") ? tag.m_128342_("ArenaHost") : null;
        this.m_6842_(false);
        this.m_20242_(true);
        this.m_21530_();
    }

    public boolean m_6469_(DamageSource source, float amount) {
        return false;
    }

    public void m_6667_(DamageSource source) {
    }

    public boolean m_6673_(DamageSource source) {
        return true;
    }

    public boolean m_6097_() {
        return false;
    }

    public boolean m_271807_() {
        return false;
    }

    public boolean m_6094_() {
        return false;
    }

    public boolean m_21525_() {
        return true;
    }

    public boolean m_6785_(double distance) {
        return false;
    }

    public boolean m_142066_() {
        return false;
    }

    public void m_147240_(double strength, double x, double z) {
    }

    protected void m_7324_(Entity entity) {
    }
}

