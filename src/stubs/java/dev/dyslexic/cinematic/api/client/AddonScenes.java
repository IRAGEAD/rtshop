package dev.dyslexic.cinematic.api.client;

import dev.dyslexic.cinematic.client.ClientStation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class AddonScenes {
    private AddonScenes() {
    }

    public static synchronized void register(ResourceLocation id, Factory factory) {
    }

    @FunctionalInterface
    public interface Factory {
        ClientStation create(BlockPos pos, BlockState state, LocalPlayer player, Direction side);
    }
}
