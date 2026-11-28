package com.rtshop.rules;

import com.rtshop.RtShop;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.84 (사용자 11-29 "서버에 처음 접속할 시 랜덤 색상의 셜커 상자를 모든 플레이어에게 하나씩 지급"): 처음 들어올 때(이미 놀던 사람은 이번 판에서 처음 들어올 때)
 * 16가지 색 가운데 하나로 물들인 셜커 상자를 한 사람에 한 번만 준다. 표시는 죽어도 남는 자리(PlayerPersisted)에 적는다.
 * world_rules.json 의 welcomeShulker=false 면 주지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class WelcomeGift {
    private WelcomeGift() {
    }

    public static final String FLAG = "rtshop_welcome_shulker";
    /** 점검용: 준 수 · 마지막으로 준 색. */
    public static volatile int givenForTest;
    public static volatile DyeColor lastColorForTest;

    /** 아직 받지 않았으면 준다. 주었으면 true. */
    public static boolean give(ServerPlayer p) {
        if (!WorldRules.get().welcomeShulker) return false;
        CompoundTag root = p.getPersistentData();
        CompoundTag keep = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (keep.getBoolean(FLAG)) return false;
        keep.putBoolean(FLAG, true);
        root.put(Player.PERSISTED_NBT_TAG, keep);
        DyeColor color = DyeColor.values()[p.getRandom().nextInt(DyeColor.values().length)];
        ItemStack box = new ItemStack(ShulkerBoxBlock.getBlockByColor(color).asItem());
        Component name = box.getHoverName();
        if (!p.getInventory().add(box)) p.drop(box, false);
        givenForTest++;
        lastColorForTest = color;
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.gift.shulker", name).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** 점검용: 받은 표시를 지운다. */
    public static void forgetForTest(ServerPlayer p) {
        CompoundTag keep = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        keep.remove(FLAG);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, keep);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && !(sp instanceof net.minecraftforge.common.util.FakePlayer)) {
            try {
                give(sp);
            } catch (Throwable t) {
                RtShop.LOGGER.warn("[rtshop] 셜커 상자 선물을 주지 못했습니다: {}", t.toString());
            }
        }
    }
}
