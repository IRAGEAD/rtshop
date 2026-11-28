/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  com.finderfeed.fdlib.network.FDPacketHandler
 *  com.finderfeed.fdlib.util.math.FDMathUtil
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.network.PacketDistributor$TargetPoint
 */
package com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake;

import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.chesed_boss.ChesedBossBuddy;
import com.finderfeed.fdbosses.content.entities.chesed_boss.earthshatter_entity.EarthShatterSettings;
import com.finderfeed.fdbosses.content.entities.chesed_boss.radial_earthquake.SpawnRadialEarthquakeOnRadiusPacket;
import com.finderfeed.fdbosses.init.BossDamageSources;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import com.finderfeed.fdlib.network.FDPacketHandler;
import com.finderfeed.fdlib.util.math.FDMathUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class RadialEarthquakeEntity
extends Entity
implements AutoSerializable {
    public static final EntityDataAccessor<Integer> RADIUS = SynchedEntityData.m_135353_(RadialEarthquakeEntity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    protected HashMap<BlockPos, EarthShatterInstance> clientEarthShatters = new HashMap();
    @SerializableField
    public float damage;
    @SerializableField
    public int endRadius = 10;
    @SerializableField
    public float speed;
    @SerializableField
    public float current = 0.0f;
    @SerializableField
    private int previousRadius = -1;
    @SerializableField
    private int removalTick = 20;

    public RadialEarthquakeEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public static RadialEarthquakeEntity summon(Level level, BlockPos pos, int startRadius, int endRadius, float speed, float damage) {
        RadialEarthquakeEntity radialEarthquakeEntity = new RadialEarthquakeEntity(BossEntities.RADIAL_EARTHQUAKE.get(), level);
        radialEarthquakeEntity.speed = speed;
        radialEarthquakeEntity.current = startRadius;
        radialEarthquakeEntity.endRadius = endRadius;
        radialEarthquakeEntity.damage = damage;
        radialEarthquakeEntity.f_19804_.m_135381_(RADIUS, (Object)endRadius);
        radialEarthquakeEntity.m_146884_(pos.m_252807_());
        level.m_7967_((Entity)radialEarthquakeEntity);
        return radialEarthquakeEntity;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            if (this.current > (float)this.endRadius) {
                if (this.removalTick-- < 0) {
                    this.m_142687_(Entity.RemovalReason.DISCARDED);
                }
            } else {
                int currentRad = (int)this.current;
                if (currentRad != this.previousRadius) {
                    for (int i = this.previousRadius + 1; i <= currentRad; ++i) {
                        this.spawnAndDamageWithRadius(i);
                    }
                }
                this.previousRadius = currentRad;
                this.current += this.speed;
            }
        } else {
            this.tickEarthShatters();
        }
    }

    private void tickEarthShatters() {
        Iterator<Map.Entry<BlockPos, EarthShatterInstance>> iterator = this.clientEarthShatters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, EarthShatterInstance> pair = iterator.next();
            EarthShatterInstance instance = pair.getValue();
            if (instance.shouldBeRemoved()) {
                iterator.remove();
                continue;
            }
            instance.tick();
        }
    }

    public void spawnAndDamageWithRadius(int rad) {
        AABB box = new AABB((double)(-rad), -0.1, (double)(-rad), (double)rad, 1.0, (double)rad).m_82383_(this.m_20182_());
        Predicate<LivingEntity> predicate = entity -> {
            double dist = entity.m_20182_().m_82542_(1.0, 0.0, 1.0).m_82554_(this.m_20182_().m_82542_(1.0, 0.0, 1.0));
            return !(entity instanceof ChesedBossBuddy) && Math.abs(dist - (double)rad) <= 1.5;
        };
        List entities = this.m_9236_().m_6443_(LivingEntity.class, box, predicate);
        for (LivingEntity entity2 : entities) {
            entity2.m_6469_(BossDamageSources.CHESED_EARTHQUAKE_SOURCE, this.damage);
        }
        FDPacketHandler.INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p((double)this.m_20185_(), (double)this.m_20186_(), (double)this.m_20189_(), (double)Math.max(this.endRadius * 2, 120), (ResourceKey)this.m_9236_().m_46472_())), (Object)new SpawnRadialEarthquakeOnRadiusPacket(this, rad));
        Vec3 tpos = this.m_20182_();
        BossUtil.posEvent((ServerLevel)this.m_9236_(), tpos, 2, rad, 60.0);
    }

    public void spawnEarthShattersOnRadius(int rad) {
        Vec3 b = new Vec3((double)rad, 0.0, 0.0);
        float angle = rad != 0 ? 0.5f / (float)rad : (float)Math.PI * 2;
        BlockPos prevPos = null;
        Vec3 tpos = this.m_20182_();
        for (float i = 0.0f; i < (float)Math.PI * 2; i += angle) {
            Vec3 pos = tpos.m_82549_(b.m_82524_(i));
            BlockPos ppos = FDMathUtil.vec3ToBlockPos((Vec3)pos);
            if (!ppos.equals(prevPos) && !this.clientEarthShatters.containsKey(ppos)) {
                Vec3 c = ppos.m_252807_();
                Vec3 dir = tpos.m_82546_(c).m_82542_(1.0, 0.0, 1.0).m_82541_().m_82520_(0.0, 1.0, 0.0);
                BlockState state = this.m_9236_().m_8055_(ppos);
                if (state.m_60795_()) continue;
                EarthShatterSettings settings = EarthShatterSettings.builder().direction(dir).stayTime(this.f_19796_.m_188503_(4)).upTime(4 - this.f_19796_.m_188503_(4)).downTime(4 - this.f_19796_.m_188503_(4)).build();
                EarthShatterInstance earthShatterInstance = new EarthShatterInstance(settings, state);
                this.clientEarthShatters.put(ppos, earthShatterInstance);
            }
            prevPos = ppos;
        }
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(RADIUS, (Object)10);
    }

    protected void m_7378_(CompoundTag tag) {
        this.autoLoad(tag);
        this.f_19804_.m_135381_(RADIUS, (Object)this.endRadius);
    }

    protected void m_7380_(CompoundTag tag) {
        this.autoSave(tag);
    }

    public AABB m_6921_() {
        int rad = (Integer)this.f_19804_.m_135370_(RADIUS) + 2;
        return new AABB((double)(-rad), (double)(-rad), (double)(-rad), (double)rad, (double)rad, (double)rad).m_82383_(this.m_20182_());
    }

    public boolean m_6783_(double sqrDistance) {
        return sqrDistance <= 14400.0;
    }

    public static class EarthShatterInstance {
        private EarthShatterSettings settings;
        private BlockState blockState;
        private int tickCount;

        public EarthShatterInstance(EarthShatterSettings earthShatterSettings, BlockState blockState) {
            this.settings = earthShatterSettings;
            this.blockState = blockState;
        }

        public BlockState getBlockState() {
            return this.blockState;
        }

        public void tick() {
            ++this.tickCount;
        }

        public boolean shouldBeRemoved() {
            return this.tickCount > this.settings.getLifetime();
        }

        public int getTickCount() {
            return this.tickCount;
        }

        public EarthShatterSettings getSettings() {
            return this.settings;
        }
    }
}

