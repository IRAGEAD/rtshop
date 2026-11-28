package com.rtshop.land;

import com.rtshop.land.LandData.Land;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;

/**
 * 3.84: 남의 땅 · 마을(성 영역)에서도 누구나 "쓸" 수 있는 블록 (부수기 · 놓기는 그대로 막힘).
 * <ul>
 *   <li>운영자가 고른 자리 (LandData.usable, /rtshop plot use): 어디서든 — 사용자 11-29 "선택한 블록을 op가 없는 플레이어들도 상호작용할 수 있게 하기".</li>
 *   <li>마을(성 영역, 산 토지 밖)의 문 · 다락문 · 울타리 문 · 버튼 (3.13 부터).</li>
 *   <li>마을의 land.json castleZoneUsable (작업대 · 강화 모루 · 룬 제단 · 의자 …, 보관함은 빼고) 과
 *       castleZoneUsableStorage (조리 도구 · 카지노 — 물건을 넣는 블록이라도).</li>
 * </ul>
 */
public final class ZoneUse {
    private ZoneUse() {
    }

    /** 이 자리의 블록을 지금 누구나 쓸 수 있는가. land = 그 자리를 지키는 토지 (LandManager.guardAt), storage = 보관함인가. */
    public static boolean usable(ServerLevel level, BlockPos pos, Land land, BlockState st, boolean storage) {
        if (land == null) return true;
        if (picked(level, pos)) return true;
        if (land != LandManager.ZONE) return false;
        if (LandEvents.zoneDoor(land, st)) return true;
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(st.getBlock());
        if (id == null) return false;
        LandConfig c = LandConfig.get();
        if (match(c.castleZoneUsableStorage, id)) return true;
        return !storage && match(c.castleZoneUsable, id);
    }

    /** 운영자가 고른 자리인가 (큰 상자는 반쪽만 골라도 둘 다). */
    public static boolean picked(ServerLevel level, BlockPos pos) {
        try {
            LandData d = LandData.get(level.getServer());
            String dim = level.dimension().location().toString();
            if (d.usable(dim, pos)) return true;
            BlockPos other = LandManager.otherHalf(level, pos);
            return other != null && d.usable(dim, other);
        } catch (Throwable t) {
            return false;
        }
    }

    /** "모드:블록" · "모드:*" · "*낱말*"(그 ID 에 낱말이 든 것) · "모드:*낱말*" 가운데 하나에 맞는가. */
    public static boolean match(List<String> pats, ResourceLocation id) {
        if (pats == null || pats.isEmpty() || id == null) return false;
        String full = id.toString();
        for (String raw : pats) {
            String p = raw.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            if (p.indexOf('*') < 0) {
                if (p.equals(full)) return true;
            } else if (p.endsWith(":*") && p.indexOf('*') == p.length() - 1) {
                if (id.getNamespace().equals(p.substring(0, p.length() - 2))) return true;
            } else if (glob(p, p.indexOf(':') >= 0 ? full : id.getPath())) {
                return true;
            }
        }
        return false;
    }

    private static boolean glob(String pat, String s) {
        String[] parts = pat.split("\\*", -1);
        int at = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            int found = s.indexOf(part, at);
            if (found < 0) return false;
            if (i == 0 && found != 0) return false;   // 맨 앞에 * 가 없으면 처음부터 맞아야 한다
            at = found + part.length();
        }
        String last = parts[parts.length - 1];
        return last.isEmpty() || s.endsWith(last);
    }
}
