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

    /** 마을(성 영역)에서 최소 이만큼 떨어진 건축물만 안내한다. */
    public static final double MIN_TOWN = 1000;

    /**
     * 구조물 ID, #태그 또는 "모드:*" 가운데 실제로 생긴 가장 가까운 자리 (반경 200 청크, 마을에서 1000블럭 이상).
     * 없으면 null. 생기지 않은 자리(다른 모드·단계 잠금·지형 조건)는 알려 주지 않는다.
     */
    public static BlockPos locate(ServerLevel level, String target, BlockPos from) {
        var set = com.rtshop.story.StructureFinder.resolve(level, target);
        if (set == null) return null;
        var f = com.rtshop.story.StructureFinder.find(level, set, from, 200, MIN_TOWN);
        return f == null ? null : f.pos;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.survey_compass.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop.survey_compass.desc2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatable("rtshop.story_item.tool_header").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.rtshop.survey_compass.desc3").withStyle(ChatFormatting.AQUA));
    }
}
