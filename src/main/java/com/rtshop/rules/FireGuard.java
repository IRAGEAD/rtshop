package com.rtshop.rules;

import com.rtshop.RtShop;
import com.rtshop.land.CastlePlots;
import com.rtshop.land.LandConfig;
import com.rtshop.land.LandManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.BiPredicate;

/**
 * 3.82 (사용자 11-28 19:08 "마을 안에서만 번지지 않게 해줘"): 모드팩의 KubeJS 가 서버를 켤 때마다 doFireTick 을 켜므로,
 * 마을(성 영역) 안에서만 불이 번지지 않게 한다. 마을 밖은 그대로 (doFireTick 을 따름).
 * <ul>
 * <li>불 블록의 틱 (FireBlockMixin): 성 영역이나 바로 둘레(가로 1칸)의 불은 번지지도, 옆 블록을 태우지도 않는다.
 *     지옥암 · 마그마처럼 불이 꺼지지 않는 블록 위의 불(벽난로)은 그대로 두고, 그 밖의 불은 첫 틱(1.5~2초 뒤)에 꺼진다.
 *     둘레 1칸까지 보는 까닭: 불은 바로 옆 칸까지만 태우거나 옮겨 붙으므로, 마을 밖 바로 옆의 불이 마을 블록을 태우지 못하게.</li>
 * <li>용암이 옆의 탈 것에 불을 붙이는 것 (FluidPlaceBlockEvent): 성 영역(+ 둘레)에서는 붙지 않는다.</li>
 * </ul>
 * 벼락이 떨어뜨린 불 · 부싯돌로 붙인 불도 불 블록이므로 위와 같이 번지지 않고 꺼진다. land.json 의 castleNoFireSpread 로 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class FireGuard {
    private FireGuard() {
    }

    /** 점검용: 막은 횟수 (불의 틱 · 용암). */
    public static volatile int stoppedForTest;
    /** 시험용: 성 영역 대신 쓸 구역 (x, z). null 이면 진짜 성 영역. */
    public static volatile BiPredicate<Integer, Integer> zoneForTest;

    private static boolean zone(ServerLevel level, int x, int z) {
        BiPredicate<Integer, Integer> t = zoneForTest;
        if (t != null) return t.test(x, z);
        return CastlePlots.inZone(x, z);
    }

    /** 이 자리의 불을 막는가: 성 영역이 있는 세계의 그 차원에서, 이 자리나 바로 둘레(가로 1칸)가 성 영역일 때. */
    public static boolean guarded(ServerLevel level, BlockPos pos) {
        try {
            if (level == null || !LandConfig.get().castleNoFireSpread) return false;
            if (zoneForTest == null && (!CastlePlots.zoneKnown(level.getServer()) || !LandManager.dim(level).equals(CastlePlots.def().dim))) {
                return false;
            }
            int x = pos.getX(), z = pos.getZ();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (zone(level, x + dx, z + dz)) return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * FireBlockMixin 이 불 블록의 틱 처음에 부른다. 막았으면 true (원래의 틱 = 번지기 · 태우기 · 꺼지기는 건너뜀).
     * doFireTick 이 꺼져 있으면 원래대로 (그때는 불이 번지지 않으므로 손대지 않는다).
     */
    public static boolean onFireTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK) || !guarded(level, pos)) return false;
        BlockPos below = pos.below();
        if (level.getBlockState(below).isFireSource(level, below, Direction.UP)) {
            // 벽난로(꺼지지 않는 불): 그대로 두고 다음 틱만 잡는다 (원래 틱도 늘 다음 틱을 잡는다)
            level.scheduleTick(pos, state.getBlock(), 30 + random.nextInt(10));
        } else {
            level.removeBlock(pos, false);
        }
        stoppedForTest++;
        return true;
    }

    /** 용암이 옆의 탈 것에 불을 붙이려 할 때: 성 영역(+ 둘레)이면 그 자리를 그대로 둔다. */
    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        try {
            if (!(event.getLevel() instanceof ServerLevel level) || !(event.getNewState().getBlock() instanceof BaseFireBlock)) return;
            if (!level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK) || !guarded(level, event.getPos())) return;
            event.setNewState(event.getOriginalState());
            stoppedForTest++;
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 용암 불 막기 실패", t);
        }
    }
}
