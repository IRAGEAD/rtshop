package com.rtshop.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.RaidPacket;
import net.minecraft.client.Minecraft;

/** 레이드·보상 편집기 패킷을 받아 화면을 연다 (클라이언트 전용). */
public final class RaidClient {
    private RaidClient() {
    }

    /** 이동 투표 창을 "조금 더 머물기"로 닫았으면 다시 띄우지 않는다. */
    static boolean voteDismissed = false;
    /** 주간 던전 보상 (Spiral Abyss 층 선택 화면 옆에 그린다). */
    public static volatile JsonObject abyss;

    public static void receive(int kind, String json) {
        Minecraft mc = Minecraft.getInstance();
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        switch (kind) {
            case RaidPacket.VIEW -> {
                voteDismissed = false;
                mc.setScreen(new RaidScreen(o));
            }
            case RaidPacket.PROMPT -> {
                if ("close".equals(o.get("mode").getAsString())) {
                    // 마을로 돌아왔다: 남아 있는 투표·실패 창을 닫는다
                    voteDismissed = false;
                    if (mc.screen instanceof RaidPromptScreen) mc.setScreen(null);
                    return;
                }
                boolean vote = "vote".equals(o.get("mode").getAsString());
                if (vote && o.get("agreed").getAsInt() == 0) {
                    voteDismissed = false;
                }
                if (mc.screen instanceof RaidPromptScreen s) {
                    s.update(o);
                } else if (!vote || !voteDismissed) {
                    mc.setScreen(new RaidPromptScreen(o));
                }
            }
            case RaidPacket.EDITOR -> {
                if (mc.screen instanceof RewardEditScreen s && s.id().equals(o.get("id").getAsString())) {
                    s.update(o);
                } else {
                    mc.setScreen(new RewardEditScreen(o));
                }
            }
            case RaidPacket.ABYSS -> abyss = o;
            default -> {
            }
        }
    }
}
