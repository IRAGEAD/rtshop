package com.rtshop.economy;

import com.rtshop.RtShop;
import com.rtshop.item.CheckItem;
import com.rtshop.item.ModItems;
import com.rtshop.net.BalancePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Gold 경제의 서버 쪽 API. 소지금(wallet)과 예금(bank)은 모두 서버에만 저장됩니다. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Economy {
    private Economy() {
    }

    private static final long MAX = 1_000_000_000_000L;

    private static EconomyData data(ServerPlayer p) {
        return EconomyData.get(p.server);
    }

    public static long wallet(ServerPlayer p) {
        return data(p).wallet(p.getUUID());
    }

    public static long bank(ServerPlayer p) {
        return data(p).bank(p.getUUID());
    }

    public static void setWallet(ServerPlayer p, long v) {
        data(p).setWallet(p.getUUID(), Math.min(MAX, Math.max(0, v)));
        sync(p);
    }

    public static void addWallet(ServerPlayer p, long delta) {
        setWallet(p, wallet(p) + delta);
    }

    /** 소지금 -> 예금. 실패 시 false. */
    public static boolean deposit(ServerPlayer p, long amount) {
        EconomyData d = data(p);
        long w = d.wallet(p.getUUID());
        if (amount <= 0 || amount > w) {
            return false;
        }
        d.setWallet(p.getUUID(), w - amount);
        d.setBank(p.getUUID(), Math.min(MAX, d.bank(p.getUUID()) + amount));
        sync(p);
        return true;
    }

    /** 예금 -> 소지금. 실패 시 false. */
    public static boolean withdraw(ServerPlayer p, long amount) {
        EconomyData d = data(p);
        long b = d.bank(p.getUUID());
        if (amount <= 0 || amount > b) {
            return false;
        }
        d.setBank(p.getUUID(), b - amount);
        d.setWallet(p.getUUID(), Math.min(MAX, d.wallet(p.getUUID()) + amount));
        sync(p);
        return true;
    }

    // ------------------------------------------------------------------ 수표

    /** 소지금에서 amount 를 빼고 그만큼의 수표를 발행해 인벤토리에 넣는다. 결과 메시지를 직접 보낸다. */
    public static boolean issueCheck(ServerPlayer p, long amount) {
        EconomyConfig c = EconomyConfig.get();
        if (amount < c.minCheckAmount) {
            p.sendSystemMessage(Component.translatable("rtshop.msg.check_min", fmt(c.minCheckAmount)));
            return false;
        }
        if (wallet(p) < amount) {
            p.sendSystemMessage(Component.translatable("rtshop.msg.no_money"));
            return false;
        }
        if (p.getInventory().getFreeSlot() < 0) {
            p.sendSystemMessage(Component.translatable("rtshop.msg.check_no_space"));
            return false;
        }
        UUID id = UUID.randomUUID();
        ItemStack stack = CheckItem.create(id, amount, p.getGameProfile().getName());
        if (!p.getInventory().add(stack)) {
            p.sendSystemMessage(Component.translatable("rtshop.msg.check_no_space"));
            return false;
        }
        data(p).setCheck(id, amount);
        setWallet(p, wallet(p) - amount);
        p.sendSystemMessage(Component.translatable("rtshop.msg.check_issued", fmt(amount)).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** 수표를 사용해 소지금에 더한다. 금액은 아이템이 아니라 서버 장부의 값을 따른다. */
    public static boolean redeemCheck(ServerPlayer p, ItemStack stack) {
        UUID id = CheckItem.id(stack);
        long amount = id == null ? 0 : data(p).checkAmount(id);
        if (amount <= 0) {
            p.sendSystemMessage(Component.translatable("rtshop.msg.check_invalid").withStyle(ChatFormatting.RED));
            return false;
        }
        data(p).setCheck(id, 0);
        stack.shrink(1);
        setWallet(p, wallet(p) + amount);
        p.sendSystemMessage(Component.translatable("rtshop.msg.check_redeemed", fmt(amount), fmt(wallet(p))).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    private static List<ItemStack> carriedChecks(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(ModItems.GOLD_CHECK.get())) {
                out.add(s);
            }
        }
        return out;
    }

    public static void sync(ServerPlayer p) {
        EconomyData d = data(p);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new BalancePacket(d.wallet(p.getUUID()), d.bank(p.getUUID())));
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            EconomyData d = data(sp);
            if (!d.has(sp.getUUID())) {
                d.setWallet(sp.getUUID(), EconomyConfig.get().startingGold);
            }
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
        }
    }

    /** 사망 시 소지금의 5%~50%(설정 가능)를 무작위로 잃습니다. 예금은 영향 없음. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (com.rtshop.story.StorySeals.nearStoryBoss(sp)) {
            // 이야기 보스와 싸우다 죽으면 Gold 를 잃지 않는다 (여러 번 도전이 기본인 보스들)
            sp.sendSystemMessage(net.minecraft.network.chat.Component.translatable("rtshop.economy.death_story_boss"));
            return;
        }
        EconomyConfig c = EconomyConfig.get();
        double pct = c.deathLossMinPercent + sp.getRandom().nextDouble() * (c.deathLossMaxPercent - c.deathLossMinPercent);
        long w = wallet(sp);
        long loss = Math.min(w, Math.round(w * pct / 100.0D));
        if (loss > 0) {
            setWallet(sp, w - loss);
            sp.sendSystemMessage(Component.translatable("rtshop.msg.death_loss",
                    fmt(loss), String.format("%.1f", pct), fmt(w - loss)).withStyle(ChatFormatting.RED));
        }
        // 들고 있던 수표도 같은 비율로 가치가 줄어든다 (보관함에 넣어 둔 수표는 영향 없음)
        if (c.checkDeathLoss) {
            long checkLoss = 0;
            EconomyData d = data(sp);
            for (ItemStack s : carriedChecks(sp)) {
                UUID id = CheckItem.id(s);
                long a = id == null ? 0 : d.checkAmount(id);
                if (a <= 0) {
                    continue;
                }
                long l = Math.min(a, Math.round(a * pct / 100.0D));
                if (l <= 0) {
                    continue;
                }
                checkLoss += l;
                d.setCheck(id, a - l);
                if (a - l <= 0) {
                    s.shrink(1);
                } else {
                    CheckItem.setAmount(s, a - l);
                }
            }
            if (checkLoss > 0) {
                sp.sendSystemMessage(Component.translatable("rtshop.msg.death_loss_check", fmt(checkLoss)).withStyle(ChatFormatting.RED));
            }
        }
    }
}
