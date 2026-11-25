package com.rtshop.compat;

import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 무기를 바꿔도 특수기의 스택(충전 횟수)이 사라지지 않게 한다 (world_rules.json 의 keepSkillStacks, 3.50).
 * Epic Fight 는 무기를 바꾸면 특수기 칸을 새로 채우면서 스택과 게이지를 0 으로 되돌린다.
 * 그래서 특수기마다 마지막 스택·게이지를 기억해 두었다가, 그 특수기가 다시 칸에 들어오면 되돌려 준다.
 * 처음 드는 특수기에는 방금 들고 있던 무기의 스택을 넘겨준다 (그 특수기의 최대 스택까지).
 * Epic Fight 를 이름으로만 건드린다 (없으면 아무 일도 하지 않는다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class EfStacks {
    private EfStacks() {
    }

    private static final class State {
        String lastId = "";
        int lastStack;
        final Map<String, float[]> saved = new HashMap<>(); // 특수기 ID -> {스택, 게이지}
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static Boolean ready;
    private static Object innateSlot;
    private static MethodHandle getContainer, getSkill, getStack, getResource, setStack, setResource, registryName;
    public static String failure = "";
    public static int restoredForTest;

    public static boolean ready() {
        if (ready == null) {
            try {
                if (!ModList.get().isLoaded("epicfight")) {
                    ready = false;
                    return false;
                }
                MethodHandles.Lookup look = MethodHandles.publicLookup();
                Class<?> patch = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
                Class<?> slot = Class.forName("yesman.epicfight.skill.SkillSlot");
                Class<?> container = Class.forName("yesman.epicfight.skill.SkillContainer");
                Class<?> skill = Class.forName("yesman.epicfight.skill.Skill");
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object s = Enum.valueOf((Class) Class.forName("yesman.epicfight.skill.SkillSlots"), "WEAPON_INNATE");
                innateSlot = s;
                getContainer = look.findVirtual(patch, "getSkill", MethodType.methodType(container, slot));
                getSkill = look.findVirtual(container, "getSkill", MethodType.methodType(skill));
                getStack = look.findVirtual(container, "getStack", MethodType.methodType(int.class));
                getResource = look.findVirtual(container, "getResource", MethodType.methodType(float.class));
                setStack = look.findVirtual(skill, "setStackSynchronize", MethodType.methodType(void.class, container, int.class));
                setResource = look.findVirtual(skill, "setConsumptionSynchronize", MethodType.methodType(void.class, container, float.class));
                registryName = look.findVirtual(skill, "getRegistryName", MethodType.methodType(ResourceLocation.class));
                ready = true;
            } catch (Throwable t) {
                failure = String.valueOf(t);
                RtShop.LOGGER.warn("[rtshop] Epic Fight 의 특수기 칸을 읽을 준비를 하지 못했습니다 (무기를 바꾸면 스택이 사라집니다): {}", t.toString());
                ready = false;
            }
        }
        return ready;
    }

    /** {특수기 칸, 특수기} (없으면 null). */
    private static Object[] innate(ServerPlayer p) throws Throwable {
        Object patch = EfWeapons.patch(p);
        if (patch == null) return null;
        Object c = (Object) getContainer.invoke(patch, innateSlot);
        if (c == null) return null;
        return new Object[]{c, (Object) getSkill.invoke(c)};
    }

    /** 점검용: "특수기ID|스택|게이지" (칸이 없으면 ""). */
    public static String probe(ServerPlayer p) {
        try {
            if (!ready()) return "";
            Object[] in = innate(p);
            if (in == null) return "";
            String id = in[1] == null ? "" : String.valueOf((Object) registryName.invoke(in[1]));
            return id + "|" + (int) getStack.invoke(in[0]) + "|" + (float) getResource.invoke(in[0]);
        } catch (Throwable t) {
            return "";
        }
    }

    /** 점검용: 이 특수기의 기억을 정해 둔다. */
    public static void rememberForTest(ServerPlayer p, String skillId, int stack) {
        STATES.computeIfAbsent(p.getUUID(), k -> new State()).saved.put(skillId, new float[]{stack, 0F});
    }

    public static void step(ServerPlayer p) {
        try {
            Object[] in = innate(p);
            if (in == null) return;
            Object container = in[0], skill = in[1];
            String id = skill == null ? "" : String.valueOf((Object) registryName.invoke(skill));
            int stack = (int) getStack.invoke(container);
            float res = (float) getResource.invoke(container);
            State st = STATES.computeIfAbsent(p.getUUID(), k -> new State());
            if (!id.equals(st.lastId)) {
                // 특수기가 바뀌었다 = 무기를 바꿨다: 기억해 둔 스택·게이지를 되돌린다 (처음 드는 특수기면 방금 것의 스택을 넘긴다)
                if (!id.isEmpty()) {
                    float[] sv = st.saved.get(id);
                    int want = sv != null ? (int) sv[0] : st.lastStack;
                    if (want > stack) {
                        setStack.invoke(skill, container, want);
                        restoredForTest++;
                    }
                    if (sv != null && sv[1] > res) setResource.invoke(skill, container, sv[1]);
                    stack = (int) getStack.invoke(container);
                    res = (float) getResource.invoke(container);
                }
                st.lastId = id;
            }
            if (!id.isEmpty()) {
                st.saved.put(id, new float[]{stack, res});
                st.lastStack = stack;
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && !(sp instanceof FakePlayer) && com.rtshop.rules.WorldRules.get().keepSkillStacks && ready()) step(sp);
    }

    /** 죽으면 처음부터 (되살아난 뒤에 옛 스택이 돌아오지 않게). */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) STATES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
