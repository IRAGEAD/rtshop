package dev.dyslexic.cinematic.api.client;

import dev.dyslexic.cinematic.client.MenuStation;
import dev.dyslexic.cinematic.station.StationKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** 컴파일용 자리표시 (Cinematic Interactions 의 실제 클래스가 실행 때 쓰인다; jar 에 들어가지 않음). */
public abstract class AddonMenuStation<M extends AbstractContainerMenu> extends MenuStation<M> {
    protected AddonMenuStation(BlockPos pos, Direction side, Class<M> menuType, int inputSlots) {
        super(pos, side, menuType, inputSlots);
    }

    @Override
    public final StationKind kind() {
        return StationKind.ADDON;
    }
}
