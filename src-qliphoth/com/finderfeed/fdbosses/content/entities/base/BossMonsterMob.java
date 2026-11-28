/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimatedObject
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.ModelSystem
 *  com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.entity_model_system.EntityModelSystem
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.level.Level
 */
package com.finderfeed.fdbosses.content.entities.base;

import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.AnimatedObject;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.ModelSystem;
import com.finderfeed.fdlib.systems.bedrock.animations.animation_system.model_system.entity_model_system.EntityModelSystem;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class BossMonsterMob
extends Monster
implements AnimatedObject {
    private EntityModelSystem<?> modelSystem = EntityModelSystem.create((Entity)this);

    public BossMonsterMob(EntityType<? extends Monster> etype, Level level) {
        super(etype, level);
    }

    public void m_8119_() {
        super.m_8119_();
        this.tickModelSystem();
    }

    public ModelSystem getModelSystem() {
        return this.modelSystem;
    }

    public void m_6457_(ServerPlayer player) {
        super.m_6457_(player);
        this.modelSystem.asServerside().syncToPlayer(player);
    }

    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.modelSystem.saveAttachments((HolderLookup.Provider)this.m_9236_().m_9598_(), tag);
    }

    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.modelSystem.loadAttachments((HolderLookup.Provider)this.m_9236_().m_9598_(), tag);
    }
}

