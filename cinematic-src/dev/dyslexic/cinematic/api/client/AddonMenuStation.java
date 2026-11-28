/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.inventory.AbstractContainerMenu
 */
package dev.dyslexic.cinematic.api.client;

import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class AddonMenuStation<M extends AbstractContainerMenu>
extends MenuStation<M> {
    protected AddonMenuStation(BlockPos pos, Direction side, Class<M> menuType, int inputSlots) {
        super(pos, side, menuType, inputSlots);
    }

    @Override
    public final StationKind kind() {
        return StationKind.ADDON;
    }
}

