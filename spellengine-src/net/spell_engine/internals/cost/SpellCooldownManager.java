/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  net.minecraft.core.Holder
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.cost;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.spell_engine.Platform;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.network.Packets;
import org.jetbrains.annotations.Nullable;

public class SpellCooldownManager {
    private final Map<ResourceLocation, Entry> entries = Maps.newHashMap();
    private int tick;
    private final Entity owner;
    private static final String NBT_KEY = "spell_engine_cooldowns";

    public SpellCooldownManager(Entity owner) {
        this.owner = owner;
    }

    public void tickUpdate() {
        ++this.tick;
        this.update(true);
    }

    @Nullable
    private static ResourceLocation groupId(Spell spell) {
        String group;
        if (spell.cost.cooldown != null && (group = spell.cost.cooldown.group) != null) {
            return new ResourceLocation("group", group);
        }
        return null;
    }

    public boolean isCoolingDown(Holder<Spell> spell) {
        return this.getCooldownProgress(spell, 0.0f) > 0.0f;
    }

    @Deprecated
    protected boolean isCoolingDown(ResourceLocation spell) {
        return this.getCooldownProgress(spell, 0.0f) > 0.0f;
    }

    public float getCooldownProgress(Holder<Spell> spell, float tickDelta) {
        ResourceLocation id = ((ResourceKey)spell.m_203543_().get()).m_135782_();
        ResourceLocation groupId = SpellCooldownManager.groupId((Spell)spell.m_203334_());
        if (groupId == null) {
            return this.getCooldownProgress(id, tickDelta);
        }
        return Math.max(this.getCooldownProgress(id, tickDelta), this.getCooldownProgress(groupId, tickDelta));
    }

    protected float getCooldownProgress(ResourceLocation spell, float tickDelta) {
        Entry entry = this.entries.get(spell);
        if (entry != null) {
            float f = entry.endTick - entry.startTick;
            float g = (float)entry.endTick - ((float)this.tick + tickDelta);
            return Mth.m_14036_((float)(g / f), (float)0.0f, (float)1.0f);
        }
        return 0.0f;
    }

    public int getCooldownDuration(Holder<Spell> spell) {
        ResourceLocation id = ((ResourceKey)spell.m_203543_().get()).m_135782_();
        ResourceLocation groupId = SpellCooldownManager.groupId((Spell)spell.m_203334_());
        if (groupId == null) {
            return this.getCooldownDuration(id);
        }
        return Math.max(this.getCooldownDuration(id), this.getCooldownDuration(groupId));
    }

    protected int getCooldownDuration(ResourceLocation spell) {
        Entry entry = this.entries.get(spell);
        if (entry != null) {
            return entry.timeLeft(this.tick);
        }
        return 0;
    }

    public void setDurationLeft(Holder<Spell> spell, int duration) {
        ResourceLocation spellId = ((ResourceKey)spell.m_203543_().get()).m_135782_();
        this.setDurationLeft(spellId, duration);
        ResourceLocation groupId = SpellCooldownManager.groupId((Spell)spell.m_203334_());
        if (groupId != null) {
            this.setDurationLeft(groupId, duration);
        }
    }

    protected void setDurationLeft(ResourceLocation spell, int duration) {
        Entry existingEntry = this.entries.get(spell);
        if (existingEntry != null) {
            this.entries.put(spell, new Entry(this.tick, this.tick + duration));
        } else if (duration > 0) {
            this.entries.put(spell, new Entry(this.tick, this.tick + duration));
        }
    }

    public void update(boolean sync) {
        if (!this.entries.isEmpty()) {
            Iterator<Map.Entry<ResourceLocation, Entry>> iterator = this.entries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ResourceLocation, Entry> entry = iterator.next();
                if (entry.getValue().endTick > this.tick) continue;
                iterator.remove();
                if (!sync) continue;
                this.cooldownCleared(entry.getKey());
            }
        }
    }

    public void set(Holder<Spell> spell, int duration) {
        this.set(spell, duration, true);
    }

    public void set(Holder<Spell> spell, int duration, boolean force) {
        ResourceLocation spellId = ((ResourceKey)spell.m_203543_().get()).m_135782_();
        this.set(spellId, duration, force);
        ResourceLocation groupId = SpellCooldownManager.groupId((Spell)spell.m_203334_());
        if (groupId != null) {
            this.set(groupId, duration, force);
        }
    }

    protected void set(ResourceLocation spell, int duration, boolean force) {
        if (force || !this.entries.containsKey(spell) || this.entries.get(spell).timeLeft(this.tick) < duration) {
            this.entries.put(spell, new Entry(this.tick, this.tick + duration));
            this.cooldownSet(spell, duration);
        }
    }

    @Deprecated
    private void set(ResourceLocation spell, int duration) {
        this.set(spell, duration, true);
    }

    public void remove(ResourceLocation spell) {
        this.entries.remove(spell);
        this.cooldownCleared(spell);
    }

    protected void cooldownSet(ResourceLocation spell, int duration) {
        Entity entity = this.owner;
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            Platform.util().networkS2C_Send(serverPlayer, new Packets.SpellCooldown(spell, duration));
        }
    }

    protected void cooldownCleared(ResourceLocation spell) {
        Entity entity = this.owner;
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            Platform.util().networkS2C_Send(serverPlayer, new Packets.SpellCooldown(spell, 0));
        }
    }

    public void writeCustomDataToNbt(CompoundTag nbt) {
        CompoundTag cooldowns = new CompoundTag();
        for (Map.Entry<ResourceLocation, Entry> entry : this.entries.entrySet()) {
            ResourceLocation spell = entry.getKey();
            Entry cooldown = entry.getValue();
            CompoundTag cooldownData = new CompoundTag();
            cooldownData.m_128405_("start", cooldown.startTick - this.tick);
            cooldownData.m_128405_("end", cooldown.endTick - this.tick);
            cooldowns.m_128365_(spell.toString(), (Tag)cooldownData);
        }
        nbt.m_128365_(NBT_KEY, (Tag)cooldowns);
    }

    public void readCustomDataFromNbt(CompoundTag nbt) {
        CompoundTag cooldowns = nbt.m_128469_(NBT_KEY);
        for (String key : cooldowns.m_128431_()) {
            ResourceLocation spell = new ResourceLocation(key);
            CompoundTag cooldownData = cooldowns.m_128469_(key);
            int start = cooldownData.m_128451_("start");
            int end = cooldownData.m_128451_("end");
            this.entries.put(spell, new Entry(start, end));
        }
    }

    public void reset(@Nullable ResourceLocation spellId) {
        if (spellId == null) {
            this.entries.clear();
        } else {
            this.entries.remove(spellId);
        }
        this.pushSync();
    }

    public void pushSync() {
        Entity entity = this.owner;
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            Platform.util().networkS2C_Send(serverPlayer, new Packets.SpellCooldownSync(this.tick, Map.copyOf(this.entries)));
        }
    }

    public void acceptSync(int baseTick, Map<ResourceLocation, Entry> cooldowns) {
        this.tick = baseTick;
        this.entries.clear();
        this.entries.putAll(cooldowns);
    }

    public List<ResourceLocation> spellsOnCooldown() {
        return this.entries.keySet().stream().toList();
    }

    public record Entry(int startTick, int endTick) {
        int timeLeft(int currentTick) {
            return Math.max(0, this.endTick - currentTick);
        }
    }
}

