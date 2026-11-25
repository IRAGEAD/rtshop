package com.rtshop.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Epic Fight 의 무기 정보 읽기 (리플렉션; Epic Fight 가 없으면 아무것도 돌려주지 않는다).
 * 무기 설명서가 "이 무기에 특수기가 있는가, 드는 방식마다 연속 공격은 몇 타인가"를 아는 데 쓴다.
 * 무기 능력에 적힌 표(드는 방식 → 연속 공격·특수기)를 그대로 읽으므로, 플레이어가 지금 무엇을 들고 있든 같은 답이 나온다.
 */
public final class EfWeapons {
    private EfWeapons() {
    }

    /** 드는 방식 하나: 방식 이름(one_hand, two_hand, common, sheath, ochs…), 특수기 ID(없으면 ""), 연속 공격 타수(모르면 0), 특수기 객체. */
    public record Form(String style, String skill, int combo, Object skillObject) {
    }

    /** 무기 종류(sword, tachi, spear… Epic Fight 의 이름을 소문자로), 대표 특수기, 대표 타수, 달리며·뛰어서 치는 공격이 있는가, 드는 방식들. */
    public record Info(String category, String skill, int combo, boolean dashAndAir, List<Form> forms) {
        /** 같은 종류·같은 특수기들의 무기(나무 검과 철 검)는 설명서 한 권을 함께 쓴다. */
        public String key() {
            TreeSet<String> ids = new TreeSet<>();
            for (Form f : forms) if (!f.skill().isEmpty()) ids.add(f.skill());
            if (ids.isEmpty() && !skill.isEmpty()) ids.add(skill);
            return category + "|" + String.join(",", ids);
        }

        @Override
        public String toString() {
            return key() + " x" + combo;
        }
    }

    /** 설명서에 적는 차례: 한 손 → 기본 → 양손 → 그 밖. 탈것 위(mount)는 적지 않는다. */
    private static final List<String> ORDER = List.of("one_hand", "common", "two_hand", "sheath", "ochs", "ranged");

    private static Boolean present;
    private static MethodHandle getCap, getPatch, isEmpty, getCategory, getInnate, getAuto, registryName;
    /** 준비하다가 난 오류 (점검이 보여 준다). */
    public static String failure = "";

    /**
     * Epic Fight 의 메서드를 하나씩 직접 찾는다. Class.getMethod 는 그 클래스의 모든 메서드 서명을 한꺼번에 읽어서,
     * 전용 서버에서는 화면 쪽 클래스(LocalPlayer)를 쓰는 메서드 하나 때문에 통째로 실패한다 (3.48 점검에서 드러남).
     */
    public static boolean present() {
        if (present == null) {
            try {
                if (!ModList.get().isLoaded("epicfight")) {
                    present = false;
                    return false;
                }
                MethodHandles.Lookup look = MethodHandles.publicLookup();
                Class<?> caps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
                Class<?> cap = Class.forName("yesman.epicfight.world.capabilities.item.CapabilityItem");
                Class<?> patchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
                Class<?> category = Class.forName("yesman.epicfight.world.capabilities.item.WeaponCategory");
                Class<?> skill = Class.forName("yesman.epicfight.skill.Skill");
                getCap = find(look, caps, "getItemStackCapability", true, cap, ItemStack.class);
                isEmpty = find(look, cap, "isEmpty", false, boolean.class);
                getCategory = find(look, cap, "getWeaponCategory", false, category);
                registryName = find(look, skill, "getRegistryName", false, ResourceLocation.class);
                // 아래 셋은 표(드는 방식 → 특수기·연속 공격)를 읽지 못하는 무기에서만 쓴다: 없어도 설명서는 된다
                try {
                    getPatch = find(look, caps, "getPlayerPatch", true, patchClass, Player.class);
                    getInnate = find(look, cap, "getInnateSkill", false, skill, patchClass, ItemStack.class);
                    getAuto = find(look, cap, "getAutoAttackMotion", false, List.class, patchClass);
                } catch (Throwable t) {
                    failure = "fallback: " + t;
                }
                present = true;
            } catch (Throwable t) {
                failure = String.valueOf(t);
                com.rtshop.RtShop.LOGGER.warn("[rtshop] Epic Fight 의 무기 정보를 읽을 준비를 하지 못했습니다 (무기 설명서가 꺼집니다): {}", t.toString());
                present = false;
            }
        }
        return present;
    }

    /** 메서드 하나를 직접 찾고, 그렇게 못 찾으면(모듈 접근 등) 리플렉션으로 한 번 더 찾는다. */
    private static MethodHandle find(MethodHandles.Lookup look, Class<?> owner, String name, boolean isStatic, Class<?> ret, Class<?>... args) throws Throwable {
        try {
            MethodType type = MethodType.methodType(ret, args);
            return isStatic ? look.findStatic(owner, name, type) : look.findVirtual(owner, name, type);
        } catch (ReflectiveOperationException e) {
            return look.unreflect(owner.getMethod(name, args));
        }
    }

    /** Epic Fight 의 무기 능력 객체 (없으면 null). */
    public static Object capability(ItemStack s) {
        if (s == null || s.isEmpty() || !present()) return null;
        try {
            Object cap = getCap.invoke(s);
            return cap == null || (boolean) isEmpty.invoke(cap) ? null : cap;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object patch(Player p) {
        if (p == null || !present() || getPatch == null) return null;
        try {
            return getPatch.invoke(p);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String skillId(Object skill) {
        if (skill == null) return "";
        try {
            Object rl = registryName.invoke(skill);
            return rl instanceof ResourceLocation r ? r.toString() : String.valueOf(rl);
        } catch (Throwable t) {
            return "";
        }
    }

    private static Map<?, ?> table(Object cap, String field) {
        for (Class<?> c = cap.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(field);
                f.setAccessible(true);
                return f.get(cap) instanceof Map<?, ?> m ? m : null;
            } catch (NoSuchFieldException e) {
                // 윗 단계의 클래스에서 다시 찾는다
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static String styleName(Object style) {
        return String.valueOf(style).toLowerCase(Locale.ROOT);
    }

    private static int rank(String style) {
        int i = ORDER.indexOf(style);
        return i < 0 ? ORDER.size() : i;
    }

    /** 연속 공격 목록의 타수: Epic Fight 의 약속으로 목록의 마지막 둘은 달리며 치는 공격과 뛰어서 치는 공격이다. */
    private static int comboOf(Object list) {
        return list instanceof List<?> l ? (l.size() >= 3 ? l.size() - 2 : l.size()) : 0;
    }

    @SuppressWarnings("unchecked")
    private static Object skillOf(Object provider, ItemStack s) {
        try {
            return provider instanceof Function<?, ?> fn ? ((Function<ItemStack, Object>) fn).apply(s) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 이 물건의 무기 정보. Epic Fight 가 없거나 무기 능력이 없는 물건이면 null. */
    public static Info info(Player p, ItemStack s) {
        Object cap = capability(s);
        if (cap == null) return null;
        try {
            String cat = String.valueOf((Object) getCategory.invoke(cap)).toLowerCase(Locale.ROOT);
            Map<?, ?> skills = table(cap, "innateSkill"), motions = table(cap, "autoAttackMotions");
            List<Form> forms = new ArrayList<>();
            boolean extra = false;
            if (skills != null || motions != null) {
                Object commonSkill = null, commonMotion = null;
                TreeSet<String> names = new TreeSet<>((x, y) -> rank(x) != rank(y) ? Integer.compare(rank(x), rank(y)) : x.compareTo(y));
                java.util.HashMap<String, Object> skillBy = new java.util.HashMap<>(), motionBy = new java.util.HashMap<>();
                if (skills != null) {
                    for (Map.Entry<?, ?> e : skills.entrySet()) skillBy.put(styleName(e.getKey()), e.getValue());
                    commonSkill = skillBy.get("common");
                }
                if (motions != null) {
                    for (Map.Entry<?, ?> e : motions.entrySet()) motionBy.put(styleName(e.getKey()), e.getValue());
                    commonMotion = motionBy.get("common");
                }
                names.addAll(skillBy.keySet());
                names.addAll(motionBy.keySet());
                names.remove("mount");
                for (String n : names) {
                    Object skill = skillOf(skillBy.getOrDefault(n, commonSkill), s);
                    Object motion = motionBy.getOrDefault(n, commonMotion);
                    if (motion instanceof List<?> l && l.size() >= 3) extra = true;
                    forms.add(new Form(n, skillId(skill), comboOf(motion), skill));
                }
            }
            if (forms.isEmpty()) {
                // 표가 없는 무기 능력(다른 모드가 따로 만든 것): 지금 든 자세의 것을 물어본다
                Object patch = patch(p);
                if (patch != null && getInnate != null && getAuto != null) {
                    Object skill = null, motion = null;
                    try {
                        skill = getInnate.invoke(cap, patch, s);
                    } catch (Throwable ignored) {
                    }
                    try {
                        motion = getAuto.invoke(cap, patch);
                    } catch (Throwable ignored) {
                    }
                    if (motion instanceof List<?> l && l.size() >= 3) extra = true;
                    forms.add(new Form("common", skillId(skill), comboOf(motion), skill));
                }
            }
            String skill = "";
            int combo = 0;
            for (Form f : forms) {
                if (skill.isEmpty() && !f.skill().isEmpty()) skill = f.skill();
                if (combo == 0 && f.combo() > 0) combo = f.combo();
            }
            return new Info(cat, skill, combo, extra, forms);
        } catch (Throwable t) {
            return null;
        }
    }
}
