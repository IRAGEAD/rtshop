package com.rtshop.client;

/** 뽑기 등급 색 (글자색 RGB). */
public final class GachaColors {
    private GachaColors() {
    }

    public static int of(String grade) {
        return switch (grade) {
            case "uncommon" -> 0x55C24A;
            case "rare" -> 0x4A9BE8;
            case "epic" -> 0xB45AE8;
            case "legendary" -> 0xF0A030;
            default -> 0xB8B8B8;
        };
    }

    public static int argb(String grade) {
        return 0xFF000000 | of(grade);
    }

    /** 양피지(밝은 바탕) 위 글자색. */
    public static int dark(String grade) {
        return 0xFF000000 | switch (grade) {
            case "uncommon" -> 0x2F7A26;
            case "rare" -> 0x1F5FA8;
            case "epic" -> 0x7A2FA8;
            case "legendary" -> 0xB06A00;
            default -> 0x5E5E5E;
        };
    }
}
