package com.rtshop.farm;

import com.rtshop.RtShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 농사 규칙.
 *  - 씨앗은 상점에서 산 것(rtshop_seed 표시가 있는 것)만 심을 수 있다.
 *  - 당근·감자처럼 작물이 곧 씨앗인 것은 수확물을 심을 수 없고, 상점의 "심기 전용" 품목만 심을 수 있다.
 *  - 작물에는 1~3별 등급(rtshop_star)이 붙는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class SeedRules {
    private SeedRules() {
    }

    public static final String SEED_TAG = "rtshop_seed";
    public static final String STAR_TAG = "rtshop_star";

    private static String id(ItemStack s) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(s.getItem());
        return rl == null ? "" : rl.toString();
    }

    /** 밭에 심는 씨앗류 아이템인가 (순수 씨앗 + 작물 겸 씨앗). */
    public static boolean isPlantable(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        FarmConfig cfg = FarmConfig.get();
        String id = id(s);
        if (cfg.notSeeds.contains(id)) {
            return false;
        }
        if (cfg.extraSeeds.contains(id) || cfg.dualPurpose.contains(id)) {
            return true;
        }
        try {
            if (s.is(Tags.Items.SEEDS)) {
                return true;
            }
            Item item = s.getItem();
            if (item instanceof BlockItem bi) {
                Block b = bi.getBlock();
                if (b instanceof CropBlock || b instanceof StemBlock || b instanceof NetherWartBlock
                        || b instanceof CocoaBlock || b instanceof SweetBerryBushBlock) {
                    return true;
                }
                return b.defaultBlockState().is(BlockTags.CROPS);
            }
        } catch (Throwable ignored) {
            // 태그가 아직 준비되지 않은 시점 등
        }
        return false;
    }

    /** 작물이면서 씨앗 역할도 하는 아이템인가. */
    public static boolean isDual(ItemStack s) {
        if (FarmConfig.get().dualPurpose.contains(id(s))) {
            return true;
        }
        return s.isEdible() && isPlantable(s);
    }

    /** 씨앗 역할만 하는 아이템인가 (드롭에서 제거하는 대상). */
    public static boolean isPureSeed(ItemStack s) {
        return isPlantable(s) && !isDual(s) && !isLicensed(s);
    }

    public static boolean isLicensed(ItemStack s) {
        CompoundTag t = s.getTag();
        return t != null && t.getBoolean(SEED_TAG);
    }

    /** 상점 판매용 씨앗으로 표시한다. 작물 겸 씨앗은 이름도 "… (심기 전용)" 으로 바꾼다. */
    public static ItemStack license(ItemStack s) {
        ItemStack out = s.copy();
        boolean dual = isDual(s);
        CompoundTag t = out.getOrCreateTag();
        t.putBoolean(SEED_TAG, true);
        if (dual) {
            CompoundTag display = t.getCompound("display");
            display.putString("Name", "{\"translate\":\"rtshop.seed.named\",\"italic\":false,\"with\":[{\"translate\":\""
                    + s.getItem().getDescriptionId() + "\"}]}");
            t.put("display", display);
        }
        return out;
    }

    // ------------------------------------------------------------------ 별 등급

    public static int star(ItemStack s) {
        CompoundTag t = s.getTag();
        if (t == null || !t.contains(STAR_TAG)) {
            return 0;
        }
        return Math.max(0, Math.min(3, t.getInt(STAR_TAG)));
    }

    public static void setStar(ItemStack s, int star) {
        s.getOrCreateTag().putInt(STAR_TAG, Math.max(1, Math.min(3, star)));
    }

    public static int rollStar(RandomSource random) {
        double[] ch = FarmConfig.get().starChances;
        double total = ch[0] + ch[1] + ch[2];
        if (total <= 0) {
            return 1;
        }
        double r = random.nextDouble() * total;
        if (r < ch[0]) {
            return 1;
        }
        return r < ch[0] + ch[1] ? 2 : 3;
    }

    public static String starText(int star) {
        return "★".repeat(Math.max(0, star)) + "☆".repeat(Math.max(0, 3 - star));
    }

    /** 다 자란 작물 블록인가 (별 등급을 매기는 대상). 호박·수박 블록처럼 다시 놓을 수 있는 것은 제외. */
    public static boolean isMatureCrop(BlockState state) {
        Block b = state.getBlock();
        try {
            if (b instanceof CropBlock crop) {
                return crop.isMaxAge(state);
            }
            boolean cropLike = b instanceof NetherWartBlock || b instanceof CocoaBlock || b instanceof SweetBerryBushBlock
                    || state.is(BlockTags.CROPS);
            if (!cropLike) {
                return false;
            }
            for (Property<?> p : state.getProperties()) {
                if (p instanceof IntegerProperty ip && p.getName().equals("age")) {
                    int max = 0;
                    for (int v : ip.getPossibleValues()) {
                        max = Math.max(max, v);
                    }
                    return state.getValue(ip) >= max;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        FarmConfig cfg = FarmConfig.get();
        if (!cfg.requireShopSeeds) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || player.isCreative() || !isPlantable(stack) || isLicensed(stack)) {
            return;
        }
        // 아이템 사용(심기)만 막고, 상자 열기 같은 블록 상호작용은 그대로 둔다
        event.setUseItem(Event.Result.DENY);
        BlockState target = event.getLevel().getBlockState(event.getPos());
        boolean soil = target.getBlock() instanceof FarmBlock || target.is(Blocks.SOUL_SAND) || target.is(BlockTags.DIRT)
                || target.is(BlockTags.JUNGLE_LOGS) || target.is(Blocks.MUD);
        if (event.getLevel().isClientSide) {
            if (soil) {
                player.displayClientMessage(Component.translatable(
                        isDual(stack) ? "rtshop.msg.need_shop_seed_dual" : "rtshop.msg.need_shop_seed"), true);
            }
        } else if (soil && player instanceof ServerPlayer sp) {
            sp.inventoryMenu.sendAllDataToRemote();
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (FarmConfig.get().blockEatingShopSeeds && isLicensed(stack) && stack.isEdible() && !event.getEntity().isCreative()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (event.getLevel().isClientSide) {
                event.getEntity().displayClientMessage(Component.translatable("rtshop.msg.seed_not_food"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        ItemStack stack = event.getItem();
        if (FarmConfig.get().blockEatingShopSeeds && isLicensed(stack) && stack.isEdible()
                && !(event.getEntity() instanceof Player p && p.isCreative())) {
            event.setCanceled(true);
        }
    }
}
