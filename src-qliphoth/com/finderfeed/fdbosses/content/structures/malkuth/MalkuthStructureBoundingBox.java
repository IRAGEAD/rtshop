/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 */
package com.finderfeed.fdbosses.content.structures.malkuth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class MalkuthStructureBoundingBox
extends BoundingBox {
    public MalkuthStructureBoundingBox(BoundingBox other) {
        this(other.m_162395_(), other.m_162396_(), other.m_162398_(), other.m_162399_(), other.m_162400_(), other.m_162401_());
    }

    public MalkuthStructureBoundingBox(BlockPos p_162364_) {
        super(p_162364_);
    }

    public MalkuthStructureBoundingBox(int p_71001_, int p_71002_, int p_71003_, int p_71004_, int p_71005_, int p_71006_) {
        super(p_71001_, p_71002_, p_71003_, p_71004_, p_71005_, p_71006_);
    }
}

