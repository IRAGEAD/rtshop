package com.rtshop.party;

import com.rtshop.RtShop;
import com.rtshop.pvp.Pvp;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 보스 처치 인정: 보스 최대 체력의 일정 비율(기본 10%) 이상을 직접 입힌 플레이어만 처치한 것으로 친다.
 * 마법·화살·소환수·길들인 동물이 입힌 피해는 주인의 피해로 센다 (Pvp.attackerOf).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class BossCredit {
    private BossCredit() {
    }

    /** 보스 UUID → (플레이어 UUID → 입힌 피해 합). */
    private static final Map<UUID, Map<UUID, Float>> DAMAGE = new HashMap<>();

    public static String id(Entity e) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return rl == null ? "" : rl.toString();
    }

    public static boolean isBoss(Entity e) {
        return e instanceof LivingEntity && PartyConfig.get().bosses.contains(id(e));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide() || event.isCanceled() || event.getAmount() <= 0 || !isBoss(e)) return;
        MinecraftServer server = e.getServer();
        ServerPlayer p = server == null ? null : Pvp.attackerOf(event.getSource(), server);
        if (p == null) return;
        float dealt = Math.min(event.getAmount(), Math.max(0, e.getHealth()));
        if (dealt <= 0) dealt = event.getAmount();
        DAMAGE.computeIfAbsent(e.getUUID(), k -> new HashMap<>()).merge(p.getUUID(), dealt, Float::sum);
    }

    /** 점검용·외부용: 피해를 직접 기록한다. */
    public static void record(LivingEntity boss, ServerPlayer p, float amount) {
        DAMAGE.computeIfAbsent(boss.getUUID(), k -> new HashMap<>()).merge(p.getUUID(), amount, Float::sum);
    }

    public static float dealt(LivingEntity boss, ServerPlayer p) {
        Map<UUID, Float> m = DAMAGE.get(boss.getUUID());
        return m == null ? 0 : m.getOrDefault(p.getUUID(), 0F);
    }

    /** 처치로 인정받는 사람들 (기준 이상 피해를 입힌 접속 중인 플레이어). */
    public static List<ServerPlayer> qualified(LivingEntity boss, MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        Map<UUID, Float> m = DAMAGE.get(boss.getUUID());
        if (m == null || server == null) return out;
        double need = boss.getMaxHealth() * PartyConfig.get().bossCreditPercent / 100.0;
        for (Map.Entry<UUID, Float> en : m.entrySet()) {
            if (en.getValue() + 1e-4 < need) continue;
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                for (ServerPlayer t : com.rtshop.raid.Raid.TEST_PLAYERS) if (t.getUUID().equals(en.getKey())) p = t;
            }
            if (p != null) out.add(p);
        }
        return out;
    }

    /** 죽음 처리(퀘스트·레이드)가 모두 끝난 뒤 기록을 지운다. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && !event.isCanceled()) DAMAGE.remove(event.getEntity().getUUID());
    }

    public static void forget(Entity boss) {
        DAMAGE.remove(boss.getUUID());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && !(event.getEntity() instanceof LivingEntity l && l.isDeadOrDying())) {
            // 사라진 보스(언로드·디스폰)의 기록은 지운다. 죽은 보스는 처치 처리 뒤 지운다.
            if (DAMAGE.containsKey(event.getEntity().getUUID()) && event.getEntity().isRemoved()
                    && event.getEntity().getRemovalReason() != null && event.getEntity().getRemovalReason().shouldDestroy()) {
                DAMAGE.remove(event.getEntity().getUUID());
            }
        }
    }
}
