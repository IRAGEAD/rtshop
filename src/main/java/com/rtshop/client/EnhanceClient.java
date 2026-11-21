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

    /** 강화 미리보기 설명 줄 (장비 창 옆 패널과 미리보기 아이템 설명에 함께 쓴다). */
    public static List<Component> previewLines(Player p, ItemStack equipment, ItemStack guardStack) {
        List<Component> out = new ArrayList<>();
        EnhanceConfig cfg = EnhanceConfig.client();
        int cur = Enhance.level(equipment);
        EnhanceConfig.Level l = cfg.level(cur + 1);
        if (l == null) return out;
        Enhance.Guard g = Enhance.guardOf(guardStack);
        if (!g.relevant(l)) g = Enhance.Guard.NONE;
        out.add(Component.translatable("rtshop.enhance.try", cur, cur + 1).withStyle(ChatFormatting.YELLOW));
        out.add(odds(effective(l, g, cur)));
        boolean free = p != null && p.getAbilities().instabuild;
        for (Enhance.Need n : Enhance.needs(cfg, l, p)) {
            MutableComponent c = Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(materialName(cfg, n).copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" " + n.have + "/" + n.need).withStyle(n.ok() || free ? ChatFormatting.GREEN : ChatFormatting.RED));
            out.add(c);
        }
        boolean goldOk = free || ClientEconomy.wallet >= l.gold;
        out.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(Ui.num(l.gold) + " G")
                .withStyle(goldOk ? ChatFormatting.GOLD : ChatFormatting.RED)));
        if (g != Enhance.Guard.NONE) {
            out.add(Component.translatable("rtshop.enhance.guard_used", guardStack.getHoverName()).withStyle(ChatFormatting.AQUA));
        } else if (l.destroy > 0) {
            out.add(Component.translatable("rtshop.enhance.destroy_warn").withStyle(ChatFormatting.DARK_RED));
        }
        if (free) out.add(Component.translatable("rtshop.enhance.creative").withStyle(ChatFormatting.DARK_AQUA));
        return out;
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
        if (Enhance.isPreview(s)) {
            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;
            if (p != null && p.containerMenu instanceof com.rtshop.enhance.EnhanceMenu menu) {
                tip.addAll(previewLines(p, menu.equipment(), menu.guardStack()));
                tip.add(Component.translatable("rtshop.enhance.take_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }
}
