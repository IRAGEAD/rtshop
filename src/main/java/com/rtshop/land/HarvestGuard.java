package com.rtshop.land;

import com.rtshop.RtShop;
import com.rtshop.land.LandData.Land;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;

/**
 * 3.84 (사용자 11-29 "뼛가루 들고 자신의 토지/농경지가 아닌 마을 내의 농작물도 캘 수 있는 문제 해결"):
 * 모드팩의 오른쪽 클릭 수확(Quark 의 Simple Harvest · Better Farming++)은 블록을 부수는 길(BreakEvent)을 거치지 않고 농작물을 거두고 다시 심는다.
 * 그래서 남의 땅 · 마을의 농작물은 다른 모드보다 먼저(가장 이른 순서) 막는다:
 * <ul>
 *   <li>농작물(밀 · 당근 · 열매 덤불 · 코코아 · 네더 사마귀 · 줄기 · 그 밖에 뼛가루를 받는 덤불)을 오른쪽 클릭하면, 그 자리를 부술 권한이 없으면 막는다 —
 *       손에 든 것이 무엇이든 (빈손 · 괭이 · 뼛가루).</li>
 *   <li>Quark 가 거두기 직전에 알리는 SimpleHarvestEvent 도 같은 권한으로 막는다 (그 모드가 둘레까지 한꺼번에 거둘 때).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class HarvestGuard {
    private HarvestGuard() {
    }

    /** 점검용: 막은 수. */
    public static volatile int blockedForTest, quarkBlockedForTest;

    /** 오른쪽 클릭으로 거둘 수 있는 농작물인가. */
    public static boolean crop(BlockState st) {
        var b = st.getBlock();
        return b instanceof CropBlock || b instanceof StemBlock || b instanceof SweetBerryBushBlock || b instanceof CocoaBlock
                || b instanceof NetherWartBlock || b instanceof BushBlock && b instanceof BonemealableBlock;
    }

    /** 이 사람이 이 자리의 농작물을 거둘(부술) 수 있는가. */
    public static boolean mayHarvest(ServerPlayer sp, ServerLevel level, BlockPos pos) {
        Land land = LandManager.guardAt(level, pos);
        return land == null || (LandManager.perms(sp, land) & LandData.BREAK) != 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (!crop(level.getBlockState(pos)) || mayHarvest(sp, level, pos)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        blockedForTest++;
        sp.inventoryMenu.sendAllDataToRemote();
        LandEvents.deny(sp, LandManager.guardAt(level, pos));
    }

    private static boolean hooked;

    /** Quark 의 "거두기 직전" 이벤트에 끼어든다 (없으면 아무것도 하지 않음). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void hook() {
        if (hooked || !ModList.get().isLoaded("quark")) return;
        try {
            Class cls = Class.forName("org.violetmoon.quark.api.event.SimpleHarvestEvent");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, cls, (java.util.function.Consumer) e -> {
                try {
                    Object who = call(e, "getEntity", "getPlayer", "getHarvester");
                    Object lvl = call(e, "getLevel", "getWorld");
                    Object pos = call(e, "getTargetPos", "getPos");
                    if (who instanceof ServerPlayer sp && pos instanceof BlockPos bp) {
                        ServerLevel level = lvl instanceof ServerLevel l ? l : sp.serverLevel();
                        if (!mayHarvest(sp, level, bp)) {
                            ((Event) e).setCanceled(true);
                            quarkBlockedForTest++;
                        }
                    }
                } catch (Throwable ignored) {
                }
            });
            hooked = true;
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Quark 의 오른쪽 클릭 수확에 끼어들지 못했습니다 (남의 땅의 농작물은 오른쪽 클릭 단계에서만 막습니다): {}", t.toString());
        }
    }

    public static boolean hookedForTest() {
        return hooked;
    }

    private static Object call(Object o, String... names) {
        for (String n : names) {
            try {
                Method m = o.getClass().getMethod(n);
                return m.invoke(o);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
