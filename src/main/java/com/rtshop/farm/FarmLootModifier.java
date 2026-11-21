package com.rtshop.farm;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.rtshop.RtShop;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * 전역 전리품 수정자.
 *  1) 모든 전리품(작물, 풀, 상자 등)에서 씨앗을 제거한다.
 *  2) 플레이어가 상점 씨앗으로 심어 다 자란 작물을 직접 수확했을 때 수확물에 1~3별 등급을 매긴다.
 *  3) 수확된 자리의 "심은 기록"을 지운다 (다시 심으려면 씨앗이 또 필요하다).
 */
public class FarmLootModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RtShop.MODID);

    public static final Supplier<Codec<FarmLootModifier>> CODEC = Suppliers.memoize(
            () -> RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, FarmLootModifier::new)));

    static {
        SERIALIZERS.register("farm", CODEC);
    }

    public FarmLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        try {
            FarmConfig cfg = FarmConfig.get();
            if (cfg.removeSeedDrops) {
                loot.removeIf(SeedRules::isPureSeed);
            }
            if (!ctx.hasParam(LootContextParams.BLOCK_STATE)) {
                return loot;
            }
            BlockState state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (state != null && com.rtshop.job.JobPassives.isOre(state)
                    && ctx.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof ServerPlayer miner && !miner.isCreative()) {
                // 광부: 광석을 캘 때 일정 확률로 추가 드롭 (실크 터치로 광석 블록 그대로 캔 경우는 제외)
                double chance = com.rtshop.job.Jobs.param(miner, "miner", "extraOre");
                boolean silk = false;
                for (ItemStack s : loot) {
                    silk |= s.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == state.getBlock();
                }
                if (chance > 0 && !silk && !loot.isEmpty() && ctx.getRandom().nextDouble() < chance) {
                    ItemStack extra = loot.get(0).copy();
                    extra.setCount(1);
                    loot.add(extra);
                }
            }
            if (state == null || !FarmEvents.isSeedCrop(state)) {
                return loot;
            }
            // 이 작물이 상점 씨앗으로 심은 것인지 확인하고, 수확됐으므로 기록을 지운다
            boolean licensed = true;
            if (cfg.requireShopSeeds) {
                licensed = false;
                Vec3 origin = ctx.getParamOrNull(LootContextParams.ORIGIN);
                ServerLevel level = ctx.getLevel();
                if (origin != null && level != null) {
                    CropData data = CropData.get(level);
                    String dim = CropData.dim(level);
                    BlockPos base = data.findMark(dim, BlockPos.containing(origin));
                    if (base != null) {
                        licensed = true;
                        data.unmark(dim, base);
                    }
                }
            }
            Entity breaker = ctx.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (cfg.starsEnabled && licensed && breaker instanceof ServerPlayer sp && !sp.isCreative() && SeedRules.isMatureCrop(state)) {
                double starUp = com.rtshop.job.Jobs.param(sp, "farmer", "starUp");
                ItemStack first = null;
                for (ItemStack s : loot) {
                    if (!s.isEmpty() && !SeedRules.isPureSeed(s) && SeedRules.star(s) == 0) {
                        int star = SeedRules.rollStar(ctx.getRandom());
                        // 농부: 일정 확률로 별 등급 한 단계 위
                        if (star < 3 && starUp > 0 && ctx.getRandom().nextDouble() < starUp) {
                            star++;
                        }
                        SeedRules.setStar(s, star);
                        if (first == null) {
                            first = s;
                        }
                    }
                }
                double extra = com.rtshop.job.Jobs.param(sp, "farmer", "extraHarvest");
                if (first != null && extra > 0 && ctx.getRandom().nextDouble() < extra) {
                    ItemStack more = first.copy();
                    more.setCount(1);
                    loot.add(more);
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] loot modifier error", t);
        }
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
