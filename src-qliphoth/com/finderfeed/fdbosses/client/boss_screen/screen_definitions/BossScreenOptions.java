/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.EntityType
 */
package com.finderfeed.fdbosses.client.boss_screen.screen_definitions;

import com.finderfeed.fdbosses.client.boss_screen.screen_definitions.BossInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;

public class BossScreenOptions {
    private List<BossInfo> skills = new ArrayList<BossInfo>();
    private Component TLDRComponent = null;
    private Component bossDescription = Component.m_237119_();
    private EntityType<?> entityType;

    public BossScreenOptions setBossDescription(Component bossDescription) {
        this.bossDescription = bossDescription;
        return this;
    }

    public BossScreenOptions setEntityType(EntityType<?> entityType) {
        this.entityType = entityType;
        return this;
    }

    public BossScreenOptions addSkill(BossInfo bossSkill) {
        this.skills.add(bossSkill);
        return this;
    }

    public BossScreenOptions setTLDRComponent(Component component) {
        this.TLDRComponent = component;
        return this;
    }

    public List<BossInfo> getSkills() {
        return this.skills;
    }

    public Component getBossDescription() {
        return this.bossDescription;
    }

    public EntityType<?> getEntityType() {
        return this.entityType;
    }

    public Component getTLDRComponent() {
        return this.TLDRComponent.m_6881_();
    }
}

