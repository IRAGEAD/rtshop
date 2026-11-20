package com.rtshop.client;

/** 서버가 보내준 내 소지금/예금(표시용). 실제 값은 서버에만 있습니다. */
public final class ClientEconomy {
    private ClientEconomy() {
    }

    public static volatile long wallet = 0;
    public static volatile long bank = 0;
}
