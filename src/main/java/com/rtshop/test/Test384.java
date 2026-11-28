package com.rtshop.test;

import com.mojang.authlib.GameProfile;
import com.rtshop.item.ModItems;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import com.rtshop.rules.ItemPolicy;
import com.rtshop.shop.ShopData;
import com.rtshop.shop.ShopServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.rtshop.test.SelfTest.check;
import static com.rtshop.test.SelfTest.info;

/**
 * 3.84.0 자체 점검 (docs/requests_261129.txt · worklist 「요청 (11-29 02:54 · 03:02 · 03:35 · 03:46)」).
 * 모드가 있어야 하는 줄(Small Ships · Nightfall Enhance · spmovesets · Kaleidoscope · Spell Engine · Quark · 펫 모드)은 모드팩 시험에서만 본다.
 */
final class Test384 {
    private Test384() {
    }

    /** 이 점검이 쓰는 빈 땅 (다른 점검과 겹치지 않게 멀리). */
    private static int X, Y, Z;

    static void run(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer b, int gx, int y, int gz) throws Exception {
        SelfTest.section("3.84 (11-29 requests)");
        X = gx + 2048;
        Y = y;
        Z = gz - 2048;
        level.getChunkAt(new BlockPos(X, Y, Z));
        level.getChunkAt(new BlockPos(X + 40, Y, Z + 40));
        step("item policy", () -> itemPolicy(server));
        step("world rules", () -> worldRules(server, level));
        step("check loss", () -> checkLoss(server, level));
        step("pets", Test384::pets);
        step("gacha lock", () -> gachaLock(level));
        step("zone use", () -> zoneUse(server, level, b));
        step("zone creatures", () -> zoneCreatures(server, level, a));
        step("travel", () -> travel(server, level));
        step("welcome", () -> welcome(level));
        step("harvest", () -> harvest(server, level, a, b));
        step("food", () -> food(level));
        step("resign", () -> resign(level, gx, gz));
        step("shops", () -> shops(server));
    }

    interface Body {
        void run() throws Exception;
    }

    private static void step(String name, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            check("3.84 " + name + " ran without an error", false, t.toString());
            com.rtshop.RtShop.LOGGER.error("[rtshop-selftest] 3.84 {} crashed", name, t);
        }
    }

    private static FakePlayer fresh(ServerLevel level, String name) {
        UUID id = UUID.nameUUIDFromBytes(("rtshop:" + name).getBytes(StandardCharsets.UTF_8));
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(id, name));
        p.getInventory().clearContent();
        return p;
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof TranslatableContents t ? t.getKey() : String.valueOf(c);
    }

    private static int count(FakePlayer p, java.util.function.Predicate<ItemStack> what) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && what.test(s)) n += s.getCount();
        }
        return n;
    }

    private static LandData.Land land(MinecraftServer server, ServerLevel level, UUID owner, String ownerName, int x0, int z0, int w) {
        LandData data = LandData.get(server);
        LandData.Land l = data.createLand(owner, ownerName);
        LandData.Claim c = new LandData.Claim();
        c.dim = LandManager.dim(level);
        c.type = "land";
        c.x0 = x0;
        c.z0 = z0;
        c.x1 = x0 + w - 1;
        c.z1 = z0 + w - 1;
        c.y0 = Y - 4;
        c.y1 = Y + 8;
        data.addClaim(l, c);
        return l;
    }

    // ------------------------------------------------------------------ M-1 · M-3 · M-55: 아이템 규칙 9판

    private static void itemPolicy(MinecraftServer server) {
        ItemPolicy.Config d = ItemPolicy.Config.defaults();
        check("3.84 item_policy 9: Nightfall Enhance and Small Ships items cannot be crafted, looted or dropped from blocks, the Epic Fight skill book cannot be crafted, Aquamirae gear is off the NPC shops",
                ItemPolicy.Config.VERSION == 9 && d.lootBan.containsAll(ItemPolicy.Config.NEW_MODS_384) && d.blockDropBan.containsAll(ItemPolicy.Config.NEW_MODS_384)
                        && d.recipeBan.containsAll(ItemPolicy.Config.NEW_MODS_384) && d.recipeBan.contains("epicfight:skillbook")
                        && d.recipeBanMods.containsAll(ItemPolicy.Config.NEW_MOD_IDS_384) && d.shopBanGearMods.contains("aquamirae"));
        com.google.gson.JsonObject old = d.toJson();
        old.addProperty("version", 8);
        for (String k : List.of("lootBan", "blockDropBan", "recipeBan", "recipeBanMods", "shopBanGearMods")) {
            com.google.gson.JsonArray keep = new com.google.gson.JsonArray();
            if (old.has(k)) {
                for (com.google.gson.JsonElement e : old.getAsJsonArray(k)) {
                    String v = e.getAsString();
                    if (!ItemPolicy.Config.NEW_MODS_384.contains(v) && !ItemPolicy.Config.NEW_MOD_IDS_384.contains(v) && !v.equals("epicfight:skillbook") && !v.equals("aquamirae")) keep.add(v);
                }
            }
            old.add(k, keep);
        }
        ItemPolicy.Config up = ItemPolicy.Config.fromJson(old);
        check("3.84 an item_policy.json of version 8 gets the 3.84 bans added (and keeps its other lines)",
                up.lootBan.containsAll(ItemPolicy.Config.NEW_MODS_384) && up.recipeBan.contains("epicfight:skillbook") && up.recipeBanMods.contains("smallships")
                        && up.shopBanGearMods.contains("aquamirae") && up.lootBan.size() >= d.lootBan.size() - 1, up.lootBan.size() + " / " + d.lootBan.size());
        // 모드가 있을 때: 실제 아이템
        List<String> efn = new ArrayList<>();
        int efnLoot = 0, efnRecipe = 0;
        for (Item it : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(it);
            if (id == null || !id.getNamespace().equals("efn_enhance")) continue;
            efn.add(id.getPath());
            ItemStack s = new ItemStack(it);
            if (ItemPolicy.lootBanned(s, false) && ItemPolicy.blockDropBanned(s)) efnLoot++;
            if (ItemPolicy.recipeBanned(new ResourceLocation("efn_enhance", "x_" + id.getPath()), id) && ItemPolicy.recipeBanned(new ResourceLocation("minecraft", "x"), id)) efnRecipe++;
        }
        if (ModList.get().isLoaded("efn_enhance")) {
            info("3.84 Nightfall Enhance (efn_enhance) items (" + efn.size() + "): " + String.join(", ", efn));
            check("3.84 every Nightfall Enhance item is kept out of loot and block drops, and cannot be crafted", !efn.isEmpty() && efnLoot == efn.size() && efnRecipe == efn.size(),
                    efnLoot + " / " + efnRecipe + " of " + efn.size());
        }
        if (ModList.get().isLoaded("smallships")) {
            Item cog = ForgeRegistries.ITEMS.getValue(new ResourceLocation("smallships", "oak_cog"));
            ItemStack s = cog == null ? ItemStack.EMPTY : new ItemStack(cog);
            check("3.84 a Small Ships ship cannot be crafted, looted or dropped, yet may be sold in shops",
                    !s.isEmpty() && ItemPolicy.lootBanned(s, false) && ItemPolicy.blockDropBanned(s) && ItemPolicy.recipeBanned(new ResourceLocation("smallships", "oak_cog"), new ResourceLocation("smallships", "oak_cog"))
                            && !ItemPolicy.shopBanned(s), s);
        }
        if (ModList.get().isLoaded("epicfight")) {
            check("3.84 the Epic Fight skill book cannot be crafted (any recipe making it is removed)",
                    ItemPolicy.recipeBanned(new ResourceLocation("efn", "skillbook_x"), new ResourceLocation("epicfight", "skillbook")));
        }
        if (ModList.get().isLoaded("aquamirae")) {
            int gear = 0, banned = 0, other = 0, otherBanned = 0;
            List<String> leak = new ArrayList<>();
            for (Item it : ForgeRegistries.ITEMS) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(it);
                if (id == null || !id.getNamespace().equals("aquamirae")) continue;
                ItemStack s = new ItemStack(it);
                if (ItemPolicy.gear(it)) {
                    gear++;
                    if (ItemPolicy.shopBanned(s)) banned++;
                    else leak.add(id.getPath());
                } else {
                    other++;
                    if (ItemPolicy.shopBanned(s)) otherBanned++;
                }
            }
            int listed = 0;
            for (ShopData sd : ShopServer.shops().values()) {
                for (ShopData.Entry e : sd.entries) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                    if (id != null && id.getNamespace().equals("aquamirae") && ItemPolicy.gear(e.stack.getItem())) listed++;
                }
            }
            check("3.84 Aquamirae gear (weapons, armour, tools, curios) is never on an NPC shop, its other items are not touched",
                    gear > 0 && banned == gear && listed == 0, gear + " gear, " + banned + " banned, " + listed + " still listed " + leak + "; other " + other + " (" + otherBanned + " banned)");
        }
    }

    // ------------------------------------------------------------------ M-4 · M-13 · M-17 · M-20 · M-42 · M-45 · M-56 · M-62: 월드 규칙

    private static void worldRules(MinecraftServer server, ServerLevel level) throws Exception {
        com.rtshop.rules.WorldRules w = com.rtshop.rules.WorldRules.defaults();
        check("3.84 world rules: no spmovesets starter kit, vanilla hunger, vanilla boat speed, no rune cost for RPG spells, a welcome shulker box, magic open to all (weaker for non-mages)",
                !w.spmovesetsStarterKit && w.vanillaHunger && w.vanillaBoatSpeed && w.spellItemCostOff && w.welcomeShulker && !w.magicOnlyForMage && w.rulesVersion == 2);
        com.rtshop.pvp.PvpConfig pvp = new com.rtshop.pvp.PvpConfig();
        check("3.84 PvP outside the castle zone is on by default", pvp.outside);
        // spmovesets 의 생존 꾸러미 (안내서 + 열매 뽑기 열쇠): 접속할 때 받은 것으로 표시한다
        FakePlayer k = fresh(level, "rtshop_test_kit384");
        CompoundTag keep = k.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        keep.remove(com.rtshop.compat.SpMovesetsKitOff.FLAG);
        k.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, keep);
        com.rtshop.compat.SpMovesetsKitOff.onLoad(new PlayerEvent.LoadFromFile(k, server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile(), k.getStringUUID()));
        if (ModList.get().isLoaded("spmovesets")) {
            boolean marked = com.rtshop.compat.SpMovesetsKitOff.marked(k);
            String gave = "not called";
            int items = -1;
            boolean survival = false;
            try {
                Class<?> sm = Class.forName("spmovesets.spmovesets.survival.SurvivalMode");
                survival = Boolean.TRUE.equals(sm.getMethod("isOn", Level.class).invoke(null, level));
                Class<?> kit = Class.forName("spmovesets.spmovesets.survival.start.StarterKit");
                java.lang.reflect.Method give = kit.getDeclaredMethod("give", net.minecraft.server.level.ServerPlayer.class);
                give.setAccessible(true);
                gave = String.valueOf(give.invoke(null, k));
                items = count(k, s -> {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
                    return id != null && id.getNamespace().equals("spmovesets");
                });
            } catch (Throwable t) {
                gave = "error " + t;
            }
            info("3.84 spmovesets survival mode " + (survival ? "on" : "off") + ", StarterKit.give -> " + gave);
            check("3.84 a newcomer is marked as having had the spmovesets survival kit before that mod looks, so it gives nothing",
                    marked && "false".equals(gave) && items == 0, marked + " " + gave + " items " + items);
        } else {
            check("3.84 without spmovesets nothing is marked", !com.rtshop.compat.SpMovesetsKitOff.marked(k));
        }
        // 배고픔: Kaleidoscope 의 포만 보호막 · Spell Engine 의 배고픔 소모
        if (ModList.get().isLoaded(com.rtshop.compat.HungerVanilla.COOKERY)) {
            String again = com.rtshop.compat.HungerVanilla.shieldOff();
            check("3.84 Kaleidoscope Cookery's satiated shield (damage turned into hunger) is off", "already off".equals(again) || "off".equals(again),
                    again + " / at start: " + com.rtshop.compat.HungerVanilla.lastForTest);
        }
        Object exhaust = com.rtshop.compat.SpellEngineTweaks.get("spell_cost_exhaust_multiplier");
        Object itemCost = com.rtshop.compat.SpellEngineTweaks.get("spell_cost_item_allowed");
        if (ModList.get().isLoaded("spell_engine")) {
            check("3.84 RPG (Spell Engine) spells cost no hunger and no rune items (the pack has no Runes mod: '공기 부족')",
                    exhaust instanceof Number n && n.doubleValue() == 0 && Boolean.FALSE.equals(itemCost),
                    exhaust + " / " + itemCost + " (" + com.rtshop.compat.SpellEngineTweaks.lastExhaustForTest + ", " + com.rtshop.compat.SpellEngineTweaks.lastItemForTest + ")");
        }
        if (ModList.get().isLoaded(com.rtshop.compat.SmallShipsBoats.MOD)) {
            var v = com.rtshop.compat.SmallShipsBoats.slowdown();
            check("3.84 Small Ships no longer slows vanilla boats", v != null && Boolean.FALSE.equals(v.get()), (v == null ? "no setting" : v.get()) + " / " + com.rtshop.compat.SmallShipsBoats.lastForTest);
        }
    }

    // ------------------------------------------------------------------ M-10: 셜커 상자 속 수표도 죽으면 값이 준다

    private static void checkLoss(MinecraftServer server, ServerLevel level) {
        FakePlayer p = fresh(level, "rtshop_test_check384");
        com.rtshop.economy.Economy.setWallet(p, 10_000);
        boolean issued = com.rtshop.economy.Economy.issueCheck(p, 1000);
        ItemStack chk = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(ModItems.GOLD_CHECK.get())) {
                chk = s.copy();
                p.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }
        UUID id = com.rtshop.item.CheckItem.id(chk);
        ItemStack box1 = box(chk), box2 = box(chk);   // 같은 수표를 두 상자에 (복사본): 한 번만 센다
        p.getInventory().add(box1);
        p.getInventory().add(box2);
        long lost = com.rtshop.economy.Economy.checkLoss(p, 10);
        long left = id == null ? -1 : com.rtshop.economy.EconomyData.get(server).checkAmount(id);
        ItemStack inBox = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            CompoundTag be = s.getTagElement("BlockEntityTag");
            if (be != null && be.contains("Items")) {
                inBox = ItemStack.of(be.getList("Items", 10).getCompound(0));
                break;
            }
        }
        check("3.84 a check kept inside a shulker box loses value on death like a carried one (counted once even if copied into two boxes)",
                issued && id != null && lost == 100 && left == 900 && com.rtshop.item.CheckItem.amount(inBox) == 900, issued + " lost " + lost + " left " + left + " in box " + com.rtshop.item.CheckItem.amount(inBox));
        p.getInventory().clearContent();
    }

    private static ItemStack box(ItemStack inside) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        CompoundTag be = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag e = inside.save(new CompoundTag());
        e.putByte("Slot", (byte) 3);
        items.add(e);
        be.put("Items", items);
        box.addTagElement("BlockEntityTag", be);
        return box;
    }

    // ------------------------------------------------------------------ M-19 · M-51 · M-59: 몹

    private static void pets() {
        com.rtshop.rules.MobRules.Config c = com.rtshop.rules.MobRules.fresh();
        check("3.84 pet-mod creatures never spawn on their own (gacha pets, eggs, breeding and commands still work); hollow 60%, deepling brute 5%",
                c.noNaturalSpawn.containsAll(com.rtshop.rules.MobRules.PET_MODS_384) && c.keepByName.get("hollow") == 0.6 && c.keepByName.get("deepling_brute") == 0.05
                        && com.rtshop.rules.MobRules.naturalWay(MobSpawnType.NATURAL) && com.rtshop.rules.MobRules.naturalWay(MobSpawnType.CHUNK_GENERATION)
                        && !com.rtshop.rules.MobRules.naturalWay(MobSpawnType.SPAWN_EGG) && !com.rtshop.rules.MobRules.naturalWay(MobSpawnType.BREEDING)
                        && !com.rtshop.rules.MobRules.naturalWay(MobSpawnType.COMMAND) && !com.rtshop.rules.MobRules.noNaturalSpawn(EntityType.COW));
        List<String> pets = new ArrayList<>();
        int stopped = 0;
        for (EntityType<?> t : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(t);
            if (id == null) continue;
            String ns = id.getNamespace();
            if (!ns.equals("adorablehamsterpets") && !ns.equals("crittersandcompanions") && !ns.equals("rpg_companions_tiny_dragons") && !ns.equals("fantasy") && !ns.equals("fantasymounts")) continue;
            pets.add(id.toString());
            if (com.rtshop.rules.MobRules.noNaturalSpawn(t)) stopped++;
        }
        if (!pets.isEmpty()) {
            info("3.84 pet-mod creatures kept from spawning naturally (" + pets.size() + "): " + String.join(", ", pets.subList(0, Math.min(40, pets.size()))));
            check("3.84 every creature of the pet mods is on the no-natural-spawn list", stopped == pets.size(), stopped + " / " + pets.size());
        }
    }

    // ------------------------------------------------------------------ M-12: 무기 뽑기는 1장 뒤에

    private static void gachaLock(ServerLevel level) {
        FakePlayer p = fresh(level, "rtshop_test_gacha384");
        boolean quest = com.rtshop.quest.QuestManager.loaded().quests.containsKey("story_1_41");
        com.rtshop.gacha.Gacha.lockFakeForTest = true;
        try {
            com.rtshop.quest.QuestManager.setFlag(p, "story_ch1_done", false);
            String weapon = com.rtshop.gacha.Gacha.lockedFor(p, "weapon"), pet = com.rtshop.gacha.Gacha.lockedFor(p, "pet");
            List<com.rtshop.gacha.GachaConfig.Entry> pulled = com.rtshop.gacha.Gacha.doPull(p, "weapon", "normal", 1, false);
            com.rtshop.quest.QuestManager.setFlag(p, "story_ch1_done", true);
            String after = com.rtshop.gacha.Gacha.lockedFor(p, "weapon");
            check("3.84 the weapon gacha (pulls and trades) stays shut until Chapter 1 is done; other gachas are open",
                    com.rtshop.gacha.GachaConfig.get().weaponAfterChapter1 && pet == null && after == null && (!quest || "1장".equals(weapon) && pulled == null),
                    "quest " + quest + " weapon " + weapon + " pet " + pet + " pulled " + pulled + " after " + after);
            check("3.84 weapon tickets are recognised as weapon tickets (the shop shows them as 'Chapter 1')",
                    com.rtshop.gacha.Gacha.weaponTicket(new ItemStack(com.rtshop.gacha.Gacha.ticket("weapon", "normal"))) && !com.rtshop.gacha.Gacha.weaponTicket(new ItemStack(com.rtshop.gacha.Gacha.ticket("pet", "normal"))));
        } finally {
            com.rtshop.gacha.Gacha.lockFakeForTest = false;
            com.rtshop.quest.QuestManager.setFlag(p, "story_ch1_done", false);
        }
    }

    // ------------------------------------------------------------------ M-11 · M-13 · M-35: 남의 땅 · 마을에서 쓸 수 있는 블록

    private static void zoneUse(MinecraftServer server, ServerLevel level, FakePlayer owner) {
        FakePlayer p = fresh(level, "rtshop_test_use384");
        LandData data = LandData.get(server);
        String dim = LandManager.dim(level);
        int x = X + 2, z = Z + 2;
        LandData.Land l = land(server, level, owner.getUUID(), owner.getGameProfile().getName(), x - 2, z - 2, 8);
        BlockPos left = new BlockPos(x, Y, z), right = left.east();
        BlockState chestL = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockState chestR = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT);
        level.setBlock(left.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(right.below(), Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(left, chestL, 2);
        level.setBlock(right, chestR, 2);
        try {
            boolean before = com.rtshop.land.ZoneUse.usable(level, left, l, chestL, true);
            boolean[] ev0 = click(p, level, left);
            data.setUsable(dim, right, true);   // 운영자가 반쪽만 골라도 큰 상자 둘 다
            boolean pickedL = com.rtshop.land.ZoneUse.usable(level, left, l, chestL, true), pickedR = com.rtshop.land.ZoneUse.usable(level, right, l, chestR, true);
            boolean[] ev1 = click(p, level, left);
            boolean off = !data.toggleUsable(dim, right) && !com.rtshop.land.ZoneUse.usable(level, left, l, chestL, true);
            check("3.84 a block the operator picks (/rtshop plot use) can be used by anyone, even on someone's land: both halves of a large chest; picking it again locks it",
                    !before && ev0[0] && pickedL && pickedR && !ev1[0] && off, before + " " + java.util.Arrays.toString(ev0) + " " + pickedL + " " + pickedR + " " + java.util.Arrays.toString(ev1) + " " + off);
        } finally {
            data.setUsable(dim, right, false);
            level.setBlock(left, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(right, Blocks.AIR.defaultBlockState(), 2);
            data.removeLand(l);
        }
        // 마을(성 영역): 작업대 · 강화 모루 · 룬 제단 · 의자는 누구나, 조리 도구 · 카지노는 물건을 넣는 블록이라도 누구나, 상자는 아니다
        com.rtshop.land.LandConfig lc = com.rtshop.land.LandConfig.get();
        BlockPos zp = new BlockPos(-24360, 70, -5420);
        check("3.84 in the castle zone anyone may use the crafting table, enhance anvil, rune altar, chairs, cooking blocks and the casino, but not chests",
                com.rtshop.land.ZoneUse.usable(level, zp, LandManager.ZONE, Blocks.CRAFTING_TABLE.defaultBlockState(), false)
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsable, new ResourceLocation("rtshop", "enhance_anvil"))
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsable, new ResourceLocation("rtshop", "arcane_orb"))
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsable, new ResourceLocation("somemod", "oak_chair"))
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsableStorage, new ResourceLocation("farmersdelight", "cooking_pot"))
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsableStorage, new ResourceLocation("kaleidoscope_cookery", "pot"))
                        && com.rtshop.land.ZoneUse.match(lc.castleZoneUsableStorage, new ResourceLocation("casinodeco", "slot_machine"))
                        && com.rtshop.land.ZoneUse.usable(level, zp, LandManager.ZONE, Blocks.SMOKER.defaultBlockState(), true)
                        && !com.rtshop.land.ZoneUse.usable(level, zp, LandManager.ZONE, Blocks.CHEST.defaultBlockState(), true)
                        && !com.rtshop.land.ZoneUse.usable(level, zp, LandManager.ZONE, Blocks.BARREL.defaultBlockState(), true)
                        && !com.rtshop.land.ZoneUse.match(lc.castleZoneUsable, new ResourceLocation("minecraft", "chest")),
                lc.castleZoneUsable + " / " + lc.castleZoneUsableStorage);
    }

    /** p 가 그 블록을 우클릭한다 (빈손). {막혔나, 블록 쓰기를 막았나}. */
    private static boolean[] click(FakePlayer p, ServerLevel level, BlockPos pos) {
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        PlayerInteractEvent.RightClickBlock ev = new PlayerInteractEvent.RightClickBlock(p, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        MinecraftForge.EVENT_BUS.post(ev);
        return new boolean[]{ev.isCanceled() || ev.getUseBlock() == Event.Result.DENY, ev.getUseBlock() == Event.Result.DENY};
    }

    // ------------------------------------------------------------------ M-29: 마을의 생물

    private static void zoneCreatures(MinecraftServer server, ServerLevel level, FakePlayer a) {
        LandData data = LandData.get(server);
        int was = data.castleInstalled;
        data.castleInstalled = com.rtshop.land.CastlePlots.VERSION;
        List<Entity> made = new ArrayList<>();
        try {
            double zx = -24360.5, zz = -5420.5;
            var cow = EntityType.COW.create(level);
            cow.moveTo(zx, 70, zz, 0, 0);
            var named = EntityType.COW.create(level);
            named.moveTo(zx + 1, 70, zz, 0, 0);
            named.setCustomName(Component.literal("Bessie"));
            var wolf = EntityType.WOLF.create(level);
            wolf.moveTo(zx + 2, 70, zz, 0, 0);
            wolf.tame(a);
            var zombie = EntityType.ZOMBIE.create(level);
            zombie.moveTo(zx + 3, 70, zz, 0, 0);
            var npc = EntityType.VILLAGER.create(level);
            npc.moveTo(zx + 4, 70, zz, 0, 0);
            npc.addTag("rtshop_npc");
            var stand = EntityType.ARMOR_STAND.create(level);
            stand.moveTo(zx + 5, 70, zz, 0, 0);
            var outside = EntityType.COW.create(level);
            outside.moveTo(-24650.5, 70, -5400.5, 0, 0);
            var villager = EntityType.VILLAGER.create(level);
            villager.moveTo(zx + 6, 70, zz, 0, 0);
            boolean on = com.rtshop.land.LandConfig.get().castleNoCreatures;
            check("3.84 in the castle zone wild animals, villagers and monsters go; named, tamed, RT Shop NPCs and armour stands stay; outside the zone nothing changes",
                    on && com.rtshop.land.ZoneCreatures.unwanted(level, cow) && com.rtshop.land.ZoneCreatures.unwanted(level, zombie) && com.rtshop.land.ZoneCreatures.unwanted(level, villager)
                            && !com.rtshop.land.ZoneCreatures.unwanted(level, named) && !com.rtshop.land.ZoneCreatures.unwanted(level, wolf)
                            && !com.rtshop.land.ZoneCreatures.unwanted(level, npc) && !com.rtshop.land.ZoneCreatures.unwanted(level, stand)
                            && !com.rtshop.land.ZoneCreatures.unwanted(level, outside),
                    on + " cow " + com.rtshop.land.ZoneCreatures.unwanted(level, cow) + " named " + com.rtshop.land.ZoneCreatures.unwanted(level, named)
                            + " wolf " + com.rtshop.land.ZoneCreatures.unwanted(level, wolf) + " zombie " + com.rtshop.land.ZoneCreatures.unwanted(level, zombie)
                            + " npc " + com.rtshop.land.ZoneCreatures.unwanted(level, npc) + " outside " + com.rtshop.land.ZoneCreatures.unwanted(level, outside));
            // 실제로 세계에 두고 1초마다의 정리를 돌린다 (막 불러온 먼 청크의 생물은 다음 틱에야 세계 목록에 오르므로 직접 건넨다)
            level.getChunkAt(BlockPos.containing(zx, 70, zz));
            boolean added = level.addFreshEntity(cow) & level.addFreshEntity(named);
            made.add(cow);
            made.add(named);
            int n = com.rtshop.land.ZoneCreatures.sweep(level, List.of(cow, named, stand));
            check("3.84 the castle-zone sweep removes the animal already inside and leaves the named one", added && n >= 1 && cow.isRemoved() && !named.isRemoved(),
                    added + " swept " + n + " cow " + cow.isRemoved() + " named " + named.isRemoved());
            // 산 땅 위: 동물은 그대로, 몬스터는 지운다
            LandData.Land lot = data.createLand(a.getUUID(), a.getGameProfile().getName());
            LandData.Claim c = new LandData.Claim();
            c.dim = LandManager.dim(level);
            c.type = "plot";
            c.x0 = -24380;
            c.z0 = -5440;
            c.x1 = -24370;
            c.z1 = -5430;
            c.y0 = 0;
            c.y1 = 200;
            data.addClaim(lot, c);
            try {
                var pig = EntityType.PIG.create(level);
                pig.moveTo(-24375.5, 70, -5435.5, 0, 0);
                var skel = EntityType.SKELETON.create(level);
                skel.moveTo(-24374.5, 70, -5435.5, 0, 0);
                check("3.84 on a bought lot inside the castle zone animals stay but monsters still go",
                        !com.rtshop.land.ZoneCreatures.unwanted(level, pig) && com.rtshop.land.ZoneCreatures.unwanted(level, skel));
            } finally {
                data.removeLand(lot);
            }
        } finally {
            for (Entity e : made) if (!e.isRemoved()) e.discard();
            data.castleInstalled = was;
        }
    }

    // ------------------------------------------------------------------ M-9: 성 이동권 · 귀가권

    private static void travel(MinecraftServer server, ServerLevel level) {
        FakePlayer p = fresh(level, "rtshop_test_travel384");
        com.rtshop.rules.TownSpawn.Config was = com.rtshop.rules.TownSpawn.get();
        com.rtshop.rules.TownSpawn.Config t = com.rtshop.rules.TownSpawn.fresh();
        String dim = LandManager.dim(level);
        t.set = true;
        t.dim = dim;
        t.x = X + 0.5;
        t.y = Y;
        t.z = Z + 0.5;
        com.rtshop.rules.TownSpawn.setForTest(t);
        com.rtshop.rules.TownSpawn.clearCooldown(p);
        LandData.Land mine = null;
        try {
            p.moveTo(X + 10.5, Y, Z + 10.5, 0, 0);
            Component noCastle = com.rtshop.item.TravelTicketItem.refusal(p, com.rtshop.item.TravelTicketItem.Kind.CASTLE);
            t.castleSet = true;
            t.castleDim = dim;
            t.cx = X + 300.5;
            t.cy = Y;
            t.cz = Z + 0.5;
            Component near = com.rtshop.item.TravelTicketItem.refusal(p, com.rtshop.item.TravelTicketItem.Kind.CASTLE);
            com.rtshop.rules.TownSpawn.Arrival toCastle = com.rtshop.item.TravelTicketItem.destination(p, com.rtshop.item.TravelTicketItem.Kind.CASTLE);
            p.moveTo(X + 290.5, Y, Z + 0.5, 0, 0);
            com.rtshop.rules.TownSpawn.Arrival back = com.rtshop.item.TravelTicketItem.destination(p, com.rtshop.item.TravelTicketItem.Kind.CASTLE);
            p.moveTo(X + 5000.5, Y, Z + 0.5, 0, 0);
            Component far = com.rtshop.item.TravelTicketItem.refusal(p, com.rtshop.item.TravelTicketItem.Kind.CASTLE);
            check("3.84 the castle pass: refused until the castle point is set, works in town (to the castle; near the castle, back to the fountain square), refused outside town",
                    "rtshop.travel.no_castle".equals(key(noCastle)) && near == null && toCastle != null && "castle".equals(toCastle.kind()) && Math.abs(toCastle.x() - t.cx) < 1e-6
                            && back != null && "town".equals(back.kind()) && "rtshop.travel.not_in_town".equals(key(far)),
                    key(noCastle) + " " + key(near) + " " + (toCastle == null ? null : toCastle.kind()) + " " + (back == null ? null : back.kind()) + " " + key(far));
            // 귀가권: 침대 → 토지 → 분수 광장
            com.rtshop.rules.TownSpawn.Arrival home0 = com.rtshop.item.TravelTicketItem.destination(p, com.rtshop.item.TravelTicketItem.Kind.HOME);
            mine = land(server, level, p.getUUID(), p.getGameProfile().getName(), X + 40, Z + 40, 6);
            com.rtshop.rules.TownSpawn.Arrival home1 = com.rtshop.item.TravelTicketItem.destination(p, com.rtshop.item.TravelTicketItem.Kind.HOME);
            BlockPos bed = new BlockPos(X + 20, Y + 2, Z + 20);
            level.setBlock(bed, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(bed.above(), Blocks.AIR.defaultBlockState(), 2);
            p.setRespawnPosition(level.dimension(), bed, 0F, true, false);
            com.rtshop.rules.TownSpawn.Arrival home2 = com.rtshop.item.TravelTicketItem.destination(p, com.rtshop.item.TravelTicketItem.Kind.HOME);
            Component farHome = com.rtshop.item.TravelTicketItem.refusal(p, com.rtshop.item.TravelTicketItem.Kind.HOME);
            check("3.84 the home ticket works anywhere: to my bed, else my land, else the fountain square",
                    home0 != null && "town".equals(home0.kind()) && home1 != null && "land".equals(home1.kind()) && home1.x() >= X + 40 && home1.x() <= X + 46
                            && home2 != null && "bed".equals(home2.kind()) && Math.abs(home2.x() - (bed.getX() + 0.5)) < 1e-6 && farHome == null,
                    (home0 == null ? null : home0.kind()) + " " + (home1 == null ? null : home1.kind() + " " + home1.x()) + " " + (home2 == null ? null : home2.kind()) + " " + key(farHome));
            // 쓰기: 표가 하나 줄고, 다시 쓰기까지 기다린다 (귀가권 20분, 성 이동권 30초)
            ItemStack ticket = new ItemStack(ModItems.HOME_TICKET.get(), 2);
            p.setItemInHand(InteractionHand.MAIN_HAND, ticket);
            ModItems.HOME_TICKET.get().finishUsingItem(ticket, level, p);
            Component cd = com.rtshop.item.TravelTicketItem.refusal(p, com.rtshop.item.TravelTicketItem.Kind.HOME);
            long left = com.rtshop.rules.TownSpawn.travelCooldownLeft(p, "home");
            p.moveTo(X + 10.5, Y, Z + 10.5, 0, 0);
            ItemStack pass = new ItemStack(ModItems.CASTLE_PASS.get(), 3);
            ModItems.CASTLE_PASS.get().finishUsingItem(pass, level, p);
            long passLeft = com.rtshop.rules.TownSpawn.travelCooldownLeft(p, "castle");
            check("3.84 using a ticket spends one and starts its own wait (home ticket 20 min, castle pass 30 s); the two do not share a wait",
                    ticket.getCount() == 1 && "rtshop.travel.cooldown".equals(key(cd)) && left > 19 * 60_000L && left <= 20 * 60_000L
                            && pass.getCount() == 2 && passLeft > 25_000L && passLeft <= 30_000L && t.homeCooldownMinutes == 20 && t.castleCooldownSeconds == 30,
                    ticket.getCount() + " " + key(cd) + " " + left + " / pass " + pass.getCount() + " " + passLeft);
        } finally {
            if (mine != null) LandData.get(server).removeLand(mine);
            com.rtshop.rules.TownSpawn.clearCooldown(p);
            com.rtshop.rules.TownSpawn.setForTest(was);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------ M-30: 처음 들어온 사람에게 셜커 상자

    private static void welcome(ServerLevel level) {
        FakePlayer p = fresh(level, "rtshop_test_welcome384");
        com.rtshop.rules.WelcomeGift.forgetForTest(p);
        boolean first = com.rtshop.rules.WelcomeGift.give(p), second = com.rtshop.rules.WelcomeGift.give(p);
        int boxes = count(p, s -> s.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock);
        check("3.84 a newcomer gets one shulker box of a random colour, only once", first && !second && boxes == 1 && com.rtshop.rules.WelcomeGift.lastColorForTest != null,
                first + " " + second + " " + boxes + " " + com.rtshop.rules.WelcomeGift.lastColorForTest);
        p.getInventory().clearContent();
    }

    // ------------------------------------------------------------------ M-25: 남의 밭의 농작물은 오른쪽 클릭으로도 거두지 못한다

    private static void harvest(MinecraftServer server, ServerLevel level, FakePlayer a, FakePlayer owner) {
        int x = X + 12, z = Z + 2;
        LandData.Land l = land(server, level, owner.getUUID(), owner.getGameProfile().getName(), x - 1, z - 1, 4);
        BlockPos crop = new BlockPos(x, Y, z);
        BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        level.setBlock(crop.below(), Blocks.FARMLAND.defaultBlockState(), 2);
        level.setBlock(crop, ripe, 2);
        try {
            check("3.84 crops (wheat, carrots, berries, cocoa, nether wart, stems) are what the harvest guard watches",
                    com.rtshop.land.HarvestGuard.crop(ripe) && com.rtshop.land.HarvestGuard.crop(Blocks.SWEET_BERRY_BUSH.defaultBlockState())
                            && com.rtshop.land.HarvestGuard.crop(Blocks.NETHER_WART.defaultBlockState()) && !com.rtshop.land.HarvestGuard.crop(Blocks.OAK_LOG.defaultBlockState()));
            boolean mayA = com.rtshop.land.HarvestGuard.mayHarvest(a, level, crop), mayOwner = com.rtshop.land.HarvestGuard.mayHarvest(owner, level, crop);
            int blocked0 = com.rtshop.land.HarvestGuard.blockedForTest;
            // 진짜 순서로: 뼛가루를 든 채, 그리고 빈손으로 우클릭 (Quark · Better Farming 의 수확도 이 길로 온다)
            a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 8));
            var r1 = a.gameMode.useItemOn(a, level, a.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false));
            int meal = a.getMainHandItem().getCount();
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var r2 = a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false));
            BlockState after = level.getBlockState(crop);
            check("3.84 someone else's ripe crop cannot be harvested or bone-mealed by right-click (bone meal in hand or empty-handed); the owner may",
                    !mayA && mayOwner && after.is(Blocks.WHEAT) && after.getValue(CropBlock.AGE) == 7 && meal == 8,
                    mayA + " " + mayOwner + " " + after + " meal " + meal + " " + r1 + " " + r2 + " guard " + (com.rtshop.land.HarvestGuard.blockedForTest - blocked0));
            // 마을(성 영역)의 공용 밭 — 예전에는 산 토지만 보아서 RT Shop 의 우클릭 수확으로 거둘 수 있었다
            LandData data = LandData.get(server);
            int was = data.castleInstalled;
            data.castleInstalled = com.rtshop.land.CastlePlots.VERSION;
            BlockPos zc = new BlockPos(-24360, 120, -5420);
            level.getChunkAt(zc);
            try {
                level.setBlock(zc.below(), Blocks.FARMLAND.defaultBlockState(), 2);
                level.setBlock(zc, ripe, 2);
                a.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 8));
                a.gameMode.useItemOn(a, level, a.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(zc), Direction.UP, zc, false));
                a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                a.gameMode.useItemOn(a, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(zc), Direction.UP, zc, false));
                BlockState zs = level.getBlockState(zc);
                check("3.84 the town's own fields (castle zone outside bought lots) cannot be harvested by right-click either",
                        !com.rtshop.land.HarvestGuard.mayHarvest(a, level, zc) && zs.is(Blocks.WHEAT) && zs.getValue(CropBlock.AGE) == 7, zs);
            } finally {
                level.setBlock(zc, Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(zc.below(), Blocks.AIR.defaultBlockState(), 2);
                for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(zc).inflate(3), e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
                data.castleInstalled = was;
            }
            if (ModList.get().isLoaded("quark")) {
                check("3.84 Quark's right-click harvest is hooked too (its harvest event is cancelled for other people's crops)", com.rtshop.land.HarvestGuard.hookedForTest());
                owner.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                owner.gameMode.useItemOn(owner, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false));
                BlockState own = level.getBlockState(crop);
                info("3.84 the owner's empty-hand right-click on the ripe wheat with Quark: " + own + " (age 0 = harvested and replanted)");
            }
        } finally {
            level.setBlock(crop, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(crop.below(), Blocks.AIR.defaultBlockState(), 2);
            for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(crop).inflate(3), e -> e instanceof net.minecraft.world.entity.item.ItemEntity)) e.discard();
            LandData.get(server).removeLand(l);
            a.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------ M-4 · M-16: 음식 (보이는 값 = 실제)

    private static void food(ServerLevel level) {
        FakePlayer p = fresh(level, "rtshop_test_food384");
        String lifeWas = com.rtshop.job.Jobs.entry(p).life;
        com.rtshop.rules.FoodRules.setForTest(com.rtshop.rules.FoodRules.fresh());
        try {
            com.rtshop.job.Jobs.force(p, "life", "");
            int bread = new ItemStack(Items.BREAD).getFoodProperties(p).getNutrition();
            int cookie = new ItemStack(Items.COOKIE).getFoodProperties(p).getNutrition();
            int steak = new ItemStack(Items.COOKED_BEEF).getFoodProperties(p).getNutrition();
            p.getFoodData().setFoodLevel(0);
            p.getFoodData().setSaturation(0);
            ItemStack eat = new ItemStack(Items.BREAD);
            p.getFoodData().eat(Items.BREAD, eat, p);   // Player.eat 이 부르는 그대로 (Forge: 음식 값 = item.getFoodProperties(stack, 먹는 이))
            net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(p, eat.copy(), 0, ItemStack.EMPTY);   // 먹은 뒤의 이벤트 (예전에는 여기서 깎았다)
            int filled = p.getFoodData().getFoodLevel();
            check("3.84 what a food shows is what it fills: bread 5 -> 4 (70%), cookie 2 -> 1, steak 8 unchanged, and eating bread fills exactly 4 (no second cut)",
                    com.rtshop.rules.FoodScale.mixed() && bread == 4 && cookie == 1 && steak == 8 && filled == 4,
                    com.rtshop.rules.FoodScale.mixed() + " bread " + bread + " cookie " + cookie + " steak " + steak + " ate " + filled);
            com.rtshop.job.Jobs.force(p, "life", "cook");
            int cookBread = new ItemStack(Items.BREAD).getFoodProperties(p).getNutrition();
            check("3.84 a cook's food bonus shows in the food value too (bread 4 + 20% -> 5)", cookBread == com.rtshop.rules.FoodScale.nutrition(5, true, 0.7, com.rtshop.job.Jobs.stat(p, "foodBonus")) && cookBread >= 5,
                    cookBread + " bonus " + com.rtshop.job.Jobs.stat(p, "foodBonus"));
        } finally {
            com.rtshop.job.Jobs.force(p, "life", lifeWas == null ? "" : lifeWas);
            com.rtshop.rules.FoodRules.setForTest(null);
        }
    }

    // ------------------------------------------------------------------ M-66: 직업 내려놓기

    private static void resign(ServerLevel level, int gx, int gz) {
        FakePlayer p = fresh(level, "rtshop_test_resign384");
        com.rtshop.job.JobConfig jc = com.rtshop.job.JobConfig.get();
        for (String id : jc.jobs.keySet()) com.rtshop.quest.QuestManager.setFlag(p, "unlock_job_" + id, true);
        com.rtshop.job.Jobs.force(p, "combat", "warrior");
        long cost = jc.cost("combat");
        com.rtshop.economy.Economy.setWallet(p, 0);
        try {
            var r1 = com.rtshop.job.Jobs.resign(p, "combat");
            var e = com.rtshop.job.Jobs.entry(p);
            boolean empty = e.combat.isEmpty() && "warrior".equals(e.combatLast) && Math.abs(p.getMaxHealth() - 20.0F) < 1e-3;
            var r2 = com.rtshop.job.Jobs.resign(p, "combat");
            var back = com.rtshop.job.Jobs.choose(p, "warrior");
            boolean backFree = back.ok() && "warrior".equals(com.rtshop.job.Jobs.entry(p).combat) && com.rtshop.economy.Economy.wallet(p) == 0;
            check("3.84 a job can be laid down for free (its buffs end at once) and taken up again for free; with no job there is nothing to lay down",
                    r1.ok() && empty && !r2.ok() && "rtshop.job.resign_none".equals(key(r2.message())) && backFree,
                    r1.ok() + " " + empty + " " + key(r2.message()) + " " + back.ok() + " " + key(back.message()) + " wallet " + com.rtshop.economy.Economy.wallet(p));
            com.rtshop.job.Jobs.resign(p, "combat");
            var poor = com.rtshop.job.Jobs.choose(p, "swordsman");
            com.rtshop.economy.Economy.setWallet(p, cost);
            var paid = com.rtshop.job.Jobs.choose(p, "swordsman");
            check("3.84 after laying a job down, a different job costs the usual change fee",
                    !poor.ok() && paid.ok() && "swordsman".equals(com.rtshop.job.Jobs.entry(p).combat) && com.rtshop.economy.Economy.wallet(p) == 0 && com.rtshop.job.Jobs.entry(p).combatLast.isEmpty(),
                    key(poor.message()) + " " + key(paid.message()) + " " + com.rtshop.job.Jobs.entry(p).combat + " wallet " + com.rtshop.economy.Economy.wallet(p));
            // 화면의 [내려놓기] 는 "!resign:슬롯" — 직업 안내인 곁에서만
            com.rtshop.job.Jobs.handleChoose(p, com.rtshop.job.Jobs.RESIGN + "combat");
            boolean awayKept = "swordsman".equals(com.rtshop.job.Jobs.entry(p).combat);
            p.moveTo(gx + 20.5, Y, gz + 26.5, 0, 0);   // 늘 불러와 있는 스폰 청크 (먼 청크의 생물은 다음 틱에야 찾을 수 있다)
            net.minecraft.world.entity.npc.Villager guide = com.rtshop.job.Jobs.spawnNpc(level, p.position().add(1, 0, 0), 0);
            try {
                com.rtshop.job.Jobs.open(p, guide == null ? -1 : guide.getId());
                com.rtshop.job.Jobs.handleChoose(p, com.rtshop.job.Jobs.RESIGN + "combat");
                check("3.84 the job screen's [Resign] button works at the job guide (and not away from it)", guide != null && awayKept && com.rtshop.job.Jobs.entry(p).combat.isEmpty(),
                        awayKept + " " + com.rtshop.job.Jobs.entry(p).combat);
            } finally {
                if (guide != null) guide.discard();
                com.rtshop.job.Jobs.open(p, -1);
            }
        } finally {
            com.rtshop.job.Jobs.force(p, "combat", "");
            com.rtshop.job.Jobs.resetCooldowns(p);
        }
    }

    // ------------------------------------------------------------------ M-1 · M-9: 잡화점의 표 · 배

    private static void shops(MinecraftServer server) {
        ShopData g = ShopServer.shops().get("general");
        int pass = -1, home = -1, cog = -1;
        boolean shipCat = false;
        if (g != null) {
            for (ShopData.Entry e : g.entries) {
                if (e.stack.is(ModItems.CASTLE_PASS.get())) pass = e.price;
                if (e.stack.is(ModItems.HOME_TICKET.get())) home = e.price;
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(e.stack.getItem());
                if (id != null && id.toString().equals("smallships:oak_cog")) cog = e.price;
            }
            for (ShopData.Category c : g.categories) if (c.id.equals("ship")) shipCat = true;
        }
        check("3.84 the general merchant sells the castle pass (300 G) and the home ticket (5,000 G), cheaper than return tickets (10,000 / 30,000 G)",
                pass == 300 && home == 5000, pass + " / " + home);
        if (ModList.get().isLoaded("smallships")) {
            check("3.84 the general merchant sells Small Ships ships at a very high price (cog 60,000 G) under a 'ship' tab", cog >= 60000 && shipCat, cog + " " + shipCat);
        } else {
            check("3.84 without Small Ships the empty ship tab is hidden", !shipCat && cog < 0, cog + " " + shipCat);
        }
    }
}
