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
            case "yamato" -> 0x49B6FF;
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
            case "yamato" -> 0x1250A8;
            default -> 0x5E5E5E;
        };
    }

    /** 뽑기 창에 적는 등급 이름: 야마토는 정체를 숨겨 [?] 로 적는다 (뽑아서 나온 카드에는 제 이름이 나온다). */
    public static String windowName(String grade) {
        return "yamato".equals(grade) ? "[?]" : net.minecraft.network.chat.Component.translatable("rtshop.gacha.grade." + grade).getString();
    }

    /** 뽑기 창의 등급 색: 야마토는 짙은 푸른색. */
    public static int window(String grade, boolean onParchment) {
        if ("yamato".equals(grade)) return onParchment ? 0xFF0F1F6E : 0xFF2440B8;
        return onParchment ? dark(grade) : argb(grade);
    }
}
