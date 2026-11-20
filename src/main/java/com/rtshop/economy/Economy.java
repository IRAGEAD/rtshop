package com.rtshop.economy;

import com.rtshop.RtShop;
import com.rtshop.net.BalancePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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
        long w = wallet(sp);
        if (w <= 0) {
            return;
        }
        EconomyConfig c = EconomyConfig.get();
        double pct = c.deathLossMinPercent + sp.getRandom().nextDouble() * (c.deathLossMaxPercent - c.deathLossMinPercent);
        long loss = Math.min(w, Math.round(w * pct / 100.0D));
        if (loss <= 0) {
            return;
        }
        setWallet(sp, w - loss);
        sp.sendSystemMessage(Component.translatable("rtshop.msg.death_loss",
                String.format("%,d", loss), String.format("%.1f", pct), String.format("%,d", w - loss))
                .withStyle(ChatFormatting.RED));
    }
}
