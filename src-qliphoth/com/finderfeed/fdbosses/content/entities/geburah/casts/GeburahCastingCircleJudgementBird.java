/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.nbt.AutoSerializable
 *  com.finderfeed.fdlib.nbt.SerializableField
 *  net.minecraft.commands.arguments.EntityAnchorArgument$Anchor
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package com.finderfeed.fdbosses.content.entities.geburah.casts;

import com.finderfeed.fdbosses.content.entities.geburah.casts.GeburahCastingCircle;
import com.finderfeed.fdbosses.content.entities.geburah.judgement_bird.JudgementBirdEntity;
import com.finderfeed.fdbosses.init.BossEntities;
import com.finderfeed.fdlib.nbt.AutoSerializable;
import com.finderfeed.fdlib.nbt.SerializableField;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GeburahCastingCircleJudgementBird
extends GeburahCastingCircle
implements AutoSerializable {
    @SerializableField
    private Vec3 birdFlyTo;
    private AABB roostingBox;

    public static void summon(Level level, Vec3 pos, Vec3 castDirection, Vec3 birdFlyTo, AABB roostingBox) {
        GeburahCastingCircleJudgementBird castCircle = new GeburahCastingCircleJudgementBird(BossEntities.GEBURAH_CASTING_CIRCLE_JUDGEMENT_BIRD.get(), level);
        castCircle.m_146884_(pos);
        castCircle.birdFlyTo = birdFlyTo;
        castCircle.setDirection(castDirection);
        castCircle.roostingBox = roostingBox;
        castCircle.setCastDuration(20);
        castCircle.m_20088_().m_135381_(COLOR, (Object)-5579265);
        level.m_7967_((Entity)castCircle);
    }

    public GeburahCastingCircleJudgementBird(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void cast() {
        if (this.roostingBox != null) {
            JudgementBirdEntity judgementBirdEntity = JudgementBirdEntity.summon(this.m_9236_(), this.m_20182_(), this.birdFlyTo, this.roostingBox);
            judgementBirdEntity.m_7618_(EntityAnchorArgument.Anchor.FEET, this.birdFlyTo);
            judgementBirdEntity.setMoveTargetPos(this.birdFlyTo);
        }
    }

    @Override
    public void m_7378_(CompoundTag tag) {
        super.m_7378_(tag);
        this.autoLoad(tag);
        if (tag.m_128441_("roostingBoxMinX")) {
            this.roostingBox = new AABB(tag.m_128459_("roostingBoxMinX"), tag.m_128459_("roostingBoxMinY"), tag.m_128459_("roostingBoxMinZ"), tag.m_128459_("roostingBoxMaxX"), tag.m_128459_("roostingBoxMaxY"), tag.m_128459_("roostingBoxMaxZ"));
        }
    }

    @Override
    public void m_7380_(CompoundTag tag) {
        super.m_7380_(tag);
        this.autoSave(tag);
        if (this.roostingBox != null) {
            tag.m_128347_("roostingBoxMinX", this.roostingBox.f_82288_);
            tag.m_128347_("roostingBoxMinY", this.roostingBox.f_82289_);
            tag.m_128347_("roostingBoxMinZ", this.roostingBox.f_82290_);
            tag.m_128347_("roostingBoxMaxX", this.roostingBox.f_82291_);
            tag.m_128347_("roostingBoxMaxY", this.roostingBox.f_82292_);
            tag.m_128347_("roostingBoxMaxZ", this.roostingBox.f_82293_);
        }
    }
}

