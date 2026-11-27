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

    /** WEAPON 근접(공격력 속성) · ARMOR 갑옷 · RANGED 활 · 쇠뇌 · 총(쏜 것의 피해) · MAGIC 지팡이 · 완드(주문 피해). RANGED · MAGIC 은 3.65.0 부터. */
    public enum Kind { NONE, WEAPON, ARMOR, RANGED, MAGIC }

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

    /** 미리 사용한 보호권 (플레이어 저장 데이터): 1 = 손상 방지, 2 = 파괴 방지. 다음 강화 1번에 쓰이고 사라진다. */
    public static final String GUARD_KEY = "rtshop_enh_guard";
    public static final int G_DAMAGE = 1;
    public static final int G_DESTROY = 2;
    /** 클라이언트: 지금 걸려 있는 보호. */
    public static volatile int clientGuard;

    public static Guard fromMask(int mask) {
        return switch (mask & 3) {
            case 1 -> Guard.DAMAGE;
            case 2 -> Guard.DESTROY;
            case 3 -> Guard.FULL;
            default -> Guard.NONE;
        };
    }

    public static int maskOf(Guard g) {
        return switch (g) {
            case DAMAGE -> G_DAMAGE;
            case DESTROY -> G_DESTROY;
            case FULL -> G_DAMAGE | G_DESTROY;
            default -> 0;
        };
    }

    public static int activeGuard(Player p) {
        if (p.level().isClientSide) return clientGuard;
        return p.getPersistentData().getInt(GUARD_KEY) & 3;
    }

    public static void setActiveGuard(ServerPlayer p, int mask) {
        if (mask == 0) p.getPersistentData().remove(GUARD_KEY);
        else p.getPersistentData().putInt(GUARD_KEY, mask & 3);
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EnhancePacket.guard(mask & 3));
        } catch (Exception ignored) {
        }
    }

    /**
     * 이번 강화에 적용되는 보호: 모루에 올린 보호권이 먼저다. 올린 것이 없으면, 옛 방식(우클릭)으로 미리 써 둔 보호가 남아 있을 때 그것을 쓴다
     * (3.49 전에 써 둔 보호권이 사라지지 않게).
     */
    public static Guard guardFor(Player p, ItemStack ticket) {
        Guard g = guardOf(ticket);
        return g != Guard.NONE ? g : fromMask(activeGuard(p));
    }

    /** 파괴되지 않는 장비인가: 뽑기 등급이 safeFromGrade(기본 전설) 이상이거나, safeItems 에 적힌 히든 장비. */
    public static boolean unbreakable(EnhanceConfig cfg, ItemStack s) {
        if (s.isEmpty()) return false;
        int from = com.rtshop.gacha.GachaConfig.gradeIndex(cfg.safeFromGrade);
        if (from >= 0 && com.rtshop.gacha.GachaConfig.gradeIndex(com.rtshop.gacha.Gacha.gradeOf(s)) >= from) return true;
        net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
        return id != null && cfg.safeItems.contains(id.toString());
    }

    /**
     * 이 장비에 맞춘 단계 확률: 파괴되지 않는 장비는 파괴 확률을 유지와 손상에 반씩 나눠 준다.
     * 3.66.0: 실패 누적이 있으면 성공률이 (그 단계 성공률 × failBonusPct% × 누적 수)만큼 오르고, 나머지 셋은 그 비율대로 줄어든다.
     */
    public static EnhanceConfig.Level levelFor(EnhanceConfig cfg, EnhanceConfig.Level l, ItemStack s) {
        if (l == null) return null;
        boolean safe = l.destroy > 0 && unbreakable(cfg, s);
        double add = l.success * failBonus(cfg, s);
        if (!safe && add <= 0) return l;
        EnhanceConfig.Level c = new EnhanceConfig.Level();
        c.level = l.level;
        c.success = l.success;
        c.maintain = l.maintain + (safe ? l.destroy / 2.0 : 0);
        c.damage = l.damage + (safe ? l.destroy / 2.0 : 0);
        c.destroy = safe ? 0 : l.destroy;
        c.gold = l.gold;
        c.materials.addAll(l.materials);
        double rest = c.maintain + c.damage + c.destroy;
        if (add > 0 && rest > 0) {
            add = Math.min(add, rest);
            double f = (rest - add) / rest;
            c.maintain *= f;
            c.damage *= f;
            c.destroy *= f;
            c.success += add;
        }
        return c;
    }

    // ------------------------------------------------------------------ 실패 누적 (3.66.0)

    /** 장비에 쌓인 실패 수 (성공하면 0). */
    public static final String FAIL_TAG = "rtshop_enh_fail";

    public static int failStacks(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? 0 : Math.max(0, t.getInt(FAIL_TAG));
    }

    public static void setFailStacks(ItemStack s, int n) {
        if (n <= 0) {
            CompoundTag t = s.getTag();
            if (t != null) {
                t.remove(FAIL_TAG);
                if (t.isEmpty()) s.setTag(null);
            }
        } else {
            s.getOrCreateTag().putInt(FAIL_TAG, n);
        }
    }

    /** 실패 누적으로 오르는 성공률의 배수 (0.3 = 그 단계 성공률의 30% 만큼 더). */
    public static double failBonus(EnhanceConfig cfg, ItemStack s) {
        if (cfg.failBonusPct <= 0 || cfg.failStackMax <= 0) return 0;
        return Math.min(failStacks(s), cfg.failStackMax) * cfg.failBonusPct / 100.0;
    }

    /** 이번 결과 뒤의 실패 수: 성공이면 0, 아니면 하나 더 (최대 failStackMax). */
    public static int failStacksAfter(EnhanceConfig cfg, int before, Outcome o) {
        if (o == Outcome.SUCCESS || cfg.failStackMax <= 0) return 0;
        return Math.min(cfg.failStackMax, Math.min(before, cfg.failStackMax) + 1);
    }

    /** 보호권을 손에 들고 우클릭: 미리 사용한다 (이미 걸린 보호면 쓰지 않음). 3.49 부터는 부르지 않는다 (점검용). 남아 있던 옛 보호는 3.64.2 부터 접속할 때 보호권으로 돌려준다(refundLegacyGuard). */
    public static boolean useGuardItem(ServerPlayer p, Guard g) {
        int cur = activeGuard(p);
        int add = maskOf(g);
        if ((cur & add) == add) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.enhance.guard_already").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        setActiveGuard(p, cur | add);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.enhance.guard_on", guardName(cur | add)).withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static Component guardName(int mask) {
        return switch (mask & 3) {
            case 1 -> Component.translatable("rtshop.enhance.guard_name_damage");
            case 2 -> Component.translatable("rtshop.enhance.guard_name_destroy");
            case 3 -> Component.translatable("rtshop.enhance.guard_name_full");
            default -> Component.translatable("rtshop.enhance.guard_name_none");
        };
    }

    /**
     * 3.64.2: 옛 방식(3.49 전, 보호권을 손에 들고 우클릭)으로 미리 걸어 둔 보호를 보호권 한 장으로 돌려준다 — 보호는 이제 모루 위 장비 옆 칸의 보호권으로만 건다.
     * 접속할 때 한 번 (가방이 꽉 차 있으면 발밑에 떨군다). 돌려준 장 수 (없으면 0).
     * guardFor 의 "미리 걸어 둔 보호" 갈래는 점검(가짜 플레이어)에서만 쓰이게 된다.
     */
    public static int refundLegacyGuard(ServerPlayer p) {
        int m = p.getPersistentData().getInt(GUARD_KEY) & 3;
        if (m == 0) return 0;
        Item it = m == 3 ? ModItems.GUARD_FULL.get() : m == 2 ? ModItems.GUARD_DESTROY.get() : ModItems.GUARD_DAMAGE.get();
        ItemStack back = new ItemStack(it);
        Component name = back.getHoverName();
        if (!p.getInventory().add(back)) p.drop(back, false);
        setActiveGuard(p, 0);
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.enhance.guard_refund", name).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    @SubscribeEvent
    public static void onLoginGuard(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && !(sp instanceof net.minecraftforge.common.util.FakePlayer)) refundLegacyGuard(sp);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        int m = e.getOriginal().getPersistentData().getInt(GUARD_KEY);
        if (m != 0) e.getEntity().getPersistentData().putInt(GUARD_KEY, m);
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

    private static final java.util.Map<Item, Boolean> RANGED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Item, Boolean> MAGIC = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.List<String> STAFF_WORDS = java.util.List.of("staff", "wand", "scepter", "sceptre");

    /** 활 · 쇠뇌(발사 무기), 그리고 총 (TaCZ 의 총, 갈래 이름이 …Gun / …GunItem 인 것). 서버 · 클라이언트가 설정 없이 같게 본다. */
    public static boolean rangedItem(Item it) {
        return RANGED.computeIfAbsent(it, item -> {
            if (item instanceof net.minecraft.world.item.ProjectileWeaponItem) return true;
            if (item instanceof net.minecraft.world.item.BlockItem || item.isEdible()) return false;
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id != null && id.getNamespace().equals("tacz")) return id.getPath().contains("gun") && !id.getPath().contains("smith");
            for (Class<?> k = item.getClass(); k != null && k != Item.class; k = k.getSuperclass()) {
                String n = k.getSimpleName().toLowerCase(java.util.Locale.ROOT);
                if (n.endsWith("gunitem") || n.endsWith("gun")) return true;
            }
            return false;
        });
    }

    /**
     * 마법 무기(지팡이 · 완드): 이름의 낱말(staff · wand · scepter)이나 갈래 이름(…Staff… · …Wand…)이 지팡이이고, **주문력 속성**이 붙은 것.
     * 주문력이 없는 지팡이(Weapons of Miracles 의 전투용 봉 같은 근접 무기, Create 의 대칭 지팡이 같은 도구)는 마법 무기가 아니다.
     * 갑옷 · 블록 · 음식은 아니다 (3.64.2: Wandering Magician 갑옷). 서버 · 클라이언트가 설정 없이 같게 본다.
     */
    public static boolean magicItem(Item it) {
        return MAGIC.computeIfAbsent(it, item -> {
            if (item instanceof ArmorItem || item instanceof net.minecraft.world.item.BlockItem || item.isEdible()) return false;
            boolean staffLike = false;
            net.minecraft.resources.ResourceLocation id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
            if (id != null) {
                for (String piece : id.getPath().toLowerCase(java.util.Locale.ROOT).split("[_/.\\-]")) {
                    for (String w : STAFF_WORDS) if (piece.equals(w) || piece.equals(w + "s") || piece.endsWith(w)) staffLike = true;
                }
            }
            for (Class<?> k = item.getClass(); !staffLike && k != null && k != Item.class; k = k.getSuperclass()) {
                String n = k.getSimpleName().toLowerCase(java.util.Locale.ROOT);
                if (n.contains("staff") || n.contains("wand") && !n.contains("wander")) staffLike = true;
            }
            return staffLike && spellAttribute(item);
        });
    }

    /** 손에 들면 주문력 · 마나 같은 마법 속성이 붙는가 (Iron's Spells · Spell Engine 계열 · Ars 등). */
    public static boolean spellAttribute(Item item) {
        try {
            ItemStack s = new ItemStack(item);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                for (net.minecraft.world.entity.ai.attributes.Attribute a : s.getAttributeModifiers(slot).keySet()) {
                    net.minecraft.resources.ResourceLocation k = net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getKey(a);
                    if (k == null) continue;
                    String ns = k.getNamespace();
                    String path = k.getPath();
                    if (ns.equals("irons_spellbooks") || ns.equals("spell_power") || ns.equals("spell_engine") || ns.startsWith("ars_")
                            || path.contains("spell") || path.contains("mana")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 이 단계의 보너스 % (갑옷은 방어력 표, 나머지는 무기 표). */
    public static double bonusPct(EnhanceConfig cfg, Kind k, int lv) {
        double[] arr = k == Kind.ARMOR ? cfg.armorBonus : cfg.weaponBonus;
        return arr.length == 0 ? 0 : arr[Math.max(0, Math.min(lv, arr.length - 1))];
    }

    private static boolean has(Collection<AttributeModifier> mods) {
        for (AttributeModifier m : mods) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION && m.getAmount() > 0) return true;
        }
        return false;
    }

    /**
     * 무기(공격력이 있는 한 손 장비) · 방어구(방어력이 있는 갑옷) · 원거리 무기(활 · 쇠뇌 · 총) · 마법 무기(지팡이 · 완드). 여러 개 쌓이는 아이템은 제외.
     * 원거리 · 마법 무기는 공격력 속성 대신 쏜 것 / 주문의 피해가 오른다 (EnhanceRanged). 지팡이는 근접 공격력이 조금 있어도 마법 무기로 본다.
     */
    public static Kind kind(ItemStack s) {
        if (s.isEmpty() || s.getMaxStackSize() > 1) return Kind.NONE;
        Item it = s.getItem();
        try {
            EquipmentSlot es = LivingEntity.getEquipmentSlotForItem(s);
            if (es.getType() == EquipmentSlot.Type.ARMOR
                    && (it instanceof ArmorItem || has(it.getAttributeModifiers(es, s).get(Attributes.ARMOR)))) {
                return Kind.ARMOR;
            }
            if (rangedItem(it)) return Kind.RANGED;
            if (magicItem(it)) return Kind.MAGIC;
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
        com.rtshop.rank.Ranks.spent(p, l.gold);
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

    // ---- 높은 단계의 연출 (화면은 client/EnhanceGrandFx): 단계 묶음과 긴장 구간의 길이. 서버 알림은 그만큼 늦춘다.

    /** 연출 묶음: 0 = 없음, 1 = +7~+8 (단조), 2 = +9~+10 (각인), 3 = +11 (초월). target = 도전하는 단계. */
    public static int fxTier(int target, int broadcastFrom) {
        if (target < broadcastFrom) return 0;
        return target >= 11 ? 3 : target >= 9 ? 2 : 1;
    }

    /** 결과를 공개하기 전 긴장 구간(ms). */
    public static long suspenseMs(int tier) {
        return switch (tier) {
            case 1 -> 3000;
            case 2 -> 4200;
            case 3 -> 8200;
            default -> 0;
        };
    }

    /** 시험용: 마지막 서버 알림. */
    public static volatile String lastBroadcast = "";

    static void announce(ServerPlayer p, ItemStack item, Outcome o, boolean guarded, int from, int to) {
        try {
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), EnhancePacket.fx(o, guarded, from, to, item));
        } catch (Exception ignored) {
        }
        MinecraftServer server = p.getServer();
        if (o == Outcome.SUCCESS && to >= EnhanceConfig.get().broadcastFrom && server != null) {
            // 강화한 사람의 화면에서 결과가 공개된 뒤에 알린다 (채팅이 먼저 결과를 말해 버리지 않게)
            int tier = fxTier(from + 1, EnhanceConfig.get().broadcastFrom);
            Component who = p.getDisplayName();
            String whoName = p.getGameProfile().getName();
            java.util.UUID self = p.getUUID();
            ItemStack shown = item.copy();
            com.rtshop.util.Later.run((int) (suspenseMs(tier) / 50) + 8, () -> {
                Component msg = Component.translatable(to >= 11 ? "rtshop.enhance.broadcast_max" : "rtshop.enhance.broadcast", who, shown.getHoverName(), to)
                        .withStyle(to >= 11 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD);
                com.rtshop.notice.Notice.broadcast(server, msg, false);
                lastBroadcast = to + ":" + whoName;
                if (to >= 11) {
                    // 최고 단계: 다른 모든 플레이어의 화면 위쪽에 알림 띠
                    for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                        if (other.getUUID().equals(self)) continue;
                        try {
                            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> other), EnhancePacket.legend(whoName, to, shown));
                        } catch (Exception ignored) {
                        }
                    }
                }
            });
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
            add(e, Attributes.ATTACK_DAMAGE, WEAPON_ID, weaponBonus(cfg, lv, sum(e.getModifiers().get(Attributes.ATTACK_DAMAGE))));
        } else if (k == Kind.ARMOR && slot == LivingEntity.getEquipmentSlotForItem(s) && slot.getType() == EquipmentSlot.Type.ARMOR) {
            int i = slot.getIndex();
            add(e, Attributes.ARMOR, ARMOR_ID[i], armorBonus(cfg, lv, sum(e.getModifiers().get(Attributes.ARMOR))));
            add(e, Attributes.ARMOR_TOUGHNESS, TOUGH_ID[i], toughnessBonus(cfg, lv));
        }
    }

    // ------------------------------------------------------------------ 확률표 · OP 편집기

    public static void action(ServerPlayer p, int action, String json) {
        switch (action) {
            case com.rtshop.net.EnhanceActionPacket.OPEN_ODDS -> ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    EnhancePacket.open(EnhancePacket.ODDS, EnhanceConfig.get().toJson().toString()));
            case com.rtshop.net.EnhanceActionPacket.OPEN_EDITOR -> {
                if (com.rtshop.util.Perm.op(p)) {
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                            EnhancePacket.open(EnhancePacket.EDITOR, EnhanceConfig.get().toJson().toString()));
                }
            }
            case com.rtshop.net.EnhanceActionPacket.SAVE -> {
                if (!com.rtshop.util.Perm.op(p)) return;
                try {
                    EnhanceConfig.saveFromEditor(json);
                    syncAll(p.server);
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.enhedit.saved").withStyle(ChatFormatting.GREEN));
                    ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                            EnhancePacket.open(EnhancePacket.EDITOR, EnhanceConfig.get().toJson().toString()));
                } catch (Exception e) {
                    com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.enhedit.failed", String.valueOf(e.getMessage())).withStyle(ChatFormatting.RED));
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
        if (event.getEntity() instanceof ServerPlayer sp) {
            sync(sp);
            setActiveGuard(sp, activeGuard(sp));
        }
    }
}
