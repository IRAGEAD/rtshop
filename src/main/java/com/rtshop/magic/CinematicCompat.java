package com.rtshop.magic;

import com.rtshop.RtShop;
import dev.dyslexic.cinematic.api.AddonStations;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Cinematic Interactions 연동 (서버·클라이언트 공통): 별빛 수정 구체를 Cinematic 의 '작업대'로 등록한다.
 * 이 클래스는 Cinematic 이 깔려 있을 때만 불러야 한다 (RtShop.onCommonSetup 에서 확인).
 */
public final class CinematicCompat {
    private CinematicCompat() {
    }

    public static final ResourceLocation ID = new ResourceLocation(RtShop.MODID, "arcane_orb");

    public static void register() {
        try {
            AddonStations.register(new AddonStations.Definition(ID, ID, MagicMenu.class,
                    state -> List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)));
            MagicRegistry.cinematic = true;
            RtShop.LOGGER.info("[rtshop] 별빛 수정 구체를 Cinematic Interactions 장면으로 등록했습니다.");
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Cinematic Interactions 장면 등록 실패 (구체는 보통 창으로 열립니다): {}", t.toString());
        }
    }
}
