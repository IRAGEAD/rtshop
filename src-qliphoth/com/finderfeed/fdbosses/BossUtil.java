/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.FDLibCalls
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData
 *  com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor
 *  com.finderfeed.fdlib.util.FDUtil
 *  com.google.common.collect.Multimap
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.MobType
 *  net.minecraft.world.entity.ai.attributes.Attribute
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier
 *  net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.targeting.TargetingConditions
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.enchantment.EnchantmentHelper
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.storage.loot.LootPool
 *  net.minecraft.world.level.storage.loot.LootTable
 *  net.minecraft.world.level.storage.loot.entries.LootItem
 *  net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer
 *  net.minecraft.world.level.storage.loot.entries.LootTableReference
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.PacketDistributor$TargetPoint
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package com.finderfeed.fdbosses;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.client.particles.smoke_particle.BigSmokeParticleOptions;
import com.finderfeed.fdbosses.content.entities.chesed_boss.falling_block.ChesedFallingBlock;
import com.finderfeed.fdbosses.content.entities.geburah.GeburahEntity;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthAttackType;
import com.finderfeed.fdbosses.content.entities.malkuth_boss.MalkuthEntity;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdbosses.mixin.LootTableAccessor;
import com.finderfeed.fdbosses.packets.PosLevelEventPacket;
import com.finderfeed.fdlib.FDLibCalls;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.systems.particle.particle_emitter.EmitterProcessor;
import com.finderfeed.fdlib.systems.particle.particle_emitter.ParticleEmitterData;
import com.finderfeed.fdlib.systems.particle.particle_emitter.processors.BoundToEntityProcessor;
import com.finderfeed.fdlib.util.FDUtil;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BossUtil {
    public static final int JUST_ENOUGH_DAMAGE = 9000000;
    public static final Random random = new Random();
    public static final TargetingConditions ALL = TargetingConditions.m_148353_().m_26888_(p -> true).m_148355_().m_26893_();
    public static final int CHESED_GET_BLOCKS_FROM_EARTH_EVENT = 1;
    public static final int RADIAL_EARTHQUAKE_PARTICLES = 2;
    public static final int ROCKFALL_PARTICLES = 3;
    public static final int CHESED_RAY_EXPLOSION = 4;
    public static final int CHESED_RAY_ATTACK_SMOKE = 5;
    public static final int CHESED_BOOM_PARTICLES = 6;
    public static final int MALKUTH_CANNON_SHOOT = 7;
    public static final int MALKUTH_SWORD_CHARGE_PARTICLES = 8;
    public static final int MALKUTH_FLOAT_PARTICLES = 9;
    public static final int MALKUTH_FIREBALL_EXPLODE = 10;
    public static final int MALKUTH_VOLCANO_ERRUPTION = 11;
    public static final int MALKUTH_SWORD_INSERT_PARTICLES = 12;
    public static final int MALKUTH_PLAYER_FIREBALL_EXPLODE = 13;
    public static final int GEBURAH_RAY_PARTICLES = 14;
    public static final int GEBURAH_RAY_CHARGE_PARTICLES = 15;
    public static final int GEBURAH_WEAPONS_START_LASER = 16;
    public static final int JUDGEMENT_BIRD_RAY_PARTICLES = 17;
    public static final int TRIGGER_GEBURAH_SIN_PUNISHMENT_ATTACK_EFFECT = 18;
    public static final int TRIGGER_GEBURAH_SIN_PUNISHMENT_ATTACK_IMPACT = 19;
    public static final int GEBURAH_PREPARE_RAYS = 20;
    public static final int GEBURAH_PREPARE_RAYS_FRAMES = 21;
    public static final int DIVINE_GEAR_RAY_PARTICLES = 22;
    public static final int CHESED_ADD_ROCKFALL_PARTICLE_EMITTER = 23;

    public static boolean isPlayerInSurvival(Player player) {
        return !player.m_7500_() && !player.m_5833_();
    }

    public static CompoundTag getOrCreateTag(CompoundTag main, String name) {
        if (!main.m_128441_(name)) {
            main.m_128365_(name, (Tag)new CompoundTag());
        }
        return main.m_128469_(name);
    }

    public static CompoundTag getPlayerTag(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return BossUtil.getOrCreateTag(persistentData, "fdbosses_data");
    }

    public static Vec3 matTransformDirectionVec3(Matrix4f mat, Vec3 v) {
        Vector3f v1 = mat.transformDirection((float)v.f_82479_, (float)v.f_82480_, (float)v.f_82481_, new Vector3f());
        return new Vec3((double)v1.x, (double)v1.y, (double)v1.z);
    }

    public static int randomPlusMinus() {
        return random.nextBoolean() ? -1 : 1;
    }

    public static void createOnEarthBlockExplosionEffect(Level level, Vec3 position, Vec3 attackDirection, int intensity, BlockState fallback) {
        attackDirection = attackDirection.m_82541_();
        Vec3 explosionVec = new Vec3(attackDirection.f_82479_, attackDirection.f_82480_, attackDirection.f_82481_);
        List<BlockState> states = BossUtil.collectStates(level, position, 1);
        if (states.isEmpty() && fallback != null) {
            states.add(fallback);
        }
        if (states.isEmpty()) {
            return;
        }
        for (int i = 0; i < intensity; ++i) {
            Vec3 direction = new Vec3((double)(0.05f + level.f_46441_.m_188501_() * 0.6f), 0.0, 0.0).m_82524_((float)Math.PI * 2 * level.f_46441_.m_188501_());
            float randomYSpeed = 0.25f + level.f_46441_.m_188501_() * 0.6f;
            Vec3 speed = new Vec3(direction.f_82479_, (double)randomYSpeed, direction.f_82481_);
            float explosionVecContribution = 0.7f;
            speed = speed.m_82520_(explosionVec.f_82479_ * (double)explosionVecContribution, 0.0, explosionVec.f_82481_ * (double)explosionVecContribution);
            float rndOffs = level.f_46441_.m_188501_();
            Vec3 spawnOffset = position.m_82549_(direction.m_82541_().m_82542_((double)rndOffs, (double)rndOffs, (double)rndOffs));
            ChesedFallingBlock fallingBlock = ChesedFallingBlock.summon(level, states.get(level.f_46441_.m_188503_(states.size())), spawnOffset, speed, 0.0f, 0.05f);
            float rnd = level.f_46441_.m_188501_() * 0.05f;
            FDLibCalls.addParticleEmitter((Level)level, (double)120.0, (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 25).size(1.5f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(fallingBlock.m_19879_(), Vec3.f_82478_)).position(spawnOffset).build());
        }
    }

    private static List<BlockState> collectStates(Level level, Vec3 pos, int radius) {
        BlockPos blockPos = new BlockPos((int)Math.floor(pos.f_82479_), (int)Math.floor(pos.f_82480_), (int)Math.floor(pos.f_82481_));
        ArrayList<BlockState> blockStates = new ArrayList<BlockState>();
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -radius; y <= radius; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    BlockPos offset = blockPos.m_7918_(x, y, z);
                    BlockState blockState = level.m_8055_(offset);
                    if (blockState.m_60795_() || !blockState.m_60838_((BlockGetter)level, offset)) continue;
                    blockStates.add(blockState);
                }
            }
        }
        return blockStates;
    }

    public static Vec3 calculateMortarProjectileVelocity(Vec3 startPos, Vec3 endPos, double gravity, int tickTravelTime) {
        Vec3 between = endPos.m_82546_(startPos);
        double horizontalDistance = Math.sqrt(between.f_82479_ * between.f_82479_ + between.f_82481_ * between.f_82481_);
        double d = between.f_82480_;
        double horizontalSpeed = horizontalDistance / (double)tickTravelTime;
        double verticalSpeed = (gravity * (double)tickTravelTime * (double)tickTravelTime / 2.0 - d) / (double)(-tickTravelTime);
        Vec3 result = between.m_82542_(1.0, 0.0, 1.0).m_82541_().m_82542_(horizontalSpeed, 0.0, horizontalSpeed).m_82520_(0.0, verticalSpeed, 0.0);
        return result;
    }

    public static float transformDamage(Level level, float damage) {
        Difficulty difficulty = level.m_46791_();
        switch (difficulty) {
            case EASY: {
                return BossConfigs.BOSS_CONFIG.get().easyDifficultyBossDamageMultiplier * damage;
            }
            case NORMAL: {
                return BossConfigs.BOSS_CONFIG.get().normalDifficultyBossDamageMultiplier * damage;
            }
            case HARD: {
                return BossConfigs.BOSS_CONFIG.get().hardDifficultyBossDamageMultiplier * damage;
            }
            case PEACEFUL: {
                return BossConfigs.BOSS_CONFIG.get().peacefulDifficultyBossDamageMuliplier * damage;
            }
        }
        return BossConfigs.BOSS_CONFIG.get().hardDifficultyBossDamageMultiplier * damage;
    }

    public static void applyGravity(Entity entity, double gravity) {
        entity.m_20256_(entity.m_20184_().m_82520_(0.0, -gravity, 0.0));
    }

    public static double getToolDamage(LivingEntity owner, Entity target, ItemStack itemStack) {
        AttributeInstance attribute = owner.m_21051_(Attributes.f_22281_);
        double base = attribute.m_22115_();
        Multimap modifiers = itemStack.m_41638_(EquipmentSlot.MAINHAND);
        if (modifiers.containsKey((Object)Attributes.f_22281_)) {
            Collection attributes = modifiers.get((Object)Attributes.f_22281_);
            Map<AttributeModifier.Operation, List<AttributeModifier>> operationAttributes = BossUtil.modifierCollectionToOperationMap(attributes);
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.ADDITION)) {
                base += mod.m_22218_();
            }
            double damage = base;
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.MULTIPLY_BASE)) {
                damage += base * mod.m_22218_();
            }
            for (AttributeModifier mod : operationAttributes.get(AttributeModifier.Operation.MULTIPLY_TOTAL)) {
                damage *= 1.0 + mod.m_22218_();
            }
            return damage += (double)EnchantmentHelper.m_44833_((ItemStack)itemStack, (MobType)MobType.f_21640_);
        }
        return 0.0;
    }

    private static Map<AttributeModifier.Operation, List<AttributeModifier>> modifierCollectionToOperationMap(Collection<AttributeModifier> collection) {
        LinkedHashMap<AttributeModifier.Operation, List<AttributeModifier>> operationListMap = new LinkedHashMap<AttributeModifier.Operation, List<AttributeModifier>>();
        operationListMap.put(AttributeModifier.Operation.ADDITION, new ArrayList());
        operationListMap.put(AttributeModifier.Operation.MULTIPLY_BASE, new ArrayList());
        operationListMap.put(AttributeModifier.Operation.MULTIPLY_TOTAL, new ArrayList());
        for (AttributeModifier mod : collection) {
            ((List)operationListMap.get(mod.m_22217_())).add(mod);
        }
        return operationListMap;
    }

    public static Predicate<Entity> entityInVerticalRadiusPredicate(Vec3 pos, float radius) {
        return entity -> {
            double z;
            double x = pos.f_82479_ - entity.m_20185_();
            return x * x + (z = pos.f_82481_ - entity.m_20189_()) * z <= (double)(radius * radius);
        };
    }

    public static void geburahPrepareRoundAndRoundLasers(ServerLevel serverLevel, Vec3 pos, double radius, GeburahEntity geburah) {
        BossUtil.posEvent(serverLevel, pos, 21, geburah.m_19879_(), radius);
    }

    public static void geburahChargeConstantLaserParticles(ServerLevel serverLevel, Vec3 pos, double radius, GeburahEntity geburah) {
        BossUtil.posEvent(serverLevel, pos, 20, geburah.m_19879_(), radius);
    }

    public static void geburahTriggerSinPunishmentAttackImpactEffect(ServerLevel serverLevel, Vec3 pos, double radius, int attackRadius) {
        BossUtil.posEvent(serverLevel, pos, 19, attackRadius, radius);
    }

    public static void geburahTriggerSinPunishmentAttack(ServerLevel serverLevel, Vec3 pos, double radius, GeburahEntity geburah) {
        BossUtil.posEvent(serverLevel, pos, 18, geburah.m_19879_(), radius);
    }

    public static void geburahWeaponsStartLaser(ServerLevel serverLevel, Vec3 pos, double radius, GeburahEntity geburah) {
        BossUtil.posEvent(serverLevel, pos, 16, geburah.m_19879_(), radius);
    }

    public static void judgementBirdRayParticles(ServerLevel serverLevel, Vec3 pos, double radius, Vec3 direction) {
        BossUtil.posEvent(serverLevel, pos, 17, FDUtil.encodeDirection((Vec3)direction), radius);
    }

    public static void geburahRayChargeParticles(ServerLevel serverLevel, Vec3 pos, double radius, GeburahEntity geburah) {
        BossUtil.posEvent(serverLevel, pos, 15, geburah.m_19879_(), radius);
    }

    public static void geburahRayParticles(ServerLevel serverLevel, Vec3 pos, double radius, Vec3 direction) {
        BossUtil.posEvent(serverLevel, pos, 14, FDUtil.encodeDirection((Vec3)direction), radius);
    }

    public static void divineGearRayParticles(ServerLevel serverLevel, Vec3 pos, double radius, Vec3 direction) {
        BossUtil.posEvent(serverLevel, pos, 22, FDUtil.encodeDirection((Vec3)direction), radius);
    }

    public static void chesedRockfallEmitter(ServerLevel level, Vec3 pos, double radius, int chesedEntityId) {
        BossUtil.posEvent(level, pos, 23, chesedEntityId, radius);
    }

    public static void malkuthSwordsInsertParticles(ServerLevel serverLevel, Vec3 pos, double radius, int malkuthEntityId) {
        BossUtil.posEvent(serverLevel, pos, 12, malkuthEntityId, radius);
    }

    public static void volcanoErruptionParticles(ServerLevel serverLevel, Vec3 pos, int radius16, double sendRadius) {
        BossUtil.posEvent(serverLevel, pos, 11, radius16, sendRadius);
    }

    public static void malkuthFireballExplosionParticles(ServerLevel serverLevel, Vec3 pos, MalkuthAttackType type) {
        BossUtil.posEvent(serverLevel, pos, 10, type.isFire() ? 0 : 1, 60.0);
    }

    public static void malkuthPlayerFireballExplosionParticles(ServerLevel serverLevel, Vec3 pos, MalkuthAttackType type) {
        BossUtil.posEvent(serverLevel, pos, 13, type.isFire() ? 0 : 1, 60.0);
    }

    public static void malkuthFloatParticles(ServerLevel serverLevel, MalkuthEntity malkuthEntity) {
        BossUtil.posEvent(serverLevel, malkuthEntity.m_20182_(), 9, malkuthEntity.m_19879_(), 60.0);
    }

    public static void geburahSinPunishmentAttackServerEffect(Level level, Vec3 pos, float radius, BlockState state) {
        float length = (float)Math.PI * 2 * radius;
        float step = 500.0f / length;
        float stonesDirStep = 0.3926991f;
        for (float i = 0.0f; i < length; i += step) {
            float p = i / length;
            Vec3 direction = new Vec3(1.0, 0.0, 0.0).m_82524_((float)Math.PI * 2 * p + random.nextFloat() * (float)Math.PI / 16.0f);
            Vec3 between = direction.m_82490_((double)radius);
            Vec3 reversedDir = direction.m_82548_();
            Vec3 ppos = pos.m_82549_(between);
            int stonesCount = 2;
            for (int c = 0; c < stonesCount; ++c) {
                float stonesP = (float)c / ((float)stonesCount - 1.0f);
                for (int dirswitch = -1; dirswitch <= 1; dirswitch += 2) {
                    for (int k = -1; k < 1; ++k) {
                        Vec3 stoneDir = direction.m_82490_((double)dirswitch).m_82524_((float)k * stonesDirStep);
                        Vec3 speed = stoneDir.m_82490_(1.5 * (double)(0.1f + random.nextFloat() * 0.5f)).m_82520_(0.0, (double)(0.25f + random.nextFloat() * 0.3f) * 1.25, 0.0);
                        float speedScale = random.nextFloat() * 0.8f + 0.1f;
                        speed = speed.m_82542_((double)speedScale, 1.0, (double)speedScale);
                        ChesedFallingBlock block = ChesedFallingBlock.summon(level, state, ppos.m_82520_((double)(random.nextFloat() * 2.0f - 1.0f), 0.0, (double)(random.nextFloat() * 2.0f - 1.0f)), speed, 0.0f);
                        block.softerSound = true;
                        float rnd = random.nextFloat() * 0.05f;
                        FDLibCalls.addParticleEmitter((Level)level, (double)120.0, (ParticleEmitterData)ParticleEmitterData.builder((ParticleOptions)BigSmokeParticleOptions.builder().color(0.35f - rnd, 0.35f - rnd, 0.35f - rnd).lifetime(0, 0, 10).size(1.5f).build()).lifetime(200).processor((EmitterProcessor)new BoundToEntityProcessor(block.m_19879_(), Vec3.f_82478_)).position(pos).build());
                    }
                }
            }
        }
    }

    public static void malkuthCannonShoot(ServerLevel serverLevel, MalkuthAttackType malkuthAttackType, Vec3 pos, Vec3 direction, double radius) {
        int data = FDUtil.encodeDirection((Vec3)direction);
        data <<= 1;
        if (malkuthAttackType.isFire()) {
            ++data;
        }
        BossUtil.posEvent(serverLevel, pos, 7, data, radius);
    }

    public static void malkuthSwordChargeParticles(ServerLevel serverLevel, MalkuthAttackType malkuthAttackType, MalkuthEntity malkuthEntity, double radius) {
        Vec3 yesIEncodeEnumIntoVec3DontJudgeMe = malkuthAttackType.isFire() ? new Vec3(1.0, 0.0, 0.0) : new Vec3(-1.0, 0.0, 0.0);
        yesIEncodeEnumIntoVec3DontJudgeMe = yesIEncodeEnumIntoVec3DontJudgeMe.m_82549_(malkuthEntity.m_20182_());
        BossUtil.posEvent(serverLevel, yesIEncodeEnumIntoVec3DontJudgeMe, 8, malkuthEntity.m_19879_(), radius);
    }

    public static void chesedRaySmoke(ServerLevel level, Vec3 pos, Vec3 direction, double radius) {
        BossUtil.posEvent(level, pos, 5, FDUtil.encodeDirection((Vec3)direction), radius);
    }

    public static void chesedBoomParticles(ServerLevel level, Vec3 pos, int radiusFromCenter, double packetRadius) {
        BossUtil.posEvent(level, pos, 6, radiusFromCenter, packetRadius);
    }

    public static void chesedRayExplosion(ServerLevel level, Vec3 pos, Vec3 direction, double radius, int particlesCount, float sizeModifier) {
        if (particlesCount > 15) {
            throw new RuntimeException("Cannot encode more than 16 particles count");
        }
        if (sizeModifier > 1.0f) {
            throw new RuntimeException("Cannot encode size modifier > 1");
        }
        direction = direction.m_82541_();
        int dx = (int)Math.round((direction.f_82479_ + 1.0) / 2.0 * 255.0);
        int dy = (int)Math.round((direction.f_82480_ + 1.0) / 2.0 * 255.0);
        int dz = (int)Math.round((direction.f_82481_ + 1.0) / 2.0 * 255.0);
        int size = Math.round(sizeModifier * 15.0f);
        int data = 0;
        data |= size << 28;
        data |= particlesCount << 24;
        data |= dx << 16;
        data |= dy << 8;
        BossUtil.posEvent(level, pos, 4, data |= dz, radius);
    }

    public static void posEvent(ServerLevel level, Vec3 pos, int event, int data, double radius) {
        FDPacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p((double)pos.f_82479_, (double)pos.f_82480_, (double)pos.f_82481_, (double)radius, (ResourceKey)level.m_46472_())), (Object)new PosLevelEventPacket(pos, event, data));
    }

    public static float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return c3 * x * x * x - c1 * x * x;
    }

    public static boolean itemContainsModifierForAttribute(ItemStack itemStack, Attribute attributeHolder) {
        Multimap modifiers = itemStack.m_41638_(EquipmentSlot.MAINHAND);
        return modifiers.containsKey((Object)attributeHolder);
    }

    public static List<Item> getItemsFromLootTable(MinecraftServer server, ResourceLocation lootTable) {
        LootTable loot = server.m_278653_().m_278676_(lootTable);
        return BossUtil.getItemsFromLootTable(server, loot);
    }

    public static List<Item> getItemsFromLootTable(MinecraftServer server, LootTable lootTable) {
        ArrayList<Item> items = new ArrayList<Item>();
        for (LootPool pool : ((LootTableAccessor)lootTable).getPools()) {
            for (LootPoolEntryContainer entry : pool.f_79023_) {
                if (entry instanceof LootItem) {
                    LootItem lootItem = (LootItem)entry;
                    items.add(lootItem.f_79564_);
                    continue;
                }
                if (!(entry instanceof LootTableReference)) continue;
                LootTableReference nestedLootTable = (LootTableReference)entry;
                items.addAll(BossUtil.getItemsFromLootTable(server, nestedLootTable.f_79754_));
            }
        }
        return items;
    }

    public static class StructureTags {
        public static final TagKey<Structure> EYE_OF_CHESED_LOCATED = StructureTags.create("eye_of_chesed_located");
        public static final TagKey<Structure> EYE_OF_MALKUTH_LOCATED = StructureTags.create("eye_of_malkuth_located");
        public static final TagKey<Structure> EYE_OF_GEBURAH_LOCATED = StructureTags.create("eye_of_geburah_located");

        private static TagKey<Structure> create(String id) {
            return TagKey.m_203882_((ResourceKey)Registries.f_256944_, (ResourceLocation)FDBosses.location(id));
        }
    }
}

