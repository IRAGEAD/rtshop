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

    /** 주간 레이드: 참가 티켓(레이드 상인에게서 구매), 소환의 눈(입장하면 지급, 제단에 사용), 탈출권(입장하면 지급). */
    public static final RegistryObject<Item> RAID_TICKET = ITEMS.register("raid_ticket",
            () -> new TooltipItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE), false,
                    "item.rtshop.raid_ticket.tip1", "item.rtshop.raid_ticket.tip2"));
    public static final RegistryObject<Item> RAID_EYE = ITEMS.register("raid_eye",
            () -> new TooltipItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant(), true,
                    "item.rtshop.raid_eye.tip1", "item.rtshop.raid_eye.tip2"));
    public static final RegistryObject<Item> RAID_ESCAPE = ITEMS.register("raid_escape",
            () -> new RaidEscapeItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    /** 이야기 물건: 서장에서 꿈속의 기사가 건네는 불씨 (버릴 수 있지만 끝까지 필요하다). */
    public static final RegistryObject<Item> UNDYING_EMBER = ITEMS.register("undying_ember",
            () -> new StoryItem(new Item.Properties().stacksTo(1).fireResistant().rarity(net.minecraft.world.item.Rarity.EPIC), true, 3));
    /** 잿빛 연대기의 찢긴 페이지. */
    public static final RegistryObject<Item> CHRONICLE_PAGE = ITEMS.register("chronicle_page",
            () -> new StoryItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON), false, 2));
    /** 프로스트모가 품고 있던 얼음 결정 (불을 잠재우는 얼음). */
    public static final RegistryObject<Item> FROST_HEART = ITEMS.register("frost_heart",
            () -> new StoryItem(new Item.Properties().stacksTo(4).rarity(net.minecraft.world.item.Rarity.RARE), true, 2));
    /** 스킬라의 단서 (단서 ID 는 NBT, 이름·설명은 config/rtshop/scylla.json). */
    public static final RegistryObject<Item> SCYLLA_CLUE = ITEMS.register("scylla_clue",
            () -> new ClueItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    /** 수도회 견습 서약서 (1장 증표). */
    public static final RegistryObject<Item> OATH_SCROLL = ITEMS.register("oath_scroll",
            () -> new StoryItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON), false, 2));

    /** 해독제: 독·시듦·허기를 없앤다. */
    public static final RegistryObject<Item> ANTIDOTE = ITEMS.register("antidote", () -> new AntidoteItem(new Item.Properties().stacksTo(16)));

    // ---- 이야기 물건 (2판 스토리) ----
    public static final RegistryObject<Item> MOTHER_JOURNAL = story("mother_journal", 1, net.minecraft.world.item.Rarity.RARE, true, 3);
    public static final RegistryObject<Item> GREY_COIN = story("grey_coin", 16, net.minecraft.world.item.Rarity.UNCOMMON, false, 2);
    public static final RegistryObject<Item> OLD_EXPEDITION_MAP = story("old_expedition_map", 1, net.minecraft.world.item.Rarity.RARE, false, 3);
    public static final RegistryObject<Item> EXPEDITION_TAG = story("expedition_tag", 12, net.minecraft.world.item.Rarity.UNCOMMON, false, 2);
    public static final RegistryObject<Item> BROKEN_SUN_MASK = story("broken_sun_mask", 1, net.minecraft.world.item.Rarity.RARE, true, 3);
    public static final RegistryObject<Item> CREW_NAME_TAG = story("crew_name_tag", 16, net.minecraft.world.item.Rarity.UNCOMMON, false, 2);
    public static final RegistryObject<Item> FROZEN_LOGBOOK = story("frozen_logbook", 1, net.minecraft.world.item.Rarity.RARE, true, 3);
    public static final RegistryObject<Item> GREY_ASH = story("grey_ash", 1, net.minecraft.world.item.Rarity.EPIC, true, 3);
    public static final RegistryObject<Item> MEMORY_SHARD = story("memory_shard", 8, net.minecraft.world.item.Rarity.RARE, true, 2);
    public static final RegistryObject<Item> CHRONICLE_LAST_PAGE = story("chronicle_last_page", 1, net.minecraft.world.item.Rarity.EPIC, true, 3);
    /** 조사대 나침반: 진행 중인 퀘스트가 찾는 구조물 쪽을 가리킨다 (제작하거나 비싸게 산다). */
    public static final RegistryObject<Item> SURVEY_COMPASS = ITEMS.register("survey_compass",
            () -> new SurveyCompassItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));

    private static RegistryObject<Item> story(String id, int stack, net.minecraft.world.item.Rarity r, boolean foil, int lines) {
        return ITEMS.register(id, () -> new StoryItem(new Item.Properties().stacksTo(stack).rarity(r), foil, lines));
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
    /** 강화 보호권: 손에 들고 우클릭해 미리 사용 → 다음 강화 1번에 적용 (결과와 상관없이 소모). */
    public static final RegistryObject<Item> GUARD_DAMAGE = ITEMS.register("enhance_guard_damage",
            () -> new com.rtshop.enhance.GuardItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.UNCOMMON), false,
                    com.rtshop.enhance.Enhance.Guard.DAMAGE, "item.rtshop.enhance_guard_damage.tip", "item.rtshop.enhance_guard.tip"));
    public static final RegistryObject<Item> GUARD_DESTROY = ITEMS.register("enhance_guard_destroy",
            () -> new com.rtshop.enhance.GuardItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.RARE), false,
                    com.rtshop.enhance.Enhance.Guard.DESTROY, "item.rtshop.enhance_guard_destroy.tip", "item.rtshop.enhance_guard.tip"));
    public static final RegistryObject<Item> GUARD_FULL = ITEMS.register("enhance_guard_full",
            () -> new com.rtshop.enhance.GuardItem(new Item.Properties().stacksTo(64).rarity(net.minecraft.world.item.Rarity.EPIC), true,
                    com.rtshop.enhance.Enhance.Guard.FULL, "item.rtshop.enhance_guard_full.tip", "item.rtshop.enhance_guard.tip"));

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
