/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.server.players.PlayerList
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.MobSpawnType
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.ChunkAccess
 *  net.minecraft.world.phys.HitResult
 */
package com.pla.spiral_abyss.platform.event;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.HitResult;

public final class AbyssEvents {

    public static class AddReloadListenerEvent {
        private final Consumer<PreparableReloadListener> add;

        public AddReloadListenerEvent(Consumer<PreparableReloadListener> add) {
            this.add = add;
        }

        public void addListener(PreparableReloadListener listener) {
            this.add.accept(listener);
        }
    }

    public static class PlayerInteractEvent
    extends Event {
        private final Player entity;
        private final ItemStack item;
        private final BlockPos pos;
        private final Direction face;
        private final Entity target;
        private InteractionResult result = InteractionResult.PASS;

        public PlayerInteractEvent(Player player, ItemStack item, BlockPos pos, Direction face, Entity target) {
            this.entity = player;
            this.item = item;
            this.pos = pos;
            this.face = face;
            this.target = target;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getItemStack() {
            return this.item;
        }

        public Level getLevel() {
            return this.entity.m_9236_();
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public Direction getFace() {
            return this.face;
        }

        public Entity getTarget() {
            return this.target;
        }

        public InteractionResult getCancellationResult() {
            return this.result;
        }

        public void setCancellationResult(InteractionResult value) {
            this.result = value;
        }

        public static class EntityInteractSpecific
        extends PlayerInteractEvent {
            public EntityInteractSpecific(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class EntityInteract
        extends PlayerInteractEvent {
            public EntityInteract(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class RightClickBlock
        extends PlayerInteractEvent {
            public RightClickBlock(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }

        public static class RightClickItem
        extends PlayerInteractEvent {
            public RightClickItem(Player p, ItemStack s, BlockPos b, Direction d, Entity e) {
                super(p, s, b, d, e);
            }
        }
    }

    public static class MobSpawnEvent {

        public static class FinalizeSpawn
        extends Event {
            public ServerLevelAccessor level;
            public MobSpawnType spawnType;
            private boolean spawnCancelled;

            public FinalizeSpawn(ServerLevelAccessor level, MobSpawnType spawnType) {
                this.level = level;
                this.spawnType = spawnType;
            }

            public ServerLevelAccessor getLevel() {
                return this.level;
            }

            public MobSpawnType getSpawnType() {
                return this.spawnType;
            }

            public void setSpawnCancelled(boolean value) {
                this.spawnCancelled = value;
            }

            public boolean isSpawnCancelled() {
                return this.spawnCancelled;
            }
        }

        public static class SpawnPlacementCheck
        extends Event {
            public ServerLevelAccessor level;
            public MobSpawnType spawnType;

            public SpawnPlacementCheck(ServerLevelAccessor level, MobSpawnType spawnType) {
                this.level = level;
                this.spawnType = spawnType;
            }

            public ServerLevelAccessor getLevel() {
                return this.level;
            }

            public MobSpawnType getSpawnType() {
                return this.spawnType;
            }
        }
    }

    public static class TickEvent {

        public static class LevelTickEvent
        extends Event {
            public final Phase phase;
            public Level level;

            public LevelTickEvent(Level level, Phase phase) {
                this.level = level;
                this.phase = phase;
            }
        }

        public static class ServerTickEvent
        extends Event {
            public final Phase phase;
            public MinecraftServer server;

            public ServerTickEvent(MinecraftServer server, Phase phase) {
                this.server = server;
                this.phase = phase;
            }

            public MinecraftServer getServer() {
                return this.server;
            }
        }

        public static enum Phase {
            START,
            END;

        }
    }

    public static class ChunkEvent {

        public static class Load
        extends Event {
            public LevelAccessor level;
            public ChunkAccess chunk;

            public Load(LevelAccessor level, ChunkAccess chunk) {
                this.level = level;
                this.chunk = chunk;
            }

            public LevelAccessor getLevel() {
                return this.level;
            }

            public ChunkAccess getChunk() {
                return this.chunk;
            }
        }
    }

    public static class LevelEvent {

        public static class Unload
        extends Event {
            public LevelAccessor level;

            public Unload(LevelAccessor level) {
                this.level = level;
            }

            public LevelAccessor getLevel() {
                return this.level;
            }
        }
    }

    public static class ExplosionEvent {

        public static class Start
        extends Event {
            public Level level;

            public Start(Level level) {
                this.level = level;
            }

            public Level getLevel() {
                return this.level;
            }
        }

        public static class Detonate
        extends Event {
            public Level level;
            public List<BlockPos> affectedBlocks;

            public Detonate(Level level, List<BlockPos> affectedBlocks) {
                this.level = level;
                this.affectedBlocks = affectedBlocks;
            }

            public Level getLevel() {
                return this.level;
            }

            public List<BlockPos> getAffectedBlocks() {
                return this.affectedBlocks;
            }
        }
    }

    public static class BlockEvent {

        public static class EntityPlaceEvent
        extends Event {
            public LevelAccessor level;
            public BlockPos pos;
            public BlockState placedBlock;
            public Entity entity;

            public EntityPlaceEvent(LevelAccessor level, BlockPos pos, BlockState placedBlock, Entity entity) {
                this.level = level;
                this.pos = pos;
                this.placedBlock = placedBlock;
                this.entity = entity;
            }

            public LevelAccessor getLevel() {
                return this.level;
            }

            public BlockPos getPos() {
                return this.pos;
            }

            public BlockState getPlacedBlock() {
                return this.placedBlock;
            }

            public Entity getEntity() {
                return this.entity;
            }
        }

        public static class BreakEvent
        extends Event {
            public LevelAccessor level;
            public BlockPos pos;
            public BlockState state;
            public Player player;

            public BreakEvent(LevelAccessor level, BlockPos pos, BlockState state, Player player) {
                this.level = level;
                this.pos = pos;
                this.state = state;
                this.player = player;
            }

            public LevelAccessor getLevel() {
                return this.level;
            }

            public BlockPos getPos() {
                return this.pos;
            }

            public BlockState getState() {
                return this.state;
            }

            public Player getPlayer() {
                return this.player;
            }
        }
    }

    public static class LivingEvent {

        public static class LivingTickEvent
        extends Event {
            public LivingEntity entity;

            public LivingTickEvent(LivingEntity entity) {
                this.entity = entity;
            }

            public LivingEntity getEntity() {
                return this.entity;
            }
        }
    }

    public static class PlayerEvent {

        public static class ItemPickupEvent
        extends Event {
            public Player entity;
            public ItemEntity originalEntity;
            public ItemStack stack;

            public ItemPickupEvent(Player entity, ItemEntity originalEntity, ItemStack stack) {
                this.entity = entity;
                this.originalEntity = originalEntity;
                this.stack = stack;
            }

            public Player getEntity() {
                return this.entity;
            }

            public ItemEntity getOriginalEntity() {
                return this.originalEntity;
            }

            public ItemStack getStack() {
                return this.stack;
            }
        }

        public static class PlayerLoggedOutEvent
        extends Event {
            public Player entity;

            public PlayerLoggedOutEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return this.entity;
            }
        }

        public static class PlayerLoggedInEvent
        extends Event {
            public Player entity;

            public PlayerLoggedInEvent(Player entity) {
                this.entity = entity;
            }

            public Player getEntity() {
                return this.entity;
            }
        }
    }

    public static class EntityMobGriefingEvent
    extends Event {
        public Entity entity;

        public EntityMobGriefingEvent(Entity entity) {
            this.entity = entity;
        }

        public Entity getEntity() {
            return this.entity;
        }
    }

    public static class FillBucketEvent
    extends Event {
        public Player entity;

        public FillBucketEvent(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return this.entity;
        }
    }

    public static class ArrowLooseEvent
    extends Event {
        public Player entity;
        public ItemStack bow;

        public ArrowLooseEvent(Player entity, ItemStack bow) {
            this.entity = entity;
            this.bow = bow;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getBow() {
            return this.bow;
        }
    }

    public static class ProjectileImpactEvent
    extends Event {
        public Projectile projectile;
        public HitResult rayTraceResult;
        private Supplier<ImpactResult> impactReader;

        public ProjectileImpactEvent(Projectile projectile, HitResult rayTraceResult) {
            this.projectile = projectile;
            this.rayTraceResult = rayTraceResult;
        }

        public Projectile getProjectile() {
            return this.projectile;
        }

        public HitResult getRayTraceResult() {
            return this.rayTraceResult;
        }

        public void bindImpact(Supplier<ImpactResult> reader) {
            this.impactReader = reader;
        }

        public ImpactResult getImpactResult() {
            return this.impactReader == null ? ImpactResult.DEFAULT : this.impactReader.get();
        }

        public static enum ImpactResult {
            DEFAULT,
            SKIP_ENTITY,
            STOP_AT_CURRENT,
            STOP_AT_CURRENT_NO_DAMAGE;

        }
    }

    public static class LivingEquipmentChangeEvent
    extends Event {
        public LivingEntity entity;
        public EquipmentSlot slot;
        public ItemStack to;

        public LivingEquipmentChangeEvent(LivingEntity entity, EquipmentSlot slot, ItemStack to) {
            this.entity = entity;
            this.slot = slot;
            this.to = to;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public EquipmentSlot getSlot() {
            return this.slot;
        }

        public ItemStack getTo() {
            return this.to;
        }
    }

    public static class ShieldBlockEvent
    extends Event {
        public LivingEntity entity;
        public DamageSource damageSource;
        public float blockedDamage;
        private Supplier<Float> BlockedDamageReader;

        public ShieldBlockEvent(LivingEntity entity, DamageSource damageSource, float blockedDamage) {
            this.entity = entity;
            this.damageSource = damageSource;
            this.blockedDamage = blockedDamage;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getDamageSource() {
            return this.damageSource;
        }

        public float getBlockedDamage() {
            return this.BlockedDamageReader == null ? this.blockedDamage : this.BlockedDamageReader.get().floatValue();
        }

        public void setBlockedDamage(float value) {
            this.blockedDamage = value;
        }

        public void bindBlockedDamage(Supplier<Float> reader) {
            this.BlockedDamageReader = reader;
        }
    }

    public static class LivingUseTotemEvent
    extends Event {
        public LivingEntity entity;
        public ItemStack totem;

        public LivingUseTotemEvent(LivingEntity entity, ItemStack totem) {
            this.entity = entity;
            this.totem = totem;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public ItemStack getTotem() {
            return this.totem;
        }
    }

    public static class LivingExperienceDropEvent
    extends Event {
        public LivingEntity entity;
        public int droppedExperience;
        private Supplier<Integer> DroppedExperienceReader;

        public LivingExperienceDropEvent(LivingEntity entity, int droppedExperience) {
            this.entity = entity;
            this.droppedExperience = droppedExperience;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public int getDroppedExperience() {
            return this.DroppedExperienceReader == null ? this.droppedExperience : this.DroppedExperienceReader.get();
        }

        public void setDroppedExperience(int value) {
            this.droppedExperience = value;
        }

        public void bindDroppedExperience(Supplier<Integer> reader) {
            this.DroppedExperienceReader = reader;
        }
    }

    public static class LivingDropsEvent
    extends Event {
        public LivingEntity entity;
        public Collection<ItemEntity> drops;

        public LivingDropsEvent(LivingEntity entity, Collection<ItemEntity> drops) {
            this.entity = entity;
            this.drops = drops;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public Collection<ItemEntity> getDrops() {
            return this.drops;
        }
    }

    public static class LivingDestroyBlockEvent
    extends Event {
        public LivingEntity entity;
        public BlockPos pos;
        public BlockState state;

        public LivingDestroyBlockEvent(LivingEntity entity, BlockPos pos, BlockState state) {
            this.entity = entity;
            this.pos = pos;
            this.state = state;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockState getState() {
            return this.state;
        }
    }

    public static class LivingChangeTargetEvent
    extends Event {
        public LivingEntity entity;
        public LivingEntity newTarget;
        private Supplier<LivingEntity> NewTargetReader;

        public LivingChangeTargetEvent(LivingEntity entity, LivingEntity newTarget) {
            this.entity = entity;
            this.newTarget = newTarget;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public LivingEntity getNewTarget() {
            return this.NewTargetReader == null ? this.newTarget : this.NewTargetReader.get();
        }

        public void setNewTarget(LivingEntity value) {
            this.newTarget = value;
        }

        public void bindNewTarget(Supplier<LivingEntity> reader) {
            this.NewTargetReader = reader;
        }
    }

    public static class LivingHealEvent
    extends Event {
        public LivingEntity entity;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingHealEvent(LivingEntity entity, float amount) {
            this.entity = entity;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public float getAmount() {
            return this.AmountReader == null ? this.amount : this.AmountReader.get().floatValue();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            this.AmountReader = reader;
        }
    }

    public static class LivingDeathEvent
    extends Event {
        public LivingEntity entity;
        public DamageSource source;

        public LivingDeathEvent(LivingEntity entity, DamageSource source) {
            this.entity = entity;
            this.source = source;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }
    }

    public static class LivingDamageEvent
    extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingDamageEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public float getAmount() {
            return this.AmountReader == null ? this.amount : this.AmountReader.get().floatValue();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            this.AmountReader = reader;
        }
    }

    public static class LivingHurtEvent
    extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingHurtEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public float getAmount() {
            return this.AmountReader == null ? this.amount : this.AmountReader.get().floatValue();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            this.AmountReader = reader;
        }
    }

    public static class LivingAttackEvent
    extends Event {
        public LivingEntity entity;
        public DamageSource source;
        public float amount;
        private Supplier<Float> AmountReader;

        public LivingAttackEvent(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public float getAmount() {
            return this.AmountReader == null ? this.amount : this.AmountReader.get().floatValue();
        }

        public void setAmount(float value) {
            this.amount = value;
        }

        public void bindAmount(Supplier<Float> reader) {
            this.AmountReader = reader;
        }
    }

    public static class EntityLeaveLevelEvent
    extends Event {
        public Entity entity;
        public Level level;

        public EntityLeaveLevelEvent(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }
    }

    public static class EntityJoinLevelEvent
    extends Event {
        public Entity entity;
        public Level level;

        public EntityJoinLevelEvent(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }
    }

    public static class OnDatapackSyncEvent
    extends Event {
        public ServerPlayer player;
        public PlayerList playerList;

        public OnDatapackSyncEvent(ServerPlayer player, PlayerList playerList) {
            this.player = player;
            this.playerList = playerList;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public PlayerList getPlayerList() {
            return this.playerList;
        }
    }

    public static class ServerStoppedEvent
    extends Event {
        public MinecraftServer server;

        public ServerStoppedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static class ServerStoppingEvent
    extends Event {
        public MinecraftServer server;

        public ServerStoppingEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static class ServerStartedEvent
    extends Event {
        public MinecraftServer server;

        public ServerStartedEvent(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static class Event {
        private boolean canceled;
        private BooleanSupplier canceledReader;
        private Consumer<Boolean> canceledWriter;
        private Result result = Result.DEFAULT;

        public boolean isCanceled() {
            return this.canceledReader == null ? this.canceled : this.canceledReader.getAsBoolean();
        }

        public void setCanceled(boolean value) {
            this.canceled = value;
            if (this.canceledWriter != null) {
                this.canceledWriter.accept(value);
            }
        }

        public void bindCancellation(BooleanSupplier reader, Consumer<Boolean> writer) {
            this.canceledReader = reader;
            this.canceledWriter = writer;
        }

        public Result getResult() {
            return this.result;
        }

        public void setResult(Result value) {
            this.result = value;
        }

        public static enum Result {
            DEFAULT,
            ALLOW,
            DENY;

        }
    }
}

