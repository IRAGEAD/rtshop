package com.rtshop.story;

import com.rtshop.RtShop;
import com.rtshop.job.JobConfig;
import com.rtshop.quest.QuestData;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 히든 직업으로 가는 숨은 길 (3.76.0, ⑤ — 사용자: "히든 퀘스트와 직업으로의 접근이 너무 쉽거나, 모두가 거의 확정으로 받을 수 있어서
 * 모두가 히든 직업을 가지게 되는 일이 없도록"). 문은 네 겹이다.
 * <ol>
 * <li>4장 「받침대 안쪽」(story_4_16)에서 덧쓴 마지막 줄을 기록관에게 읽어 주는가(밝힘 hj_reveal) · 숨기는가(숨김 hj_conceal). 한 사람에 하나뿐.
 *     그 전에 마친 사람은 기록관의 짧은 퀘스트(hidden_jobs_0)로 같은 선택을 한다.</li>
 * <li>숨은 사람이 정해진 때에만, 낮은 확률로 나타난다 — 숨김: 밤에 룬 제단 곁에 서 있을 때 한 밤에 한 번 (기본 15%),
 *     밝힘: 비 오는 날 기록관과 이야기할 때 하루에 한 번 (기본 30%), 또는 거래소의 봉랍 확인으로 위조 증서를 처음 밝혀냈을 때.</li>
 * <li>값을 치르고, 틀릴 수 있는 시험을 본다 (틀리면 hiddenRetryDays 일 동안 다시 볼 수 없다).</li>
 * <li>직업을 고를 때 서버의 인원 한도 (기본 위조자 2 · 조사관 2, Jobs.choose).</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class HiddenJobs {
    private HiddenJobs() {
    }

    public static final String REVEAL = "hj_reveal", CONCEAL = "hj_conceal";
    public static final String FORGER_SEEN = "hj_forger_seen", INSPECTOR_SEEN = "hj_inspector_seen";
    public static final String FORGER_PASSED = "hj_forger_passed", INSPECTOR_PASSED = "hj_inspector_passed";
    public static final String FORGER_FAILED = "hj_forger_failed", INSPECTOR_FAILED = "hj_inspector_failed";
    public static final String FORGER_TRIAL = "hidden_forger_2", INSPECTOR_TRIAL = "hidden_inspector_2";
    /** 선택을 하는 퀘스트 (4장의 받침대 · 그 전에 마친 사람의 기록관 퀘스트). */
    public static final String CHOICE_QUEST = "story_4_16", MAKEUP_QUEST = "hidden_jobs_0";
    private static final String NIGHT_TAG = "rtshop_hj_night", RAIN_TAG = "rtshop_hj_rain", FAIL_TAG = "rtshop_hj_fail_";
    private static final Random RNG = new Random();
    /** 점검용: 다음 굴림 (null = 무작위) · 시계를 미는 몫(ms). */
    public static volatile Double testRoll;
    public static long testShiftMs;

    static double roll() {
        Double t = testRoll;
        return t != null ? t : RNG.nextDouble();
    }

    static long now() {
        return System.currentTimeMillis() + testShiftMs;
    }

    // ------------------------------------------------------------------ 퀘스트를 마칠 때

    /** QuestManager.turnIn 이 부른다 (보상 · 선택의 표식을 준 뒤). */
    public static void afterTurnIn(ServerPlayer p, QuestDef q, int choice) {
        String id = q.id;
        if (id.equals(CHOICE_QUEST) || id.equals(MAKEUP_QUEST)) {
            // 고른 쪽의 작은 낌새 (어디로 가라고 하지 않는다)
            if (QuestManager.hasFlag(p, CONCEAL)) {
                say(p, StoryText.pick(p, "(밑판을 도로 밀어 넣는 손끝에 보랏빛이 묻었다. 닦아도 지워지지 않는다. 밤이 되면 번질 것 같은 색이다.)",
                        "(As you push the plate back, a little violet stays on your fingertips. It will not wipe off. The kind of colour that spreads at night.)"), ChatFormatting.DARK_PURPLE);
            } else if (QuestManager.hasFlag(p, REVEAL)) {
                say(p, StoryText.pick(p, "(이사벨이 그 줄을 수첩에 옮겨 적는다. \"비 오는 날 기록고 정리를 도와주시면, 보여 드릴 게 있어요.\")",
                        "(Isabel copies the line into her notebook. \"If you help me tidy the archive on a rainy day, there is something I'd like to show you.\")"), ChatFormatting.AQUA);
            }
            return;
        }
        if (id.equals(FORGER_TRIAL)) trial(p, q, FORGER_PASSED, FORGER_FAILED, "forger");
        else if (id.equals(INSPECTOR_TRIAL)) trial(p, q, INSPECTOR_PASSED, INSPECTOR_FAILED, "inspector");
    }

    /** 시험: 맞혔으면 알리고 끝. 틀렸으면 그 퀘스트를 '안 한 것'으로 되돌리고, 다시 볼 수 있을 때까지 막는다. */
    private static void trial(ServerPlayer p, QuestDef q, String passed, String failed, String kind) {
        if (QuestManager.hasFlag(p, passed)) {
            say(p, StoryText.pick(p, kind.equals("forger") ? "(연습장의 보랏빛 글씨가 또렷해진다. 다음 장이 저절로 넘어간다.)" : "(이사벨이 고개를 끄덕인다. 서랍 맨 안쪽의 놋쇠가 조금 더 밝아 보인다.)",
                    kind.equals("forger") ? "(The violet writing in the practice book sharpens. The next page turns by itself.)"
                            : "(Isabel nods. The brass at the back of the drawer seems a little brighter.)"), ChatFormatting.GOLD);
            return;
        }
        if (!QuestManager.hasFlag(p, failed)) return;
        QuestData.Progress pr = QuestManager.progressOf(p);
        pr.completed.remove(q.id);
        QuestData.get(p.server).dirty();
        p.getPersistentData().putLong(FAIL_TAG + kind, now());
        int days = JobConfig.get().hiddenRetryDays;
        say(p, StoryText.pick(p, kind.equals("forger") ? "(연습장이 스르르 닫힌다. 표지에 뒤집힌 글씨가 떠오른다: \"이레 뒤에.\") — " + days + "일 뒤에 다시 볼 수 있습니다."
                        : "(이사벨이 쪽을 거둔다. \"틀렸어요. 규칙은 규칙이에요. 이레 뒤에 다시 봐요.\") — " + days + "일 뒤에 다시 볼 수 있습니다.",
                kind.equals("forger") ? "(The practice book closes on its own. Reversed letters rise on the cover: \"In seven days.\") — You can try again in " + days + " days."
                        : "(Isabel gathers the pages. \"Wrong. Rules are rules. Try again in seven days.\") — You can try again in " + days + " days."), ChatFormatting.RED);
    }

    /** 틀린 시험의 막힘이 풀렸으면 풀어 준다 (접속할 때 · 1분마다). */
    public static void clearExpired(ServerPlayer p) {
        long span = JobConfig.get().hiddenRetryDays * 86_400_000L;
        for (String kind : new String[]{"forger", "inspector"}) {
            String failed = kind.equals("forger") ? FORGER_FAILED : INSPECTOR_FAILED;
            if (!QuestManager.hasFlag(p, failed)) continue;
            long at = p.getPersistentData().getLong(FAIL_TAG + kind);
            if (at <= 0 || now() - at >= span) {
                QuestManager.setFlag(p, failed, false);
                p.getPersistentData().remove(FAIL_TAG + kind);
                say(p, StoryText.pick(p, kind.equals("forger") ? "(어딘가에서 연습장이 다시 열리는 소리가 들린 것 같다.)" : "(기록고 서랍의 자물쇠가 다시 풀린 것 같다.)",
                        kind.equals("forger") ? "(Somewhere, a practice book seems to fall open again.)" : "(Somewhere in the archive, a drawer lock seems to give again.)"), ChatFormatting.GRAY);
            }
        }
    }

    // ------------------------------------------------------------------ 숨은 사람이 나타나는 때

    /** 숨김을 고른 사람: 밤에 룬 제단 곁에 있으면 한 밤에 한 번 굴린다. */
    public static boolean nightAtAltar(ServerPlayer p) {
        if (!QuestManager.hasFlag(p, CONCEAL) || QuestManager.hasFlag(p, FORGER_SEEN)) return false;
        ServerLevel level = p.serverLevel();
        if (level.dimension() != Level.OVERWORLD) return false;
        long t = level.getDayTime();
        long tod = Math.floorMod(t, 24000L);
        if (tod < 13000 || tod > 23000 || !nearAltar(p)) return false;
        long night = Math.floorDiv(t, 24000L);
        if (p.getPersistentData().getLong(NIGHT_TAG) == night + 1) return false;   // 이 밤에는 이미 굴렸다 (0 과 구별하려고 +1)
        p.getPersistentData().putLong(NIGHT_TAG, night + 1);
        if (roll() >= JobConfig.get().hiddenNightChance) return false;
        QuestManager.setFlag(p, FORGER_SEEN, true);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1F, 0.6F);
        say(p, StoryText.pick(p, "받침대의 빈 홈 하나에서 보랏빛 글씨가 번져 나온다. 거울 글씨다. 손바닥에 비추어 읽는다: \"행운 상인의 서랍. 셋째 칸.\"",
                "Violet writing seeps out of one of the empty grooves in the pedestal. Mirror script. You read it reflected in your palm: \"The luck merchant's drawer. The third one.\""), ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** 이 사람 곁(가로 6칸 · 세로 4칸)에 룬 제단이 있는가. 마을의 제단을 정해 두었으면 그것만. */
    /** 점검용: 제단 곁인지를 정해 둔다 (null = 실제로 본다). */
    public static volatile Boolean testNearAltar;

    static boolean nearAltar(ServerPlayer p) {
        Boolean t = testNearAltar;
        if (t != null) return t;
        try {
            com.rtshop.rules.WorldRules w = com.rtshop.rules.WorldRules.get();
            if (w.altarSet) {
                return p.level().dimension().location().toString().equals(w.altarDim)
                        && Math.abs(p.getX() - (w.altarX + 0.5)) <= 6 && Math.abs(p.getZ() - (w.altarZ + 0.5)) <= 6 && Math.abs(p.getY() - w.altarY) <= 4;
            }
            BlockPos c = p.blockPosition();
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
                if (p.level().getBlockState(q).is(com.rtshop.magic.MagicRegistry.ORB.get())) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 밝힘을 고른 사람: 비 오는 날 기록관과 이야기하면 하루에 한 번 굴린다 (Dialogue 가 부른다). */
    public static boolean talkedTo(ServerPlayer p, String npcKey) {
        if (!"quest_order_archivist".equals(npcKey) || !QuestManager.hasFlag(p, REVEAL) || QuestManager.hasFlag(p, INSPECTOR_SEEN)) return false;
        if (!p.server.overworld().getLevelData().isRaining()) return false;   // 비가 오는 날 (빗줄기가 짙어지기를 기다리지 않는다)
        long day = Math.floorDiv(now(), 86_400_000L);
        if (p.getPersistentData().getLong(RAIN_TAG) == day + 1) return false;
        p.getPersistentData().putLong(RAIN_TAG, day + 1);
        if (roll() >= JobConfig.get().hiddenRainChance) return false;
        return inspectorSeen(p);
    }

    /** 밝힘을 고른 사람이 거래소의 봉랍 확인으로 위조 증서를 처음 밝혀냈다 (Forgery.check 가 부른다). */
    public static boolean sealCheckFound(ServerPlayer p) {
        if (!QuestManager.hasFlag(p, REVEAL) || QuestManager.hasFlag(p, INSPECTOR_SEEN)) return false;
        return inspectorSeen(p);
    }

    private static boolean inspectorSeen(ServerPlayer p) {
        QuestManager.setFlag(p, INSPECTOR_SEEN, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHISELED_BOOKSHELF_PICKUP, SoundSource.PLAYERS, 1F, 0.9F);
        say(p, StoryText.pick(p, "기록고의 잠긴 서랍 하나가 열려 있다. 젖은 종이를 말리려고 이사벨이 연 것이다. 서랍 맨 안쪽에서 놋쇠 테가 반짝인다.",
                "One of the locked drawers in the archive stands open; Isabel opened it to dry damp papers. At the very back, a brass rim glints."), ChatFormatting.AQUA);
        return true;
    }

    private static void say(ServerPlayer p, String text, ChatFormatting color) {
        com.rtshop.notice.Notice.send(p, Component.literal(text).withStyle(color));
    }

    // ------------------------------------------------------------------ 시계

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server == null) return;
        int t = server.getTickCount();
        if (t % 40 != 13) return;
        boolean minute = t % 1200 == 13;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p instanceof FakePlayer) continue;
            try {
                nightAtAltar(p);
                if (minute) clearExpired(p);
            } catch (Throwable ex) {
                RtShop.LOGGER.debug("[rtshop] hidden jobs", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer)) {
            try {
                clearExpired(p);
            } catch (Throwable ignored) {
            }
        }
    }

    /** 점검용: 시험에서 틀린 때를 옮긴다. */
    public static void failedAtForTest(ServerPlayer p, String kind, long at) {
        CompoundTag d = p.getPersistentData();
        d.putLong(FAIL_TAG + kind, at);
    }
}
