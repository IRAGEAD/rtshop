package dev.dyslexic.cinematic.api;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Function;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public final class AddonStations {
    private AddonStations() {
    }

    public static synchronized void register(Definition definition) {
    }

    public static synchronized Definition byId(ResourceLocation id) {
        return null;
    }

    public record Definition(ResourceLocation id, ResourceLocation blockId, Class<? extends AbstractContainerMenu> menuClass,
                             Function<BlockState, List<Direction>> sides) {
    }
}
