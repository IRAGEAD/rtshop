package com.rtshop.client;

import com.rtshop.net.QuestHudPacket;

import java.util.ArrayList;
import java.util.List;

/** 서버가 보내 준 진행 중인 퀘스트 목록 (퀘스트 HUD 용). */
public final class ClientQuestHud {
    private ClientQuestHud() {
    }

    public static volatile List<QuestHudPacket.Entry> entries = List.of();

    public static void set(List<QuestHudPacket.Entry> list) {
        entries = List.copyOf(new ArrayList<>(list));
    }

    public static void clear() {
        entries = List.of();
    }
}
