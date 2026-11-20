package com.rtshop.farm;

import com.rtshop.RtShop;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 농사 규칙의 나머지 절반.
 *  - 상점 씨앗으로 심은 자리를 기록한다 (CropData).
 *  - 다 자란 작물을 우클릭하면 수확하고, 인벤토리의 상점 씨앗 1개를 써서 다시 심는다. 씨앗이 없으면 빈 밭이 된다.
 *    (다른 모드의 "수확 후 공짜로 다시 심기" 기능보다 먼저 처리해서, 씨앗을 계속 사야 하는 규칙을 지킨다.)
 *  - 다른 모드나 주민이 씨앗 없이 심어 놓은 싹은 치운다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class FarmEvents {
    private FarmEvents() {
    }

    private record Pending(ServerLevel level, BlockPos pos) {
    }

    private static final Map<Block, Boolean> SEED_CROP = new IdentityHashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();

    public static void clearCache() {
        synchronized (SEED_CROP) {
            SEED_CROP.clear();
        }
    }

    // ------------------------------------------------------------------ 판별

    /** 씨앗을 심어서 기르는 작물 블록인가. */
    public static boolean isSeedCrop(BlockState state) {
        Block b = state.getBlock();
        synchronized (SEED_CROP) {
            Boolean c = SEED_CROP.get(b);
            if (c == null) {
                c = computeSeedCrop(b);
                SEED_CROP.put(b, c);
            }
            return c;
        }
    }

    private static boolean computeSeedCrop(Block b) {
        try {
            if (b instanceof GrowingPlantBlock) {
                return false; // 덩굴처럼 스스로 뻗어 나가는 식물
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
            if (id != null && FarmConfig.get().notCrops.contains(id.toString())) {
                return false;
            }
            if (b instanceof CropBlock || b instanceof StemBlock || b instanceof AttachedStemBlock
                    || b instanceof NetherWartBlock || b instanceof CocoaBlock) {
                return true;
            }
            Item item = b.asItem();
            return item != Items.AIR && ageProperty(b.defaultBlockState()) != null && SeedRules.isPlantable(new ItemStack(item));
        } catch (Throwable t) {
            return false;
        }
    }

    @Nullable
    public static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty ip && p.getName().equals("age")) {
                return ip;
            }
        }
        return null;
    }

    private static int minAge(IntegerProperty p) {
        int min = Integer.MAX_VALUE;
        for (int v : p.getPossibleValues()) {
            min = Math.min(min, v);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private static int maxAge(IntegerProperty p) {
        int max = 0;
        for (int v : p.getPossibleValues()) {
            max = Math.max(max, v);
        }
        return max;
    }

    /** 우클릭 수확 대상인가: 다 자란 일반 작물, 네더 사마귀, 코코아. */
    public static boolean harvestable(BlockState state) {
        Block b = state.getBlock();
        try {
            if (b instanceof CropBlock crop) {
                if (!crop.isMaxAge(state)) {
                    return false;
                }
            } else if (b instanceof NetherWartBlock || b instanceof CocoaBlock) {
                IntegerProperty age = ageProperty(state);
                if (age == null || state.getValue(age) < maxAge(age)) {
                    return false;
                }
            } else {
                return false;
            }
            if (ageProperty(state) == null) {
                return false;
            }
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(b);
            return id == null || !FarmConfig.get().noRightClickHarvest.contains(id.toString());
        } catch (Throwable t) {
            return false;
        }
    }

    /** 이 작물을 심는 데 쓰는 씨앗 아이템. */
    public static Item seedItemOf(BlockState state, Level level, BlockPos pos) {
        Block b = state.getBlock();
        Item item = b.asItem();
        try {
            if (item == Items.AIR || !SeedRules.isPlantable(new ItemStack(item))) {
                ItemStack clone = b.getCloneItemStack(level, pos, state);
                if (!clone.isEmpty()) {
                    item = clone.getItem();
                }
            }
        } catch (Throwable ignored) {
        }
        return item;
    }

    private static boolean isHoe(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        try {
            return s.getItem() instanceof HoeItem || s.is(ItemTags.HOES);
        } catch (Throwable t) {
            return s.getItem() instanceof HoeItem;
        }
    }

    // ------------------------------------------------------------------ 심은 자리 기록

    private static boolean licensedHands(ServerPlayer sp) {
        if (sp.isCreative() || !FarmConfig.get().requireShopSeeds) {
            return true;
        }
        ItemStack main = sp.getMainHandItem();
        ItemStack off = sp.getOffhandItem();
        if (SeedRules.isLicensed(main) || SeedRules.isLicensed(off)) {
            return true;
        }
        // 씨앗이 아닌 특수한 아이템으로 심는 모드 작물은 막지 않는다
        return !SeedRules.isPlantable(main) && !SeedRules.isPlantable(off);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        try {
            if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                boolean any = false;
                for (BlockSnapshot snap : multi.getReplacedBlockSnapshots()) {
                    if (isSeedCrop(level.getBlockState(snap.getPos()))) {
                        any = true;
                        break;
                    }
                }
                if (any && licensedHands(sp)) {
                    CropData data = CropData.get(level);
                    for (BlockSnapshot snap : multi.getReplacedBlockSnapshots()) {
                        if (isSeedCrop(level.getBlockState(snap.getPos()))) {
                            data.mark(CropData.dim(level), snap.getPos());
                        }
                    }
                }
            } else if (isSeedCrop(event.getPlacedBlock()) && licensedHands(sp)) {
                CropData.get(level).mark(CropData.dim(level), event.getPos());
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] crop mark failed", t);
        }
    }

    // ------------------------------------------------------------------ 씨앗 없이 심긴 싹 치우기

    private static boolean unlicensedSprout(ServerLevel level, BlockPos pos, BlockState state) {
        if (!isSeedCrop(state)) {
            return false;
        }
        IntegerProperty age = ageProperty(state);
        if (age == null || state.getValue(age) != minAge(age)) {
            return false;
        }
        return CropData.get(level).findMark(CropData.dim(level), pos) == null;
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        try {
            BlockState state = event.getState();
            if (!isSeedCrop(state)) {
                return;
            }
            FarmConfig cfg = FarmConfig.get();
            if (!cfg.requireShopSeeds || !cfg.removeUnlicensedSprouts) {
                return;
            }
            if (unlicensedSprout(level, event.getPos(), state)) {
                synchronized (PENDING) {
                    PENDING.add(new Pending(level, event.getPos().immutable()));
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] sprout check failed", t);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            flushPending();
        }
    }

    /** 씨앗 없이 심긴 것으로 의심되는 싹을 다시 확인하고 치운다 (매 틱 끝에 호출). */
    public static void flushPending() {
        List<Pending> list;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            list = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (Pending p : list) {
            try {
                if (!p.level.isLoaded(p.pos)) {
                    continue;
                }
                BlockState st = p.level.getBlockState(p.pos);
                if (unlicensedSprout(p.level, p.pos, st)) {
                    p.level.destroyBlock(p.pos, false);
                }
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] sprout removal failed", t);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        synchronized (PENDING) {
            PENDING.clear();
        }
        clearCache();
    }

    // ------------------------------------------------------------------ 우클릭 수확

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickCrop(PlayerInteractEvent.RightClickBlock event) {
        FarmConfig cfg = FarmConfig.get();
        if (!cfg.rightClickHarvest || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.isSpectator()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!harvestable(state)) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        boolean hoe = isHoe(main);
        if (player.isSecondaryUseActive() && !main.isEmpty() && !hoe) {
            return; // 웅크린 채 아이템을 쓰는 경우(블록 설치 등)는 그대로 둔다
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || !(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return;
        }
        try {
            harvestArea(sp, sl, pos, main, hoe);
        } catch (Throwable t) {
            RtShop.LOGGER.error("[rtshop] harvest failed", t);
        }
    }

    private static void harvestArea(ServerPlayer sp, ServerLevel level, BlockPos center, ItemStack tool, boolean hoe) {
        FarmConfig cfg = FarmConfig.get();
        int radius = 0;
        if (hoe && !sp.isShiftKeyDown()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(tool.getItem());
            Integer r = id == null ? null : cfg.harvestSweep.get(id.toString());
            radius = r == null ? 0 : r;
        }
        int harvested = 0;
        int notReplanted = 0;
        boolean denied = false;
        outer:
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, 0, dz);
                    BlockState st = level.getBlockState(p);
                    if (!harvestable(st)) {
                        continue;
                    }
                    if (!LandManager.can(sp, level, p, LandData.BREAK) || !level.mayInteract(sp, p)) {
                        denied = true;
                        continue;
                    }
                    if (!harvestOne(sp, level, p, st, tool)) {
                        notReplanted++;
                    }
                    harvested++;
                    if (hoe && cfg.harvestToolDamage > 0 && tool.isDamageableItem()) {
                        tool.hurtAndBreak(cfg.harvestToolDamage, sp, x -> x.broadcastBreakEvent(EquipmentSlot.MAINHAND));
                        if (tool.isEmpty()) {
                            break outer;
                        }
                    }
                }
            }
        }
        if (notReplanted > 0) {
            sp.displayClientMessage(Component.translatable("rtshop.msg.harvest_no_seed", notReplanted)
                    .withStyle(ChatFormatting.YELLOW), true);
        } else if (denied && harvested == 0) {
            sp.displayClientMessage(Component.translatable("rtshop.msg.harvest_denied").withStyle(ChatFormatting.RED), true);
        }
    }

    private static boolean takeLicensedSeed(ServerPlayer sp, Item seed) {
        if (seed == null || seed == Items.AIR) {
            return false;
        }
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(seed) && SeedRules.isLicensed(s)) {
                s.shrink(1);
                if (s.isEmpty()) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
                inv.setChanged();
                return true;
            }
        }
        return false;
    }

    /**
     * 작물 하나를 수확한다. 수확물은 플레이어에게 바로 주고, 상점 씨앗이 있으면 1개를 써서 다시 심는다.
     * @return 다시 심었으면 true, 씨앗이 없어 빈 밭이 됐으면 false
     */
    public static boolean harvestOne(ServerPlayer sp, ServerLevel level, BlockPos pos, BlockState state, ItemStack tool) {
        Item seed = seedItemOf(state, level, pos);
        // 전리품 수정자가 씨앗을 빼고, 별 등급을 매기고, 이 자리의 "심은 기록"을 지운다
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), sp, tool));
        boolean replant = sp.isCreative() || !FarmConfig.get().requireShopSeeds || takeLicensedSeed(sp, seed);
        level.levelEvent(2001, pos, Block.getId(state));
        IntegerProperty age = ageProperty(state);
        if (replant && age != null) {
            CropData.get(level).mark(CropData.dim(level), pos);
            level.setBlock(pos, state.setValue(age, minAge(age)), 3);
        } else {
            replant = false;
            level.removeBlock(pos, false);
        }
        for (ItemStack d : drops) {
            if (!d.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(sp, d);
            }
        }
        return replant;
    }

    // ------------------------------------------------------------------ 다른 모드와의 연동 (API 없이 이름으로만 연결)

    /**
     * Kaleidoscope Cookery 의 낫: 넓은 범위의 작물을 수확하고 공짜로 다시 심는다.
     * 그 모드가 설치돼 있으면 낫의 수확 이벤트를 받아서 (1) 남의 토지에서는 막고 (2) 우리 규칙대로 수확·재파종한다.
     */
    public static void registerCompat() {
        try {
            Class<?> cls = Class.forName("com.github.ysbbbbbb.kaleidoscopecookery.api.event.SickleHarvestEvent");
            if (!PlayerEvent.class.isAssignableFrom(cls)) {
                return;
            }
            Method getPos = cls.getMethod("getHarvestPos");
            Method getState = cls.getMethod("getHarvestState");
            Method getSickle = cls.getMethod("getSickle");
            Method setCost = cls.getMethod("setCostDurability", boolean.class);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<Event> type = (Class) cls;
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, type, e -> onSickle(e, getPos, getState, getSickle, setCost));
            RtShop.LOGGER.info("[rtshop] Kaleidoscope Cookery 낫 연동을 켰습니다.");
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            // 그 모드가 없거나 버전이 달라 이벤트가 없음: 연동 없이 동작
        } catch (Throwable t) {
            RtShop.LOGGER.warn("[rtshop] 낫 연동을 켜지 못했습니다: {}", t.toString());
        }
    }

    private static void onSickle(Event e, Method getPos, Method getState, Method getSickle, Method setCost) {
        try {
            if (!(e instanceof PlayerEvent pe) || !(pe.getEntity() instanceof ServerPlayer sp)
                    || !(sp.level() instanceof ServerLevel level) || !e.isCancelable()) {
                return;
            }
            BlockPos pos = (BlockPos) getPos.invoke(e);
            BlockState state = (BlockState) getState.invoke(e);
            if (pos == null || state == null) {
                return;
            }
            if (!LandManager.can(sp, level, pos, LandData.BREAK)) {
                e.setCanceled(true);
                return;
            }
            if (FarmConfig.get().rightClickHarvest && harvestable(state)) {
                e.setCanceled(true);
                Object sickle = getSickle.invoke(e);
                harvestOne(sp, level, pos, state, sickle instanceof ItemStack s ? s : ItemStack.EMPTY);
                setCost.invoke(e, true);
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] sickle hook failed", t);
        }
    }
}
