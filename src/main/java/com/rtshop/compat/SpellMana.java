package com.rtshop.compat;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.RtShop;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 마나 통일: RPG Series(Spell Engine) 무기·주문도 Iron's Spells 'n Spellbooks 의 마나를 쓴다.
 * Spell Engine 에는 마나가 없다(쿨타임·룬만 있다). 그래서 시전을 시도할 때 Iron's 마나가 모자라면 막고, 시전이 나갈 때 마나를 깎는다.
 * 두 모드 모두 리플렉션으로만 건드린다 (없으면 아무 일도 하지 않는다). 설정: config/rtshop/spell_mana.json
 */
public final class SpellMana {
    private SpellMana() {
    }

    public static boolean enabled = true;
    /** 주문 등급(tier)별 마나. 등급이 표에 없으면 가장 높은 등급 값을 쓴다. */
    public static final Map<Integer, Integer> TIER = new LinkedHashMap<>();
    /** 주문 ID 별 예외 (0 = 마나를 쓰지 않음). */
    public static final Map<String, Integer> SPELL = new LinkedHashMap<>();
    /** 집중(채널링) 주문이 한 번 나갈 때마다 드는 몫 (등급 마나의 %). */
    public static int channelPercent = 25;
    private static boolean hooked = false;
    public static int blockedForTest = 0, spentForTest = 0;

    static {
        defaults();
    }

    private static void defaults() {
        TIER.clear();
        TIER.put(0, 8);
        TIER.put(1, 12);
        TIER.put(2, 20);
        TIER.put(3, 32);
        TIER.put(4, 48);
    }

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rtshop").resolve("spell_mana.json");
    }

    public static synchronized void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (o.has("enabled")) enabled = o.get("enabled").getAsBoolean();
                if (o.has("channelPercent")) channelPercent = Math.max(0, Math.min(100, o.get("channelPercent").getAsInt()));
                if (o.has("tierMana") && o.get("tierMana").isJsonObject()) {
                    TIER.clear();
                    for (var e : o.getAsJsonObject("tierMana").entrySet()) TIER.put(Integer.parseInt(e.getKey().trim()), Math.max(0, e.getValue().getAsInt()));
                    if (TIER.isEmpty()) defaults();
                }
                SPELL.clear();
                if (o.has("spellMana") && o.get("spellMana").isJsonObject()) {
                    for (var e : o.getAsJsonObject("spellMana").entrySet()) SPELL.put(e.getKey().trim(), Math.max(0, e.getValue().getAsInt()));
                }
            } else {
                JsonObject o = new JsonObject();
                o.addProperty("_help", "마나 통일: RPG Series(Spell Engine: 마법사·성기사·도적·궁수 무기의 주문)도 Iron's Spells 의 마나를 쓴다. enabled 를 false 로 하면 끈다. "
                        + "tierMana: 주문 등급(tier)별 마나. spellMana: 주문 ID 별 예외 (예: \"wizards:fireball\": 15, 0 이면 마나를 쓰지 않음). "
                        + "channelPercent: 집중(계속 쏘는) 주문이 한 번 나갈 때마다 드는 몫(등급 마나의 %). 룬·쿨타임은 원래대로다. 고친 뒤 /rtshop reload.");
                o.addProperty("enabled", enabled);
                JsonObject t = new JsonObject();
                TIER.forEach((k, v) -> t.addProperty(String.valueOf(k), v));
                o.add("tierMana", t);
                o.add("spellMana", new JsonObject());
                o.addProperty("channelPercent", channelPercent);
                Files.createDirectories(p.getParent());
                Files.writeString(p, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(o), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            RtShop.LOGGER.warn("[rtshop] spell_mana.json 을 읽지 못했습니다: {}", e.toString());
        }
    }

    /** 이 주문 한 번에 드는 마나. */
    public static int cost(String spellId, int tier) {
        Integer s = SPELL.get(spellId);
        if (s != null) return s;
        Integer t = TIER.get(tier);
        if (t != null) return t;
        int best = 0, bestTier = Integer.MIN_VALUE;
        for (var e : TIER.entrySet()) {
            if (e.getKey() > bestTier) {
                bestTier = e.getKey();
                best = e.getValue();
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ Iron's Spells 마나

    private static Method mGet, mMana, mSetMana, mClientMana, mSend;
    private static java.lang.reflect.Constructor<?> cSync;

    private static Object magic(Player p) throws Exception {
        if (mGet == null) {
            Class<?> c = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            mGet = c.getMethod("getPlayerMagicData", net.minecraft.world.entity.LivingEntity.class);
            mMana = c.getMethod("getMana");
            mSetMana = c.getMethod("setMana", float.class);
        }
        return mGet.invoke(null, p);
    }

    /** 지금 마나 (알 수 없으면 -1: 그때는 막지 않는다). */
    public static float mana(Player p) {
        try {
            if (p.level().isClientSide) {
                if (mClientMana == null) mClientMana = Class.forName("io.redspace.ironsspellbooks.player.ClientMagicData").getMethod("getPlayerMana");
                return ((Number) mClientMana.invoke(null)).floatValue();
            }
            return ((Number) mMana.invoke(magic(p))).floatValue();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void spend(ServerPlayer p, float amount) {
        try {
            Object md = magic(p);
            float now = ((Number) mMana.invoke(md)).floatValue();
            mSetMana.invoke(md, Math.max(0F, now - amount));
            if (cSync == null) {
                cSync = Class.forName("io.redspace.ironsspellbooks.network.SyncManaPacket").getConstructor(md.getClass());
                mSend = Class.forName("io.redspace.ironsspellbooks.setup.PacketDistributor").getMethod("sendToPlayer", ServerPlayer.class, Object.class);
            }
            mSend.invoke(null, p, cSync.newInstance(md));
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 마나를 깎지 못했습니다: {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ Spell Engine 에 끼어들기

    private static Object call(Object o, String name) throws Exception {
        Method m = o.getClass().getMethod(name);
        m.setAccessible(true);
        return m.invoke(o);
    }

    private static int costOf(Object holder) {
        try {
            Holder<?> h = (Holder<?>) holder;
            String id = h.unwrapKey().map(k -> k.location().toString()).orElse("");
            Object spell = h.value();
            int tier = spell.getClass().getField("tier").getInt(spell);
            return cost(id, tier);
        } catch (Throwable t) {
            return 0;
        }
    }

    private static long lastWarn;

    /** 시전을 시도할 때: 마나가 모자라면 실패로 돌려준다 (null = 그대로 진행). */
    static Object onAttempt(Object args, Class<?> attemptClass, Class<?> cooldownInfo) {
        try {
            if (!enabled) return null;
            Player p = (Player) call(args, "caster");
            if (p.getAbilities().instabuild) return null;
            int cost = costOf(call(args, "spell"));
            if (cost <= 0) return null;
            float mana = mana(p);
            if (mana < 0 || mana >= cost) return null;
            blockedForTest++;
            long now = System.currentTimeMillis();
            if (p.level().isClientSide && now - lastWarn > 1500) {
                lastWarn = now;
                p.displayClientMessage(Component.translatable("rtshop.spellmana.low", cost, (int) mana).withStyle(net.minecraft.ChatFormatting.AQUA), true);
            }
            return attemptClass.getMethod("failOnCooldown", cooldownInfo).invoke(null, cooldownInfo.getDeclaredConstructor().newInstance());
        } catch (Throwable t) {
            return null;
        }
    }

    /** 시전이 나갔을 때 (서버): 마나를 깎는다. 발동형(무기의 숨은 효과)은 깎지 않는다. */
    static void onCast(Object args) {
        try {
            if (!enabled) return;
            if (!(call(args, "caster") instanceof ServerPlayer p) || p.getAbilities().instabuild) return;
            String action = String.valueOf(call(args, "action"));
            if (action.equals("TRIGGER")) return;
            int cost = costOf(call(args, "spell"));
            if (cost <= 0) return;
            float amount = action.equals("CHANNEL") ? Math.max(1F, cost * channelPercent / 100F) : cost;
            spend(p, amount);
            spentForTest += Math.round(amount);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 주문 마나 처리 실패: {}", t.toString());
        }
    }

    /** 두 모드가 다 있을 때 한 번 건다. */
    public static synchronized boolean hook() {
        if (hooked) return true;
        try {
            load();
            Class<?> events = Class.forName("net.spell_engine.api.spell.event.SpellEvents");
            Class<?> attemptEvent = Class.forName("net.spell_engine.api.spell.event.SpellEvents$CastingAttemptEvent");
            Class<?> castEvent = Class.forName("net.spell_engine.api.spell.event.SpellEvents$SpellCastEvent");
            Class<?> attempt = Class.forName("net.spell_engine.internals.casting.SpellCast$Attempt");
            Class<?> cooldownInfo = Class.forName("net.spell_engine.internals.casting.SpellCast$Attempt$OnCooldownInfo");
            Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            Object staged = events.getField("CASTING_ATTEMPT").get(null);
            Object pre = staged.getClass().getField("PRE").get(staged);
            Object a = Proxy.newProxyInstance(attemptEvent.getClassLoader(), new Class<?>[]{attemptEvent}, (proxy, m, ar) -> switch (m.getName()) {
                case "onCastingAttempt" -> onAttempt(ar[0], attempt, cooldownInfo);
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == ar[0];
                default -> "rtshop spell mana (attempt)";
            });
            pre.getClass().getMethod("register", Object.class).invoke(pre, a);
            Object castEv = events.getField("SPELL_CAST").get(null);
            Object c = Proxy.newProxyInstance(castEvent.getClassLoader(), new Class<?>[]{castEvent}, (proxy, m, ar) -> {
                switch (m.getName()) {
                    case "onSpellCast":
                        onCast(ar[0]);
                        return null;
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == ar[0];
                    default:
                        return "rtshop spell mana (cast)";
                }
            });
            castEv.getClass().getMethod("register", Object.class).invoke(castEv, c);
            hooked = true;
            RtShop.LOGGER.info("[rtshop] 마나 통일: RPG Series 주문이 Iron's Spells 마나를 씁니다 (config/rtshop/spell_mana.json)");
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 마나 통일을 걸지 못했습니다 (Spell Engine / Iron's Spells 판이 다를 수 있음): {}", t.toString());
        }
        return hooked;
    }

    /** 점검용: 마나를 이 값으로 맞춘다. */
    public static boolean setManaForTest(Player p, float v) {
        try {
            Object md = magic(p);
            mSetMana.invoke(md, v);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hookedForTest() {
        return hooked;
    }
}
