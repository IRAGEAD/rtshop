package com.rtshop.client;

/** 뽑기 등급 색 (글자색 RGB). */
public final class GachaColors {
    private GachaColors() {
    }

    /** 등급의 색: 기본 등급은 제 색, 직접 만든 등급은 정해 둔 색 (GachaGrades). */
    public static int of(String grade) {
        com.rtshop.gacha.GachaGrades.Def d = com.rtshop.gacha.GachaGrades.def(grade);
        if (d != null) return d.color & 0xFFFFFF;
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
        com.rtshop.gacha.GachaGrades.Def d = com.rtshop.gacha.GachaGrades.def(grade);
        if (d != null && (!d.base() || d.color != com.rtshop.gacha.GachaGrades.BASE_COLOR[com.rtshop.gacha.GachaGrades.baseIndex(grade)])) {
            // 직접 정한 색: 밝은 바탕에서 읽히게 어둡게
            int c = d.color;
            int r = (int) ((c >> 16 & 255) * 0.58F), g = (int) ((c >> 8 & 255) * 0.58F), b = (int) ((c & 255) * 0.58F);
            return 0xFF000000 | r << 16 | g << 8 | b;
        }
        return 0xFF000000 | switch (grade) {
            case "uncommon" -> 0x2F7A26;
            case "rare" -> 0x1F5FA8;
            case "epic" -> 0x7A2FA8;
            case "legendary" -> 0xB06A00;
            case "yamato" -> 0x1250A8;
            default -> 0x5E5E5E;
        };
    }

    /** 등급의 이름: 직접 정한 이름이 있으면 그것, 없으면 게임 언어의 이름. */
    public static String name(String grade) {
        String custom = com.rtshop.gacha.GachaGrades.customName(grade);
        if (!custom.isEmpty()) return custom;
        String key = "rtshop.gacha.grade." + grade;
        return net.minecraft.client.resources.language.I18n.exists(key) ? net.minecraft.client.resources.language.I18n.get(key) : grade;
    }

    /** 뽑기 창에 적는 등급 이름: 야마토는 정체를 숨겨 [?] 로 적는다 (뽑아서 나온 카드에는 제 이름이 나온다). */
    public static String windowName(String grade) {
        return "yamato".equals(grade) ? "[?]" : name(grade);
    }

    /** 뽑기 창의 등급 색: 야마토는 짙은 푸른색. */
    public static int window(String grade, boolean onParchment) {
        if ("yamato".equals(grade)) return onParchment ? 0xFF0F1F6E : 0xFF2440B8;
        return onParchment ? dark(grade) : argb(grade);
    }

    /** 이 등급의 연출 모습 (0 일반 ~ 5 야마토). */
    public static int fx(String grade) {
        return com.rtshop.gacha.GachaGrades.fx(grade);
    }
}
