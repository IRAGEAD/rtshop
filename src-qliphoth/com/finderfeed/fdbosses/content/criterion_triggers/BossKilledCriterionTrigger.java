/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance
 *  net.minecraft.advancements.critereon.ContextAwarePredicate
 *  net.minecraft.advancements.critereon.DeserializationContext
 *  net.minecraft.advancements.critereon.SerializationContext
 *  net.minecraft.advancements.critereon.SimpleCriterionTrigger
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.content.criterion_triggers;

import com.finderfeed.fdbosses.FDBosses;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public class BossKilledCriterionTrigger
extends SimpleCriterionTrigger<Instance> {
    private ResourceLocation id = FDBosses.location("boss_killed");

    public void trigger(ServerPlayer player, EntityType<?> bossEntityType) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(bossEntityType);
        this.m_66234_(player, inst -> inst.bossEntityType.equals(key.toString()));
    }

    public void trigger(ServerPlayer player, Entity entity) {
        this.trigger(player, entity.m_6095_());
    }

    protected Instance createInstance(JsonObject json, ContextAwarePredicate predicate, DeserializationContext ctx) {
        return new Instance(this.m_7295_(), predicate, json.get("boss_entity_type").getAsString());
    }

    public ResourceLocation m_7295_() {
        return this.id;
    }

    public static class Instance
    extends AbstractCriterionTriggerInstance {
        private String bossEntityType;

        public Instance(ResourceLocation location, ContextAwarePredicate predicate, String bossEntityType) {
            super(location, predicate);
            this.bossEntityType = bossEntityType;
        }

        public JsonObject m_7683_(SerializationContext ctx) {
            JsonObject jsonObject = super.m_7683_(ctx);
            jsonObject.addProperty("boss_entity_type", this.bossEntityType);
            return jsonObject;
        }
    }
}

