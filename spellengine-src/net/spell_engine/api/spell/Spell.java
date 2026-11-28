/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.spell_power.api.SpellSchool
 *  net.spell_power.api.SpellSchool$Archetype
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3f
 */
package net.spell_engine.api.spell;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.world.entity.EquipmentSlot;
import net.spell_engine.api.spell.fx.Fx;
import net.spell_engine.api.spell.fx.ModelEffect;
import net.spell_engine.api.spell.fx.ParticleGroup;
import net.spell_engine.api.spell.fx.PlayerAnimation;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.summon.AttributeScaling;
import net.spell_engine.api.spell.summon.SummonBehaviour;
import net.spell_engine.api.util.AlwaysGenerate;
import net.spell_engine.api.util.NeverGenerate;
import net.spell_engine.api.util.TriState;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellSchool;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class Spell {
    public SpellSchool school;
    @Nullable
    public ExtendedArchetype secondary_archetype = null;
    public float range = 50.0f;
    @Nullable
    public RangeMechanic range_mechanic;
    @AlwaysGenerate
    public int tier = 1;
    public int sub_tier = 1;
    public String group = "";
    @Nullable
    public Learn learn;
    @Nullable
    public Tooltip tooltip;
    public Type type = Type.ACTIVE;
    public Active active;
    public Passive passive;
    public List<Modifier> modifiers = List.of();
    public Release release = new Release();
    public Target target = new Target();
    public Delivery deliver = new Delivery();
    public List<Impact> impacts = List.of();
    @Nullable
    public AreaImpact area_impact;
    @Nullable
    public ArrowPerks arrow_perks = null;
    public Cost cost = new Cost();

    public Tooltip tooltip() {
        return this.tooltip != null ? this.tooltip : Tooltip.DEFAULT;
    }

    public static enum ExtendedArchetype {
        ARCHERY,
        MAGIC,
        MELEE,
        ANY;

    }

    public static enum Type {
        ACTIVE,
        PASSIVE,
        MODIFIER;

    }

    public static class Release {
        public PlayerAnimation animation;
        public Fx.Visuals visuals = new Fx.Visuals();
        public float pitch_shift = 0.0f;
        public Sound sound;
    }

    public static class Target {
        public Type type = Type.CASTER;
        public int cap = 0;
        public Aim aim;
        public Beam beam;
        public Area area;

        public static enum Type {
            NONE,
            CASTER,
            AIM,
            BEAM,
            AREA,
            FROM_TRIGGER;

        }

        public static class Area {
            public DropoffCurve distance_dropoff = DropoffCurve.NONE;
            public float horizontal_range_multiplier = 1.0f;
            public float vertical_range_multiplier = 1.0f;
            public float angle_degrees = 0.0f;
            public boolean include_caster = false;

            public static enum DropoffCurve {
                NONE,
                SQUARED;

            }
        }

        public static class Beam {
            public Luminance luminance = Luminance.HIGH;
            public String texture_id = "textures/entity/beacon_beam.png";
            public long color_rgba = 0xFFFFFFFFL;
            public long inner_color_rgba = 0xFFFFFFFFL;
            public float width = 0.1f;
            public float flow = 1.0f;
            public Fx.Visuals block_hit = new Fx.Visuals();

            public static enum Luminance {
                LOW,
                MEDIUM,
                HIGH;

            }
        }

        public static class Aim {
            public boolean required = false;
            public boolean sticky = false;
            public boolean use_caster_as_fallback = false;
            public float reposition_vertically = 0.0f;
        }
    }

    public static class Delivery {
        public Type type = Type.DIRECT;
        public int delay = 0;
        public ShootProjectile projectile;
        public Meteor meteor;
        public ShootArrow shoot_arrow;
        public AffectArrow affect_arrow;
        public Melee melee;
        public List<Cloud> clouds;
        public StashEffect stash_effect;
        public Custom custom;

        public static enum Type {
            DIRECT,
            PROJECTILE,
            METEOR,
            CLOUD,
            SHOOT_ARROW,
            AFFECT_ARROW,
            MELEE,
            STASH_EFFECT,
            CUSTOM;

        }

        public static class Custom {
            public String handler;
        }

        public static class StashEffect {
            public String id;
            public int amplifier = 0;
            public float amplifier_power_multiplier = 0.0f;
            public boolean stacking = false;
            public float duration = 10.0f;
            public boolean show_particles = false;
            public List<Trigger> triggers = List.of();
            public int consume = 1;
            public boolean consumed_next_tick = false;
            public boolean consume_any_stacks = false;
            public ImpactMode impact_mode = ImpactMode.PERFORM;

            public static enum ImpactMode {
                PERFORM,
                TRANSFER;

            }
        }

        public static class Cloud {
            @Nullable
            public String entity_type_id;
            public int spawn_ticks = 0;
            public int despawn_ticks = 0;
            public AreaImpact volume = new AreaImpact();
            public float time_to_live_seconds = 0.0f;
            public Growth growth = new Growth();
            public int impact_tick_interval = 5;
            public int impact_cap = 0;
            public Fx.Visuals impact = new Fx.Visuals();
            public int delay_ticks = 0;
            public boolean placement_delay_stacks = true;
            public EntityPlacement placement = new EntityPlacement();
            public List<EntityPlacement> additional_placements = List.of();
            @Nullable
            public Sound presence_sound;
            public ClientData client_data = new ClientData();
            public Spawn spawn = new Spawn();
            public Despawn despawn = new Despawn();

            public static class Growth {
                public float radius_step = 0.0f;
                public int step_interval = 1;
                public int start_tick = 0;
                public int duration_ticks = 0;
            }

            public static class ClientData {
                public int light_level = 0;
                public List<ParticleGroup> particles = List.of();
                public int particle_spawn_interval = 1;
                public List<ParticleGroup> interval_particles = List.of();
                public List<ModelEffect> model_fx = List.of();
            }

            public static class Spawn {
                public Sound sound;
                public Fx.Visuals visuals = new Fx.Visuals();
            }

            public static class Despawn {
                public Sound sound;
                public Fx.Visuals visuals = new Fx.Visuals();
            }
        }

        public static class Melee {
            public boolean allow_airborne = true;
            public List<Attack> attacks = List.of();

            public static class HitBox {
                public float length = 1.0f;
                public float width = 1.0f;
                public float height = 1.0f;
                public float roll = 0.0f;
                public float arc = 0.0f;
            }

            public static class Attack {
                @NeverGenerate
                public String id = UUID.randomUUID().toString();
                public float damage_bonus = 0.0f;
                public int duration = 0;
                public float attack_speed_multiplier = 1.0f;
                public float delay = 0.25f;
                public int additional_strikes = 0;
                public float additional_strike_delay = 0.25f;
                public boolean additional_hits_on_same_target = true;
                public float forward_momentum = 0.0f;
                public boolean allow_momentum_airborne = false;
                public float movement_speed = 1.0f;
                public float movement_slipperiness = 0.0f;
                public HitBox hitbox = new HitBox();
                public PlayerAnimation animation;
                public Sound swing_sound;
                public Sound impact_sound;
                public int impact_sound_cap = 3;
                public Fx.Visuals visuals = new Fx.Visuals();
            }
        }

        public static class AffectArrow {
        }

        public static class ShootArrow {
            public boolean consume_arrow = true;
            public float divergence = 5.0f;
            public boolean arrow_critical_strike = true;
            public LaunchProperties launch_properties = new LaunchProperties().velocity(3.15f);
        }

        public static class Meteor {
            public float launch_height = 10.0f;
            public int offset_requires_sequence = 1;
            public int divergence_requires_sequence = 1;
            public int follow_target_requires_sequence = -1;
            public float launch_radius = 0.0f;
            public LaunchProperties launch_properties = new LaunchProperties();
            public ProjectileData projectile;
        }

        public static class ShootProjectile {
            public boolean inherit_shooter_velocity = false;
            public boolean inherit_shooter_yaw = true;
            public boolean inherit_shooter_pitch = true;
            public DirectionOffset[] direction_offsets;
            public boolean direction_offsets_require_target = false;
            public boolean direct_towards_target = false;
            public LaunchProperties launch_properties = new LaunchProperties();
            public ProjectileData projectile;

            public static class DirectionOffset {
                public float yaw = 0.0f;
                public float pitch = 0.0f;

                public DirectionOffset() {
                }

                public DirectionOffset(float yaw, float pitch) {
                    this.yaw = yaw;
                    this.pitch = pitch;
                }
            }
        }
    }

    public static class ArrowPerks {
        public float damage_multiplier = 1.0f;
        public float velocity_multiplier = 1.0f;
        public boolean bypass_iframes = false;
        public int iframe_to_set = 0;
        public boolean skip_arrow_damage = false;
        public int pierce = 0;
        public float knockback = 1.0f;
        public List<ParticleGroup> travel_particles = List.of();
        public Fx.Visuals launch_visuals = new Fx.Visuals();
        @Nullable
        public Sound launch_sound;
        @Nullable
        public ProjectileModelComposite composite_model;

        public ArrowPerks copy() {
            ArrowPerks copy = new ArrowPerks();
            copy.damage_multiplier = this.damage_multiplier;
            copy.velocity_multiplier = this.velocity_multiplier;
            copy.bypass_iframes = this.bypass_iframes;
            copy.iframe_to_set = this.iframe_to_set;
            copy.skip_arrow_damage = this.skip_arrow_damage;
            copy.pierce = this.pierce;
            copy.knockback = this.knockback;
            copy.travel_particles = this.travel_particles;
            copy.launch_visuals = this.launch_visuals;
            copy.launch_sound = this.launch_sound;
            copy.composite_model = this.composite_model;
            return copy;
        }

        public void mutatingCombine(ArrowPerks other) {
            this.damage_multiplier *= other.damage_multiplier;
            this.velocity_multiplier *= other.velocity_multiplier;
            this.knockback *= other.knockback;
            this.pierce += other.pierce;
            this.bypass_iframes = this.bypass_iframes || other.bypass_iframes;
            this.skip_arrow_damage = this.skip_arrow_damage || other.skip_arrow_damage;
            this.iframe_to_set = Math.max(this.iframe_to_set, other.iframe_to_set);
        }

        public static ArrowPerks EMPTY() {
            return new ArrowPerks();
        }
    }

    public static class Cost {
        public boolean batching = false;
        public float exhaust = 0.1f;
        public int durability = 1;
        @Nullable
        public String effect_id;
        public Cooldown cooldown = new Cooldown();
        @Nullable
        public Item item;

        public static class Cooldown {
            @Nullable
            public String group;
            public float attempt_duration = 0.0f;
            public float duration = 0.0f;
            public boolean proportional = false;
            public boolean haste_affected = true;
            public boolean hosting_item = true;
        }

        public static class Item {
            public String id;
            public int amount = 1;
            public boolean consume = true;
        }
    }

    public static class Tooltip {
        public static final Tooltip DEFAULT = new Tooltip();
        public boolean show_header = true;
        public boolean show_activation = true;
        public boolean show_range = true;
        public LineOptions name = new LineOptions(true, true);
        public LineOptions description = new LineOptions(false, true);

        public static class LineOptions {
            public String color = "GRAY";
            public boolean show_in_compact = true;
            public boolean show_in_details = true;

            public LineOptions() {
            }

            public LineOptions(boolean show_in_compact, boolean show_in_details) {
                this.show_in_compact = show_in_compact;
                this.show_in_details = show_in_details;
            }
        }
    }

    public static class TargetCondition {
        public float health_percent_above = 0.0f;
        public float health_percent_below = 1.0f;
        @Nullable
        public String entity_type;
        @Nullable
        public String entity_predicate_id;
        @Nullable
        public String entity_predicate_param;
    }

    public static class EntityPlacement {
        public int delay_ticks = 0;
        public boolean force_onto_ground = true;
        public boolean line_of_sight = false;
        public float location_offset_by_look = 0.0f;
        public float location_yaw_offset = 0.0f;
        public boolean apply_yaw = false;
        public boolean apply_pitch = false;
        public float location_offset_x = 0.0f;
        public float location_offset_y = 0.0f;
        public float location_offset_z = 0.0f;

        public EntityPlacement copy() {
            EntityPlacement copy = new EntityPlacement();
            copy.delay_ticks = this.delay_ticks;
            copy.force_onto_ground = this.force_onto_ground;
            copy.line_of_sight = this.line_of_sight;
            copy.location_offset_by_look = this.location_offset_by_look;
            copy.location_yaw_offset = this.location_yaw_offset;
            copy.apply_yaw = this.apply_yaw;
            copy.apply_pitch = this.apply_pitch;
            copy.location_offset_x = this.location_offset_x;
            copy.location_offset_y = this.location_offset_y;
            copy.location_offset_z = this.location_offset_z;
            return copy;
        }
    }

    public static class ProjectileModelComposite {
        public List<Model> models = List.of();

        public static enum Orientation {
            TOWARDS_CAMERA,
            TOWARDS_MOTION,
            ALONG_MOTION;

        }

        public static class Model {
            public Orientation orientation = Orientation.TOWARDS_MOTION;
            public float rotate_degrees_per_tick = 2.0f;
            public float rotate_degrees_offset = 0.0f;
            public boolean use_held_item = false;
            public ModelEffect fx = new ModelEffect();
        }
    }

    public static class ProjectileData {
        public float divergence = 0.0f;
        public float homing_angle = 1.0f;
        @Nullable
        public float[] homing_angles = null;
        public float homing_after_absolute_distance = 0.0f;
        public float homing_after_relative_distance = 0.0f;
        public int travel_sound_interval = 20;
        @Nullable
        public Sound travel_sound;
        @Nullable
        public Motion motion;
        public Perks perks = new Perks();
        @Nullable
        public HitBox hitbox;
        public Client client_data;

        public static class Perks {
            public int ricochet = 0;
            public float ricochet_range = 5.0f;
            public int bounce = 0;
            public boolean bounce_ricochet_sync = true;
            public int pierce = 0;
            public int chain_reaction_size = 0;
            public int chain_reaction_triggers = 1;
            public int chain_reaction_increment = -1;

            public Perks copy() {
                Perks copy = new Perks();
                copy.ricochet = this.ricochet;
                copy.ricochet_range = this.ricochet_range;
                copy.bounce = this.bounce;
                copy.bounce_ricochet_sync = this.bounce_ricochet_sync;
                copy.pierce = this.pierce;
                copy.chain_reaction_size = this.chain_reaction_size;
                copy.chain_reaction_triggers = this.chain_reaction_triggers;
                copy.chain_reaction_increment = this.chain_reaction_increment;
                return copy;
            }

            public void mutatingCombine(Perks other) {
                this.ricochet += other.ricochet;
                this.ricochet_range += other.ricochet_range;
                this.bounce += other.bounce;
                this.pierce += other.pierce;
                this.chain_reaction_size += other.chain_reaction_size;
                this.chain_reaction_triggers += other.chain_reaction_triggers;
                this.chain_reaction_increment += other.chain_reaction_increment;
            }

            public static Perks EMPTY() {
                Perks empty = new Perks();
                empty.ricochet = 0;
                empty.ricochet_range = 0.0f;
                empty.bounce = 0;
                empty.pierce = 0;
                empty.chain_reaction_size = 0;
                empty.chain_reaction_triggers = 0;
                empty.chain_reaction_increment = 0;
                return empty;
            }
        }

        public static class Client {
            public int light_level = 0;
            public List<ParticleGroup> travel_particles = List.of();
            public ProjectileModelComposite composite_model;
        }

        public static class HitBox {
            public float width = 0.5f;
            public float height = 0.5f;
            public float length = 0.0f;

            public HitBox() {
            }

            public HitBox(float width, float height) {
                this.width = width;
                this.height = height;
            }
        }

        public static class Motion {
            public float gravity = 0.0f;
            public float drag = 0.0f;
            public float drag_fluid = 0.0f;
            public List<FluidOverride> fluid_overrides = List.of();
            public float min_speed = 0.0f;

            public Motion copy() {
                Motion copy = new Motion();
                copy.gravity = this.gravity;
                copy.drag = this.drag;
                copy.drag_fluid = this.drag_fluid;
                copy.fluid_overrides = this.fluid_overrides;
                copy.min_speed = this.min_speed;
                return copy;
            }

            public static class FluidOverride {
                public String fluid = "";
                public float drag = 0.0f;
                public float gravity_multiply = 1.0f;
            }
        }
    }

    public static class LaunchProperties {
        public float velocity = 1.0f;
        public int extra_launch_mod = -1;
        public int extra_launch_count = 0;
        public int extra_launch_delay = 2;
        @Nullable
        public Sound sound;

        public LaunchProperties velocity(float value) {
            this.velocity = value;
            return this;
        }

        public LaunchProperties copy() {
            LaunchProperties copy = new LaunchProperties();
            copy.velocity = this.velocity;
            copy.extra_launch_mod = this.extra_launch_mod;
            copy.extra_launch_count = this.extra_launch_count;
            copy.extra_launch_delay = this.extra_launch_delay;
            copy.sound = this.sound != null ? this.sound.copy() : null;
            return copy;
        }

        public void mutatingCombine(LaunchProperties other) {
            this.velocity += other.velocity;
            this.extra_launch_mod = other.extra_launch_mod >= 0 ? other.extra_launch_mod : this.extra_launch_mod;
            this.extra_launch_count += other.extra_launch_count;
            this.extra_launch_delay += other.extra_launch_delay;
        }

        public static LaunchProperties EMPTY() {
            LaunchProperties empty = new LaunchProperties();
            empty.velocity = 0.0f;
            empty.extra_launch_mod = -1;
            empty.extra_launch_count = 0;
            empty.extra_launch_delay = 0;
            return empty;
        }
    }

    public static class AreaImpact {
        @Nullable
        public Impact.Action.Type triggering_action_type;
        @Nullable
        public Impact.Action.Type execute_action_type;
        public boolean force_indirect = false;
        public float radius = 1.0f;
        public ExtraRadius extra_radius = new ExtraRadius();
        public Target.Area area = new Target.Area();
        public Fx.Visuals visuals = new Fx.Visuals();
        @Nullable
        public Sound sound;

        public float combinedRadius(double power) {
            return this.radius + this.extra_radius.power_coefficient * (float)Math.min((double)this.extra_radius.power_cap, power);
        }

        public static class ExtraRadius {
            public float power_coefficient = 0.0f;
            public float power_cap = 0.0f;
        }
    }

    public static class Trigger {
        @Nullable
        public TargetSelector target_override;
        @Nullable
        public TargetSelector aoe_source_override;
        public Type type;
        public Stage stage = Stage.POST;
        public int fire_delay = 0;
        public int cap_per_tick = 0;
        public float chance = 1.0f;
        public boolean chance_batching = false;
        @Nullable
        public EquipmentSlot equipment_condition;
        @Nullable
        public String weapon_condition;
        @Nullable
        public List<TargetCondition> caster_conditions;
        @Nullable
        public List<TargetCondition> target_conditions;
        public SpellCondition spell;
        public ImpactCondition impact;
        public DamageCondition damage;
        public MeleeCondition melee;
        public EffectCondition effect;
        public ArrowShotCondition arrow_shot;

        public static enum Stage {
            PRE,
            POST;

        }

        public static class ArrowShotCondition {
            @Nullable
            public Boolean from_spell;
        }

        public static class EffectCondition {
            public String id;
        }

        public static class MeleeCondition {
            @Nullable
            public Boolean is_combo;
            @Nullable
            public Boolean is_offhand;
            @Nullable
            public Boolean critical;
        }

        public static class DamageCondition {
            @Nullable
            public Float amount_min = null;
            @Nullable
            public Float amount_max = null;
            @Nullable
            public String damage_type;
            @Nullable
            public Boolean fatal;
        }

        public static class ImpactCondition {
            @Nullable
            public String impact_type;
            @Nullable
            public Boolean critical;
        }

        public static class SpellCondition {
            @Nullable
            public String school;
            @Nullable
            public SpellSchool.Archetype archetype;
            @Nullable
            public net.spell_engine.api.spell.Spell$Type type;
            @Nullable
            public String id;
            public float cooldown_min = 0.0f;
        }

        public static enum Type {
            ARROW_SHOT,
            ARROW_IMPACT,
            MELEE_IMPACT,
            SPELL_CAST,
            SPELL_IMPACT_ANY,
            SPELL_IMPACT_SPECIFIC,
            SPELL_AREA_IMPACT,
            EFFECT_TICK,
            EVASION,
            DAMAGE_TAKEN,
            SHIELD_BLOCK,
            ROLL;

        }

        public static enum TargetSelector {
            CASTER,
            AOE_SOURCE,
            TARGET;

        }
    }

    public static class Impact {
        public float chance = 1.0f;
        @Nullable
        public SpellSchool school;
        @Nullable
        public List<PowerBlend> power_blend;
        public boolean attribute_from_target = false;
        @Nullable
        public String attribute;
        public List<TargetModifier> target_modifiers = List.of();
        public Action action;
        public Fx.Visuals visuals = new Fx.Visuals();
        public Sound sound;

        public static class Action {
            public Type type;
            public boolean allow_on_center_target = true;
            public boolean apply_to_caster = false;
            public float min_power = 1.0f;
            public float max_power = 999999.0f;
            public Damage damage;
            public Heal heal;
            public StatusEffect status_effect;
            public Fire fire;
            public List<Spawn> spawns;
            public Teleport teleport;
            public Cooldown cooldown;
            public Aggro aggro;
            public Disrupt disrupt;
            public Immunity immunity;
            public Velocity velocity;
            public Custom custom;
            public Summon summon;

            public static class Summon {
                public String entity_type_id;
                public SummonBehaviour behaviour = new SummonBehaviour();
                public AttributeScaling attribute_scaling = new AttributeScaling();
                public List<EntityPlacement> placements = new ArrayList<EntityPlacement>();
                public int spawn_count = 1;
                public List<EntityPlacement> group_placements = new ArrayList<EntityPlacement>();
                public int group_count = 1;
                @Nullable
                public Fx.Visuals group_spawn_fx = null;
                @Nullable
                public Sound group_spawn_sound = null;

                public Summon() {
                }

                public Summon(String entity_type_id, SummonBehaviour behaviour, List<EntityPlacement> placements, int spawn_count) {
                    this.entity_type_id = entity_type_id;
                    this.behaviour = behaviour;
                    this.placements = placements;
                    this.spawn_count = spawn_count;
                }

                public Summon(String entity_type_id, SummonBehaviour behaviour, List<EntityPlacement> placements, int spawn_count, List<EntityPlacement> group_placements, int group_count) {
                    this(entity_type_id, behaviour, placements, spawn_count);
                    this.group_placements = group_placements;
                    this.group_count = group_count;
                }
            }

            public static class Custom {
                public SpellTarget.Intent intent = SpellTarget.Intent.HELPFUL;
                public String handler;
            }

            public static class Velocity {
                public Frame frame = Frame.ORIGIN;
                public Vector3f push = new Vector3f(0.0f, 0.5f, 0.0f);
                public float power_coefficient = 0.0f;
                public boolean reset_velocity = false;
                public SpellTarget.Intent intent = SpellTarget.Intent.HARMFUL;

                public static enum Frame {
                    LOOK,
                    ORIGIN;

                }
            }

            public static class Immunity {
                @Nullable
                public String damage_type;
                @Nullable
                public Boolean damage_indirect;
                public boolean effect_any_harmful = false;
                public int duration_ticks = 20;
            }

            public static class Disrupt {
                public boolean shield_blocking = false;
                public float item_usage_seconds = 0.0f;
            }

            public static class Aggro {
                public boolean only_if_targeted = false;
                public Mode mode = Mode.SET;

                public static enum Mode {
                    SET,
                    CLEAR;

                }
            }

            public static class Cooldown {
                @Nullable
                public Modify actives;
                @Nullable
                public Modify passives;

                public static class Modify {
                    @Nullable
                    public String school;
                    @Nullable
                    public String id;
                    public float duration_add = 0.0f;
                    public float duration_multiplier = 1.0f;
                }
            }

            public static class Teleport {
                public Mode mode;
                public int required_clearance_block_y = 1;
                public float minimum_distance = 0.0f;
                @Nullable
                public Fizzle fizzle;
                public SpellTarget.Intent intent = SpellTarget.Intent.HELPFUL;
                public Forward forward;
                public BehindTarget behind_target;
                @Nullable
                public Fx.Visuals depart;
                @Nullable
                public Fx.Visuals arrive;

                public static class BehindTarget {
                    public float distance = 1.5f;
                }

                public static class Forward {
                    public float distance = 10.0f;
                }

                public static class Fizzle {
                    @Nullable
                    public Sound sound;
                    public Fx.Visuals visuals = new Fx.Visuals();
                }

                public static enum Mode {
                    FORWARD,
                    BEHIND_TARGET;

                }
            }

            public static class Spawn {
                public SpellTarget.Intent intent = SpellTarget.Intent.HELPFUL;
                public String entity_type_id;
                public int time_to_live_seconds = 0;
                public int delay_ticks = 0;
                public EntityPlacement placement = new EntityPlacement();

                public Spawn copy() {
                    Spawn copy = new Spawn();
                    copy.intent = this.intent;
                    copy.entity_type_id = this.entity_type_id;
                    copy.time_to_live_seconds = this.time_to_live_seconds;
                    copy.delay_ticks = this.delay_ticks;
                    copy.placement = this.placement;
                    return copy;
                }
            }

            public static class Fire {
                public float duration = 2.0f;
                public int tick_offset = 10;
            }

            public static class StatusEffect {
                public String effect_id;
                public float duration = 10.0f;
                public int amplifier = 0;
                public float amplifier_power_multiplier = 0.0f;
                public int amplifier_cap = 0;
                public float amplifier_cap_power_multiplier = 0.0f;
                public boolean refresh_duration = true;
                public ApplyMode apply_mode = ApplyMode.SET;
                @Nullable
                public ApplyLimit apply_limit;
                public boolean show_particles = false;
                public Remove remove;

                public static enum ApplyMode {
                    SET,
                    ADD,
                    REMOVE;

                }

                public static class Remove {
                    @Nullable
                    public String id;
                    public Selector selector = Selector.RANDOM;
                    public boolean select_beneficial = false;
                    @Nullable
                    public Boolean movement_impairing;

                    public static enum Selector {
                        RANDOM,
                        FIRST,
                        ALL;

                    }
                }

                public static class ApplyLimit {
                    public float health_base = 0.0f;
                    public float spell_power_multiplier = 0.0f;
                }
            }

            public static class Heal {
                public float spell_power_coefficient = 1.0f;
            }

            public static class Damage {
                public boolean bypass_iframes = true;
                public float spell_power_coefficient = 1.0f;
                public float knockback = 1.0f;
            }

            public static enum Type {
                DAMAGE,
                HEAL,
                STATUS_EFFECT,
                FIRE,
                SPAWN,
                SUMMON,
                TELEPORT,
                COOLDOWN,
                AGGRO,
                DISRUPT,
                IMMUNITY,
                VELOCITY,
                CUSTOM;

            }
        }

        public static class Modifier {
            public float power_multiplier = 0.0f;
            public float critical_chance_bonus = 0.0f;
            public float critical_damage_bonus = 0.0f;
        }

        public static class TargetModifier {
            public boolean all_required = false;
            public List<TargetCondition> conditions = List.of();
            public TriState execute = TriState.PASS;
            @Nullable
            public Modifier modifier;
        }

        public static class PowerBlend {
            public SpellSchool school;
            public boolean power = false;
            public boolean critical_chance = false;
            public boolean critical_damage = false;
            public float weight = 1.0f;
        }
    }

    public static class Modifier {
        public String spell_pattern;
        public float range_add = 0.0f;
        @Nullable
        public ImpactListModifier mutate_impacts;
        public List<Impact> impacts = List.of();
        @Nullable
        public AreaImpact replacing_area_impact;
        public List<ImpactFilter> impact_filters = List.of();
        @Nullable
        public LaunchProperties projectile_launch;
        @Nullable
        public ProjectileData.Perks projectile_perks;
        @Nullable
        public ArrowPerks arrow_perks;
        public float projectile_scale_multiply = 0.0f;
        public float meteor_launch_radius_add = 0.0f;
        @Nullable
        public Fx.Visuals release = null;
        @Nullable
        public Impact.Modifier power_modifier;
        public int channel_ticks_add = 0;
        public float knockback_multiply_base = 0.0f;
        public float spawn_duration_add = 0.0f;
        public float teleport_distance_add = 0.0f;
        public int effect_amplifier_add = 0;
        public int effect_amplifier_cap_add = 0;
        public int stash_amplifier_add = 0;
        public float effect_duration_add = 0.0f;
        public float cooldown_duration_deduct = 0.0f;
        public float melee_momentum_add = 0.0f;
        public float melee_slipperiness_add = 0.0f;
        public float melee_damage_multiplier = 0.0f;
        @Nullable
        public List<Delivery.Melee.Attack> melee_attacks = null;
        public List<EntityPlacement> additional_placements = List.of();
        public Cloud cloud = new Cloud();
        @Nullable
        public AttributeScaling summon_attribute_scaling;
        public int summon_spawn_count_add = 0;
        public int summon_group_count_add = 0;
        public SummonBehaviourModifier summon_behaviour = new SummonBehaviourModifier();

        public static class Cloud {
            public float radius_add = 0.0f;
            public Delivery.Cloud.Growth growth = new Delivery.Cloud.Growth();
        }

        public static class SummonBehaviourModifier {
            public List<SummonBehaviour.Action.Entry> actions_add = List.of();
            public Lifespan lifespan = new Lifespan();

            public static class Lifespan {
                public int spawn_ticks_add = 0;
                public int active_seconds_add = 0;
                public int despawn_ticks_add = 0;
            }
        }

        public static class ImpactFilter {
            @Nullable
            public SpellSchool school;
            @Nullable
            public Impact.Action.Type type;
        }

        public static enum ImpactListModifier {
            PREPEND,
            APPEND;

        }
    }

    public static class Passive {
        public List<Trigger> triggers = List.of();
    }

    public static class Active {
        public Cast cast = new Cast();

        public static class Cast {
            public boolean haste_affected = true;
            public float duration = 0.0f;
            public Type type = Type.STANDARD;
            @Nullable
            public Channel channel;
            @Nullable
            public Charge charge;
            public PlayerAnimation animation;
            public boolean animation_pitch = true;
            public float animation_spin = 0.0f;
            public boolean animates_ranged_weapon = false;
            public float movement_speed = 0.2f;
            public Sound start_sound;
            public Sound sound;
            public List<ParticleGroup> particles = List.of();

            public int channelTicks() {
                if (this.type == Type.CHANNEL && this.channel != null) {
                    return this.channel.ticks;
                }
                return 0;
            }

            public boolean channelReleaseFx() {
                return this.type == Type.CHANNEL && this.channel != null && this.channel.release_fx;
            }

            public static enum Type {
                STANDARD,
                CHANNEL,
                CHARGE;

            }

            public static class Channel {
                public int ticks = 0;
                public boolean release_fx = false;
            }

            public static class Charge {
                public float min_release_ratio = 0.2f;
                public float output_scaling = 1.0f;
                public Curve curve = Curve.LINEAR;
                public Modifier bonus = new Modifier();

                public static enum Curve {
                    LINEAR,
                    EASE_IN_QUAD,
                    EASE_OUT_QUAD,
                    EASE_IN_OUT_QUAD,
                    EASE_IN_QUART,
                    EASE_OUT_QUART,
                    EASE_IN_OUT_QUART,
                    EASE_IN_EXPO,
                    EASE_OUT_EXPO,
                    EASE_IN_OUT_EXPO;


                    public float apply(float t) {
                        t = Math.max(0.0f, Math.min(1.0f, t));
                        switch (this.ordinal()) {
                            case 0: {
                                return t;
                            }
                            case 1: {
                                return (float)Math.pow(t, 2.0);
                            }
                            case 2: {
                                return 1.0f - (float)Math.pow(1.0f - t, 2.0);
                            }
                            case 3: {
                                return t < 0.5f ? 2.0f * (float)Math.pow(t, 2.0) : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0) / 2.0f;
                            }
                            case 4: {
                                return (float)Math.pow(t, 4.0);
                            }
                            case 5: {
                                return 1.0f - (float)Math.pow(1.0f - t, 4.0);
                            }
                            case 6: {
                                return t < 0.5f ? 8.0f * (float)Math.pow(t, 4.0) : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 4.0) / 2.0f;
                            }
                            case 7: {
                                return t == 0.0f ? 0.0f : (float)Math.pow(2.0, 10.0f * t - 10.0f);
                            }
                            case 8: {
                                return t == 1.0f ? 1.0f : 1.0f - (float)Math.pow(2.0, -10.0f * t);
                            }
                            case 9: {
                                if (t == 0.0f) {
                                    return 0.0f;
                                }
                                if (t == 1.0f) {
                                    return 1.0f;
                                }
                                return t < 0.5f ? (float)Math.pow(2.0, 20.0f * t - 10.0f) / 2.0f : (2.0f - (float)Math.pow(2.0, -20.0f * t + 10.0f)) / 2.0f;
                            }
                        }
                        return t;
                    }
                }
            }
        }
    }

    public static class Learn {
        public int level_cost_per_tier = 3;
        public int level_requirement_per_tier = 10;
    }

    public static enum RangeMechanic {
        MELEE;

    }
}

