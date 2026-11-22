package com.rtshop.item;

import com.rtshop.RtShop;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RtShop.MODID);

    /** Gold 수표: 우클릭하면 적힌 금액이 소지금에 들어온다. */
    public static final RegistryObject<Item> GOLD_CHECK = ITEMS.register("gold_check",
            () -> new CheckItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> FIRST_LAND_DEED = ITEMS.register("first_land_deed",
            () -> new DeedItem(DeedItem.Type.FIRST, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LAND_DEED = ITEMS.register("land_deed",
            () -> new DeedItem(DeedItem.Type.NORMAL, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LARGE_LAND_DEED = ITEMS.register("large_land_deed",
            () -> new DeedItem(DeedItem.Type.LARGE, new Item.Properties().stacksTo(16)));

    /** 구획 지팡이 (OP 전용): 칸 단위 모양의 구획 토지를 고른다. */
    public static final RegistryObject<Item> PLOT_WAND = ITEMS.register("plot_wand",
            () -> new PlotWandItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** 일일 출석 진행도 복구권. */
    public static final RegistryObject<Item> RESTORE_TICKET = ITEMS.register("attendance_restore_ticket",
            () -> new RestoreTicketItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC)));

    /** 뽑기 티켓: 종류(무기·펫·장신구) × 단계(일반·중급·고급). 주고받을 수 있다. 무기 티켓은 예전 이름 그대로. */
    public static final RegistryObject<Item> TICKET_NORMAL = ticket("gacha_ticket_normal", "weapon", 0);
    public static final RegistryObject<Item> TICKET_MID = ticket("gacha_ticket_mid", "weapon", 1);
    public static final RegistryObject<Item> TICKET_HIGH = ticket("gacha_ticket_high", "weapon", 2);
    public static final RegistryObject<Item> PET_TICKET_NORMAL = ticket("gacha_ticket_pet_normal", "pet", 0);
    public static final RegistryObject<Item> PET_TICKET_MID = ticket("gacha_ticket_pet_mid", "pet", 1);
    public static final RegistryObject<Item> PET_TICKET_HIGH = ticket("gacha_ticket_pet_high", "pet", 2);
    public static final RegistryObject<Item> ACC_TICKET_NORMAL = ticket("gacha_ticket_accessory_normal", "accessory", 0);
    public static final RegistryObject<Item> ACC_TICKET_MID = ticket("gacha_ticket_accessory_mid", "accessory", 1);
    public static final RegistryObject<Item> ACC_TICKET_HIGH = ticket("gacha_ticket_accessory_high", "accessory", 2);

    private static RegistryObject<Item> ticket(String id, String cat, int tier) {
        net.minecraft.world.item.Rarity r = tier == 2 ? net.minecraft.world.item.Rarity.RARE
                : tier == 1 ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.COMMON;
        return ITEMS.register(id, () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(r), tier == 2,
                "item.rtshop.gacha_ticket.tip_" + cat, "item.rtshop.gacha_ticket.tip2"));
    }

    /** 뽑기 조각: 뽑은 아이템을 분해하면 나오고, 모아서 원하는 아이템과 바꾼다. */
    public static final RegistryObject<Item> SHARD = ITEMS.register("gacha_shard",
            () -> new TooltipItem(new Item.Properties().stacksTo(64), false, "item.rtshop.gacha_shard.tip"));
    /** 펫 소환권 (뽑기에서 나오는 펫). */
    public static final RegistryObject<Item> PET_SUMMON = ITEMS.register("pet_summon",
            () -> new PetSummonItem(new Item.Properties().stacksTo(1)));

    /** 강화 모루 (블록). */
    public static final RegistryObject<Item> ENHANCE_ANVIL = ITEMS.register("enhance_anvil",
            () -> new net.minecraft.world.item.BlockItem(com.rtshop.enhance.ModEnhance.ANVIL.get(),
                    new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                            java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.enhance_anvil.tip1").withStyle(net.minecraft.ChatFormatting.GRAY));
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.enhance_anvil.tip2").withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
    /** 강화 방지권: 강화 모루의 둘째 칸에 넣고 강화하면 한 장씩 쓴다. */
    public static final RegistryObject<Item> GUARD_DAMAGE = ITEMS.register("enhance_guard_damage",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON), false,
                    "item.rtshop.enhance_guard_damage.tip", "item.rtshop.enhance_guard.tip"));
    public static final RegistryObject<Item> GUARD_DESTROY = ITEMS.register("enhance_guard_destroy",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.RARE), false,
                    "item.rtshop.enhance_guard_destroy.tip", "item.rtshop.enhance_guard.tip"));
    public static final RegistryObject<Item> GUARD_FULL = ITEMS.register("enhance_guard_full",
            () -> new TooltipItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.EPIC), true,
                    "item.rtshop.enhance_guard_full.tip", "item.rtshop.enhance_guard.tip"));

    /** 플레이어 상점 진열대 (블록). */
    public static final RegistryObject<Item> PLAYER_SHOP = ITEMS.register("player_shop",
            () -> new net.minecraft.world.item.BlockItem(com.rtshop.enhance.ModEnhance.PSHOP.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                            java.util.List<net.minecraft.network.chat.Component> tip, net.minecraft.world.item.TooltipFlag flag) {
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.player_shop.tip1").withStyle(net.minecraft.ChatFormatting.GRAY));
                    tip.add(net.minecraft.network.chat.Component.translatable("block.rtshop.player_shop.tip2").withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            });
}
