package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Spell Engine(RPG Series 의 주문) 서버 설정을 메모리에서 바꾼다. 그 모드는 켜질 때 config/spell_engine/server.json5 를 읽어 객체에 담아 두고,
 * 주문을 쓸 때마다 그 객체의 칸을 본다 — 그래서 켤 때마다 그 칸을 다시 고친다 (파일은 건드리지 않음).
 * <ul>
 *   <li>3.84 (사용자 11-29 "rpg series, iron's spells 마나 통합 제대로 안됨(rpg series 모드의 스킬 사용시 '공기 부족'이라는 문구가 나오고, 마법 사용이 불가)"):
 *       마법사 · 성기사 주문은 Runes 모드의 룬을 값으로 치르는데, 모드팩에 Runes 모드가 없어서 그 룬이 「공기(minecraft:air)」로 읽혔다 —
 *       Spell Engine 의 "%s 부족"(hud.cast_attempt_error.missing_item)이 "공기 부족"으로 뜨고 주문이 나가지 않았다.
 *       주문의 물건 값을 끈다 (spell_cost_item_allowed = false): 값은 RT Shop 의 마나 통일(SpellMana)로 Iron's 마나에서 낸다.
 *       화면 쪽도 주문을 쓰기 전에 같은 칸을 보므로 클라이언트에서도 고친다 (멀티플레이 서버에 들어갈 때마다).</li>
 *   <li>3.84 (배고픔을 바닐라처럼, HungerVanilla): 주문의 배고픔 소모 spell_cost_exhaust_multiplier = 0.</li>
 * </ul>
 * world_rules.json: spellItemCostOff (기본 true) · vanillaHunger (기본 true).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class SpellEngineTweaks {
    private SpellEngineTweaks() {
    }

    public static final String MOD_CLASS = "net.spell_engine.SpellEngineMod";
    /** 점검용: 마지막 결과. */
    public static volatile String lastItemForTest = "", lastExhaustForTest = "";

    /** Spell Engine 의 설정 객체 안에서 그 이름의 칸을 찾아 값을 넣는다. 결과 글 ("set (was …)", "already", "absent", "no field"). */
    public static String set(String field, Object value) {
        try {
            Class<?> mod = Class.forName(MOD_CLASS);
            for (Field f : mod.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                String r = put(f.get(null), field, value, 0);
                if (r != null) return r;
            }
            return "no field";
        } catch (ClassNotFoundException e) {
            return "absent";
        } catch (Throwable t) {
            return "error " + t;
        }
    }

    /** 그 칸의 지금 값 (없으면 null). */
    public static Object get(String field) {
        try {
            Class<?> mod = Class.forName(MOD_CLASS);
            for (Field f : mod.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                Object r = find(f.get(null), field, 0);
                if (r != null) return r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object find(Object o, String field, int depth) throws Exception {
        if (o == null || depth > 2 || o.getClass().getName().startsWith("java.")) return null;
        for (Field f : o.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getName().equals(field)) return f.get(o);
        }
        for (Field f : o.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
            Object r = find(f.get(o), field, depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    private static String put(Object o, String field, Object value, int depth) throws Exception {
        if (o == null || depth > 2 || o.getClass().getName().startsWith("java.")) return null;
        for (Field f : o.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || !f.getName().equals(field)) continue;
            Object was = f.get(o);
            if (String.valueOf(was).equals(String.valueOf(value)) || was instanceof Number n && value instanceof Number v && n.doubleValue() == v.doubleValue()) {
                return "already";
            }
            Class<?> t = f.getType();
            if (t == boolean.class) f.setBoolean(o, (Boolean) value);
            else if (t == float.class) f.setFloat(o, ((Number) value).floatValue());
            else if (t == double.class) f.setDouble(o, ((Number) value).doubleValue());
            else if (t == int.class) f.setInt(o, ((Number) value).intValue());
            else f.set(o, value);
            return "set (was " + was + ")";
        }
        for (Field f : o.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
            String r = put(f.get(o), field, value, depth + 1);
            if (r != null) return r;
        }
        return null;
    }

    /** 주문의 물건(룬) 값을 끈다. */
    public static String itemCostOff() {
        if (!com.rtshop.rules.WorldRules.get().spellItemCostOff) return lastItemForTest = "allowed";
        return lastItemForTest = set("spell_cost_item_allowed", false);
    }

    /** 주문의 배고픔 소모를 끈다. */
    public static String exhaustOff() {
        if (!com.rtshop.rules.WorldRules.get().vanillaHunger) return lastExhaustForTest = "allowed";
        return lastExhaustForTest = set("spell_cost_exhaust_multiplier", 0F);
    }

    @SubscribeEvent
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        String r = itemCostOff();
        if (!r.equals("absent")) RtShop.LOGGER.info("[rtshop] RPG 주문의 룬 값을 껐습니다 (모드팩에 Runes 모드가 없어 '공기 부족'으로 막히던 것): {}", r);
    }

    /** 화면 쪽: 처음 한 번 · 서버에 들어갈 때마다 (그 서버가 보낸 설정이 칸을 덮을 수 있어서 조금 뒤에 한 번 더). */
    @Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
    public static final class Client {
        private Client() {
        }

        private static int again = 1;

        @SubscribeEvent
        public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
            again = 40;
        }

        @SubscribeEvent
        public static void onTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || again <= 0) return;
            if (--again == 0) {
                try {
                    itemCostOff();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
