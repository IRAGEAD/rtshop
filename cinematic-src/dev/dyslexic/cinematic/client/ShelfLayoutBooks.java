/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.ChiseledBookShelfBlock
 *  net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dyslexic.cinematic.client.ClientOnly;
import dev.dyslexic.cinematic.client.OpenBook;
import dev.dyslexic.cinematic.client.Scene;
import dev.dyslexic.cinematic.client.ShelfBooks;
import dev.dyslexic.cinematic.client.ShelfStation;
import dev.dyslexic.cinematic.client.SpatialLayouts;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

final class ShelfLayoutBooks
implements BlockEntityRenderer<ChiseledBookShelfBlockEntity> {
    private final OpenBook book = new OpenBook();
    private static final ItemStack UNKNOWN_BOOK = new ItemStack((ItemLike)Items.f_42517_);
    private static final Set<BlockPos> VISIBLE_SHELVES = new HashSet<BlockPos>();
    private static double restoringUntil = -1.0;

    static boolean drawsRestingBooks() {
        return ClientOnly.serverHasMod() && (SpatialLayouts.customShelf() || Scene.clock() < restoringUntil);
    }

    static void layoutChanged(boolean custom) {
        if (!custom) {
            restoringUntil = Scene.clock() + 0.35;
        }
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null) {
            return;
        }
        HashSet<SectionPos> sections = new HashSet<SectionPos>();
        for (BlockPos pos : VISIBLE_SHELVES) {
            sections.add(SectionPos.m_123199_((BlockPos)pos));
        }
        for (SectionPos section : sections) {
            mc.f_91060_.m_109770_(section.m_123170_(), section.m_123206_(), section.m_123222_());
        }
    }

    static void unloadChunk(ChunkPos chunk) {
        VISIBLE_SHELVES.removeIf(pos -> pos.m_123341_() >> 4 == chunk.f_45578_ && pos.m_123343_() >> 4 == chunk.f_45579_);
    }

    static void clear() {
        VISIBLE_SHELVES.clear();
        restoringUntil = -1.0;
    }

    ShelfLayoutBooks(BlockEntityRendererProvider.Context context) {
    }

    public void render(ChiseledBookShelfBlockEntity shelf, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockPos pos = shelf.m_58899_();
        VISIBLE_SHELVES.add(pos);
        if (!ShelfLayoutBooks.drawsRestingBooks()) {
            return;
        }
        ShelfStation mine = Scene.station(ShelfStation.class);
        if (mine != null && mine.pos.equals((Object)pos)) {
            return;
        }
        BlockState state = shelf.m_58900_();
        Direction facing = (Direction)state.m_61143_((Property)BlockStateProperties.f_61374_);
        Vec3 origin = Vec3.m_82528_((Vec3i)pos);
        for (int i = 0; i < 6; ++i) {
            if (!((Boolean)state.m_61143_((Property)ChiseledBookShelfBlock.f_260698_.get(i))).booleanValue() || ShelfBooks.flight(pos, i) != null) continue;
            ShelfBooks.draw(pose, buffers, origin, i, UNKNOWN_BOOK, ShelfBooks.inSlot(pos, facing, i, 0.0), this.book, light);
        }
    }
}

