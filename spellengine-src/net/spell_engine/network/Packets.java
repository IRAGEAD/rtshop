/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.netty.buffer.Unpooled
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.phys.Vec3
 */
package net.spell_engine.network;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.config.ServerConfig;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.cost.SpellCooldownManager;
import net.spell_engine.internals.delivery.melee.Melee;

public class Packets {
    private static void writeTargetSnapshot(FriendlyByteBuf buffer, SpellCast.TargetSnapshot snapshot) {
        buffer.m_130089_(snapshot.entityIds().stream().mapToInt(Integer::intValue).toArray());
        Vec3 location = snapshot.location();
        if (location != null) {
            buffer.writeBoolean(true);
            buffer.writeDouble(location.f_82479_);
            buffer.writeDouble(location.f_82480_);
            buffer.writeDouble(location.f_82481_);
        } else {
            buffer.writeBoolean(false);
        }
    }

    private static SpellCast.TargetSnapshot readTargetSnapshot(FriendlyByteBuf buffer) {
        List<Integer> entityIds = Arrays.stream(buffer.m_130100_()).boxed().toList();
        Vec3 location = null;
        if (buffer.readBoolean()) {
            location = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }
        return new SpellCast.TargetSnapshot(entityIds, location);
    }

    public record AttackFxBroadcast(Melee.AttackContext attackContext) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "attack_fx_broadcast");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.attackContext.spellId().toString());
            buffer.m_130070_(this.attackContext.attackId());
            buffer.writeFloat(this.attackContext.charge());
        }

        public static AttackFxBroadcast read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            String attackId = buffer.m_130277_();
            float charge = buffer.readFloat();
            return new AttackFxBroadcast(new Melee.AttackContext(spellId, attackId, charge));
        }
    }

    public record AttackPerform(Melee.AttackContext attackContext, int[] targetIds) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "attack_perform");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.attackContext.spellId().toString());
            buffer.m_130070_(this.attackContext.attackId());
            buffer.writeFloat(this.attackContext.charge());
            buffer.m_130089_(this.targetIds);
        }

        public static AttackPerform read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            String attackId = buffer.m_130277_();
            float charge = buffer.readFloat();
            Melee.AttackContext context = new Melee.AttackContext(spellId, attackId, charge);
            int[] targetIds = buffer.m_130100_();
            return new AttackPerform(context, targetIds);
        }
    }

    public record AttackAvailable(ResourceLocation spellId, List<Melee.Attack> attacks) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "attack_available");
        private static final Gson gson = new Gson();

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.spellId.toString());
            buffer.writeInt(this.attacks.size());
            for (Melee.Attack attack : this.attacks) {
                String attackJson = gson.toJson((Object)attack);
                buffer.m_130070_(attackJson);
            }
        }

        public static AttackAvailable read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            int attackCount = buffer.readInt();
            ArrayList<Melee.Attack> attacks = new ArrayList<Melee.Attack>();
            for (int i = 0; i < attackCount; ++i) {
                String attackJson = buffer.m_130277_();
                Melee.Attack attack = (Melee.Attack)gson.fromJson(attackJson, Melee.Attack.class);
                attacks.add(attack);
            }
            return new AttackAvailable(spellId, attacks);
        }
    }

    public record SpellRegistrySync(List<String> chunks) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_registry_sync");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(this.chunks.size());
            for (String chunk : this.chunks) {
                buffer.m_130070_(chunk);
            }
        }

        public static SpellRegistrySync read(FriendlyByteBuf buffer) {
            int chunkCount = buffer.readInt();
            ArrayList<String> chunks = new ArrayList<String>();
            for (int i = 0; i < chunkCount; ++i) {
                chunks.add(buffer.m_130277_());
            }
            return new SpellRegistrySync(chunks);
        }
    }

    public record ConfigSync(ServerConfig config) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "config_sync");
        private static final Gson gson = new Gson();

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            String json = gson.toJson((Object)this.config);
            buffer.m_130070_(json);
        }

        public static ConfigSync read(FriendlyByteBuf buffer) {
            Gson gson = new Gson();
            String json = buffer.m_130277_();
            ServerConfig config = (ServerConfig)gson.fromJson(json, ServerConfig.class);
            return new ConfigSync(config);
        }
    }

    public record SpellContainerSync(LinkedHashMap<String, SpellContainer> containers) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_container_sync");
        private static final Gson gson = new Gson();

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(this.containers.size());
            for (Map.Entry<String, SpellContainer> entry : this.containers.entrySet()) {
                buffer.m_130070_(entry.getKey());
                String json = gson.toJson((Object)entry.getValue());
                buffer.m_130070_(json);
            }
        }

        public static SpellContainerSync read(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            LinkedHashMap<String, SpellContainer> containers = new LinkedHashMap<String, SpellContainer>();
            for (int i = 0; i < size; ++i) {
                String key = buffer.m_130277_();
                String json = buffer.m_130277_();
                SpellContainer container = (SpellContainer)gson.fromJson(json, SpellContainer.class);
                containers.put(key, container);
            }
            return new SpellContainerSync(containers);
        }
    }

    public record ParticleEffects(SourceType sourceType, float countMultiplier, List<Spawn> spawns) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "particle_effects");
        private static final Gson gson = new Gson();

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(this.sourceType.ordinal());
            buffer.writeFloat(this.countMultiplier);
            buffer.writeInt(this.spawns.size());
            for (Spawn spawn : this.spawns) {
                buffer.writeInt(spawn.sourceEntityId);
                buffer.writeFloat(spawn.yaw);
                buffer.writeFloat(spawn.pitch);
                buffer.writeDouble(spawn.sourceLocation.f_82479_);
                buffer.writeDouble(spawn.sourceLocation.f_82480_);
                buffer.writeDouble(spawn.sourceLocation.f_82481_);
                buffer.m_130070_(gson.toJson((Object)spawn.effect));
            }
        }

        public static ParticleEffects read(FriendlyByteBuf buffer) {
            SourceType sourceType = SourceType.values()[buffer.readInt()];
            float countMultiplier = buffer.readFloat();
            int spawnCount = buffer.readInt();
            ArrayList<Spawn> spawns = new ArrayList<Spawn>();
            for (int i = 0; i < spawnCount; ++i) {
                spawns.add(new Spawn(buffer.readInt(), buffer.readFloat(), buffer.readFloat(), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), (ParticleGroup)gson.fromJson(buffer.m_130277_(), ParticleGroup.class)));
            }
            return new ParticleEffects(sourceType, countMultiplier, spawns);
        }

        public static enum SourceType {
            ENTITY,
            COORDINATE;

        }

        public record Spawn(int sourceEntityId, float yaw, float pitch, Vec3 sourceLocation, ParticleGroup effect) {
        }
    }

    public record SpellMessage(String translationKey, ChatFormatting format) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_message");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.translationKey);
            buffer.writeInt(this.format.ordinal());
        }

        public static SpellMessage read(FriendlyByteBuf buffer) {
            String text = buffer.m_130277_();
            ChatFormatting format = ChatFormatting.values()[buffer.readInt()];
            return new SpellMessage(text, format);
        }
    }

    public record SpellAnimation(int playerId, SpellCast.Animation type, String name, float speed) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_animation");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(this.playerId);
            buffer.writeInt(this.type.ordinal());
            buffer.m_130070_(this.name);
            buffer.writeFloat(this.speed);
        }

        public static SpellAnimation read(FriendlyByteBuf buffer) {
            int playerId = buffer.readInt();
            SpellCast.Animation type = SpellCast.Animation.values()[buffer.readInt()];
            String name = buffer.m_130277_();
            float speed = buffer.readFloat();
            return new SpellAnimation(playerId, type, name, speed);
        }
    }

    public record SpellCooldownSync(int baseTick, Map<ResourceLocation, SpellCooldownManager.Entry> cooldowns) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "cooldown_sync");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeInt(this.baseTick);
            buffer.writeInt(this.cooldowns.size());
            for (Map.Entry<ResourceLocation, SpellCooldownManager.Entry> entry : this.cooldowns.entrySet()) {
                buffer.m_130070_(entry.getKey().toString());
                buffer.writeInt(entry.getValue().startTick());
                buffer.writeInt(entry.getValue().endTick());
            }
        }

        public static SpellCooldownSync read(FriendlyByteBuf buffer) {
            int baseTick = buffer.readInt();
            int size = buffer.readInt();
            HashMap<ResourceLocation, SpellCooldownManager.Entry> cooldowns = new HashMap<ResourceLocation, SpellCooldownManager.Entry>();
            for (int i = 0; i < size; ++i) {
                ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
                int startTick = buffer.readInt();
                int endTick = buffer.readInt();
                cooldowns.put(spellId, new SpellCooldownManager.Entry(startTick, endTick));
            }
            return new SpellCooldownSync(baseTick, cooldowns);
        }
    }

    public record SpellCooldown(ResourceLocation spellId, int duration) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "spell_cooldown");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.spellId.toString());
            buffer.writeInt(this.duration);
        }

        public static SpellCooldown read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            int duration = buffer.readInt();
            return new SpellCooldown(spellId, duration);
        }
    }

    public record CastInput(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "cast_input");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.spellId.toString());
            Packets.writeTargetSnapshot(buffer, this.snapshot);
        }

        public static CastInput read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            return new CastInput(spellId, Packets.readTargetSnapshot(buffer));
        }
    }

    public record TargetStream(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "target_stream");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.spellId.toString());
            Packets.writeTargetSnapshot(buffer, this.snapshot);
        }

        public static TargetStream read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            SpellCast.TargetSnapshot snapshot = Packets.readTargetSnapshot(buffer);
            return new TargetStream(spellId, snapshot);
        }
    }

    public record CastRequest(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) implements Payload
    {
        public static final ResourceLocation ID = new ResourceLocation("spell_engine", "cast_request");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.m_130070_(this.spellId.toString());
            Packets.writeTargetSnapshot(buffer, this.snapshot);
        }

        public static CastRequest read(FriendlyByteBuf buffer) {
            ResourceLocation spellId = new ResourceLocation(buffer.m_130277_());
            return new CastRequest(spellId, Packets.readTargetSnapshot(buffer));
        }
    }

    public static interface Payload {
        public ResourceLocation id();

        public void write(FriendlyByteBuf var1);

        default public FriendlyByteBuf toBuffer() {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            this.write(buffer);
            return buffer;
        }
    }
}

