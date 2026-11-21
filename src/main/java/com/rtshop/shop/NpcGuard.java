package com.rtshop.shop;

import com.rtshop.RtShop;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * RT Shop 의 NPC(상인·은행원·직업·퀘스트·출석·뽑기)는 다치거나 죽지 않는다.
 * 크리에이티브 공격, 다른 모드의 특수 피해, 번개(마녀 변신), 좀비화도 막는다.
 * OP 가 치우려고 쓰는 /kill 과 월드 밖 추락만은 통과시킨다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class NpcGuard {
    private NpcGuard() {
    }

    public static boolean isNpc(Entity e) {
        if (e == null) {
            return false;
        }
        for (String t : e.getTags()) {
            if (t.startsWith("rtshop_shop_") || t.startsWith("rtshop_quest_") || t.equals("rtshop_bank")
                    || t.equals("rtshop_job") || t.equals("rtshop_attendance") || t.equals("rtshop_gacha")) {
                return true;
            }
        }
        return false;
    }

    private static boolean allowed(DamageSource src) {
        return src != null && (src.is(DamageTypes.GENERIC_KILL) || src.is(DamageTypes.FELL_OUT_OF_WORLD));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent e) {
        if (isNpc(e.getEntity()) && !allowed(e.getSource())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurt(LivingHurtEvent e) {
        if (isNpc(e.getEntity()) && !allowed(e.getSource())) {
            e.setAmount(0);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingDamageEvent e) {
        if (isNpc(e.getEntity()) && !allowed(e.getSource())) {
            e.setAmount(0);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (isNpc(v) && !allowed(e.getSource())) {
            e.setCanceled(true);
            v.setHealth(v.getMaxHealth());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKnockBack(LivingKnockBackEvent e) {
        if (isNpc(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLightning(EntityStruckByLightningEvent e) {
        if (isNpc(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onConvert(LivingConversionEvent.Pre e) {
        if (isNpc(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    /**
     * 클라이언트 쪽 우클릭: 엔티티 태그는 서버에만 있어 클라이언트는 RT Shop NPC 를 모른다. 그래서 그대로 두면 클라이언트가
     * 주민 거래 목록을 만들려다 다른 모드의 거래 수정(Mixin) 오류로 튕길 수 있다 (수산물 상인 = 어부 주민).
     * RT Shop NPC 는 모두 AI 가 꺼진 주민이므로, 클라이언트에서는 AI 가 꺼진 주민의 기본 동작을 막는다.
     * 상호작용 패킷은 이미 서버로 간 뒤라 서버가 창을 연다 (다른 AI 꺼진 주민도 서버가 거래창을 보내므로 그대로 쓸 수 있다).
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientInteract(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract e) {
        if (e.getLevel().isClientSide() && e.getTarget() instanceof net.minecraft.world.entity.npc.AbstractVillager v && v.isNoAi()) {
            e.setCanceled(true);
            e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    /** 피해 이벤트를 거치지 않고 체력을 직접 깎는 모드가 있어도 매 초 체력을 되돌린다. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide() || v.tickCount % 20 != 0 || !isNpc(v)) {
            return;
        }
        if (v.getHealth() < v.getMaxHealth() && v.isAlive()) {
            v.setHealth(v.getMaxHealth());
        }
        if (v.isOnFire()) {
            v.clearFire();
        }
        if (!v.getActiveEffects().isEmpty()) {
            v.removeAllEffects();
        }
        if (!v.isInvulnerable()) {
            v.setInvulnerable(true);
        }
    }
}
