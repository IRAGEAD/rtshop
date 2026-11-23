package com.rtshop.item;

import com.mojang.datafixers.util.Pair;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 조사대 나침반: 쓰면 진행 중인 퀘스트가 찾는 구조물 가운데 가장 가까운 것의 방향과 거리를 알려 준다 (/locate 와 같은 방법).
 * 한 번 쓰면 잠시 쉰다. 이미 다녀온 목표는 건너뛴다.
 */
public class SurveyCompassItem extends Item {
    public SurveyCompassItem(Properties p) {
        super(p);
    }

    private static final String[] DIRS = {"남", "남서", "서", "북서", "북", "북동", "동", "남동"};
    private static final String[] DIRS_EN = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResultHolder.success(stack);
        player.getCooldowns().addCooldown(this, 100);
        String target = null, label = null;
        for (QuestDef q : QuestManager.activeQuests(p)) {
            int[] pg = QuestManager.progressOf(p, q.id);
            for (int i = 0; i < q.objectives.size(); i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type != QuestDef.Type.STRUCTURE || !QuestDef.modOk(ob.mod)) continue;
                if (pg != null && i < pg.length && pg[i] >= ob.count) continue;
                target = ob.target;
                label = ob.text.isEmpty() ? ob.target : ob.text;
                break;
            }
            if (target != null) break;
        }
        if (target == null) {
            p.displayClientMessage(Component.translatable("item.rtshop.survey_compass.none").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(stack);
        }
        BlockPos at = locate(p.serverLevel(), target, p.blockPosition());
        if (at == null) {
            p.displayClientMessage(Component.translatable("item.rtshop.survey_compass.far", label).withStyle(ChatFormatting.RED), true);
        } else {
            double dx = at.getX() + 0.5 - p.getX(), dz = at.getZ() + 0.5 - p.getZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            int idx = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
            boolean ko = !"en".equals(com.rtshop.story.Loc.lang(p));
            p.displayClientMessage(Component.translatable("item.rtshop.survey_compass.found", label, ko ? DIRS[idx] : DIRS_EN[idx], dist)
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, p.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1F, 1F);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** 구조물 ID, #태그 또는 "모드:*" 의 가장 가까운 자리 (반경 96 청크). 없으면 null. */
    public static BlockPos locate(ServerLevel level, String target, BlockPos from) {
        Registry<Structure> reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        HolderSet<Structure> set = null;
        if (target.startsWith("#")) {
            set = reg.getTag(TagKey.create(Registries.STRUCTURE, new ResourceLocation(target.substring(1)))).orElse(null);
        } else if (target.endsWith(":*")) {
            String ns = target.substring(0, target.length() - 2);
            List<Holder<Structure>> hs = reg.holders().filter(h -> h.key().location().getNamespace().equals(ns)).map(h -> (Holder<Structure>) h).toList();
            set = hs.isEmpty() ? null : HolderSet.direct(hs);
        } else {
            var h = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(target)));
            set = h.isPresent() ? HolderSet.direct(h.get()) : null;
        }
        if (set == null) return null;
        Pair<BlockPos, Holder<Structure>> r = level.getChunkSource().getGenerator().findNearestMapStructure(level, set, from, 96, false);
        return r == null ? null : r.getFirst();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.survey_compass.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop.survey_compass.desc2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
