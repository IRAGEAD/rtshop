package com.rtshop.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 퀘스트 본문을 화면에 보이기 좋게 바꾼다.
 *  - "[모드ID] 내용" 으로 시작하는 줄은 그 모드가 있을 때만 보이고, 모드 이름이 붙은 도움말로 그린다.
 *  - "{key:키 이름}" 은 그 플레이어가 실제로 정해 둔 키로 바뀐다 (예: {key:key.jump} → [Space]).
 */
public final class QuestText {
    private QuestText() {
    }

    public static final int TIP_COLOR = 0x2E5A73;
    public static final int TIP_HEAD = 0x8A5A12;
    private static final Pattern MOD_LINE = Pattern.compile("^\\s*\\[([a-z0-9_.\\-]+)]\\s*(.*)$");
    private static final Pattern KEY = Pattern.compile("\\{key:([^}]+)}");

    /** 한 줄: 일반 줄이면 mod 가 null. */
    public record Line(String text, String mod) {
        public boolean tip() {
            return mod != null;
        }
    }

    public static List<Line> lines(String raw) {
        List<Line> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String line : raw.split("\n", -1)) {
            Matcher m = MOD_LINE.matcher(line);
            if (m.matches()) {
                String mod = m.group(1);
                if (!ModList.get().isLoaded(mod)) {
                    continue;
                }
                out.add(new Line(keys(m.group(2)), mod));
            } else {
                out.add(new Line(keys(line), null));
            }
        }
        return out;
    }

    /** 화면에 그릴 글: 도움말 줄은 앞에 "◆ 모드 이름" 머리말이 붙는다. */
    public static Component component(Line l) {
        if (!l.tip()) {
            return Component.literal(l.text());
        }
        MutableComponent head = Component.literal("◆ " + modName(l.mod()) + "  ")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TIP_HEAD)));
        return head.append(Component.literal(l.text()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(TIP_COLOR))));
    }

    public static String modName(String id) {
        return ModList.get().getModContainerById(id).map(c -> c.getModInfo().getDisplayName()).orElse(id);
    }

    /** {key:이름} 을 플레이어가 정한 키로 바꾼다. */
    public static String keys(String s) {
        if (s.indexOf("{key:") < 0) {
            return s;
        }
        Matcher m = KEY.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("[" + keyLabel(m.group(1).trim()) + "]"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** 보이는 줄에 쓰였지만 이 게임에 없는 키 이름들 (점검용). */
    public static List<String> missingKeys(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String line : raw.split("\n", -1)) {
            Matcher mm = MOD_LINE.matcher(line);
            if (mm.matches() && !ModList.get().isLoaded(mm.group(1))) {
                continue;
            }
            Matcher m = KEY.matcher(line);
            while (m.find()) {
                if (find(m.group(1).trim()) == null) {
                    out.add(m.group(1).trim());
                }
            }
        }
        return out;
    }

    public static String keyLabel(String name) {
        KeyMapping k = find(name);
        if (k == null) {
            return I18n.exists(name) ? I18n.get(name) : name;
        }
        return k.getTranslatedKeyMessage().getString();
    }

    public static KeyMapping find(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return null;
        }
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getName().equals(name)) {
                return k;
            }
        }
        return null;
    }
}
