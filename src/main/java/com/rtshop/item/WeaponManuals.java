package com.rtshop.item;

import com.rtshop.RtShop;
import com.rtshop.compat.EfWeapons;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 무기 설명서 지급: 특수기(기본 공격 말고 따로 쓰는 기술)가 있는 무기를 처음 손에 넣으면 그 무기의 설명서를 한 권 준다.
 * 같은 종류·같은 특수기의 무기(나무 검과 철 검 등)는 한 권으로 친다. 받은 기록은 플레이어에게 남아 죽어도 다시 주지 않는다.
 * 끄기: config/rtshop/world_rules.json 의 weaponManuals.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class WeaponManuals {
    private WeaponManuals() {
    }

    private static final String KEY = "rtshop_manuals";
    /** 화면 자체 점검 중에는 스스로 나눠 주지 않는다 (점검이 가방의 칸 수를 세는 곳이 많다; 점검은 scan/offer 를 직접 부른다). */
    private static final boolean QUIET = Boolean.getBoolean("rtshop.clienttest");

    private static CompoundTag store(ServerPlayer p, boolean create) {
        CompoundTag root = p.getPersistentData();
        CompoundTag pers = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (create && !root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, pers);
        if (create && !pers.contains(KEY)) pers.put(KEY, new CompoundTag());
        return pers.getCompound(KEY);
    }

    public static boolean has(ServerPlayer p, String key) {
        return store(p, false).getBoolean(key);
    }

    public static void forgetAll(ServerPlayer p) {
        p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).remove(KEY);
    }

    /** 이 무기의 설명서를 아직 받지 않았으면 준다. 준 설명서(없으면 빈 것). */
    public static ItemStack offer(ServerPlayer p, ItemStack weapon) {
        EfWeapons.Info info = EfWeapons.info(p, weapon);
        if (info == null || info.skill().isEmpty()) return ItemStack.EMPTY; // 특수기가 없는 물건은 설명서가 없다
        if (has(p, info.key())) return ItemStack.EMPTY;
        store(p, true).putBoolean(info.key(), true);
        ItemStack manual = WeaponManualItem.of(weapon);
        ItemStack given = manual.copy();
        if (!p.getInventory().add(manual)) p.drop(manual, false);
        if (!(p instanceof FakePlayer)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.manual.got", weapon.getHoverName()).withStyle(ChatFormatting.AQUA));
        }
        return given;
    }

    /** 가방을 살펴 새로 얻은 무기의 설명서를 준다. 준 권수. */
    public static int scan(ServerPlayer p) {
        if (!EfWeapons.present() || !com.rtshop.rules.WorldRules.get().weaponManuals) return 0;
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty() || s.getItem() instanceof WeaponManualItem) continue;
            if (!offer(p, s).isEmpty()) n++;
        }
        return n;
    }

    /** /rtshop manual : 손에 든 무기의 설명서를 다시 받는다 (잃어버렸거나 버렸을 때). */
    @SubscribeEvent
    public static void onCommands(net.minecraftforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("rtshop").then(net.minecraft.commands.Commands.literal("manual").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            ItemStack held = p.getMainHandItem();
            EfWeapons.Info info = EfWeapons.info(p, held);
            if (info == null || info.skill().isEmpty()) {
                ctx.getSource().sendFailure(Component.translatable("rtshop.manual.cmd_none"));
                return 0;
            }
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.getItem() instanceof WeaponManualItem) {
                    EfWeapons.Info other = EfWeapons.info(p, WeaponManualItem.weapon(s));
                    if (other != null && other.key().equals(info.key())) {
                        ctx.getSource().sendFailure(Component.translatable("rtshop.manual.cmd_have"));
                        return 0;
                    }
                }
            }
            store(p, true).putBoolean(info.key(), true);
            ItemStack manual = WeaponManualItem.of(held);
            if (!p.getInventory().add(manual)) p.drop(manual, false);
            ctx.getSource().sendSuccess(() -> Component.translatable("rtshop.manual.got", held.getHoverName()), false);
            return 1;
        })));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || QUIET) return;
        if (event.player instanceof ServerPlayer sp && !(sp instanceof FakePlayer) && sp.tickCount % 60 == 31 && !sp.isSpectator()) {
            try {
                scan(sp);
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] weapon manual scan failed: {}", t.toString());
            }
        }
    }
}
