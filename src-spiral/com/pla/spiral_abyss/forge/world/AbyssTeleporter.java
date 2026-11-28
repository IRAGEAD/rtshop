/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.portal.PortalInfo
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.common.util.ITeleporter
 */
package com.pla.spiral_abyss.forge.world;

import java.util.function.Function;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

public class AbyssTeleporter
implements ITeleporter {
    private final Vec3 position;
    private final float yaw;
    private final float pitch;

    public AbyssTeleporter(Vec3 position, float yaw, float pitch) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Entity placeEntity(Entity entity, ServerLevel currentLevel, ServerLevel destinationLevel, float yaw, Function<Boolean, Entity> repositionEntity) {
        return repositionEntity.apply(false);
    }

    public PortalInfo getPortalInfo(Entity entity, ServerLevel destinationLevel, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return new PortalInfo(this.position, Vec3.f_82478_, this.yaw, this.pitch);
    }
}

