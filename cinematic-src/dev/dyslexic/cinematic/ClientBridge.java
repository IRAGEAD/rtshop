/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic;

import dev.dyslexic.cinematic.station.StationKind;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class ClientBridge {
    public static BiConsumer<BlockPos, StationKind> useStation = (pos, kind) -> {};
    public static BiPredicate<BlockPos, StationKind> vanillaUse = (pos, kind) -> false;
    public static Vec3 useAt = Vec3.f_82478_;

    private ClientBridge() {
    }
}

