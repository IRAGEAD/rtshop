package com.rtshop.land;

import com.rtshop.RtShop;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 3.84 (사용자 11-29 "마을 구역에서 플레이어를 제외한 모든 생명체 스폰, 진입 금지" → "추천대로, 이미 있는 동물도 삭제"):
 * 마을(성 영역)에는 플레이어와 정해 둔 것만 있을 수 있다.
 * <ul>
 *   <li>남는 것: 플레이어, RT Shop 의 NPC · 동료 · 뽑기 펫(태그가 rtshop 으로 시작), 주인이 있는 것(길들인 동물 · 내 펫 · 탈것), 목줄에 묶인 것,
 *       누가 탔거나 무엇에 실린 것, 이름표를 붙인 것, 갑옷 거치대, land.json castleCreatureKeep 에 적은 것 (카지노의 딜러 등).</li>
 *   <li>공용 구역(산 토지 밖)에서는 그 밖의 생물이 저절로 생기지 않고(자연 · 새끼 · 행사 …), 1초마다 들어온 것 · 이미 있던 것을 지운다 (떨어뜨리는 것 없이).</li>
 *   <li>플레이어가 산 땅 위의 동물은 그대로 두고, 몬스터는 산 땅 위에서도 지운다.</li>
 * </ul>
 * land.json 의 castleNoCreatures=false 면 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class ZoneCreatures {
    private ZoneCreatures() {
    }

    /** 점검용: 지운 수 · 막은 스폰 수. */
    public static volatile int removedForTest, blockedForTest;
    private static long lastLog;
    private static int sinceLog;

    public static boolean on() {
        return LandConfig.get().castleNoCreatures;
    }

    /** 이 생물은 마을에 있어도 되는가 (자리와 상관없이 그 생물만 보고). */
    public static boolean keep(Entity e) {
        if (!(e instanceof LivingEntity) || e instanceof Player || e instanceof ArmorStand) return true;
        for (String t : e.getTags()) if (t.startsWith("rtshop")) return true;
        if (com.rtshop.shop.ShopServer.isNpc(e) || e instanceof com.rtshop.story.CompanionEntity) return true;
        if (e.hasCustomName()) return true;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null) return true;
        if (e instanceof AbstractHorse h && (h.isTamed() || h.getOwnerUUID() != null)) return true;
        if (e instanceof Mob m && m.isLeashed()) return true;
        if (e.isVehicle() || e.isPassenger()) return true;
        if (summoned(e)) return true;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return id != null && ZoneUse.match(LandConfig.get().castleCreatureKeep, id);
    }

    /** Iron's Spells 같은 주문으로 불러낸 것인가 (그 모드의 MagicSummon 따위 이름의 인터페이스). */
    static boolean summoned(Entity e) {
        for (Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                String n = i.getSimpleName();
                if (n.equals("MagicSummon") || n.equals("IMagicSummon") || n.equals("ISummon")) return true;
            }
        }
        return false;
    }

    /** 몬스터인가 (산 땅 위에서도 지운다). */
    static boolean monster(Entity e) {
        return e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER;
    }

    /** 이 자리의 이 생물을 지워야 하는가. */
    public static boolean unwanted(ServerLevel level, Entity e) {
        if (!on() || keep(e)) return false;
        BlockPos pos = e.blockPosition();
        if (!LandManager.inCastleZone(level, pos)) return false;
        if (monster(e)) return true;
        return LandManager.guardAt(level, pos) == LandManager.ZONE;   // 공용 구역만 (플레이어가 산 땅의 동물은 그대로)
    }

    /** 저절로 생기는 스폰을 막는다 (뽑기 펫 · 스폰 알은 생긴 뒤에 길들여지므로 여기서 막지 않는다 — 1초마다 보는 쪽이 지운다). */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFinalize(MobSpawnEvent.FinalizeSpawn event) {
        try {
            if (!on()) return;
            var t = event.getSpawnType();
            if (t == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG || t == net.minecraft.world.entity.MobSpawnType.COMMAND
                    || t == net.minecraft.world.entity.MobSpawnType.BUCKET || t == net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED) return;
            ServerLevel sl = event.getLevel().getLevel();
            if (sl == null) return;
            Mob mob = event.getEntity();
            if (unwanted(sl, mob)) {
                event.setSpawnCancelled(true);
                blockedForTest++;
            }
        } catch (Throwable ignored) {
        }
    }

    /** 새끼 · 알에서 깬 것 · 소환된 것: 세계에 들어오는 순간 (저장돼 있던 것은 1초마다 보는 쪽이). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onJoin(EntityJoinLevelEvent event) {
        try {
            if (event.loadedFromDisk() || !on() || !(event.getLevel() instanceof ServerLevel sl) || !(event.getEntity() instanceof Mob mob)) return;
            if (mob.isBaby() && unwanted(sl, mob)) {
                event.setCanceled(true);
                blockedForTest++;
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel sl) || sl.getGameTime() % 20 != 7) return;
        try {
            if (!on() || !CastlePlots.zoneActive(sl.getServer()) || !LandManager.dim(sl).equals(CastlePlots.def().dim)) return;
            sweep(sl);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] 마을 생물 정리 실패: {}", t.toString());
        }
    }

    /** 마을 안의 지울 생물을 지운다. 지운 수. */
    public static int sweep(ServerLevel sl) {
        return sweep(sl, sl.getAllEntities());
    }

    /** 이 생물들 가운데 마을 안의 지울 것을 지운다 (점검은 막 불러온 청크의 생물을 직접 건넨다). 지운 수. */
    public static int sweep(ServerLevel sl, Iterable<Entity> entities) {
        int[] box = CastlePlots.zoneBox();
        if (box == null) return 0;
        List<Entity> gone = new ArrayList<>();
        for (Entity e : entities) {
            if (!(e instanceof LivingEntity) || e instanceof Player || !e.isAlive()) continue;
            double x = e.getX(), z = e.getZ();
            if (x < box[0] || x > box[2] || z < box[1] || z > box[3]) continue;
            if (unwanted(sl, e)) gone.add(e);
        }
        for (Entity e : gone) {
            e.ejectPassengers();
            e.discard();
        }
        if (!gone.isEmpty()) {
            removedForTest += gone.size();
            sinceLog += gone.size();
            long now = System.currentTimeMillis();
            if (now - lastLog > 60_000) {
                RtShop.LOGGER.info("[rtshop] 마을(성 영역)에 들어온 생물 {}마리를 지웠습니다 (castleNoCreatures). 예: {}", sinceLog,
                        ForgeRegistries.ENTITY_TYPES.getKey(gone.get(0).getType()));
                lastLog = now;
                sinceLog = 0;
            }
        }
        return gone.size();
    }
}
