package com.rtshop.client;

import com.rtshop.RtShop;
import com.rtshop.enhance.Enhance;
import com.rtshop.enhance.EnhanceConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** 강화 단계 표시(아이템 이름 앞 +N)와 강화 미리보기 설명 (Cinematic 모루 장면에서도 이 설명이 보인다). */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class EnhanceClient {
    private EnhanceClient() {
    }

    private static final java.util.Map<String, String> DEFAULT_NAMES = java.util.Map.of(
            "gem_common", "흔한 보석", "gem_fine", "고운 보석", "gem_nether", "네더 보석", "gem_rare", "희귀 보석");

    public static ChatFormatting color(int lv) {
        if (lv >= 10) return ChatFormatting.LIGHT_PURPLE;
        if (lv >= 7) return ChatFormatting.GOLD;
        if (lv >= 4) return ChatFormatting.AQUA;
        return ChatFormatting.GREEN;
    }

    public static String pct(double v) {
        return (v == Math.floor(v) ? Long.toString((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v)) + "%";
    }

    /** 방지권까지 적용한 실제 확률: [성공, 유지, 손상, 파괴]. */
    public static double[] effective(EnhanceConfig.Level l, Enhance.Guard g, int cur) {
        double total = l.success + l.maintain + l.damage + l.destroy;
        double s = l.success, m = l.maintain, d = l.damage, x = l.destroy;
        if (g == Enhance.Guard.DAMAGE || g == Enhance.Guard.FULL) {
            m += d;
            d = 0;
        }
        if (g == Enhance.Guard.FULL) {
            m += x;
            x = 0;
        } else if (g == Enhance.Guard.DESTROY) {
            d += x;
            x = 0;
        }
        if (cur == 0) {
            m += d;
            d = 0;
        }
        return new double[]{s * 100 / total, m * 100 / total, d * 100 / total, x * 100 / total};
    }

    public static Component odds(double[] e) {
        return Component.translatable("rtshop.enhance.odds_success", pct(e[0])).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("rtshop.enhance.odds_maintain", pct(e[1])).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("rtshop.enhance.odds_damage", pct(e[2])).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable("rtshop.enhance.odds_destroy", pct(e[3])).withStyle(ChatFormatting.RED));
    }

    public static Component materialName(EnhanceConfig cfg, Enhance.Need n) {
        if (n.material.id.startsWith("#")) {
            String g = n.material.id.substring(1);
            String name = cfg.groupNames.getOrDefault(g, g);
            Component label = cfg.groupNames.get(g) != null && cfg.groupNames.get(g).equals(DEFAULT_NAMES.get(g))
                    ? Component.translatableWithFallback("rtshop.enhance.group_" + g, name) : Component.literal(name);
            return Component.translatable("rtshop.enhance.group", label);
        }
        return n.items.get(0).getDescription();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        int lv = Enhance.level(s);
        List<Component> tip = e.getToolTip();
        if (lv > 0 && !tip.isEmpty()) {
            tip.set(0, Component.literal("+" + lv + " ").withStyle(color(lv), ChatFormatting.BOLD).append(tip.get(0)));
            EnhanceConfig cfg = EnhanceConfig.client();
            Enhance.Kind k = Enhance.kind(s);
            String key = k == Enhance.Kind.ARMOR ? "rtshop.enhance.bonus_armor" : "rtshop.enhance.bonus_weapon";
            double[] arr = k == Enhance.Kind.ARMOR ? cfg.armorBonus : cfg.weaponBonus;
            tip.add(Math.min(1, tip.size()), Component.translatable(key, lv, EnhanceConfig.MAX_LEVEL, pct(arr[Math.min(lv, arr.length - 1)]))
                    .withStyle(color(lv)));
        }
    }

    /**
     * 강화 모루의 장비·결과에 마우스를 올려도 설명 창을 띄우지 않는다 (정보는 강화 정보 창에 모두 있다).
     * Cinematic Interactions 의 모루 장면은 아이템 없이 글줄만 넘겨 그리므로(RenderTooltipEvent.Pre 의 아이템이 비어 있음),
     * 글줄을 모으는 단계에서 첫 줄(아이템 이름)을 보고 막는다.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipGather(net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents e) {
        if (gathering) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack s = e.getItemStack();
        if (!s.isEmpty() && Enhance.isPreview(s)) {
            e.setCanceled(true);
            return;
        }
        if (!(mc.player.containerMenu instanceof com.rtshop.enhance.EnhanceMenu menu)) return;
        ItemStack eq = menu.equipment(), res = menu.getSlot(2).getItem();
        if (!s.isEmpty()) {
            if (s == eq || s == res) e.setCanceled(true);
            return;
        }
        if (e.getTooltipElements().isEmpty()) return;
        String first = e.getTooltipElements().get(0).left().map(net.minecraft.network.chat.FormattedText::getString).orElse("");
        if (!first.isEmpty() && (named(first, eq) || named(first, res))) e.setCanceled(true);
    }

    private static boolean named(String firstLine, ItemStack s) {
        return !s.isEmpty() && firstLine.contains(s.getHoverName().getString());
    }

    /** 강화 정보 창이 장비 설명을 직접 모으는 동안에는 위의 가로채기를 쉬게 한다. */
    static boolean gathering;

    /** 아이템의 설명 글줄 (설명 창에 뜨던 내용 그대로). */
    public static List<Component> describe(ItemStack s) {
        gathering = true;
        try {
            return net.minecraft.client.gui.screens.Screen.getTooltipFromItem(Minecraft.getInstance(), s);
        } catch (Exception ex) {
            return List.of(s.getHoverName());
        } finally {
            gathering = false;
        }
    }

    /** 시험용: Cinematic 장면처럼 글줄만으로 설명 창을 그리려 할 때 남는 줄 수 (0 = 숨겨짐). */
    public static int sceneTooltipForTest(ItemStack s) {
        Minecraft mc = Minecraft.getInstance();
        List<Component> lines = new ArrayList<>(net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, s));
        lines.add(Component.literal("LMB - forge"));
        return net.minecraftforge.client.ForgeHooksClient.gatherTooltipComponents(ItemStack.EMPTY, lines, java.util.Optional.empty(), 0,
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), mc.font).size();
    }

    /** 장면의 모루 믹스인이 설명 창을 막은 횟수 (점검용). */
    public static int sceneTooltipBlocked;

    /** 설명 창의 글줄 하나를 글자로 (글줄 부품 안의 글을 꺼내 읽는다; 못 읽으면 ""). */
    private static String textOf(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent c) {
        try {
            for (java.lang.reflect.Field f : c.getClass().getDeclaredFields()) {
                if (!net.minecraft.util.FormattedCharSequence.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(c);
                if (v instanceof net.minecraft.util.FormattedCharSequence seq) {
                    StringBuilder sb = new StringBuilder();
                    seq.accept((i, style, cp) -> {
                        sb.appendCodePoint(cp);
                        return true;
                    });
                    return sb.toString();
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /**
     * 아이템 없이 글줄만으로 그려지는 설명 창(Cinematic 장면의 방식)이 강화 모루 위의 장비·결과의 것인가.
     * 첫 줄이 그 장비의 이름이면 그렇다.
     */
    public static boolean hidesLines(java.util.List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> parts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || parts == null || parts.isEmpty()) return false;
        if (!(mc.player.containerMenu instanceof com.rtshop.enhance.EnhanceMenu menu)) return false;
        String first = textOf(parts.get(0));
        return !first.isEmpty() && (named(first, menu.equipment()) || named(first, menu.getSlot(2).getItem()));
    }

    /** 시험용: 장면이 실제로 쓰는 방식(글줄 부품)으로 그 아이템의 설명 창을 그리려 할 때 막히는가. */
    public static boolean sceneLinesHiddenForTest(ItemStack s) {
        Minecraft mc = Minecraft.getInstance();
        List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> parts = new ArrayList<>();
        for (Component c : net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, s)) {
            parts.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(c.getVisualOrderText()));
        }
        parts.add(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(Component.literal("LMB - forge").getVisualOrderText()));
        return hidesLines(parts);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTooltipRender(net.minecraftforge.client.event.RenderTooltipEvent.Pre e) {
        ItemStack s = e.getItemStack();
        if (s.isEmpty()) {
            // 아이템 없이 글줄만 그리는 설명 창: 강화 모루 위 장비의 것이면 막는다 (장면의 모루가 이렇게 그린다)
            if (hidesLines(e.getComponents())) e.setCanceled(true);
            return;
        }
        if (Enhance.isPreview(s)) {
            e.setCanceled(true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof com.rtshop.enhance.EnhanceMenu menu
                && (s == menu.equipment() || s == menu.getSlot(2).getItem())) {
            e.setCanceled(true);
        }
    }
}
