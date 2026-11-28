/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Targeting
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package com.finderfeed.fdbosses.content.entities;

import com.finderfeed.fdbosses.content.entities.FDDespawnable;
import com.finderfeed.fdbosses.init.BossConfigs;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Targeting;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BossDespawner<T extends LivingEntity> {
    private T boss;
    private AABB bossEntitiesDespawnBox;
    private int bossNoTargetTime;
    private List<Class<? extends Entity>> entityTypesToDelete;
    private int currentNoTargetTime = 0;

    public BossDespawner(T boss, AABB entityDespawnBox, int bossNoTargetTime, Class<? extends Entity> ... entityTypesToDelete) {
        this.boss = boss;
        this.bossEntitiesDespawnBox = entityDespawnBox;
        this.bossNoTargetTime = bossNoTargetTime;
        this.entityTypesToDelete = List.of(entityTypesToDelete);
    }

    public void tick() {
        if (BossConfigs.BOSS_CONFIG.get().bossesDespawn && !this.boss.m_213877_()) {
            this.currentNoTargetTime = ((Targeting)this.boss).m_5448_() != null ? 0 : ++this.currentNoTargetTime;
            if (this.currentNoTargetTime > this.bossNoTargetTime) {
                this.removeAllEntities();
            }
        }
    }

    private void removeAllEntities() {
        FDDespawnable fdDespawnable;
        T t = this.boss;
        if (t instanceof FDDespawnable && !(fdDespawnable = (FDDespawnable)t).onFDDespawn()) {
            return;
        }
        this.boss.m_142467_(Entity.RemovalReason.DISCARDED);
        AABB box = this.bossEntitiesDespawnBox.m_82383_(this.boss.m_20182_());
        Level level = this.boss.m_9236_();
        List list = level.m_6443_(Entity.class, box, entity -> {
            Class<?> clazz = entity.getClass();
            return this.entityTypesToDelete.stream().anyMatch(e -> e.isAssignableFrom(clazz));
        });
        for (Entity entity2 : list) {
            entity2.m_142467_(Entity.RemovalReason.DISCARDED);
        }
    }

    public T getBoss() {
        return this.boss;
    }

    public AABB getBossEntitiesDespawnBox() {
        return this.bossEntitiesDespawnBox;
    }

    public int getBossNoTargetTime() {
        return this.bossNoTargetTime;
    }
}

