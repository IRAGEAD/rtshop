/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.internals.casting;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.casting.SpellCasting;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.Binding;
import net.spell_engine.utils.EntityScale;
import net.spell_engine.utils.RegistryHelper;
import net.spell_engine.utils.SoundHelper;
import org.jetbrains.annotations.Nullable;

public class SpellCastInteractor {
    private static final Gson syncGson = new Gson();
    private final Player player;
    private final Ports ports;
    private List<SpellCast.Option> options = List.of();
    private String lastOptionsRaw = "";
    @Nullable
    private SpellCast.Process process = null;
    private String lastProcessRaw = "";
    private int channelTickIndex = 0;
    @Nullable
    private SpellCast.TargetSnapshot latestSnapshot = null;
    @Nullable
    private ResourceLocation latestSnapshotSpellId = null;

    public static SpellCastInteractor forPlayer(Player player, Binding<String> processSync, Binding<String> optionsSync) {
        return new SpellCastInteractor(player, new Ports(() -> SpellCast.Option.allOf(player), processSync, optionsSync));
    }

    public SpellCastInteractor(Player player, Ports ports) {
        this.player = player;
        this.ports = ports;
    }

    public List<SpellCast.Option> options() {
        String raw = this.ports.optionsSync().get();
        if (!raw.equals(this.lastOptionsRaw)) {
            this.lastOptionsRaw = raw;
            this.options = this.parseOptions(raw);
        }
        return this.options;
    }

    public void invalidateOptions() {
        List<SpellCast.Option> computed = this.ports.resolveOptions().get();
        if (!computed.equals(this.options)) {
            String raw;
            this.options = computed;
            this.lastOptionsRaw = raw = SpellCastInteractor.serializeOptions(computed);
            this.ports.optionsSync().set(raw);
        }
    }

    private static String serializeOptions(List<SpellCast.Option> options) {
        SpellCast.Option.SyncFormat[] syncFormats = (SpellCast.Option.SyncFormat[])options.stream().map(SpellCast.Option::sync).toArray(SpellCast.Option.SyncFormat[]::new);
        return syncGson.toJson((Object)syncFormats);
    }

    private List<SpellCast.Option> parseOptions(String raw) {
        if (raw.isEmpty()) {
            return List.of();
        }
        SpellCast.Option.SyncFormat[] syncFormats = (SpellCast.Option.SyncFormat[])syncGson.fromJson(raw, SpellCast.Option.SyncFormat[].class);
        return Arrays.stream(syncFormats).map(sync -> SpellCast.Option.fromSync(this.player.m_9236_(), sync)).filter(Objects::nonNull).toList();
    }

    @Nullable
    public SpellCast.Process process() {
        String raw = this.ports.processSync().get();
        if (!raw.equals(this.lastProcessRaw)) {
            this.lastProcessRaw = raw;
            this.process = this.parseProcess(raw);
        }
        return this.process;
    }

    public void setProcess(@Nullable SpellCast.Process process) {
        String raw;
        if (process != null && ((Spell)process.spell().m_203334_()).active == null) {
            return;
        }
        this.process = process;
        this.lastProcessRaw = raw = process != null ? process.fastSyncJSON() : "";
        this.ports.processSync().set(raw);
    }

    @Nullable
    private SpellCast.Process parseProcess(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        SpellCast.Process.SyncFormat syncFormat = (SpellCast.Process.SyncFormat)syncGson.fromJson(raw, SpellCast.Process.SyncFormat.class);
        return SpellCast.Process.fromSync((LivingEntity)this.player, this.player.m_9236_(), syncFormat, this.player.m_21205_().m_41720_(), this.player.m_9236_().m_46467_());
    }

    public int channelTickIndex() {
        return this.channelTickIndex;
    }

    public void setChannelTickIndex(int value) {
        this.channelTickIndex = value;
    }

    public void requestClear() {
        this.setProcess(null);
        this.channelTickIndex = 0;
    }

    private void fire(Holder<Spell> spellEntry, SpellCast.Action action, float progress) {
        SpellExecution.performSpell(this.player.m_9236_(), this.player, spellEntry, this.resolveTargets(spellEntry, progress), action, progress);
    }

    private SpellTarget.SearchResult resolveTargets(Holder<Spell> spellEntry, float progress) {
        Level level;
        float range = SpellParameters.getRange((LivingEntity)this.player, spellEntry, progress);
        SpellCast.Option.Targeting targeting = SpellCast.Option.Targeting.of((Spell)spellEntry.m_203334_());
        if (!targeting.clientResolved()) {
            return SpellTarget.findTargets((LivingEntity)this.player, spellEntry, SpellTarget.SearchResult.empty(), true, range);
        }
        SpellCast.TargetSnapshot snapshot = this.latestSnapshot(((ResourceKey)spellEntry.m_203543_().get()).m_135782_());
        if (snapshot == null || !((level = this.player.m_9236_()) instanceof ServerLevel)) {
            return SpellTarget.SearchResult.empty();
        }
        ServerLevel serverWorld = (ServerLevel)level;
        float scaledRange = range * EntityScale.of((Entity)this.player);
        float allowed = scaledRange + SpellEngineMod.config.spell_target_range_tolerance;
        float squaredAllowed = allowed * allowed;
        Vec3 origin = this.player.m_146892_();
        ArrayList<Entity> targets = new ArrayList<Entity>();
        for (Integer targetId : snapshot.entityIds()) {
            Entity entity = serverWorld.m_143317_(targetId.intValue());
            if (entity == null || entity.m_213877_() || !(entity.m_20191_().m_272282_(origin) <= (double)squaredAllowed)) continue;
            targets.add(entity);
        }
        Vec3 location = snapshot.location();
        if (location != null && location.m_82557_(origin) > (double)squaredAllowed) {
            location = origin.m_82549_(location.m_82546_(origin).m_82541_().m_82490_((double)scaledRange));
        }
        return new SpellTarget.SearchResult(targets, location);
    }

    public void tick() {
        SpellCast.Process process = this.process;
        if (process == null) {
            return;
        }
        Spell spell = (Spell)process.spell().m_203334_();
        SpellCast.Mode mode = SpellCast.Mode.from(spell);
        long time = this.player.m_9236_().m_46467_();
        if (!this.player.m_6084_() || this.player.m_21205_().m_41720_() != process.item() || ((SpellCaster.Player)this.player).getCooldownManager().isCoolingDown(process.spell())) {
            this.requestClear();
            return;
        }
        switch (mode) {
            case CASTING: {
                if (process.spellCastTicksSoFar(time) < process.length()) break;
                this.fire(process.spell(), SpellCast.Action.RELEASE, 1.0f);
                if (this.process != process) break;
                this.requestClear();
                break;
            }
            case CHANNEL: {
                if (process.isDue(time)) {
                    process.markDue();
                    this.fire(process.spell(), SpellCast.Action.CHANNEL, process.progress(time).ratio());
                }
                if (this.process != process || !(process.progress(time).ratio() >= 1.0f)) break;
                this.fire(process.spell(), SpellCast.Action.RELEASE, 1.0f);
                if (this.process != process) break;
                this.requestClear();
                break;
            }
            case CHARGED: {
                break;
            }
        }
    }

    public void requestCast(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) {
        Holder.Reference spellEntry = RegistryHelper.getEntry(SpellRegistry.from(this.player.m_9236_()), spellId).orElse(null);
        if (spellEntry == null) {
            return;
        }
        Spell spell = (Spell)spellEntry.m_203334_();
        if (spell.active == null) {
            return;
        }
        if (SpellContainerSource.getFirstSourceOfSpell(spellId, this.player) == null) {
            return;
        }
        ItemStack itemStack = this.player.m_21205_();
        SpellCast.Attempt attempt = SpellCasting.attempt(this.player, itemStack, spellId);
        if (!attempt.isSuccess()) {
            return;
        }
        if (this.process != null) {
            this.requestClear();
        }
        this.submitTargets(spellId, snapshot);
        if (SpellParameters.isInstantCast((Holder<Spell>)spellEntry, (LivingEntity)this.player)) {
            this.fire((Holder<Spell>)spellEntry, SpellCast.Action.RELEASE, 1.0f);
            return;
        }
        SpellCast.Duration details = SpellParameters.getCastTimeDetails((LivingEntity)this.player, spell);
        SpellCast.Process process = new SpellCast.Process((LivingEntity)this.player, (Holder<Spell>)spellEntry, itemStack.m_41720_(), details.speed(), details.length(), this.player.m_9236_().m_46467_());
        this.channelTickIndex = 0;
        this.setProcess(process);
        SoundHelper.playSound(this.player.m_9236_(), (Entity)this.player, spell.active.cast.start_sound);
    }

    public void requestEnd(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) {
        this.submitTargets(spellId, snapshot);
        SpellCast.Process process = this.process;
        if (process == null || !process.id().equals((Object)spellId)) {
            return;
        }
        Spell spell = (Spell)process.spell().m_203334_();
        SpellCast.Mode mode = SpellCast.Mode.from(spell);
        float ratio = process.progress(this.player.m_9236_().m_46467_()).ratio();
        switch (mode) {
            case CASTING: {
                this.requestClear();
                break;
            }
            case CHANNEL: {
                this.fire(process.spell(), SpellCast.Action.RELEASE, ratio);
                if (this.process != process) break;
                this.requestClear();
                break;
            }
            case CHARGED: {
                Spell.Active.Cast.Charge charge = spell.active.cast.charge;
                if (charge != null && ratio < charge.min_release_ratio) {
                    this.requestClear();
                    return;
                }
                this.fire(process.spell(), SpellCast.Action.RELEASE, ratio);
                if (this.process != process) break;
                this.requestClear();
                break;
            }
            default: {
                this.requestClear();
            }
        }
    }

    public void submitTargets(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot) {
        Holder.Reference spellEntry = RegistryHelper.getEntry(SpellRegistry.from(this.player.m_9236_()), spellId).orElse(null);
        if (spellEntry == null) {
            return;
        }
        this.latestSnapshot = snapshot.sanitizedFor(SpellCast.Option.Targeting.of((Spell)spellEntry.m_203334_()));
        this.latestSnapshotSpellId = spellId;
    }

    @Nullable
    public SpellCast.TargetSnapshot latestSnapshot(ResourceLocation spellId) {
        return spellId.equals((Object)this.latestSnapshotSpellId) ? this.latestSnapshot : null;
    }

    public record Ports(Supplier<List<SpellCast.Option>> resolveOptions, Binding<String> processSync, Binding<String> optionsSync) {
    }
}

