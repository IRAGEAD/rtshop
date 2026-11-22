package com.rtshop.enhance;

import com.rtshop.RtShop;
import com.rtshop.client.ClientEconomy;
import com.rtshop.economy.Economy;
import com.rtshop.item.ModItems;
import com.rtshop.net.EnhancePacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** 장비 강화: 강화 단계(NBT), 강화 가능 여부, 재료 확인·소모, 결과 굴림, 능력치 보너스. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Enhance {
    private Enhance() {
    }

    /** 아이템 NBT: 강화 단계 (0 이면 지움). */
    public static final String TAG = "rtshop_enh";
    /** 강화 모루 결과 칸의 미리보기 표시 (꺼내면 바로 결과로 바뀐다). */
    public static final String TRY_TAG = "rtshop_enh_try";

    public enum Kind { NONE, WEAPON, ARMOR }

    /** 시험용: 정해 두면 다음 강화의 굴림값(0~1)으로 쓰인다. */
    public static volatile Double testRoll = null;

    public enum Outcome { SUCCESS, MAINTAIN, DAMAGE, DESTROY }

    public enum Guard {
        NONE, DAMAGE, DESTROY, FULL;

        /** 이 단계에서 방지권이 쓸모가 있는가 (쓸모없으면 소모하지 않음). */
        public boolean relevant(EnhanceConfig.Level l) {
            return switch (this) {
                case NONE -> false;
                case DAMAGE -> l.damage > 0;
                case DESTROY -> l.destroy > 0;
                case FULL -> l.damage > 0 || l.destroy > 0;
            };
        }
    }

    public static Guard guardOf(ItemStack s) {
        if (s.isEmpty()) return Guard.NONE;
        Item it = s.getItem();
        if (it == ModItems.GUARD_DAMAGE.get()) return Guard.DAMAGE;
        if (it == ModItems.GUARD_DESTROY.get()) return Guard.DESTROY;
        if (it == ModItems.GUARD_FULL.get()) return Guard.FULL;
        return Guard.NONE;
    }

    // ------------------------------------------------------------------ 단계

    public static int level(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? 0 : Math.max(0, Math.min(EnhanceConfig.MAX_LEVEL, t.getInt(TAG)));
    }

    public static void setLevel(ItemStack s, int lv) {
        lv = Math.max(0, Math.min(EnhanceConfig.MAX_LEVEL, lv));
        if (lv == 0) {
            CompoundTag t = s.getTag();
            if (t != null) {
                t.remove(TAG);
                if (t.isEmpty()) s.setTag(null);
            }
        } else {
            s.getOrCreateTag().putInt(TAG, lv);
        }
    }

    public static boolean isPreview(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.contains(TRY_TAG);
    }

    public static void clearPreview(ItemStack s) {
        CompoundTag t = s.getTag();
        if (t != null) {
            t.remove(TRY_TAG);
            if (t.isEmpty()) s.setTag(null);
        }
    }

    // ------------------------------------------------------------------ 종류

    private static boolean has(Collection<AttributeModifier> mods) {
        for (AttributeModifier m : mods) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION && m.getAmount() > 0) return true;
        }
        return false;
    }

    /** 무기(공격력이 있는 한 손 장비) 또는 방어구(방어력이 있는 갑옷). 여러 개 쌓이는 아이템은 제외. */
    public static Kind kind(ItemStack s) {
        if (s.isEmpty() || s.getMaxStackSize() > 1) return Kind.NONE;
        Item it = s.getItem();
        try {
            EquipmentSlot es = LivingEntity.getEquipmentSlotForItem(s);
            if (es.getType() == EquipmentSlot.Type.ARMOR
                    && (it instanceof ArmorItem || has(it.getAttributeModifiers(es, s).get(Attributes.ARMOR)))) {
                return Kind.ARMOR;
            }
            if (it instanceof SwordItem || it instanceof AxeItem || it instanceof TridentItem
                    || has(it.getAttributeModifiers(EquipmentSlot.MAINHAND, s).get(Attributes.ATTACK_DAMAGE))) {
                return Kind.WEAPON;
            }
        } catch (Throwable t) {
            return Kind.NONE;
        }
        return Kind.NONE;
    }

    public static boolean enhanceable(ItemStack s) {
        return kind(s) != Kind.NONE;
    }

    // ------------------------------------------------------------------ 재료

    public static final class Need {
        public final EnhanceConfig.Material material;
        public final List<Item> items;
        public final int need;
        public final int have;

        Need(EnhanceConfig.Material m, List<Item> items, int have) {
            this.material = m;
            this.items = items;
            this.need = m.count;
            this.have = have;
        }

        public boolean ok() {
            return have >= need;
        }
    }

    private static int count(Inventory inv, List<Item> items) {
        int n = 0;
        for (ItemStack s : inv.items) {
            if (!s.isEmpty() && items.contains(s.getItem()) && !isPreview(s) && level(s) == 0) n += s.getCount();
        }
        return n;
    }

    /** 이 단계에 필요한 재료와 가진 개수 (설치되지 않은 재료는 빠짐). */
    public static List<Need> needs(EnhanceConfig cfg, EnhanceConfig.Level l, Player p) {
        List<Need> out = new ArrayList<>();
        for (EnhanceConfig.Material m : l.materials) {
            List<Item> items = cfg.itemsOf(m);
            if (items.isEmpty()) continue;
            out.add(new Need(m, items, p == null ? 0 : count(p.getInventory(), items)));
        }
        return out;
    }

    public static long wallet(Player p) {
        if (p instanceof ServerPlayer sp) return Economy.wallet(sp);
        return ClientEconomy.wallet;
    }

    public static EnhanceConfig config(Player p) {
        return p != null && p.level().isClientSide ? EnhanceConfig.client() : EnhanceConfig.get();
    }

    /** 강화 시도에 필요한 것을 모두 갖췄는가 (창작 모드는 무료). */
    public static boolean affordable(Player p, EnhanceConfig cfg, EnhanceConfig.Level l) {
        if (p.getAbilities().instabuild) return true;
        if (wallet(p) < l.gold) return false;
        for (Need n : needs(cfg, l, p)) {
            if (!n.ok()) return false;
        }
        return true;
    }

    static void consume(ServerPlayer p, EnhanceConfig cfg, EnhanceConfig.Level l) {
        if (p.getAbilities().instabuild) return;
        Economy.addWallet(p, -l.gold);
        Inventory inv = p.getInventory();
        for (Need n : needs(cfg, l, p)) {
            int left = n.need;
            for (Item want : n.items) {
                for (int i = 0; i < inv.items.size() && left > 0; i++) {
                    ItemStack s = inv.items.get(i);
                    if (!s.isEmpty() && s.getItem() == want && !isPreview(s) && level(s) == 0) {
                        int take = Math.min(left, s.getCount());
                        s.shrink(take);
                        left -= take;
                    }
                }
            }
        }
        inv.setChanged();
    }

    // ------------------------------------------------------------------ 결과

    public static Outcome roll(EnhanceConfig.Level l, double r01) {
        double total = l.success + l.maintain + l.damage + l.destroy;
        double r = r01 * total;
        if (r < l.success) return Outcome.SUCCESS;
        if (r < l.success + l.maintain) return Outcome.MAINTAIN;
        if (r < l.success + l.maintain + l.damage) return Outcome.DAMAGE;
        return Outcome.DESTROY;
    }

    /** 방지권 적용: 손상 방지권 = 손상→유지, 파괴 방지권 = 파괴→손상, 강화 보호권 = 손상·파괴→유지. */
    public static Outcome protect(Outcome o, Guard g) {
        if (o == Outcome.DAMAGE && (g == Guard.DAMAGE || g == Guard.FULL)) return Outcome.MAINTAIN;
        if (o == Outcome.DESTROY && g == Guard.FULL) return Outcome.MAINTAIN;
        if (o == Outcome.DESTROY && g == Guard.DESTROY) return Outcome.DAMAGE;
        return o;
    }

    public static int levelAfter(Outcome o, int cur, EnhanceConfig cfg) {
        return switch (o) {
            case SUCCESS -> Math.min(EnhanceConfig.MAX_LEVEL, cur + 1);
            case MAINTAIN -> cur;
            case DAMAGE -> Math.max(0, cur - cfg.damageDrop);
            case DESTROY -> -1;
        };
    }

    static void announce(ServerPlayer p, ItemStack item, Outcome o, boolean guarded, int from, int to) {
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EnhancePacket.fx(o, guarded, from, to, item));
        } catch (Exception ignored) {
        }
        MinecraftServer server = p.getServer();
        if (o == Outcome.SUCCESS && to >= 10 && server != null) {
            Component msg = Component.translatable("rtshop.enhance.broadcast", p.getDisplayName(), item.getHoverName(), to)
                    .withStyle(ChatFormatting.GOLD);
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
        RtShop.LOGGER.info("[rtshop] 강화 {} +{} -> {} ({}{}) by {}", item.getItem(), from, o, to, guarded ? ", 방지권" : "",
                p.getGameProfile().getName());
    }

    // ------------------------------------------------------------------ 능력치

    private static final UUID WEAPON_ID = UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a01");
    private static final UUID[] ARMOR_ID = {
            UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a10"), UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a11"),
            UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a12"), UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a13")};
    private static final UUID[] TOUGH_ID = {
            UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a20"), UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a21"),
            UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a22"), UUID.fromString("6a1f0c52-3b4e-4d8a-9c11-2e7f5b0d4a23")};

    private static double sum(Collection<AttributeModifier> mods) {
        double v = 0;
        for (AttributeModifier m : mods) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) v += m.getAmount();
        }
        return v;
    }

    /** 무기 공격력 보너스 (기본 공격력 = 아이템 + 맨손 1). */
    public static double weaponBonus(EnhanceConfig cfg, int lv, double baseDamage) {
        if (lv <= 0) return 0;
        double pct = cfg.weaponBonus[Math.min(lv, cfg.weaponBonus.length - 1)];
        return Math.max((baseDamage + 1.0) * pct / 100.0, 0.4 * lv);
    }

    public static double armorBonus(EnhanceConfig cfg, int lv, double baseArmor) {
        if (lv <= 0) return 0;
        double pct = cfg.armorBonus[Math.min(lv, cfg.armorBonus.length - 1)];
        return Math.max(baseArmor * pct / 100.0, 0.25 * lv);
    }

    public static double toughnessBonus(EnhanceConfig cfg, int lv) {
        return lv >= cfg.toughnessFrom ? (lv - cfg.toughnessFrom + 1) * cfg.toughnessPerLevel : 0;
    }

    private static void add(ItemAttributeModifierEvent e, Attribute a, UUID id, double v) {
        if (v > 0) e.addModifier(a, new AttributeModifier(id, "rtshop_enhance", v, AttributeModifier.Operation.ADDITION));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onAttributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        int lv = level(s);
        if (lv <= 0) return;
        Kind k = kind(s);
        boolean client = net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()
                && net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient();
        EnhanceConfig cfg = client ? EnhanceConfig.client() : EnhanceConfig.get();
        EquipmentSlot slot = e.getSlotType();
        if (k == Kind.WEAPON && slot == EquipmentSlot.MAINHAND) {
            add(e, Attributes.ATTACK_DAMAGE, WEAPON_ID, weaponBonus(cfg, lv, sum(e.getOriginalModifiers().get(Attributes.ATTACK_DAMAGE))));
        } else if (k == Kind.ARMOR && slot == LivingEntity.getEquipmentSlotForItem(s) && slot.getType() == EquipmentSlot.Type.ARMOR) {
            int i = slot.getIndex();
            add(e, Attributes.ARMOR, ARMOR_ID[i], armorBonus(cfg, lv, sum(e.getOriginalModifiers().get(Attributes.ARMOR))));
            add(e, Attributes.ARMOR_TOUGHNESS, TOUGH_ID[i], toughnessBonus(cfg, lv));
        }
    }

    // ------------------------------------------------------------------ 확률표 · OP 편집기

    public static void action(ServerPlayer p, int action, String json) {
        switch (action) {
            case com.rtshop.net.EnhanceActionPacket.OPEN_ODDS -> ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    EnhancePacket.open(EnhancePacket.ODDS, EnhanceConfig.get().toJson().toString()));
            case com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR -> {
                if (p.hasPermissions(2)) {
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                            EnhancePacket.open(EnhancePacket.EDITOR, EnhanceConfig.get().toJson().toString()));
                }
            }
            case com.rtshop.net.EnhanceActionPacket.SAVE -> {
                if (!p.hasPermissions(2)) return;
                try {
                    EnhanceConfig.saveFromEditor(json);
                    syncAll(p.server);
                    p.sendSystemMessage(Component.translatable("rtshop.enhedit.saved").withStyle(ChatFormatting.GREEN));
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                            EnhancePacket.open(EnhancePacket.EDITOR, EnhanceConfig.get().toJson().toString()));
                } catch (Exception e) {
                    p.sendSystemMessage(Component.translatable("rtshop.enhedit.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
                }
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 설정 동기화

    public static void sync(ServerPlayer p) {
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EnhancePacket.config(EnhanceConfig.get().toJson().toString()));
        } catch (Exception ignored) {
        }
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) sync(sp);
    }
}
