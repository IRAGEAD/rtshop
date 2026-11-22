package com.rtshop.story;

import com.rtshop.quest.QuestManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * 장비 상인의 진열대를 이야기로 연다: 희귀(RARE) 장비는 1장, 서사(EPIC) 장비는 2장을 마쳐야 살 수 있다.
 * 그 장을 끝내는 퀘스트가 없는 서버(이야기 퀘스트를 지운 서버)에서는 막지 않는다.
 */
public final class StoryGates {
    private StoryGates() {
    }

    public static boolean enabled = true;
    /** 점검용: 가짜 플레이어에게도 적용한다. 평소의 자동 점검(상점 구매 등)은 가짜 플레이어로 돌기 때문에 꺼 둔다. */
    public static boolean applyToFakeForTest;

    static boolean gear(ItemStack s) {
        return s.isDamageableItem() || s.getItem() instanceof ArmorItem;
    }

    /** 막혀 있으면 "1장"/"2장", 아니면 null. */
    public static String lockedChapter(ServerPlayer p, ItemStack s) {
        if (!enabled || p.isCreative() || s.isEmpty() || !gear(s)) return null;
        if (p instanceof net.minecraftforge.common.util.FakePlayer && !applyToFakeForTest) return null;
        Rarity r = s.getRarity();
        if (r == Rarity.EPIC && !passed(p, "story_ch2_done", "story_2_10")) return "2장";
        if ((r == Rarity.RARE || r == Rarity.EPIC) && !passed(p, "story_ch1_done", "story_1_8")) return "1장";
        return null;
    }

    static boolean passed(ServerPlayer p, String flag, String questId) {
        return !QuestManager.loaded().quests.containsKey(questId) || QuestManager.hasFlag(p, flag);
    }
}
