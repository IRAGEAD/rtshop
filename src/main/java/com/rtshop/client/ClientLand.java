package com.rtshop.client;

import net.minecraft.core.BlockPos;

/** 서버가 알려 준 "지금 열려 있는 보관함" 정보. PIN 버튼을 보여 줄지 판단하는 데만 쓴다. */
public final class ClientLand {
    private ClientLand() {
    }

    public static volatile int containerId = -1;
    public static volatile BlockPos containerPos = null;
    public static volatile boolean canSetPin = false;
    public static volatile boolean hasPin = false;

    public static void clear() {
        containerId = -1;
        containerPos = null;
        canSetPin = false;
        hasPin = false;
    }
}
