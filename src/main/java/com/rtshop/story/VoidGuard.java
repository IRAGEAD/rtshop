package com.rtshop.story;

import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 공허 추락 방지: 환술사의 '타락한 공허' 차원과 기억의 문(rtshop:memory)에서 공허로 떨어지면
 * 마지막으로 서 있던 땅으로 되돌려 보낸다 (잠깐 천천히 떨어지기). 공허 피해로는 죽지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class VoidGuard {
    private VoidGuard() {
    }

    public static final Set<String> DIMS = Set.of("illusioner:corrupted_void", "rtshop:memory");
    private static final Map<UUID, Vec3> SAFE = new HashMap<>();
    private static final Map<UUID, String> SAFE_DIM = new HashMap<>();

    public static boolean guarded(ServerLevel level) {
        return DIMS.contains(level.dimension().location().toString());
    }

    /** 발밑 기둥 전체가 비어 있는가 (공허 위). */
    static boolean overVoid(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlockPos b = p.blockPosition();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, b.getX(), b.getZ());
        return top <= level.getMinBuildHeight() || top < b.getY() - 64;
    }

    public static void rescue(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 safe = SAFE.get(p.getUUID());
        String dim = level.dimension().location().toString();
        if (safe == null || !dim.equals(SAFE_DIM.get(p.getUUID()))) {
            // 기록이 없으면 그 자리 위의 가장 높은 땅, 그것도 없으면 차원의 시작점
            BlockPos sp = level.getSharedSpawnPos();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, sp.getX(), sp.getZ());
            safe = new Vec3(sp.getX() + 0.5, Math.max(y, level.getMinBuildHeight() + 1) + 1, sp.getZ() + 0.5);
        }
        p.teleportTo(level, safe.x, safe.y + 0.1, safe.z, p.getYRot(), p.getXRot());
        if (p instanceof net.minecraftforge.common.util.FakePlayer) p.setPos(safe.x, safe.y + 0.1, safe.z); // 점검용 가짜 플레이어는 연결이 없다
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0;
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
        p.displayClientMessage(Component.translatable("rtshop.void.rescued").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        if (!guarded(level)) return;
        if (p.onGround() && !overVoid(p)) {
            SAFE.put(p.getUUID(), p.position());
            SAFE_DIM.put(p.getUUID(), level.dimension().location().toString());
            return;
        }
        Vec3 safe = SAFE.get(p.getUUID());
        boolean deep = p.getY() < level.getMinBuildHeight() + 4;
        boolean falling = safe != null && p.getY() < safe.y - 24 && overVoid(p) && p.getDeltaMovement().y < 0 && !p.getAbilities().flying;
        if (deep || falling) rescue(p);
    }

    /** 혹시 공허 피해가 들어오면 막고 구한다. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !guarded(p.serverLevel())) return;
        if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && p.getY() < p.serverLevel().getMinBuildHeight()) {
            event.setCanceled(true);
            rescue(p);
        }
    }
}
