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
        String[] pick = pickTarget(p);
        if (pick == null) {
            com.rtshop.notice.Notice.send(p, Component.translatable("item.rtshop.survey_compass.none").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.consume(stack);
        }
        String target = pick[0], label = com.rtshop.story.StoryText.t(p, pick[1]);
        Item eye = eyeFor(target);
        if (eye != null) {
            // 3.74.0 (사용자 W-11): Cataclysm 보스 건축물은 그 모드의 「눈」을 던져서 찾는다
            com.rtshop.notice.Notice.send(p, Component.translatable("item.rtshop.survey_compass.eye", label, new ItemStack(eye).getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.consume(stack);
        }
        BlockPos at = locate(p.serverLevel(), target, p.blockPosition());
        if (at == null) {
            com.rtshop.notice.Notice.send(p, Component.translatable(inThisDimension(p.serverLevel(), target) ? "item.rtshop.survey_compass.far" : "item.rtshop.survey_compass.dim", label)
                    .withStyle(ChatFormatting.RED), true);
        } else {
            double dx = at.getX() + 0.5 - p.getX(), dz = at.getZ() + 0.5 - p.getZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            int idx = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(-dx, dz)) / 45.0), 8);
            boolean ko = !"en".equals(com.rtshop.story.Loc.lang(p));
            com.rtshop.notice.Notice.send(p, Component.translatable("item.rtshop.survey_compass.found", label, ko ? DIRS[idx] : DIRS_EN[idx], dist)
                    .withStyle(ChatFormatting.GOLD), true);
            level.playSound(null, p.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1F, 1F);
        }
        return InteractionResultHolder.consume(stack);
    }

    /**
     * 나침반이 찾을 목표 {구조물, 이름}: 진행 중인 퀘스트의 아직 못 찾은 STRUCTURE 목표 가운데 첫째. Cataclysm 보스 건축물(눈으로 찾는 곳)은
     * 다른 목표가 있으면 건너뛴다 (그것뿐이면 그것을 돌려주고, 쓰는 쪽에서 눈을 쓰라고 알린다). 없으면 null.
     */
    public static String[] pickTarget(ServerPlayer p) {
        String[] eyeOnly = null;
        for (QuestDef q : QuestManager.activeQuests(p)) {
            int[] pg = QuestManager.progressOf(p, q.id);
            for (int i = 0; i < q.objectives.size(); i++) {
                QuestDef.Objective ob = q.objectives.get(i);
                if (ob.type != QuestDef.Type.STRUCTURE || !QuestDef.modOk(ob.mod)) continue;
                if (pg != null && i < pg.length && pg[i] >= ob.count) continue;
                String[] t = {ob.target, ob.text.isEmpty() ? ob.target : ob.text};
                if (eyeFor(ob.target) == null) return t;
                if (eyeOnly == null) eyeOnly = t;
            }
        }
        return eyeOnly;
    }

    /**
     * 3.74.0 (사용자 W-11 "cataclysm 모드의 보스 구조물은 조사대 나침반 대신 소환 아이템인 눈으로 찾아가도록"): 보스 건축물 → 그곳을 찾는 Cataclysm 의 눈
     * (엔더의 눈처럼 던지면 그쪽으로 날아간다). 자수정 둥지처럼 보스가 없는 곳은 나침반이 그대로 찾는다.
     */
    public static final java.util.Map<String, String> CATACLYSM_EYES = java.util.Map.of(
            "cataclysm:soul_black_smith", "cataclysm:monstrous_eye",
            "cataclysm:burning_arena", "cataclysm:flame_eye",
            "cataclysm:ruined_citadel", "cataclysm:void_eye",
            "cataclysm:sunken_city", "cataclysm:abyss_eye",
            "cataclysm:ancient_factory", "cataclysm:mech_eye",
            "cataclysm:cursed_pyramid", "cataclysm:desert_eye",
            "cataclysm:frosted_prison", "cataclysm:cursed_eye",
            "cataclysm:acropolis", "cataclysm:storm_eye");

    /** 이 건축물을 찾는 Cataclysm 의 눈 (그 아이템이 이 게임에 있을 때만). 아니면 null. */
    @Nullable
    public static Item eyeFor(String target) {
        String id = target == null ? null : CATACLYSM_EYES.get(target);
        if (id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(rl)) return null;
        return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl);
    }

    /** 살피는 반경(청크): 차례로 넓힌다. */
    public static final int[] SEARCH_CHUNKS = {200, 640, 1500};

    /** 이 차원에서 생길 수 있는 건축물인가 (아니면 "다른 차원에서 써 보라"고 알린다). */
    public static boolean inThisDimension(ServerLevel level, String target) {
        var set = com.rtshop.story.StructureFinder.resolve(level, target);
        if (set == null || set.isEmpty()) return false;
        var state = level.getChunkSource().getGeneratorState();
        for (var h : set) if (!state.getPlacementsForStructure(h).isEmpty()) return true;
        return false;
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
        // 가까운 데부터 차례로 넓혀 본다 (약 3천 → 1만 → 2만 4천 칸). 드문 건축물도 찾되, 한 번 쓰는 데 드는 시간은 묶어 둔다.
        long t0 = System.nanoTime();
        for (int r : SEARCH_CHUNKS) {
            var f = com.rtshop.story.StructureFinder.find(level, set, from, r, MIN_TOWN);
            if (f != null) return f.pos;
            if (System.nanoTime() - t0 > 4_000_000_000L) break;
        }
        return null;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.rtshop.survey_compass.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.rtshop.survey_compass.desc2").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tip.add(Component.translatable("rtshop.story_item.tool_header").withStyle(ChatFormatting.GOLD));
        tip.add(Component.translatable("item.rtshop.survey_compass.desc3").withStyle(ChatFormatting.AQUA));
        if (net.minecraftforge.fml.ModList.get().isLoaded("cataclysm")) {
            tip.add(Component.translatable("item.rtshop.survey_compass.desc4").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
