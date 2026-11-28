/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.casting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.cost.Ammo;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.RegistryHelper;
import org.jetbrains.annotations.Nullable;

public class SpellCast {

    public static enum Animation {
        CASTING,
        RELEASE,
        MISC;

    }

    public static enum Action {
        CHANNEL,
        RELEASE,
        TRIGGER;

    }

    public record TargetSnapshot(List<Integer> entityIds, @Nullable Vec3 location) {
        public static final TargetSnapshot EMPTY = new TargetSnapshot(List.of(), null);

        public static TargetSnapshot of(SpellTarget.SearchResult result) {
            return new TargetSnapshot(result.entities().stream().map(Entity::m_19879_).toList(), result.location());
        }

        public TargetSnapshot sanitizedFor(Option.Targeting targeting) {
            Vec3 clampedLocation;
            if (!targeting.clientResolved()) {
                return EMPTY;
            }
            List<Integer> ids = this.entityIds.size() > targeting.maxEntities() ? List.copyOf(this.entityIds.subList(0, targeting.maxEntities())) : this.entityIds;
            Vec3 vec3 = clampedLocation = targeting.locationAllowed() ? this.location : null;
            if (ids == this.entityIds && clampedLocation == this.location) {
                return this;
            }
            return new TargetSnapshot(ids, clampedLocation);
        }
    }

    public record Option(Holder<Spell> spell, Mode mode, Targeting targeting, float range, int channelTicks) {
        public static Option of(Player caster, Holder<Spell> spellEntry) {
            Spell spell = (Spell)spellEntry.m_203334_();
            return new Option(spellEntry, Mode.from(spell), Targeting.of(spell), SpellParameters.getMaxRange((LivingEntity)caster, spellEntry), SpellParameters.channelTicks((LivingEntity)caster, spellEntry));
        }

        public static List<Option> allOf(Player player) {
            return SpellContainerSource.activeSpellsOf(player).stream().map(entry -> Option.of(player, (Holder<Spell>)entry)).toList();
        }

        public ResourceLocation id() {
            return ((ResourceKey)this.spell.m_203543_().get()).m_135782_();
        }

        public SyncFormat sync() {
            return new SyncFormat(this.id().toString(), this.range, this.channelTicks);
        }

        @Nullable
        public static Option fromSync(Level world, SyncFormat sync) {
            Holder.Reference entry = RegistryHelper.getEntry(SpellRegistry.from(world), new ResourceLocation(sync.i())).orElse(null);
            if (entry == null) {
                return null;
            }
            Spell spell = (Spell)entry.m_203334_();
            return new Option((Holder<Spell>)entry, Mode.from(spell), Targeting.of(spell), sync.r(), sync.t());
        }

        public record Targeting(Shape shape, int maxEntities, boolean entityRequired, boolean locationAllowed) {
            public static final int RAY_ENTITY_CAP = 32;

            public boolean clientResolved() {
                return this.shape == Shape.CURSOR_ENTITY || this.shape == Shape.CURSOR_RAY;
            }

            public static Targeting of(Spell spell) {
                Spell.Target target = spell.target;
                return switch (target.type) {
                    default -> throw new IncompatibleClassChangeError();
                    case Spell.Target.Type.NONE, Spell.Target.Type.FROM_TRIGGER -> new Targeting(Shape.NONE, 0, false, false);
                    case Spell.Target.Type.CASTER -> new Targeting(Shape.SELF, 0, false, false);
                    case Spell.Target.Type.AREA -> new Targeting(Shape.AROUND_CASTER, 0, false, false);
                    case Spell.Target.Type.AIM -> {
                        boolean required = target.aim != null && target.aim.required;
                        yield new Targeting(Shape.CURSOR_ENTITY, 1, required, !required);
                    }
                    case Spell.Target.Type.BEAM -> new Targeting(Shape.CURSOR_RAY, target.cap > 0 ? Math.min(target.cap, 32) : 32, false, false);
                };
            }

            public static enum Shape {
                NONE,
                SELF,
                AROUND_CASTER,
                CURSOR_ENTITY,
                CURSOR_RAY;

            }
        }

        public record SyncFormat(String i, float r, int t) {
        }
    }

    public static enum Mode {
        INSTANT,
        CASTING,
        CHANNEL,
        CHARGED,
        PASSIVE,
        ITEM_USE;


        public static Mode from(Spell spell) {
            if (spell.active != null) {
                if (spell.active.cast.duration <= 0.0f) {
                    return INSTANT;
                }
                if (SpellParameters.isChanneled(spell)) {
                    return CHANNEL;
                }
                if (spell.active.cast.type == Spell.Active.Cast.Type.CHARGE) {
                    return CHARGED;
                }
                return CASTING;
            }
            return PASSIVE;
        }
    }

    public record Progress(float ratio, Process process) {
    }

    public record Process(Holder<Spell> spell, Item item, float speed, int length, long startedAt, TickHolder tickHolder) {
        public Process(LivingEntity caster, Holder<Spell> spell, Item item, float speed, int length, long startedAt) {
            this(spell, item, speed, length, startedAt, new TickHolder());
            if (SpellParameters.isChanneled((Spell)spell.m_203334_())) {
                int channelCount = SpellParameters.channelTicks(caster, spell);
                float interval = this.channelInterval(caster);
                float offset = -interval * 0.5f;
                for (int i = 1; i <= channelCount; ++i) {
                    this.tickHolder.ticks.add(Float.valueOf(interval * (float)i + offset));
                }
            }
        }

        public int spellCastTicksSoFar(long worldTime) {
            return (int)Math.max(worldTime - this.startedAt, 0L);
        }

        public Progress progress(int castTicks) {
            if (this.length <= 0) {
                return new Progress(1.0f, this);
            }
            float ratio = Math.min((float)castTicks / (float)this.length(), 1.0f);
            return new Progress(ratio, this);
        }

        public Progress progress(long worldTime) {
            int castTicks = this.spellCastTicksSoFar(worldTime);
            return this.progress(castTicks);
        }

        public ResourceLocation id() {
            return ((ResourceKey)this.spell.m_203543_().get()).m_135782_();
        }

        public SyncFormat sync() {
            return new SyncFormat(this.id().toString(), this.speed, this.length, this.tickHolder.ticks.size());
        }

        public String fastSyncJSON() {
            return "{\"i\":\"" + this.id().toString() + "\",\"s\":" + this.speed + ",\"l\":" + this.length + ",\"t\":" + this.tickHolder.ticks.size() + "}";
        }

        @Nullable
        public static Process fromSync(LivingEntity caster, Level world, SyncFormat sync, Item item, long startedAt) {
            String spellId = sync.i();
            if (spellId.isEmpty()) {
                return null;
            }
            ResourceLocation id = new ResourceLocation(spellId);
            Holder.Reference spellEntry = RegistryHelper.getEntry(SpellRegistry.from(world), id).orElse(null);
            return new Process(caster, (Holder<Spell>)spellEntry, item, sync.s(), sync.l(), startedAt);
        }

        public float channelInterval(LivingEntity caster) {
            int ticks = SpellParameters.channelTicks(caster, this.spell());
            if (ticks > 0) {
                return (float)this.length / (float)ticks;
            }
            return this.length;
        }

        public boolean isDue(long time) {
            int castTicks = this.spellCastTicksSoFar(time);
            if (this.tickHolder.ticks.isEmpty()) {
                return false;
            }
            return (float)castTicks >= this.tickHolder.ticks.get(0).floatValue();
        }

        public void markDue() {
            if (!this.tickHolder.ticks.isEmpty()) {
                this.tickHolder.ticks.remove(0);
            }
        }

        public record SyncFormat(String i, float s, int l, int t) {
        }
    }

    public static class TickHolder {
        public ArrayList<Float> ticks = new ArrayList();
    }

    public record Duration(float speed, int length) {
        public static final Duration EMPTY = new Duration(0.0f, 0);
    }

    public record Attempt(Result result, @Nullable MissingItemInfo missingItem, @Nullable OnCooldownInfo onCooldown) {
        public static Attempt none() {
            return new Attempt(Result.NONE, null, null);
        }

        public static Attempt success() {
            return new Attempt(Result.SUCCESS, null, null);
        }

        public static Attempt failMissingItem(MissingItemInfo missingItem) {
            return new Attempt(Result.MISSING_ITEM, missingItem, null);
        }

        public static Attempt failOnCooldown(OnCooldownInfo onCooldown) {
            return new Attempt(Result.ON_COOLDOWN, null, onCooldown);
        }

        public boolean isSuccess() {
            return this.result == Result.SUCCESS;
        }

        public boolean isFail() {
            return this.result != Result.SUCCESS && this.result != Result.NONE;
        }

        public static enum Result {
            SUCCESS,
            MISSING_ITEM,
            ON_COOLDOWN,
            NONE;

        }

        public record MissingItemInfo(Ammo.Searched item) {
        }

        public record OnCooldownInfo() {
        }
    }
}

