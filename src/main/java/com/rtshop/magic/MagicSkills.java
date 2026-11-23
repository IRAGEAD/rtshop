package com.rtshop.magic;

import com.rtshop.RtShop;
import com.rtshop.gacha.GachaConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 이 세계의 Epic Fight 스킬(Epic Fight·Nightfall·Weapons of Miracles 등)을 찾아 마법 뽑기 목록을 채운다.
 * Epic Fight 를 직접 참조하지 않고 이름으로 찾는다 (없으면 아무것도 하지 않음).
 * 스킬북 = epicfight:skillbook + NBT {skill:"모드:스킬"}.
 */
public final class MagicSkills {
    private MagicSkills() {
    }

    public static final String BOOK = "epicfight:skillbook";

    /** 찾은 스킬 하나. */
    public record Skill(String id, String efCategory, boolean learnable) {
    }

    /** 스킬북으로 배울 수 있는 스킬 (Epic Fight 가 없으면 빈 목록). */
    public static List<Skill> discover() {
        List<Skill> out = new ArrayList<>();
        try {
            Class<?> sm = Class.forName("yesman.epicfight.api.data.reloader.SkillManager");
            Method getSkills = sm.getMethod("getSkills", Predicate.class);
            Collection<?> all = (Collection<?>) getSkills.invoke(null, (Predicate<Object>) s -> true);
            Class<?> skillCls = Class.forName("yesman.epicfight.skill.Skill");
            Class<?> catCls = Class.forName("yesman.epicfight.skill.SkillCategory");
            Method getCategory = skillCls.getMethod("getCategory");
            Method learnableM = catCls.getMethod("learnable");
            java.util.Set<String> books = bookSkills();
            for (Object s : all) {
                try {
                    Object cat = getCategory.invoke(s);
                    boolean learnable = (boolean) learnableM.invoke(cat);
                    String id = skillId(s);
                    if (id == null) continue;
                    // 창작 탭에 스킬북이 있으면 배울 수 있는 것으로 본다 (모드가 따로 만든 분류도 포함)
                    out.add(new Skill(id, String.valueOf(cat), learnable || books.contains(id)));
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.info("[rtshop] 마법 뽑기: Epic Fight 스킬을 찾지 못했습니다 ({})", t.toString());
        }
        out.sort((a, b) -> a.id.compareTo(b.id));
        return out;
    }

    /** 창작 탭에 진열된 스킬북의 스킬 ID 들 (모드가 실제로 내놓은 스킬북). */
    static java.util.Set<String> bookSkills() {
        java.util.Set<String> out = new java.util.HashSet<>();
        try {
            var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return out;
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), true, server.registryAccess());
            for (var tab : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) {
                for (net.minecraft.world.item.ItemStack st : tab.getDisplayItems()) {
                    ResourceLocation rid = ForgeRegistries.ITEMS.getKey(st.getItem());
                    if (rid != null && BOOK.equals(rid.toString()) && st.hasTag() && st.getTag().contains("skill")) out.add(st.getTag().getString("skill"));
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static String skillId(Object skill) {
        for (String m : new String[]{"getRegistryName", "registryName"}) {
            try {
                Object rl = skill.getClass().getMethod(m).invoke(skill);
                if (rl != null) return rl.toString();
            } catch (Throwable ignored) {
            }
        }
        try {
            Class<?> sm = Class.forName("yesman.epicfight.api.data.reloader.SkillManager");
            Object reg = sm.getMethod("getSkillRegistry").invoke(null);
            Object rl = reg.getClass().getMethod("getKey", Object.class).invoke(reg, skill);
            return rl == null ? null : rl.toString();
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Epic Fight 분류 → 마법 뽑기 분야. null = 넣지 않음. */
    public static String categoryOf(Skill s) {
        if (!s.learnable) return null;
        String c = s.efCategory.toUpperCase(Locale.ROOT);
        return switch (c) {
            case "DODGE" -> "dodge";
            case "GUARD" -> "guard";
            case "MOVER" -> "mover";
            case "PASSIVE" -> "passive";
            case "IDENTITY" -> "attack";
            case "BASIC_ATTACK", "AIR_ATTACK", "WEAPON_INNATE", "WEAPON_PASSIVE", "KNOCKDOWN_WAKEUP" -> null; // 무기·몸에 딸린 것
            // 모드가 따로 만든 분류 (Nightfall 의 기술 등): 공격 분야
            default -> c.contains("GUARD") || c.contains("PARRY") ? "guard" : c.contains("DODGE") ? "dodge" : "attack";
        };
    }

    /** 정해 둔 등급 (스킬 ID → 등급). 나머지는 모드·분야로 정한다. */
    static final Map<String, String> FIXED = new LinkedHashMap<>();

    static {
        // Epic Fight 기본기
        for (String x : new String[]{"epicfight:roll", "epicfight:step", "epicfight:guard", "epicfightx:roll", "epicfightx:step"}) FIXED.put(x, "common");
        for (String x : new String[]{"epicfight:parrying", "epicfight:impact_guard", "epicfight:endurance", "epicfight:technician",
                "epicfight:stamina_pillager", "epicfightx:combat_mastery_i"}) FIXED.put(x, "uncommon");
        for (String x : new String[]{"epicfight:hypervitality", "epicfight:swordmaster", "epicfight:berserker", "epicfight:death_harvest",
                "epicfight:emergency_escape", "epicfight:adaptive_skin", "epicfight:adrenaline_fiend", "epicfight:catharsis", "epicfight:bonebreaker",
                "epicfight:vengeance", "epicfight:forbidden_strength", "epicfightx:combat_mastery_ii"}) FIXED.put(x, "rare");
        for (String x : new String[]{"epicfight:meteor_slam", "epicfight:demolition_leap", "epicfight:phantom_ascent", "epicfight:revelation"})
            FIXED.put(x, "epic");
        // Weapons of Miracles
        for (String x : new String[]{"wom:natural_sprinter", "wom:aqua_maneuvre", "wom:arrow_tenacity", "wom:meditation", "wom:inner_growth"}) FIXED.put(x, "rare");
        for (String x : new String[]{"wom:precise_roll", "wom:shadow_step", "wom:ender_step", "wom:bull_charge", "wom:soul_protection", "wom:shulker_cloak",
                "wom:spider_techniques", "wom:pain_anticipation", "wom:latent_retribution", "wom:vampirize", "wom:critical_knowledge", "wom:heart_shield",
                "wom:mindset", "wom:adrenaline", "wom:dancing_blade", "wom:dopamine", "wom:manipulator", "wom:counter_attack", "wom:punishment_kick"})
            FIXED.put(x, "epic");
        for (String x : new String[]{"wom:dodge_master", "wom:ender_obscuris", "wom:time_travel", "wom:vengeful_parry", "wom:perfect_bulwark",
                "wom:buster_parade", "wom:lethal_focus", "wom:lunatic_vivacity", "wom:voodoo_magic", "wom:back_and_forth", "wom:shooting_style",
                "wom:proximity_exploiter", "wom:all_eyes_on_you", "wom:all_eyes_on_me", "wom:avatar_of_might"}) FIXED.put(x, "legendary");
        // Nightfall: 소울라이크 회피·패리는 영웅, 상징 기술은 전설, 심판 절단(야마토)은 야마토
        for (String x : new String[]{"efn:efn_dodge", "efn:efn_step", "efn:efn_parry", "efn:stomp", "efn:execution"}) FIXED.put(x, "epic");
        for (String x : new String[]{"efn:parry_master", "efn:precise_parry", "efn:indestructible", "efn:yamato_step", "efn:murasama_parry",
                "efn:murasama_dodge", "efn:zansetsu", "efn:mortal_blade"}) FIXED.put(x, "legendary");
        FIXED.put("efn:judgementcutend", "yamato");
    }

    /** 이 스킬의 등급. */
    public static String gradeOf(Skill s, String cat) {
        String g = FIXED.get(s.id);
        if (g != null) return "yamato".equals(g) && !"attack".equals(cat) ? "legendary" : g;
        String ns = s.id.substring(0, Math.max(0, s.id.indexOf(':')));
        String path = s.id.substring(s.id.indexOf(':') + 1);
        if (path.contains("judgement") || path.contains("judgment")) return "attack".equals(cat) ? "yamato" : "legendary";
        // 단계가 붙은 보상 스킬 (…1 ~ …4): 단계대로
        char last = path.isEmpty() ? ' ' : path.charAt(path.length() - 1);
        if (last >= '1' && last <= '4' && ns.equals("dodge_parry_reward")) return new String[]{"common", "uncommon", "rare", "epic"}[last - '1'];
        return switch (ns) {
            case "epicfight", "epicfightx" -> "attack".equals(cat) || "mover".equals(cat) ? "epic" : "rare";
            case "efs_iss" -> "passive".equals(cat) ? "rare" : "epic";
            case "efn" -> "attack".equals(cat) ? "legendary" : "epic";
            case "wom" -> "attack".equals(cat) ? "legendary" : "epic";
            default -> "attack".equals(cat) ? "epic" : "uncommon";
        };
    }

    /** 찾은 스킬로 목록을 다시 만든다 (스킬북 아이템이 없으면 그대로 둔다). 넣은 수. */
    public static int autoFill(MagicConfig c) {
        Item book = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(BOOK));
        if (book == null || book == net.minecraft.world.item.Items.AIR) return 0;
        List<Skill> skills = discover();
        if (skills.isEmpty()) return 0;
        for (String cat : MagicConfig.CATS) c.pool(cat).clear();
        int n = 0;
        for (Skill s : skills) {
            String cat = categoryOf(s);
            if (cat == null) continue;
            GachaConfig.Entry e = new GachaConfig.Entry();
            e.item = BOOK;
            e.nbt = "{skill:\"" + s.id + "\"}";
            e.grade = gradeOf(s, cat);
            e.weight = 10;
            c.pool(cat).add(e);
            n++;
        }
        c.normalize();
        RtShop.LOGGER.info("[rtshop] 마법 뽑기: 스킬북 {}개를 목록에 넣었습니다", n);
        return n;
    }
}
