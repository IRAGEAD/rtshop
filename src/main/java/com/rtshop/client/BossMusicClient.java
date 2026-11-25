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
        mc.getSoundManager().play(t);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
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

    /** 우리 곡이 흐르는 동안에는 다른 모드의 보스 음악(이름에 music·theme 가 든 소리)을 틀지 않게 한다. */
    @SubscribeEvent
    public static void onOtherMusic(PlaySoundEvent e) {
        if (!muteOthers || current == null || current.isStopped() || e.getSound() == null || e.getSound() == current) return;
        ResourceLocation loc = e.getSound().getLocation();
        if (loc.getNamespace().equals(RtShop.MODID) || loc.getNamespace().equals("minecraft")) return;
        String path = loc.getPath();
        if (path.contains("music") || path.contains("theme") || path.contains("bgm")) e.setSound(null);
    }
}
