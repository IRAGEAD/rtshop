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
 * 무기 설명서 지급: 특별한 조작(그 무기만의 특수기)이 있는 모드 무기를 처음 손에 넣었을 때만 그 무기의 설명서를 한 권 준다.
 * <ul>
 * <li>바닐라 무기(minecraft:)와, 검·도끼·창 같은 흔한 무기 종류의 기본 특수기만 가진 모드 무기는 받지 않는다.</li>
 * <li>같은 종류·같은 특수기의 무기는 한 권으로 친다. 받은 기록은 플레이어에게 남아 죽어도, 다시 얻어도 또 주지 않는다.</li>
 * <li>어떤 무기든 손에 들고 /rtshop manual 을 치면 그 무기의 설명서를 받을 수 있다 (특수기가 있는 무기라면).</li>
 * </ul>
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

    /** Epic Fight 가 흔한 무기 종류(검·도끼·창·대검·태도…)에 기본으로 붙이는 특수기들: 이것만 가진 무기는 "특별한 조작"으로 치지 않는다. */
    private static final java.util.Set<String> COMMON_SKILLS = java.util.Set.of(
            "sweeping_edge", "dancing_edge", "the_guillotine", "heartpiercer", "grasping_spire", "steel_whirlwind", "battojutsu",
            "rushing_tempo", "relentless_combo", "sharp_stab", "liechtenauer", "eviscerate", "blade_rush", "tsunami",
            "wrathful_lighting", "everlasting_allegiance");

    private static boolean commonSkill(String id) {
        int c = id.indexOf(':');
        String ns = c < 0 ? "epicfight" : id.substring(0, c), path = c < 0 ? id : id.substring(c + 1);
        return (ns.equals("epicfight") || ns.equals("epicfightx")) && COMMON_SKILLS.contains(path);
    }

    /** 그 무기만의 특수기가 하나라도 있는가. */
    public static boolean special(EfWeapons.Info info) {
        if (info == null) return false;
        if (!info.skill().isEmpty() && !commonSkill(info.skill())) return true;
        for (EfWeapons.Form f : info.forms()) if (!f.skill().isEmpty() && !commonSkill(f.skill())) return true;
        return false;
    }

    /** 스스로 설명서를 주는 무기인가: 모드가 더한 무기이고, 그 무기만의 특수기가 있다. */
    public static boolean handedOut(ItemStack weapon, EfWeapons.Info info) {
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(weapon.getItem());
        return id != null && !"minecraft".equals(id.getNamespace()) && special(info);
    }

    /** 이 무기의 설명서를 아직 받지 않았으면 준다. 준 설명서(없으면 빈 것). */
    public static ItemStack offer(ServerPlayer p, ItemStack weapon) {
        EfWeapons.Info info = EfWeapons.info(p, weapon);
        if (!handedOut(weapon, info)) return ItemStack.EMPTY; // 바닐라 무기, 특별한 조작이 없는 무기는 설명서를 주지 않는다
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
