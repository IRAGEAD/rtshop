/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 */
package com.pla.spiral_abyss.block.entity;

import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.init.BlockEntityInit;
import com.pla.spiral_abyss.world.AbyssParties;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import com.pla.spiral_abyss.world.ArenaSession;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

public final class AbyssPortalBlockEntity
extends BlockEntity {
    private UUID portalId = UUID.randomUUID();
    private UUID host;
    private int displayedFloor;
    private boolean hostRunning;

    public UUID portalId() {
        return this.portalId;
    }

    public UUID host() {
        return this.host;
    }

    public int displayedFloor() {
        return this.displayedFloor;
    }

    public boolean hostRunning() {
        return this.hostRunning;
    }

    public void bindHost(UUID host) {
        this.host = host;
        this.m_6596_();
        this.refreshIndicator();
    }

    private void refreshIndicator() {
        boolean running;
        Level level = this.f_58857_;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        ArenaSession bound = AbyssParties.get(serverLevel.m_7654_()).portals().boundSession(this);
        int floor = bound == null ? 0 : bound.controller().currentFloor();
        boolean bl = running = bound != null && bound.controller().active();
        if (floor == this.displayedFloor && running == this.hostRunning) {
            return;
        }
        this.displayedFloor = floor;
        this.hostRunning = running;
        this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
    }

    public CompoundTag m_5995_() {
        CompoundTag tag = this.m_187482_();
        tag.m_128405_("DisplayedFloor", this.displayedFloor);
        tag.m_128379_("HostRunning", this.hostRunning);
        return tag;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.m_195640_((BlockEntity)this);
    }

    protected void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        tag.m_128362_("PortalId", this.portalId);
        if (this.host != null) {
            tag.m_128362_("Host", this.host);
        }
    }

    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        if (tag.m_128403_("PortalId")) {
            this.portalId = tag.m_128342_("PortalId");
        }
        this.host = tag.m_128403_("Host") ? tag.m_128342_("Host") : null;
        this.displayedFloor = Math.max(0, Math.min(7, tag.m_128451_("DisplayedFloor")));
        this.hostRunning = tag.m_128471_("HostRunning");
    }

    public AbyssPortalBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)BlockEntityInit.ABYSS_PORTAL.get(), pos, state);
    }

    public AbyssPortalFrame frame() {
        return AbyssPortalFrame.fromActivation(this.f_58858_, this.m_58900_());
    }

    public static void tick(Level level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity;
        if (level.m_46467_() % 5L == 0L && (blockEntity = level.m_7702_(pos)) instanceof AbyssPortalBlockEntity) {
            AbyssPortalBlockEntity portal = (AbyssPortalBlockEntity)blockEntity;
            portal.refreshIndicator();
        }
        if (level.m_46467_() % 20L != 0L) {
            return;
        }
        AbyssPortalFrame frame = AbyssPortalFrame.fromActivation(pos, state);
        if (!frame.loaded(level)) {
            return;
        }
        if (((Boolean)state.m_61143_((Property)AbyssGateActivationBlock.ACTIVE)).booleanValue()) {
            if (!level.m_46472_().equals(Level.f_46428_) || !frame.active(level)) {
                frame.deactivate(level);
            }
        } else if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            frame.activate(serverLevel);
        }
    }

    public AABB getRenderBoundingBox() {
        return new AABB(this.frame().at(-2, 0), this.frame().at(2, 5).m_7918_(1, 1, 1)).m_82377_(1.0, 0.5, 1.0);
    }
}

