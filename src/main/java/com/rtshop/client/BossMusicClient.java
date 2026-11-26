package com.rtshop.client;

import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 보스·레이드 음악 (클라이언트): 서버가 알려 준 곡(rtshop:music.&lt;이름&gt;)을 이어서 틀고, 싸움이 끝나면 잦아들게 한다.
 * 곡이 흐르는 동안에는 게임의 배경 음악을 멈춰 두고, (설정에 따라) 다른 모드의 보스 음악도 막는다.
 * 음량은 게임 설정의 '음악' 슬라이더를 따른다. 그 이름의 소리가 없으면 아무것도 하지 않는다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class BossMusicClient {
    private BossMusicClient() {
    }

    /** 이어서 흐르는 곡 하나: 음량을 천천히 올리고 내린다. */
    static final class Track extends AbstractTickableSoundInstance {
        final String name;
        float target = 1F;
        float step = 0.05F;

        Track(String name, ResourceLocation id) {
            super(SoundEvent.createVariableRangeEvent(id), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.name = name;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            if (volume < target) volume = Math.min(target, volume + step);
            else if (volume > target) volume = Math.max(target, volume - step);
            if (target <= 0F && volume <= 0.001F) stop();
        }

        void fadeOut() {
            target = 0F;
        }
    }

    private static Track current;
    private static int entityId = -1;
    private static int range = 48;
    private static boolean muteOthers = true;
    private static float step = 0.02F;

    public static String playingForTest() {
        return current == null || current.isStopped() ? "" : current.name;
    }

    /** 이 이름의 곡(소리)이 이 클라이언트에 있는가. */
    public static boolean has(String track) {
        return !track.isEmpty() && Minecraft.getInstance().getSoundManager().getSoundEvent(id(track)) != null;
    }

    static ResourceLocation id(String track) {
        return new ResourceLocation(RtShop.MODID, "music." + track.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_"));
    }

    /** 서버가 알려 준 곡으로 바꾼다 ("" = 멈춤). */
    public static void receive(JsonObject o) {
        String track = o.has("track") ? o.get("track").getAsString() : "";
        entityId = o.has("entity") ? o.get("entity").getAsInt() : -1;
        range = o.has("range") ? o.get("range").getAsInt() : 48;
        muteOthers = !o.has("muteOthers") || o.get("muteOthers").getAsBoolean();
        double fade = o.has("fade") ? o.get("fade").getAsDouble() : 2.5;
        step = fade <= 0 ? 1F : (float) Math.max(0.005, 1.0 / (fade * 20.0));
        play(track);
    }

    private static void play(String track) {
        Minecraft mc = Minecraft.getInstance();
        if (current != null && !current.isStopped() && current.name.equals(track) && current.target > 0F) return; // 같은 곡이 이미 흐른다
        if (current != null) {
            current.step = step;
            current.fadeOut();
            current = null;
        }
        if (track.isEmpty() || !has(track)) return;
        Track t = new Track(track, id(track));
        t.step = step;
        current = t;
        if (muteOthers) stopOthers(BACKGROUND); // 3.63: 우리 곡이 시작될 때 이미 흐르던 다른 모드의 보스 음악도 멈춘다 (전에는 새로 트는 것만 막았다)
        mc.getSoundManager().play(t);
    }

    // ------------------------------------------------------------------ 3.63: 음악이 겹치지 않게
    /** 다른 모드가 트는 소리의 갈래: 음악 아님 / 배경 음악(게임의 음악 관리자가 트는 식의 평범한 소리) / 보스 음악(그 밖의 음악). */
    static final int NOT_MUSIC = 0, BACKGROUND = 1, BOSS = 2;

    private static final class Other {
        final SoundInstance sound;
        final int kind;
        final long born;

        Other(SoundInstance sound, int kind, long born) {
            this.sound = sound;
            this.kind = kind;
            this.born = born;
        }
    }

    /** 지금 흐르는 다른 모드의 음악들 (끝난 것은 틱마다 치운다). */
    private static final java.util.List<Other> OTHERS = new java.util.ArrayList<>();
    private static long ticks;
    /** 점검용: 겹치지 않게 하려고 틀지 않은 음악 수 / 멈춘 음악 수. */
    public static int deniedForTest, stoppedForTest;

    /**
     * 이 소리가 어떤 음악인가. 이름에 music · theme · bgm 이 든 다른 모드의 소리만 음악으로 본다.
     * 주크박스(RECORDS)와 게임 본래의 소리, 우리 소리는 건드리지 않는다. plain = 게임의 음악 관리자가 쓰는 평범한 소리(SimpleSoundInstance)인가.
     */
    static int kindOf(ResourceLocation loc, SoundSource source, boolean plain) {
        if (loc == null || source == SoundSource.RECORDS) return NOT_MUSIC;
        if (loc.getNamespace().equals(RtShop.MODID) || loc.getNamespace().equals("minecraft")) return NOT_MUSIC;
        String path = loc.getPath();
        if (!path.contains("music") && !path.contains("theme") && !path.contains("bgm")) return NOT_MUSIC;
        return plain && source == SoundSource.MUSIC ? BACKGROUND : BOSS;
    }

    /**
     * 새로 틀려는 음악을 어떻게 할지: 0 = 그대로 튼다, 1 = 틀지 않는다, 2 = 틀고, 먼저 흐르던 배경 음악은 멈춘다.
     * ours = 우리 보스 곡이 흐르는 중(그리고 다른 음악을 막기로 함), bossPlaying = 다른 모드의 보스 음악이 이미 흐르는 중, one = 한 번에 하나만(client.json 의 oneBossMusic).
     * 보스 음악은 먼저 시작한 쪽이 이긴다: 나중 것을 틀고 먼저 것을 끄면, 틱마다 다시 틀려는 모드 둘이 서로를 끄며 번갈아 끊긴다.
     */
    static int decide(int kind, boolean ours, boolean bossPlaying, boolean one) {
        if (kind == NOT_MUSIC) return 0;
        if (ours) return 1;
        if (!one) return 0;
        return bossPlaying ? 1 : 2;
    }

    public static int decideForTest(String sound, String source, boolean plain, boolean ours, boolean bossPlaying, boolean one) {
        return decide(kindOf(ResourceLocation.tryParse(sound), SoundSource.valueOf(source), plain), ours, bossPlaying, one);
    }

    private static void prune() {
        Minecraft mc = Minecraft.getInstance();
        OTHERS.removeIf(o -> ticks - o.born > 5 && !mc.getSoundManager().isActive(o.sound));
    }

    private static boolean otherBossPlaying(SoundInstance except) {
        for (Other o : OTHERS) if (o.kind == BOSS && o.sound != except) return true;
        return false;
    }

    /** 흐르고 있는 다른 모드의 음악 가운데 이 갈래 이상인 것을 멈춘다 (BACKGROUND = 전부, BOSS = 보스 음악만). */
    private static void stopOthers(int least) {
        Minecraft mc = Minecraft.getInstance();
        for (Other o : new java.util.ArrayList<>(OTHERS)) {
            if (o.kind < least) continue;
            mc.getSoundManager().stop(o.sound);
            OTHERS.remove(o);
            stoppedForTest++;
        }
    }

    private static void stopBackgrounds(SoundInstance except) {
        Minecraft mc = Minecraft.getInstance();
        for (Other o : new java.util.ArrayList<>(OTHERS)) {
            if (o.kind != BACKGROUND || o.sound == except) continue;
            mc.getSoundManager().stop(o.sound);
            OTHERS.remove(o);
            stoppedForTest++;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        if (!OTHERS.isEmpty()) {
            if (mc.level == null) OTHERS.clear();
            else {
                prune();
                // 다른 모드의 보스 음악이 흐르는 동안 게임의 배경 음악은 쉰다 (그 모드가 스스로 멈추지 않는 경우가 있다)
                if (ClientConfig.get().oneBossMusic && otherBossPlaying(null)) mc.getMusicManager().stopPlaying();
            }
        }
        if (current == null) return;
        if (mc.level == null || mc.player == null || current.isStopped()) {
            if (mc.level == null) mc.getSoundManager().stop(current);
            current = null;
            return;
        }
        mc.getMusicManager().stopPlaying(); // 우리 곡이 흐르는 동안 배경 음악은 쉰다
        // 보스가 사라졌거나 멀어졌으면 서버의 알림을 기다리지 않고 잦아들게 한다
        if (entityId >= 0) {
            Entity boss = mc.level.getEntity(entityId);
            if (boss == null || !boss.isAlive() || boss.distanceToSqr(mc.player) > (double) (range + 16) * (range + 16)) {
                current.step = step;
                current.fadeOut();
                current = null;
            }
        }
    }

    /**
     * 음악이 겹치지 않게 한다 (3.63).
     * 우리 곡이 흐르는 동안에는 다른 모드의 음악을 틀지 않게 하고(예전부터), 우리 곡이 없을 때에도 다른 모드의 보스 음악은 한 번에 하나만 흐르게 한다:
     * 보스 음악이 흐르는 중이면 새 음악(다른 보스 음악 · 배경 음악)은 틀지 않고, 보스 음악이 시작될 때 흐르던 배경 음악은 멈춘다.
     */
    @SubscribeEvent
    public static void onOtherMusic(PlaySoundEvent e) {
        SoundInstance snd = e.getSound();
        if (snd == null || snd == current) return;
        try {
            boolean ours = muteOthers && current != null && !current.isStopped();
            int kind = kindOf(snd.getLocation(), snd.getSource(), snd instanceof net.minecraft.client.resources.sounds.SimpleSoundInstance);
            if (kind == NOT_MUSIC) {
                // 예전 규칙 그대로: 우리 곡이 흐르는 동안에는 이름이 음악인 다른 모드의 소리를 (주크박스라도) 틀지 않는다
                if (ours && snd.getLocation() != null && !snd.getLocation().getNamespace().equals(RtShop.MODID) && !snd.getLocation().getNamespace().equals("minecraft")) {
                    String path = snd.getLocation().getPath();
                    if (path.contains("music") || path.contains("theme") || path.contains("bgm")) e.setSound(null);
                }
                return;
            }
            prune();
            for (Other o : OTHERS) if (o.sound == snd) return; // 이미 흐르는 것을 다시 튼 것
            int what = decide(kind, ours, otherBossPlaying(snd), ClientConfig.get().oneBossMusic);
            if (what == 1) {
                e.setSound(null);
                deniedForTest++;
                return;
            }
            if (what == 2) {
                stopBackgrounds(snd);
                // 보스 음악이 시작되면 게임의 배경 음악도 멈춘다. (배경 음악 갈래일 때는 부르지 않는다: 음악 관리자가 지금 틀려는 것이 바로 그 소리일 수 있다)
                if (kind == BOSS) Minecraft.getInstance().getMusicManager().stopPlaying();
            }
            OTHERS.add(new Other(snd, kind, ticks));
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] boss music guard", t);
        }
    }
}
