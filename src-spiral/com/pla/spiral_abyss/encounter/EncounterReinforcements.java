/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.MobCategory
 *  net.minecraft.world.entity.OwnableEntity
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.monster.Enemy
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter;

import com.pla.spiral_abyss.encounter.WaveAllies;
import com.pla.spiral_abyss.encounter.WaveScaling;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.world.ArenaSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EncounterReinforcements {
    public static final String PHASE_BOSS = "spiral_abyss_phase_boss";
    private static final TagKey<EntityType<?>> OWNER_GETTER = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "reinforcement_owner_getter"));
    private static final TagKey<EntityType<?>> SHOOTER_FIELD = TagKey.m_203882_((ResourceKey)Registries.f_256939_, (ResourceLocation)new ResourceLocation("spiral_abyss", "reinforcement_shooter_field"));
    private static final ClassValue<Optional<Method>> OWNER_METHODS = new ClassValue<Optional<Method>>(){

        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                Method method = type.getMethod("getOwner", new Class[0]);
                return Entity.class.isAssignableFrom(method.getReturnType()) ? Optional.of(method) : Optional.empty();
            }
            catch (ReflectiveOperationException | SecurityException ex) {
                return Optional.empty();
            }
        }
    };
    private static final ClassValue<Optional<Field>> SHOOTER_FIELDS = new ClassValue<Optional<Field>>(){

        @Override
        protected Optional<Field> computeValue(Class<?> type) {
            try {
                Field field = type.getField("shooter");
                return Entity.class.isAssignableFrom(field.getType()) ? Optional.of(field) : Optional.empty();
            }
            catch (ReflectiveOperationException | SecurityException ex) {
                return Optional.empty();
            }
        }
    };
    private final ArenaSession session;
    private final Map<ResourceLocation, LivingEntity> phases = new HashMap<ResourceLocation, LivingEntity>();
    private final Set<UUID> roots = new HashSet<UUID>();

    public EncounterReinforcements(ArenaSession session) {
        this.session = session;
    }

    public void begin(List<LivingEntity> initial) {
        this.clear();
        for (LivingEntity root : initial) {
            Set<ResourceLocation> ids = MobCatalog.bossOptions(EntityType.m_20613_((EntityType)root.m_6095_())).phaseMobs();
            if (!ids.isEmpty()) {
                this.roots.add(root.m_20148_());
            }
            for (ResourceLocation id : ids) {
                this.phases.put(id, root);
            }
        }
    }

    public boolean retainsEnvironment(LivingEntity root) {
        return this.roots.contains(root.m_20148_());
    }

    public void discover(UUID run, Consumer<Mob> accept) {
        Vec3 center = this.session.bounds().center();
        AABB bounds = new AABB(center.f_82479_ - 128.0, (double)this.session.level().m_141937_(), center.f_82481_ - 128.0, center.f_82479_ + 128.0, 256.0, center.f_82481_ + 128.0);
        for (Mob mob : this.session.level().m_45976_(Mob.class, bounds)) {
            LivingEntity root;
            LivingEntity livingEntity;
            OwnableEntity owned;
            CompoundTag data = AbyssPlatform.data((Entity)mob);
            if (data.m_128403_("spiral_abyss_run") || !mob.m_6084_() || mob.m_213877_() || data.m_128403_("spiral_abyss_environment_owner") || data.m_128403_("spiral_abyss_host") && !this.session.host().equals(data.m_128342_("spiral_abyss_host")) || mob instanceof ArenaInteractable || this.playerOwned(mob)) continue;
            if (mob instanceof OwnableEntity && (owned = (OwnableEntity)mob).m_21805_() != null) {
                livingEntity = owned.m_269323_();
                if (!(livingEntity instanceof Mob)) continue;
                Mob owner = (Mob)livingEntity;
                if (!this.session.controller().owns((Entity)owner)) continue;
            }
            if ((root = this.phases.get(AbyssRegistries.ENTITY_TYPES.getKey(mob.m_6095_()))) == null && !(mob instanceof Enemy) && mob.m_6095_().m_20674_() != MobCategory.MONSTER) {
                livingEntity = mob.m_5448_();
                if (!(livingEntity instanceof Player)) continue;
                Player player = (Player)livingEntity;
                if (!this.session.admitted().contains(player.m_20148_())) continue;
            }
            data.m_128362_("spiral_abyss_run", run);
            data.m_128362_("spiral_abyss_host", this.session.host());
            mob.m_21530_();
            if (root != null) {
                data.m_128379_("spiral_abyss_wave_boss", true);
                data.m_128359_(PHASE_BOSS, EntityType.m_20613_((EntityType)root.m_6095_()).toString());
                EncounterReinforcements.inheritScaling(root, mob);
            }
            accept.accept(mob);
            WaveAllies.add((LivingEntity)mob);
        }
    }

    private boolean playerOwned(Mob mob) {
        try {
            Optional<Field> field;
            Optional<Method> getter;
            if (mob.m_6095_().m_204039_(OWNER_GETTER) && ((getter = OWNER_METHODS.get(mob.getClass())).isEmpty() || getter.get().invoke((Object)mob, new Object[0]) instanceof Player)) {
                return true;
            }
            if (mob.m_6095_().m_204039_(SHOOTER_FIELD) && ((field = SHOOTER_FIELDS.get(mob.getClass())).isEmpty() || field.get().get(mob) instanceof Player)) {
                return true;
            }
        }
        catch (ReflectiveOperationException | RuntimeException ex) {
            return true;
        }
        CompoundTag data = AbyssPlatform.data((Entity)mob);
        for (String key : data.m_128431_()) {
            String name = key.toLowerCase(Locale.ROOT);
            if (!name.endsWith("owneruuid") && !name.endsWith("summoneruuid") && !name.equals("owner") && !name.equals("summoner") || !data.m_128403_(key)) continue;
            UUID owner = data.m_128342_(key);
            if (!this.session.admitted().contains(owner) && this.session.level().m_7654_().m_6846_().m_11259_(owner) == null) continue;
            return true;
        }
        return false;
    }

    private static void inheritScaling(LivingEntity root, Mob phase) {
        AttributeModifier modifier;
        AttributeInstance original = root.m_21051_(Attributes.f_22276_);
        AttributeInstance health = phase.m_21051_(Attributes.f_22276_);
        AttributeModifier attributeModifier = modifier = original == null ? null : original.m_22111_(WaveScaling.HEALTH_MODIFIER);
        if (health != null && modifier != null) {
            float fraction = phase.m_21233_() > 0.0f ? phase.m_21223_() / phase.m_21233_() : 1.0f;
            health.m_22120_(WaveScaling.HEALTH_MODIFIER);
            health.m_22125_(new AttributeModifier(WaveScaling.HEALTH_MODIFIER, "Spiral Abyss phase health", modifier.m_22218_(), AttributeModifier.Operation.MULTIPLY_TOTAL));
            phase.m_21153_(phase.m_21233_() * fraction);
        }
        AbyssPlatform.data((Entity)phase).m_128347_("spiral_abyss_damage_scale", AbyssPlatform.data((Entity)root).m_128459_("spiral_abyss_damage_scale"));
    }

    public void clear() {
        this.phases.clear();
        this.roots.clear();
    }
}

