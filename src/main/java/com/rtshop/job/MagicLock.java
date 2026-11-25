package com.rtshop.job;

import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

/**
 * 마법은 마법사만 쓴다 (world_rules.json 의 magicOnlyForMage, 3.49).
 * <ul>
 * <li>Iron's Spells 'n Spellbooks: 주문을 쓰려는 순간(SpellPreCastEvent)에 막는다 — 주문서·두루마리·주문이 든 무기 모두.</li>
 * <li>Spell Engine(Wizards·RPG Series): 마법 계열 주문만 막는다. 근접·활 계열(무기의 기술)은 누구나 쓴다 (compat/SpellMana 가 시전 시도에 끼어든다).</li>
 * </ul>
 * 크리에이티브는 언제나 쓸 수 있다. 두 모드 모두 이름으로만 건드린다 (없으면 아무 일도 하지 않는다).
 */
public final class MagicLock {
    private MagicLock() {
    }

    public static final String MAGE = "mage";
    public static int blockedForTest;
    private static long lastWarn;

    /** 이 사람이 지금 마법을 쓸 수 있는가. */
    public static boolean canCast(Player p) {
        if (p == null || p.getAbilities().instabuild) return true;
        if (p.level().isClientSide) return !ClientJobCache.spellLocked;
        if (!(p instanceof ServerPlayer sp) || p instanceof FakePlayer && !testFake) return true;
        return !com.rtshop.rules.WorldRules.get().magicOnlyForMage || Jobs.has(sp, MAGE);
    }

    /** 점검용: 가짜 플레이어에게도 규칙을 적용한다. */
    public static boolean testFake;

    /** 막았음을 알린다 (화면 아래 한 줄; 잇달아 뜨지 않게). */
    public static void warn(Player p) {
        blockedForTest++;
        long now = System.currentTimeMillis();
        if (now - lastWarn < 1500 && p.level().isClientSide) return;
        lastWarn = now;
        p.displayClientMessage(Component.translatable("rtshop.job.magic_locked").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** Spell Engine 의 주문 계열 이름으로 마법인지 본다: 근접(physical_melee)·활(physical_ranged) 계열은 무기 기술이다. */
    public static boolean magicSchool(Object spell) {
        try {
            Object school = spell.getClass().getField("school").get(spell);
            if (school == null) return true;
            try {
                Object arch = school.getClass().getField("archetype").get(school);
                if (arch != null) return arch.toString().equalsIgnoreCase("MAGIC");
            } catch (Throwable ignored) {
            }
            return !String.valueOf(school.getClass().getField("id").get(school)).contains("physical");
        } catch (Throwable t) {
            return true;
        }
    }

    private static boolean hooked;

    /** Iron's Spells 의 "주문을 쓰기 직전" 이벤트에 끼어든다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void hook() {
        if (hooked || !ModList.get().isLoaded("irons_spellbooks")) return;
        try {
            Class cls = Class.forName("io.redspace.ironsspellbooks.api.events.SpellPreCastEvent");
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, cls, (java.util.function.Consumer) e -> {
                try {
                    if (e instanceof net.minecraftforge.event.entity.player.PlayerEvent pe && !canCast(pe.getEntity())) {
                        ((Event) e).setCanceled(true);
                        warn(pe.getEntity());
                    }
                } catch (Throwable ignored) {
                }
            });
            hooked = true;
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] Iron\'s Spells 의 주문 이벤트에 끼어들지 못했습니다 (마법사 전용 규칙이 Iron\'s 주문에는 걸리지 않습니다): {}", t.toString());
        }
    }

    public static boolean hookedForTest() {
        return hooked;
    }
}
