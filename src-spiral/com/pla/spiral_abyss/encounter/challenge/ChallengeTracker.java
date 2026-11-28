/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.AbstractArrow
 *  net.minecraft.world.entity.projectile.FishingHook
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.entity.projectile.ThrownEnderpearl
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.ProjectileWeaponItem
 *  net.minecraft.world.item.enchantment.Enchantment
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.phys.Vec3
 */
package com.pla.spiral_abyss.encounter.challenge;

import com.pla.spiral_abyss.encounter.FloorController;
import com.pla.spiral_abyss.encounter.MobDeathLifecycle;
import com.pla.spiral_abyss.encounter.WaveChallenges;
import com.pla.spiral_abyss.encounter.challenge.Challenge;
import com.pla.spiral_abyss.encounter.challenge.ChallengeCounter;
import com.pla.spiral_abyss.entity.ArenaInteractable;
import com.pla.spiral_abyss.platform.AbyssPlatform;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import com.pla.spiral_abyss.platform.event.AbyssEvents;
import com.pla.spiral_abyss.world.ArenaSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class ChallengeTracker {
    private static final int DOT_ATTRIBUTION_TICKS = 300;
    private static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private final ArenaSession session;
    private final int waveEnemies;
    private final List<Challenge> challenges;
    private final List<ChallengeCounter> counters;
    private final int equipmentPartySize;
    private final int requiredEquipmentPlayers;
    private final List<Set<UUID>> armorViolations;
    private final int[] equipmentProgress;
    private final Map<UUID, Shot> shots = new HashMap<UUID, Shot>();
    private final Map<UUID, Use> uses = new HashMap<UUID, Use>();
    private final Map<UUID, Damage> lastHits = new HashMap<UUID, Damage>();
    private final Map<UUID, Damage> delayedHits = new HashMap<UUID, Damage>();
    private final List<Damage> damage = new ArrayList<Damage>();
    private final List<AbyssEvents.LivingDamageEvent> receivedDamage = new ArrayList<AbyssEvents.LivingDamageEvent>();
    private final List<Impact> impacts = new ArrayList<Impact>();
    private final List<AbyssEvents.LivingUseTotemEvent> usedTotems = new ArrayList<AbyssEvents.LivingUseTotemEvent>();
    private final List<AbyssEvents.ShieldBlockEvent> shieldBlocks = new ArrayList<AbyssEvents.ShieldBlockEvent>();
    private final Map<UUID, Death> deaths = new LinkedHashMap<UUID, Death>();
    private final Set<UUID> countedDeaths = new HashSet<UUID>();

    public ChallengeTracker(ArenaSession session, List<Challenge> challenges, int waveEnemies) {
        if (challenges.size() != 3) {
            throw new IllegalArgumentException("A floor needs three available challenges");
        }
        this.session = session;
        this.waveEnemies = waveEnemies;
        this.challenges = List.copyOf(challenges);
        this.equipmentPartySize = session.participants().size();
        this.requiredEquipmentPlayers = Challenge.requiredEquipmentPlayers(this.equipmentPartySize);
        this.armorViolations = challenges.stream().map(c -> new HashSet()).toList();
        this.equipmentProgress = new int[challenges.size()];
        this.counters = challenges.stream().map(c -> new ChallengeCounter(c.requiredCount(waveEnemies))).toList();
        this.update(0);
    }

    public List<Progress> progress() {
        ArrayList<Progress> result = new ArrayList<Progress>();
        for (int index = 0; index < this.challenges.size(); ++index) {
            Challenge challenge = this.challenges.get(index);
            ChallengeCounter counter = this.counters.get(index);
            boolean count = challenge.type() != Challenge.Type.TIME && challenge.type() != Challenge.Type.NO_ARMOR && challenge.type() != Challenge.Type.ARMOR_ENCHANTMENT;
            Component description = challenge.description(this.waveEnemies);
            if (challenge.type() == Challenge.Type.NO_ARMOR || challenge.type() == Challenge.Type.ARMOR_ENCHANTMENT) {
                description = Component.m_237110_((String)"challenge.spiral_abyss.players_progress", (Object[])new Object[]{description, this.equipmentProgress[index], this.requiredEquipmentPlayers});
            }
            result.add(new Progress(description, count ? counter.progress() : 0, count ? counter.target() : 0, counter.complete(), counter.failed()));
        }
        return List.copyOf(result);
    }

    public int stars() {
        return (int)this.counters.stream().filter(ChallengeCounter::complete).count();
    }

    /*
     * WARNING - void declaration
     */
    public void update(int elapsedTicks) {
        void var3_15;
        int index;
        for (Impact impact : this.impacts) {
            if (impact.event.isCanceled() || impact.event.getImpactResult() != AbyssEvents.ProjectileImpactEvent.ImpactResult.DEFAULT) continue;
            for (index = 0; index < this.challenges.size(); ++index) {
                if (this.challenges.get(index).type() != impact.type) continue;
                this.counters.get(index).increment();
            }
        }
        this.impacts.clear();
        for (AbyssEvents.LivingUseTotemEvent livingUseTotemEvent : this.usedTotems) {
            if (livingUseTotemEvent.isCanceled()) continue;
            for (index = 0; index < this.challenges.size(); ++index) {
                if (this.challenges.get(index).type() != Challenge.Type.TOTEM) continue;
                this.counters.get(index).increment();
            }
        }
        this.usedTotems.clear();
        for (AbyssEvents.ShieldBlockEvent shieldBlockEvent : this.shieldBlocks) {
            if (shieldBlockEvent.isCanceled() || !(shieldBlockEvent.getBlockedDamage() > 0.0f)) continue;
            for (index = 0; index < this.challenges.size(); ++index) {
                if (this.challenges.get(index).type() != Challenge.Type.SHIELD_BLOCK) continue;
                this.counters.get(index).increment();
            }
        }
        this.shieldBlocks.clear();
        for (AbyssEvents.LivingDamageEvent livingDamageEvent : this.receivedDamage) {
            if (livingDamageEvent.isCanceled() || !(livingDamageEvent.getAmount() > 0.0f)) continue;
            for (index = 0; index < this.challenges.size(); ++index) {
                if (this.challenges.get(index).type() != Challenge.Type.TAKE_DAMAGE) continue;
                this.counters.get(index).increment();
            }
        }
        this.receivedDamage.clear();
        for (Damage damage : this.damage) {
            if (damage.event.isCanceled() || !(damage.event.getAmount() > 0.0f)) continue;
            for (index = 0; index < this.challenges.size(); ++index) {
                Challenge challenge = this.challenges.get(index);
                if (challenge.type() == Challenge.Type.MOB_EFFECT && damage.hit.mobEffects.containsAll(challenge.ids())) {
                    this.counters.get(index).incrementDistinct(damage.mob.m_20148_());
                }
                if (challenge.type() != Challenge.Type.TRICKSHOT || !damage.tracked || !damage.hit.trickshot) continue;
                this.counters.get(index).increment();
            }
        }
        this.damage.clear();
        Iterator<Death> iterator = this.deaths.values().iterator();
        while (iterator.hasNext()) {
            Death death = iterator.next();
            if (death.event.isCanceled() || death.damageEvent != null && (death.damageEvent.isCanceled() || !(death.damageEvent.getAmount() > 0.0f))) {
                iterator.remove();
                continue;
            }
            if (death.mob.m_21224_() || death.mob.m_146911_() == Entity.RemovalReason.KILLED) {
                if (this.countedDeaths.add(death.mob.m_20148_())) {
                    this.countKill(death.mob.m_20148_(), death.hit);
                }
                iterator.remove();
                continue;
            }
            if (!death.mob.m_213877_() && !death.mob.m_6084_()) continue;
            iterator.remove();
        }
        List<ServerPlayer> combatants = this.session.combatants();
        boolean bl = false;
        while (var3_15 < this.challenges.size()) {
            Challenge challenge = this.challenges.get((int)var3_15);
            ChallengeCounter counter = this.counters.get((int)var3_15);
            switch (challenge.type()) {
                case TIME: {
                    if (!WaveChallenges.beforeDeadline(elapsedTicks, challenge.target())) {
                        counter.fail();
                    }
                    counter.set(counter.failed() ? 0 : challenge.target());
                    break;
                }
                case NO_ARMOR: {
                    Set<UUID> violations = this.armorViolations.get((int)var3_15);
                    combatants.stream().filter(player -> challenge.forbiddenSlots().stream().anyMatch(slot -> !player.m_6844_(slot).m_41619_())).forEach(player -> violations.add(player.m_20148_()));
                    this.updateNoArmor((int)var3_15, combatants);
                    break;
                }
                case ARMOR_ENCHANTMENT: {
                    this.equipmentProgress[var3_15] = Math.min(this.requiredEquipmentPlayers, (int)combatants.stream().filter(player -> ChallengeTracker.wearsArmorEnchantments(player, challenge.ids())).count());
                    counter.set(this.equipmentProgress[var3_15] >= this.requiredEquipmentPlayers ? 1 : 0);
                    break;
                }
            }
            ++var3_15;
        }
        long l = this.session.level().m_46467_();
        this.lastHits.values().removeIf(hit -> now - hit.hit.attributedAt > 300L || hit.event.isCanceled() || !(hit.event.getAmount() > 0.0f));
        this.delayedHits.values().removeIf(hit -> hit.mob.m_213877_() || !this.challengeTarget(hit.mob));
    }

    private void updateNoArmor(int index, List<ServerPlayer> combatants) {
        Set<UUID> violations = this.armorViolations.get(index);
        ChallengeCounter counter = this.counters.get(index);
        if (this.equipmentPartySize - violations.size() < this.requiredEquipmentPlayers) {
            counter.fail();
        }
        long qualifying = combatants.stream().filter(player -> !violations.contains(player.m_20148_())).count();
        this.equipmentProgress[index] = Math.min(this.requiredEquipmentPlayers, (int)qualifying);
        counter.set(!counter.failed() && qualifying >= (long)this.requiredEquipmentPlayers ? 1 : 0);
    }

    private boolean tracked(LivingEntity mob) {
        return this.session.controller().state() == FloorController.State.RUNNING && this.session.controller().owns((Entity)mob) && this.session.controller().enemies().contains(mob);
    }

    private boolean challengeTarget(LivingEntity mob) {
        if (this.session.controller().state() != FloorController.State.RUNNING || mob.m_9236_() != this.session.level() || mob instanceof ArenaInteractable || mob instanceof Player) {
            return false;
        }
        Vec3 center = this.session.bounds().center();
        if (Math.abs(mob.m_20185_() - center.f_82479_) > 128.0 || Math.abs(mob.m_20189_() - center.f_82481_) > 128.0 || mob.m_20191_().f_82292_ > 240.0) {
            return false;
        }
        CompoundTag data = AbyssPlatform.data((Entity)mob);
        if (data.m_128403_("spiral_abyss_host") && !this.session.host().equals(data.m_128342_("spiral_abyss_host"))) {
            return false;
        }
        return !data.m_128403_("spiral_abyss_run") || this.session.controller().owns((Entity)mob) || this.session.controller().bossEnvironments().ownsPlacement((Entity)mob);
    }

    public void projectileLaunched(Projectile projectile) {
        ServerPlayer player;
        Entity entity;
        if (this.session.controller().state() == FloorController.State.RUNNING && (entity = projectile.m_19749_()) instanceof ServerPlayer && this.session.isCombatant(player = (ServerPlayer)entity)) {
            this.shots.put(projectile.m_20148_(), new Shot(player.m_20148_(), this.projectileWeapon(player), !player.m_20096_() && !player.m_20072_() && !player.m_20077_() && !player.m_150110_().f_35935_));
        }
    }

    public void projectileImpacted(AbyssEvents.ProjectileImpactEvent event, LivingEntity mob) {
        Challenge.Type type;
        ServerPlayer player;
        Entity entity;
        if (!(this.tracked(mob) && (entity = event.getProjectile().m_19749_()) instanceof ServerPlayer && this.session.isCombatant(player = (ServerPlayer)entity))) {
            return;
        }
        Challenge.Type type2 = event.getProjectile() instanceof FishingHook ? Challenge.Type.FISHING : (type = event.getProjectile() instanceof ThrownEnderpearl ? Challenge.Type.ENDER_PEARL_HIT : null);
        if (type != null) {
            this.impacts.add(new Impact(type, event));
        }
    }

    public void totemUsed(ServerPlayer player, AbyssEvents.LivingUseTotemEvent event) {
        if (this.session.controller().state() == FloorController.State.RUNNING && this.session.isCombatantUsingTotem(player) && event.getTotem().m_150930_(Items.f_42747_)) {
            this.usedTotems.add(event);
        }
    }

    public void shieldBlocked(ServerPlayer player, AbyssEvents.ShieldBlockEvent event) {
        LivingEntity mob;
        Entity entity;
        if (!(this.session.controller().state() == FloorController.State.RUNNING && this.session.isCombatant(player) && AbyssPlatform.canShieldBlock(player.m_21211_()) && event.getBlockedDamage() > 0.0f)) {
            return;
        }
        Entity attacker = event.getDamageSource().m_7639_();
        if (!(attacker instanceof LivingEntity) && (entity = event.getDamageSource().m_7640_()) instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            attacker = projectile.m_19749_();
        }
        if (attacker instanceof LivingEntity && this.tracked(mob = (LivingEntity)attacker)) {
            this.shieldBlocks.add(event);
        }
    }

    public void itemUsed(ServerPlayer player, ItemStack stack) {
        if (this.session.isCombatant(player)) {
            this.uses.put(player.m_20148_(), new Use(stack.m_41777_(), this.session.level().m_46467_()));
        }
    }

    public void equipmentChanged(ServerPlayer player, EquipmentSlot slot, ItemStack equipped) {
        if (this.session.controller().state() != FloorController.State.RUNNING || !this.session.isCombatant(player) || equipped.m_41619_()) {
            return;
        }
        for (int index = 0; index < this.challenges.size(); ++index) {
            Challenge challenge = this.challenges.get(index);
            if (challenge.type() != Challenge.Type.NO_ARMOR || !challenge.forbiddenSlots().contains(slot)) continue;
            this.armorViolations.get(index).add(player.m_20148_());
            this.updateNoArmor(index, this.session.combatants());
        }
    }

    public void damaged(LivingEntity mob, AbyssEvents.LivingDamageEvent event) {
        if (!this.challengeTarget(mob)) {
            return;
        }
        Hit hit = this.capture(mob, event.getSource());
        if (MobDeathLifecycle.delaysDeathEvent(mob)) {
            this.delayedHits.put(mob.m_20148_(), new Damage(mob, hit, event, this.tracked(mob)));
        }
        if (!(event.getAmount() > 0.0f)) {
            return;
        }
        if (hit == null) {
            return;
        }
        Damage evidence = new Damage(mob, hit, event, this.tracked(mob));
        this.lastHits.put(mob.m_20148_(), evidence);
        this.damage.add(evidence);
    }

    public void playerDamaged(ServerPlayer player, AbyssEvents.LivingDamageEvent event) {
        LivingEntity mob;
        Entity entity;
        if (!(event.getAmount() > 0.0f) || !this.session.isCombatant(player) || player.m_9232_()) {
            return;
        }
        if (event.getSource().m_276093_(DamageTypes.f_286979_) || event.getSource().m_276093_(DamageTypes.f_268724_)) {
            return;
        }
        Entity attacker = event.getSource().m_7639_();
        if (attacker == null && (entity = event.getSource().m_7640_()) instanceof Projectile) {
            Projectile projectile = (Projectile)entity;
            attacker = projectile.m_19749_();
        }
        if (attacker instanceof LivingEntity && this.tracked(mob = (LivingEntity)attacker)) {
            this.receivedDamage.add(event);
        }
    }

    public void died(LivingEntity mob, AbyssEvents.LivingDeathEvent event) {
        Hit hit;
        if (!this.challengeTarget(mob)) {
            return;
        }
        Damage delayed = this.delayedHits.get(mob.m_20148_());
        boolean sameHit = delayed != null && delayed.event.getSource() == event.getSource();
        Hit hit2 = hit = sameHit ? delayed.hit : this.capture(mob, event.getSource());
        if (sameHit && hit != null && this.session.combatants().stream().noneMatch(player -> player.m_20148_().equals(hit.player))) {
            return;
        }
        if (hit != null) {
            this.deaths.put(mob.m_20148_(), new Death(mob, hit, event, sameHit ? delayed.event : null));
        }
    }

    private void countKill(UUID mob, Hit hit) {
        for (int index = 0; index < this.challenges.size(); ++index) {
            boolean qualifies;
            Challenge challenge = this.challenges.get(index);
            switch (challenge.type()) {
                case WEAPON_ENCHANTMENT: {
                    boolean bl;
                    if (!hit.weapon.m_41619_() && challenge.ids().stream().allMatch(id -> ChallengeTracker.hasEnchantment(hit.weapon, id))) {
                        bl = true;
                        break;
                    }
                    bl = false;
                    break;
                }
                case WEAPON: {
                    boolean bl;
                    if (!hit.weapon.m_41619_() && challenge.ids().contains(AbyssRegistries.ITEMS.getKey(hit.weapon.m_41720_()))) {
                        bl = true;
                        break;
                    }
                    bl = false;
                    break;
                }
                case PROJECTILE: {
                    boolean bl = hit.projectile;
                    break;
                }
                case PLAYER_EFFECT: {
                    boolean bl = hit.playerEffects.containsAll(challenge.ids());
                    break;
                }
                case EXPLOSION: {
                    boolean bl = hit.explosion;
                    break;
                }
                case PLAYER_BURNING: {
                    boolean bl = hit.playerBurning;
                    break;
                }
                case MOB_BURNING: {
                    boolean bl = hit.mobBurning;
                    break;
                }
                default: {
                    boolean bl = qualifies = false;
                }
            }
            if (!qualifies) continue;
            this.counters.get(index).incrementDistinct(mob);
        }
    }

    private Hit capture(LivingEntity mob, DamageSource source) {
        Damage prior;
        ServerPlayer p;
        Projectile projectile;
        ServerPlayer player;
        ServerPlayer p2;
        if (source.m_276093_(DamageTypes.f_286979_) || source.m_276093_(DamageTypes.f_268724_)) {
            return null;
        }
        Entity direct = source.m_7640_();
        Shot shot = direct == null ? null : this.shots.get(direct.m_20148_());
        Entity entity = source.m_7639_();
        Object object = entity instanceof ServerPlayer ? (p2 = (ServerPlayer)entity) : (player = direct instanceof Projectile && (entity = (projectile = (Projectile)direct).m_19749_()) instanceof ServerPlayer ? (p = (ServerPlayer)entity) : null);
        if (player == null && shot != null) {
            player = this.session.level().m_7654_().m_6846_().m_11259_(shot.player);
        }
        ItemStack weapon = shot == null ? null : shot.weapon;
        long attributedAt = this.session.level().m_46467_();
        if (player == null && source.m_7639_() == null && source.m_7640_() == null && ChallengeTracker.damageOverTime(source) && (prior = this.lastHits.get(mob.m_20148_())) != null && !prior.event.isCanceled() && prior.event.getAmount() > 0.0f && attributedAt - prior.hit.attributedAt <= 300L) {
            player = this.session.level().m_7654_().m_6846_().m_11259_(prior.hit.player);
            weapon = prior.hit.weapon;
            attributedAt = prior.hit.attributedAt;
        }
        if (player == null || !this.session.isCombatant(player) || player.m_9232_()) {
            return null;
        }
        if (weapon == null) {
            weapon = direct == player && !source.m_276093_(DamageTypes.f_268440_) && !source.m_269533_(DamageTypeTags.f_268415_) && !source.m_269533_(DamageTypeTags.f_268524_) && !ChallengeTracker.damageOverTime(source) ? player.m_21205_().m_41777_() : ItemStack.f_41583_;
        }
        boolean projectile2 = direct instanceof Projectile || source.m_269533_(DamageTypeTags.f_268524_) || direct != null && this.challenges.stream().filter(c -> c.type() == Challenge.Type.PROJECTILE).anyMatch(c -> c.ids().contains(AbyssRegistries.ENTITY_TYPES.getKey(direct.m_6095_())));
        boolean trickshot = direct instanceof AbstractArrow && shot != null && shot.airborne && (weapon.m_150930_(Items.f_42411_) || weapon.m_150930_(Items.f_42717_));
        return new Hit(player.m_20148_(), weapon, projectile2, trickshot, source.m_269533_(DamageTypeTags.f_268415_), player.m_6060_(), mob.m_6060_(), ChallengeTracker.effectIds((LivingEntity)player), ChallengeTracker.effectIds(mob), attributedAt);
    }

    private static boolean damageOverTime(DamageSource source) {
        return source.m_269533_(DamageTypeTags.f_268745_) || source.m_276093_(DamageTypes.f_268515_) || source.m_276093_(DamageTypes.f_268493_);
    }

    private static Set<ResourceLocation> effectIds(LivingEntity entity) {
        HashSet ids = new HashSet();
        entity.m_21220_().forEach(effect -> ids.add(AbyssRegistries.MOB_EFFECTS.getKey(effect.m_19544_())));
        return Set.copyOf(ids);
    }

    private static boolean hasEnchantment(ItemStack stack, ResourceLocation id) {
        Enchantment enchantment = AbyssRegistries.ENCHANTMENTS.getValue(id);
        return enchantment != null && EnchantmentHelper.m_44843_((Enchantment)enchantment, (ItemStack)stack) > 0;
    }

    public static boolean wearsArmorEnchantments(ServerPlayer player, List<ResourceLocation> ids) {
        return ids.stream().allMatch(id -> Arrays.stream(ARMOR).anyMatch(slot -> ChallengeTracker.hasEnchantment(player.m_6844_(slot), id)));
    }

    private ItemStack projectileWeapon(ServerPlayer player) {
        boolean offRanged;
        ItemStack using = player.m_21211_();
        if (!using.m_41619_()) {
            return using.m_41777_();
        }
        Use use = this.uses.get(player.m_20148_());
        if (use != null && use.tick == this.session.level().m_46467_()) {
            return use.weapon.m_41777_();
        }
        ItemStack main = player.m_21205_();
        ItemStack off = player.m_21206_();
        boolean mainRanged = main.m_41720_() instanceof ProjectileWeaponItem || main.m_150930_(Items.f_42713_);
        boolean bl = offRanged = off.m_41720_() instanceof ProjectileWeaponItem || off.m_150930_(Items.f_42713_);
        if (mainRanged && !offRanged) {
            return main.m_41777_();
        }
        if (offRanged && !mainRanged) {
            return off.m_41777_();
        }
        return ItemStack.f_41583_;
    }

    public record Progress(Component description, int progress, int target, boolean complete, boolean failed) {
    }

    private record Impact(Challenge.Type type, AbyssEvents.ProjectileImpactEvent event) {
    }

    private record Damage(LivingEntity mob, Hit hit, AbyssEvents.LivingDamageEvent event, boolean tracked) {
    }

    private record Hit(UUID player, ItemStack weapon, boolean projectile, boolean trickshot, boolean explosion, boolean playerBurning, boolean mobBurning, Set<ResourceLocation> playerEffects, Set<ResourceLocation> mobEffects, long attributedAt) {
    }

    private record Death(LivingEntity mob, Hit hit, AbyssEvents.LivingDeathEvent event, AbyssEvents.LivingDamageEvent damageEvent) {
    }

    private record Shot(UUID player, ItemStack weapon, boolean airborne) {
    }

    private record Use(ItemStack weapon, long tick) {
    }
}

