package com.rtshop.rules;

import com.rtshop.RtShop;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * 3.68.1: 블록 드롭 금지(item_policy 의 blockDropBan — Create · Better Nether)의 마지막 그물.
 * 전리품표 규칙(LootRules)은 전리품표로 드롭을 정하는 블록만 막는다. 블록이 코드로 직접 자기 자신을 떨어뜨리면(Better Nether 의 바탕 라이브러리 등) 빠져나가므로,
 * 플레이어가 캤거나 폭발로 부서진 자리에서 같은 틱에 생긴 금지 물건의 아이템 엔티티를 생기지 않게 한다.
 * 플레이어가 버리거나 죽어서 떨어뜨린 물건(부서진 블록 자리가 아님)은 건드리지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class BlockDropGuard {
    private BlockDropGuard() {
    }

    /** 차원 → 최근에 부서진 자리 → 그때의 게임 시각. */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> RECENT = new HashMap<>();
    /** 점검용: 막은 아이템 수. */
    public static int blockedForTest;

    static void mark(ServerLevel sl, BlockPos p) {
        RECENT.computeIfAbsent(sl.dimension(), k -> new HashMap<>()).put(p.immutable(), sl.getGameTime());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (!e.isCanceled() && e.getLevel() instanceof ServerLevel sl) mark(sl, e.getPos());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExplode(ExplosionEvent.Detonate e) {
        if (e.getLevel() instanceof ServerLevel sl) {
            for (BlockPos p : e.getAffectedBlocks()) mark(sl, p);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() || !(e.getEntity() instanceof ItemEntity ie) || !(e.getLevel() instanceof ServerLevel sl)) return;
        Map<BlockPos, Long> m = RECENT.get(sl.dimension());
        if (m == null || m.isEmpty() || !ItemPolicy.blockDropBanned(ie.getItem())) return;
        long now = sl.getGameTime();
        BlockPos at = ie.blockPosition();
        for (BlockPos q : new BlockPos[]{at, at.below(), at.above()}) {   // 블록에서 튀어나온 물건은 그 블록 안(± 조금)에 생긴다
            Long t = m.get(q);
            if (t != null && now - t <= 1) {
                e.setCanceled(true);
                blockedForTest++;
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel sl)) return;
        Map<BlockPos, Long> m = RECENT.get(sl.dimension());
        if (m != null && !m.isEmpty()) {
            long now = sl.getGameTime();
            m.values().removeIf(t -> now - t > 1);
        }
    }
}
