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
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_min", fmt(c.minCheckAmount)));
            return false;
        }
        if (wallet(p) < amount) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.no_money"));
            return false;
        }
        if (p.getInventory().getFreeSlot() < 0) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_no_space"));
            return false;
        }
        UUID id = UUID.randomUUID();
        ItemStack stack = CheckItem.create(id, amount, p.getGameProfile().getName());
        if (!p.getInventory().add(stack)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_no_space"));
            return false;
        }
        data(p).setCheck(id, amount);
        setWallet(p, wallet(p) - amount);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_issued", fmt(amount)).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** 수표를 사용해 소지금에 더한다. 금액은 아이템이 아니라 서버 장부의 값을 따른다. */
    public static boolean redeemCheck(ServerPlayer p, ItemStack stack) {
        UUID id = CheckItem.id(stack);
        long amount = id == null ? 0 : data(p).checkAmount(id);
        if (amount <= 0) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_invalid").withStyle(ChatFormatting.RED));
            return false;
        }
        data(p).setCheck(id, 0);
        stack.shrink(1);
        setWallet(p, wallet(p) + amount);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.msg.check_redeemed", fmt(amount), fmt(wallet(p))).withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    /**
     * 들고 있는 수표의 가치를 pct% 줄인다: 가방 칸 · 갑옷 칸 · 보조 손, 그리고 가방 안의 셜커 상자(BlockEntityTag.Items) · 배낭(아이템 칸을 가진 물건) 속 수표까지.
     * 같은 수표(일련번호)는 한 번만 센다. 줄어든 Gold 합계를 돌려준다.
     */
    public static long checkLoss(ServerPlayer sp, double pct) {
        EconomyData d = data(sp);
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        long[] total = {0};
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(ModItems.GOLD_CHECK.get())) {
                total[0] += lose(d, s, pct, seen);
                continue;
            }
            total[0] += insideBox(d, s, pct, seen, 0);
            total[0] += insideHandler(d, s, pct, seen, 0);
        }
        return total[0];
    }

    /** 수표 하나의 가치를 줄인다 (장부와 아이템 모두). 다 깎이면 수표가 사라진다. */
    private static long lose(EconomyData d, ItemStack s, double pct, java.util.Set<UUID> seen) {
        UUID id = CheckItem.id(s);
        if (id == null || !seen.add(id)) return 0;
        long a = d.checkAmount(id);
        if (a <= 0) return 0;
        long l = Math.min(a, Math.round(a * pct / 100.0D));
        if (l <= 0) return 0;
        d.setCheck(id, a - l);
        if (a - l <= 0) {
            s.shrink(1);
        } else {
            CheckItem.setAmount(s, a - l);
        }
        return l;
    }

    /** 셜커 상자처럼 내용물을 BlockEntityTag.Items 에 적어 두는 물건 속 수표. */
    private static long insideBox(EconomyData d, ItemStack box, double pct, java.util.Set<UUID> seen, int depth) {
        if (depth > 2) return 0;
        net.minecraft.nbt.CompoundTag be = box.getTagElement("BlockEntityTag");
        if (be == null || !be.contains("Items", net.minecraft.nbt.Tag.TAG_LIST)) return 0;
        net.minecraft.nbt.ListTag items = be.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
        long sum = 0;
        for (int i = 0; i < items.size(); i++) {
            net.minecraft.nbt.CompoundTag e = items.getCompound(i);
            ItemStack in = ItemStack.of(e);
            if (in.isEmpty()) continue;
            long l;
            if (in.is(ModItems.GOLD_CHECK.get())) {
                l = lose(d, in, pct, seen);
            } else {
                l = insideBox(d, in, pct, seen, depth + 1);
            }
            if (l <= 0) continue;
            sum += l;
            if (in.isEmpty()) {
                items.remove(i);
                i--;
            } else {
                byte slot = e.getByte("Slot");
                net.minecraft.nbt.CompoundTag ne = in.save(new net.minecraft.nbt.CompoundTag());
                ne.putByte("Slot", slot);
                items.set(i, ne);
            }
        }
        return sum;
    }

    /** 배낭처럼 아이템 칸(Forge 아이템 핸들러)을 가진 물건 속 수표. */
    private static long insideHandler(EconomyData d, ItemStack bag, double pct, java.util.Set<UUID> seen, int depth) {
        if (depth > 1) return 0;
        long[] sum = {0};
        try {
            bag.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).ifPresent(h -> {
                for (int i = 0; i < h.getSlots(); i++) {
                    ItemStack in = h.getStackInSlot(i);
                    if (in.isEmpty() || !in.is(ModItems.GOLD_CHECK.get())) continue;
                    ItemStack copy = in.copy();
                    long l = lose(d, copy, pct, seen);
                    if (l <= 0) continue;
                    sum[0] += l;
                    if (h instanceof net.minecraftforge.items.IItemHandlerModifiable m) {
                        m.setStackInSlot(i, copy);
                    } else {
                        h.extractItem(i, in.getCount(), false);
                        if (!copy.isEmpty()) h.insertItem(i, copy, false);
                    }
                }
            });
        } catch (Throwable ignored) {
        }
        return sum[0];
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
            com.rtshop.notice.Notice.send(sp, net.minecraft.network.chat.Component.translatable("rtshop.economy.death_story_boss"));
            return;
        }
        EconomyConfig c = EconomyConfig.get();
        double pct = c.deathLossMinPercent + sp.getRandom().nextDouble() * (c.deathLossMaxPercent - c.deathLossMinPercent);
        long w = wallet(sp);
        long loss = com.rtshop.rank.Ranks.minus(sp, "death_keep", Math.min(w, Math.round(w * pct / 100.0D)));   // 칭호 효과: 덜 잃는다
        if (loss > 0) {
            setWallet(sp, w - loss);
            com.rtshop.notice.Notice.send(sp, Component.translatable("rtshop.msg.death_loss",
                    fmt(loss), String.format("%.1f", pct), fmt(w - loss)).withStyle(ChatFormatting.RED));
        }
        // 들고 있던 수표도 같은 비율로 가치가 줄어든다 (보관함에 넣어 둔 수표는 영향 없음).
        // 3.84 (사용자 11-29 "수표도 사망 시에 골드처럼 일부를 잃어버리게"): 가방 안의 셜커 상자 · 배낭 속 수표도 들고 있는 것으로 본다
        // (모두에게 셜커 상자를 하나씩 주므로, 상자에 넣어 들고 다니면 잃지 않던 틈을 막는다)
        if (c.checkDeathLoss) {
            long checkLoss = checkLoss(sp, pct);
            if (checkLoss > 0) {
                com.rtshop.notice.Notice.send(sp, Component.translatable("rtshop.msg.death_loss_check", fmt(checkLoss)).withStyle(ChatFormatting.RED));
            }
        }
    }
}
