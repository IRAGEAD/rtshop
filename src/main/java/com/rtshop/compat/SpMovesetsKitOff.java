package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.84 (사용자 11-29 "spmoveset 모든 아이템 서버 참여 시 지급 금지"): spmovesets 는 생존 모드 세계에서 처음 들어오거나 다시 살아날 때
 * 시작 꾸러미(생존 안내서 + 열매 뽑기 열쇠)를 준다 — spmovesets.survival.start.StarterKit.give 가 지속 데이터
 * PlayerPersisted.spmovesets_survival_kit 를 보고, 없으면 주고 표시를 붙인다.
 * 그래서 그 모드보다 먼저(플레이어 파일을 읽을 때 · 접속 · 다시 살아남, 가장 이른 순서) 그 표시를 붙여 둔다: 그 모드는 받은 사람으로 보고 아무것도 주지 않는다.
 * world_rules.json 의 spmovesetsStarterKit=true 면 건드리지 않는다. 이미 받은 꾸러미는 그대로다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class SpMovesetsKitOff {
    private SpMovesetsKitOff() {
    }

    public static final String MOD = "spmovesets";
    public static final String FLAG = "spmovesets_survival_kit";
    /** 점검용: 표시를 붙인 수. */
    public static volatile int markedForTest;

    static boolean on() {
        try {
            return !com.rtshop.rules.WorldRules.get().spmovesetsStarterKit && ModList.get().isLoaded(MOD);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 이 사람이 꾸러미를 받은 것으로 둔다. 새로 붙였으면 true. */
    public static boolean mark(Player p) {
        CompoundTag root = p.getPersistentData();
        CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (keep.getBoolean(FLAG)) return false;
        keep.putBoolean(FLAG, true);
        root.put(Player.PERSISTED_NBT_TAG, keep);
        markedForTest++;
        return true;
    }

    /** 이 사람이 그 모드의 꾸러미를 받은 것으로 되어 있는가. */
    public static boolean marked(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(FLAG);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLoad(PlayerEvent.LoadFromFile event) {
        if (on() && event.getEntity() instanceof ServerPlayer sp) mark(sp);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (on() && event.getEntity() instanceof ServerPlayer sp) mark(sp);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (on() && event.getEntity() instanceof ServerPlayer sp) mark(sp);
    }
}
