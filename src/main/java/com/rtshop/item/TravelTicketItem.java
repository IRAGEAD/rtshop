package com.rtshop.item;

import com.rtshop.land.LandData;
import com.rtshop.rules.TownSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 3.84 (사용자 11-29 "마을 내에 있을 때만 사용할 수 있는 성으로 이동하는 이동 티켓과 내 침대 혹은 내토지, 둘 모두 없으면 분수 광장(내가 설정한 스폰 포인트)로
 * 이동하는 귀가권을 새롭게 추가해줘. 가격은 귀환권보다는 저렴하게"): 우클릭을 누르고 있으면(귀환권과 같은 3초) 옮겨 준다.
 * <ul>
 *   <li>성 이동권 (CASTLE): 마을(성 영역) 안에서만. 성의 도착 자리(/rtshop spawn castle set)로 — 이미 성 곁(castleRange 칸)이면 분수 광장으로.</li>
 *   <li>귀가권 (HOME): 어디서든. 내 침대(리스폰 자리, 리스폰 정박기는 충전을 쓰지 않음) → 없으면 내 토지 → 둘 다 없으면 분수 광장(마을의 기준 자리).</li>
 * </ul>
 * 레이드 구역 · 기억의 문 안에서는 쓸 수 없다 (귀환권과 같음). 다시 쓰기까지의 시간은 town_spawn.json.
 */
public class TravelTicketItem extends Item {
    public enum Kind { CASTLE, HOME }

    private final Kind kind;

    public TravelTicketItem(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    static String key(Kind k) {
        return k == Kind.CASTLE ? "castle" : "home";
    }

    /** 지금 쓸 수 없는 까닭 (쓸 수 있으면 null). */
    public static Component refusal(ServerPlayer sp, Kind kind) {
        try {
            if (com.rtshop.raid.Raid.inArena(sp) || sp.level().dimension() == com.rtshop.story.StoryArena.DIM) {
                return Component.translatable("rtshop.travel.in_arena");
            }
        } catch (Throwable ignored) {
        }
        if (kind == Kind.CASTLE) {
            if (TownSpawn.castlePoint(sp.server) == null) return Component.translatable("rtshop.travel.no_castle");
            if (!TownSpawn.inTown(sp)) return Component.translatable("rtshop.travel.not_in_town");
            if (TownSpawn.atCastle(sp) && TownSpawn.townPoint(sp.server) == null) return Component.translatable("rtshop.return.no_spot");
        } else if (destination(sp) == null) {
            return Component.translatable("rtshop.return.no_spot");
        }
        long left = sp.getAbilities().instabuild ? 0 : TownSpawn.travelCooldownLeft(sp, key(kind));
        if (left > 0) return Component.translatable("rtshop.travel.cooldown", TownSpawn.leftText(left));
        return null;
    }

    /** 이 표가 데려갈 자리 (갈 곳이 없으면 null). */
    @Nullable
    public static TownSpawn.Arrival destination(ServerPlayer sp, Kind kind) {
        if (kind == Kind.CASTLE) {
            return TownSpawn.atCastle(sp) ? TownSpawn.townPoint(sp.server) : TownSpawn.castlePoint(sp.server);
        }
        return destination(sp);
    }

    /** 귀가권이 데려갈 자리: 내 침대 → 내 토지 → 분수 광장. */
    @Nullable
    public static TownSpawn.Arrival destination(ServerPlayer sp) {
        TownSpawn.Arrival bed = bed(sp);
        if (bed != null) return bed;
        TownSpawn.Arrival land = land(sp);
        if (land != null) return land;
        return TownSpawn.townPoint(sp.server);
    }

    /** 내 침대 (리스폰 자리). 침대가 부서졌거나 막혔으면 null. 리스폰 정박기는 충전을 쓰지 않고 본다. */
    @Nullable
    public static TownSpawn.Arrival bed(ServerPlayer sp) {
        BlockPos rp = sp.getRespawnPosition();
        if (rp == null) return null;
        ServerLevel lvl = sp.server.getLevel(sp.getRespawnDimension());
        if (lvl == null) return null;
        Optional<Vec3> v = Player.findRespawnPositionAndUseSpawnBlock(lvl, rp, sp.getRespawnAngle(), sp.isRespawnForced(), true);
        return v.map(p -> new TownSpawn.Arrival(lvl, p.x, p.y, p.z, sp.getRespawnAngle(), 0F, "bed")).orElse(null);
    }

    /** 내 토지 (처음 산 것부터): 그 첫 구역의 가운데, 땅 위. 상점 토지보다 살림 토지를 먼저. */
    @Nullable
    public static TownSpawn.Arrival land(ServerPlayer sp) {
        LandData.Land best = null;
        for (LandData.Land l : LandData.get(sp.server).lands()) {
            if (l.owner == null || !l.owner.equals(sp.getUUID()) || l.claims.isEmpty()) continue;
            if (best == null || best.shopLot && !l.shopLot || best.shopLot == l.shopLot && l.id < best.id) best = l;
        }
        if (best == null) return null;
        LandData.Claim c = best.claims.get(0);
        ResourceLocation rl = ResourceLocation.tryParse(c.dim);
        ServerLevel lvl = rl == null ? null : sp.server.getLevel(ResourceKey.create(Registries.DIMENSION, rl));
        if (lvl == null) return null;
        int x = (c.x0 + c.x1) / 2, z = (c.z0 + c.z1) / 2;
        if (c.mask != null && !c.hasColumn(x, z)) {
            // 모양이 있는 구획: 가운데가 구획 밖이면 구획 안의 첫 칸
            outer:
            for (int zz = c.z0; zz <= c.z1; zz++) {
                for (int xx = c.x0; xx <= c.x1; xx++) {
                    if (c.hasColumn(xx, zz)) {
                        x = xx;
                        z = zz;
                        break outer;
                    }
                }
            }
        }
        lvl.getChunk(x >> 4, z >> 4);
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new TownSpawn.Arrival(lvl, x + 0.5, y, z + 0.5, sp.getYRot(), 0F, "land");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            Component no = refusal(sp, kind);
            if (no != null) {
                com.rtshop.notice.Notice.send(sp, no.copy().withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return TownSpawn.get().channelTicks;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer sp) {
            Component no = refusal(sp, kind);
            TownSpawn.Arrival to = no == null ? destination(sp, kind) : null;
            if (to == null) {
                com.rtshop.notice.Notice.send(sp, (no == null ? Component.translatable("rtshop.return.no_spot") : no).copy().withStyle(ChatFormatting.RED));
                return stack;
            }
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
            go(sp, to);
            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
                TownSpawn.travelUsed(sp, key(kind));
            }
            sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
            com.rtshop.notice.Notice.send(sp, Component.translatable("rtshop.travel.done." + to.kind()).withStyle(ChatFormatting.GREEN));
        }
        return stack;
    }

    /** 그 자리로 옮긴다. */
    public static void go(ServerPlayer sp, TownSpawn.Arrival to) {
        sp.stopRiding();
        sp.teleportTo(to.level(), to.x(), to.y(), to.z(), to.yaw(), to.pitch());
        sp.fallDistance = 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        String k = kind == Kind.CASTLE ? "castle_pass" : "home_ticket";
        tip.add(Component.translatable("item.rtshop." + k + ".tip1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop." + k + ".tip2").withStyle(ChatFormatting.DARK_GRAY));
        TownSpawn.Config c = TownSpawn.get();
        if (kind == Kind.HOME && c.homeCooldownMinutes > 0) {
            tip.add(Component.translatable("item.rtshop.home_ticket.tip_cd", c.homeCooldownMinutes).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
