/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.event.entity.living.LivingDamageEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.living.LivingDropsEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.living.MobEffectEvent$Applicable
 *  net.minecraftforge.event.entity.player.AttackEntityEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerRespawnEvent
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem
 *  net.minecraftforge.event.level.BlockEvent$BreakEvent
 *  net.minecraftforge.event.level.BlockEvent$EntityPlaceEvent
 *  net.minecraftforge.event.level.ExplosionEvent$Detonate
 *  net.minecraftforge.eventbus.api.Event$Result
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.NetworkDirection
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.content.data_components.ItemCoreDataComponent;
import com.finderfeed.fdbosses.content.entities.IEffectImmune;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdbosses.content.entities.chesed_boss.chesed_mini_ray.ChesedMiniRay;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthWeaknessHandler;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.packets.SetClientMalkuthWeaknessAmountPacket;
import com.finderfeed.fdbosses.content.items.CoreItem;
import com.finderfeed.fdbosses.content.items.WeaponCoreItem;
import com.finderfeed.fdbosses.content.projectiles.MalkuthPlayerFireIceBall;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEffects;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid="fdbosses", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BossEvents {
    public static final TagKey<MobEffect> NOT_CURABLE_EFFECTS = TagKey.m_203882_((ResourceKey)Registries.f_256929_, (ResourceLocation)FDBosses.location("not_curable"));
    private static final EquipmentSlot[] EQUIPMENT_SLOTS = new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD};

    @SubscribeEvent
    public static void effectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof IEffectImmune) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void playerEnterWorld(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            FDPacketHandler.INSTANCE.sendTo((Object)new SetClientMalkuthWeaknessAmountPacket(MalkuthWeaknessHandler.getCurrentWeaknessLevel((Player)serverPlayer)), serverPlayer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            FDPacketHandler.INSTANCE.sendTo((Object)new SetClientMalkuthWeaknessAmountPacket(MalkuthWeaknessHandler.getCurrentWeaknessLevel((Player)serverPlayer)), serverPlayer.f_8906_.f_9742_, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    @SubscribeEvent
    public static void preventArenaDestruction(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        if (!level.m_5776_()) {
            BlockState state;
            boolean v;
            BlockPos pos = event.getPos();
            List spawners = level.m_45976_(BossSpawnerEntity.class, new AABB(-100.0, -100.0, -100.0, 100.0, 100.0, 100.0).m_82383_(pos.m_252807_()));
            if (!spawners.isEmpty() && (v = BossEvents.checkBlockStateAllowedToBreakInArena(state = level.m_8055_(pos)))) {
                return;
            }
            Component message = null;
            for (BossSpawnerEntity bossSpawnerEntity : spawners) {
                if (bossSpawnerEntity.canInteractWithBlockPos(pos)) continue;
                message = bossSpawnerEntity.onArenaDestructionMessage();
                event.setCanceled(true);
                break;
            }
            if (event.isCanceled() && event.getPlayer() != null && message != null) {
                event.getPlayer().m_213846_(message);
            }
        }
    }

    public static boolean checkBlockStateAllowedToBreakInArena(BlockState blockState) {
        List<Pattern> patterns = BossConfigs.BOSS_CONFIG.get().blocksAllowedToBreakInArenaPatterns;
        for (Pattern pattern : patterns) {
            ResourceLocation location = BuiltInRegistries.f_256975_.m_7981_((Object)blockState.m_60734_());
            String string = location.toString();
            boolean test = pattern.asPredicate().test(string);
            if (!test) continue;
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void preventArenaDestruction(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor level = event.getLevel();
        if (!level.m_5776_()) {
            Entity entity;
            BlockPos pos = event.getPos();
            List spawners = level.m_45976_(BossSpawnerEntity.class, new AABB(-100.0, -100.0, -100.0, 100.0, 100.0, 100.0).m_82383_(pos.m_252807_()));
            Component message = null;
            for (BossSpawnerEntity bossSpawnerEntity : spawners) {
                if (bossSpawnerEntity.canInteractWithBlockPos(pos)) continue;
                message = bossSpawnerEntity.onArenaDestructionMessage();
                event.setCanceled(true);
                break;
            }
            if (event.isCanceled() && (entity = event.getEntity()) instanceof Player) {
                Player player = (Player)entity;
                if (message != null) {
                    player.m_213846_(message);
                }
            }
        }
    }

    @SubscribeEvent
    public static void preventArenaDestruction(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!level.m_5776_()) {
            Object object;
            List list = event.getAffectedBlocks();
            ListIterator blockPosIterator = list.listIterator();
            List spawners = level.m_45976_(BossSpawnerEntity.class, new AABB(-100.0, -100.0, -100.0, 100.0, 100.0, 100.0).m_82383_(event.getExplosion().getPosition()));
            Component message = null;
            block0: while (blockPosIterator.hasNext()) {
                BlockPos pos = (BlockPos)blockPosIterator.next();
                object = spawners.iterator();
                while (object.hasNext()) {
                    BossSpawnerEntity bossSpawnerEntity = (BossSpawnerEntity)((Object)object.next());
                    if (bossSpawnerEntity.canInteractWithBlockPos(pos)) continue;
                    message = bossSpawnerEntity.onArenaDestructionMessage();
                    blockPosIterator.remove();
                    continue block0;
                }
            }
            if (message != null && (object = event.getExplosion().m_252906_()) instanceof Player) {
                Player player = (Player)object;
                player.m_213846_(message);
            }
        }
    }

    @SubscribeEvent
    public static void livingDeathEvent(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof Player) {
            DamageSource source;
            Player player = (Player)livingEntity;
            if (!player.m_9236_().f_46443_ && event.getSource() != null && (source = event.getSource()).m_276093_(BossDamageSources.CHESED_LOR_EASTER_EGG_ATTACK)) {
                ItemStack itemStack = Items.f_42517_.m_7968_();
                itemStack.m_41714_((Component)player.m_7755_().m_6881_());
                ItemEntity item = new ItemEntity(player.m_9236_(), player.m_20185_(), player.m_20186_(), player.m_20189_(), itemStack);
                player.m_9236_().m_7967_((Entity)item);
            }
        }
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack itemStack = event.getItemStack();
        Player player = event.getEntity();
        Level level = player.m_9236_();
        ItemCoreDataComponent.CoreType itemCore = WeaponCoreItem.getItemCore(itemStack);
        if (!level.f_46443_ && itemCore != null && itemCore == ItemCoreDataComponent.CoreType.FIRE_AND_ICE) {
            Vec3 look = player.m_20154_();
            float speed = 4.0f;
            Vec3 speedVec = look.m_82542_((double)speed, (double)speed, (double)speed);
            MalkuthAttackType attackType = player.m_6047_() ? MalkuthAttackType.ICE : MalkuthAttackType.FIRE;
            MalkuthPlayerFireIceBall.summon((LivingEntity)player, player.m_146892_().m_82520_(0.0, -0.25, 0.0).m_82549_(look), speedVec, attackType, itemStack);
            if (!player.m_7500_()) {
                int cooldown = BossConfigs.BOSS_CONFIG.get().itemConfig.playerMalkuthFireballAbilityCooldown;
                player.m_36335_().m_41524_(itemStack.m_41720_(), cooldown);
            }
            player.m_21011_(event.getHand(), true);
        }
    }

    @SubscribeEvent
    public static void onEntityAttack(AttackEntityEvent event) {
        Entity entity;
        Player player = event.getEntity();
        ItemStack itemStack = player.m_21205_();
        ItemCoreDataComponent.CoreType core = WeaponCoreItem.getItemCore(itemStack);
        if (!player.m_9236_().f_46443_ && (entity = event.getTarget()) instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity)entity;
            if (core != null) {
                Item item = itemStack.m_41720_();
                float power = player.m_36403_(0.5f);
                if (power > 0.5f && core == ItemCoreDataComponent.CoreType.LIGHTNING && BossUtil.itemContainsModifierForAttribute(itemStack, Attributes.f_22281_)) {
                    float p = Mth.m_14036_((float)(BossConfigs.BOSS_CONFIG.get().itemConfig.chanceToSummonLightningStrike / 100.0f), (float)0.0f, (float)1.0f);
                    RandomSource randomSource = livingEntity.m_217043_();
                    float n = randomSource.m_188501_();
                    if (n < p) {
                        ChesedMiniRay.summon(player.m_9236_(), livingEntity, player.m_21205_(), (LivingEntity)player);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void deathEvent(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            Level level = serverPlayer.m_9236_();
            Vec3 pos = serverPlayer.m_20182_();
            float radius = 50.0f;
            List spawners = level.m_6443_(BossSpawnerEntity.class, new AABB((double)(-radius), (double)(-radius), (double)(-radius), (double)radius, (double)radius, (double)radius).m_82383_(pos), bossSpawner -> !bossSpawner.isActive());
            for (BossSpawnerEntity spawner : spawners) {
                Vec3 itemsPos = spawner.getPlayerItemsDropPosition(serverPlayer, pos);
                if (itemsPos == null) continue;
                for (ItemEntity itemEntity : event.getDrops()) {
                    itemEntity.m_146884_(itemsPos);
                    itemEntity.m_20256_(Vec3.f_82478_);
                }
            }
        }
    }

    @SubscribeEvent
    public static void justiceCore(LivingDamageEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!livingEntity.m_9236_().f_46443_) {
            DamageSource source = event.getSource();
            if (source.m_269533_(DamageTypeTags.f_268738_)) {
                return;
            }
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                ItemCoreDataComponent.CoreType core;
                ItemStack itemStack = livingEntity.m_6844_(slot);
                if (itemStack == null || !CoreItem.hasCore(itemStack) || (core = CoreItem.getItemCore(itemStack)) != ItemCoreDataComponent.CoreType.JUSTICE_CORE) continue;
                float amount = event.getAmount();
                CompoundTag data = livingEntity.getPersistentData();
                if (data.m_128441_("fdbosses_last_damage_received")) {
                    float damage = data.m_128457_("fdbosses_last_damage_received");
                    data.m_128350_("fdbosses_last_damage_received", amount);
                    amount = FDMathUtil.clamp((float)amount, (float)0.0f, (float)damage);
                } else {
                    data.m_128350_("fdbosses_last_damage_received", amount);
                }
                int reduction = BossConfigs.BOSS_CONFIG.get().itemConfig.justiceCoreDamageReduction;
                float p = 1.0f - (float)reduction / 100.0f;
                event.setAmount(amount *= p);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.getOriginal().m_9236_().f_46443_) {
            Player original = event.getOriginal();
            Player clone = event.getEntity();
            CompoundTag originalData = original.getPersistentData();
            CompoundTag cloneData = clone.getPersistentData();
            cloneData.m_128379_("fdbosses_boss_codex_received", originalData.m_128471_("fdbosses_boss_codex_received"));
        }
    }

    @SubscribeEvent
    public static void livingDamageEvent(LivingHurtEvent event) {
        LivingEntity damager;
        DamageSource damageSource;
        Entity entity;
        LivingEntity livingEntity = event.getEntity();
        if (!livingEntity.m_9236_().f_46443_ && (entity = (damageSource = event.getSource()).m_7639_()) instanceof LivingEntity && (damager = (LivingEntity)entity).m_21023_((MobEffect)BossEffects.SHOCKED.get())) {
            float amount = event.getAmount();
            float p = Mth.m_14036_((float)(1.0f - BossConfigs.BOSS_CONFIG.get().effectConfig.shockDamageReductionPercent / 100.0f), (float)0.0f, (float)1.0f);
            event.setAmount(amount * p);
        }
    }
}

