package com.rtshop.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.MagicPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * 마법 뽑기 (클라이언트): 서버가 보낸 창 내용·결과를 받는다.
 * 구체를 우클릭하면 먼저 짧은 다가가기 연출(시선이 구슬로 모이고, 구슬이 밝아지며 고리가 빨라짐)을 보여 준 뒤 창을 연다.
 */
public final class MagicClient {
    private MagicClient() {
    }

    /** 구체 자리 -> 마지막으로 반응한 시각 (구슬이 밝아지고 고리가 빨라진다). */
    public static final Map<BlockPos, Long> FLARE = new HashMap<>();

    /** 다가가기 연출 (시선 이동): 남은 틱, 시작·목표 각도. */
    static int approachTicks;
    static float fromYaw, fromPitch, toYaw, toPitch;
    static JsonObject pending;
    static final int APPROACH = 12;

    public static boolean approachingForTest() {
        return approachTicks > 0;
    }

    private static BlockPos scenePos;
    private static long sceneSeen;
    private static float sceneK;

    /** Cinematic 장면이 매 프레임 알린다: 이 구체가 장면 속에 있고, 장면이 얼마나 펼쳐졌는가(0~1). */
    public static void sceneAt(BlockPos pos, float k) {
        scenePos = pos;
        sceneSeen = System.currentTimeMillis();
        sceneK = k;
    }

    /** 이 구체가 장면 속에 있는 정도 (장면이 아니면 0). */
    public static float sceneK(BlockPos pos) {
        if (scenePos == null || !scenePos.equals(pos) || System.currentTimeMillis() - sceneSeen > 250) return 0F;
        return Mth.clamp(sceneK, 0F, 1F);
    }

    /** 마지막으로 받은 창 내용 (메뉴로 열리는 창·Cinematic 장면이 처음에 쓴다). */
    public static JsonObject lastView;

    /** Cinematic Interactions 장면이 등록되어 있는가. */
    static boolean cinematic() {
        return com.rtshop.magic.MagicRegistry.cinematic;
    }

    /** 장면이 진행 중이면 서버의 알림(실패 등)을 장면에 보여 준다. */
    public static boolean sceneResult(net.minecraft.network.chat.Component msg, boolean ok) {
        return cinematic() && CinematicOrb.result(msg, ok);
    }

    public static void receive(int kind, String json) {
        Minecraft mc = Minecraft.getInstance();
        JsonObject o;
        try {
            o = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return;
        }
        if (kind == MagicPacket.VIEW) lastView = o;
        if (kind != MagicPacket.EDITOR && cinematic() && CinematicOrb.route(kind, o)) return;
        switch (kind) {
            case MagicPacket.VIEW -> {
                if (o.has("open") && mc.player != null) {
                    BlockPos pos = new BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt());
                    FLARE.put(pos, System.currentTimeMillis());
                    if (mc.screen instanceof MagicGachaScreen s) {
                        s.update(o, false);
                        return;
                    }
                    // 다가가기: 구슬 쪽으로 시선을 모은 뒤 창을 연다
                    Vec3 eye = mc.player.getEyePosition();
                    Vec3 orb = Vec3.atCenterOf(pos).add(0, 0.65, 0);
                    Vec3 d = orb.subtract(eye);
                    fromYaw = mc.player.getYRot();
                    fromPitch = mc.player.getXRot();
                    toYaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90F;
                    toPitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
                    toYaw = fromYaw + Mth.wrapDegrees(toYaw - fromYaw);
                    approachTicks = APPROACH;
                    pending = o;
                    mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F));
                } else if (mc.screen instanceof MagicGachaScreen s) {
                    s.update(o, false);
                }
            }
            case MagicPacket.RESULT -> {
                if (mc.screen instanceof MagicGachaScreen s) s.update(o, true);
            }
            case MagicPacket.EDITOR -> {
                if (mc.screen instanceof MagicEditScreen s) s.update(o);
                else mc.setScreen(new MagicEditScreen(o));
            }
            default -> {
            }
        }
    }

    /** 매 틱: 다가가기 연출을 진행하고, 끝나면 창을 연다. */
    public static void tick() {
        if (cinematic()) CinematicOrb.tick();
        if (approachTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            approachTicks = 0;
            return;
        }
        approachTicks--;
        if (approachTicks == 0 && pending != null) {
            JsonObject o = pending;
            pending = null;
            mc.setScreen(new MagicGachaScreen(o));
        }
    }

    /** 매 프레임: 시선을 부드럽게 구슬로 (연출 중에는 마우스로 돌리지 않는다). */
    public static void frame(float partial) {
        if (approachTicks <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        float t = 1F - (approachTicks - partial) / APPROACH;
        t = Mth.clamp(t, 0F, 1F);
        float e = t * t * (3 - 2 * t);
        mc.player.setYRot(Mth.lerp(e, fromYaw, toYaw));
        mc.player.setXRot(Mth.lerp(e, fromPitch, toPitch));
        mc.player.yRotO = mc.player.getYRot();
        mc.player.xRotO = mc.player.getXRot();
    }

    /** 구슬이 반응한 정도 (0~1, 반응 뒤 3초에 걸쳐 줄어든다). */
    public static float flare(BlockPos pos) {
        Long t = FLARE.get(pos);
        if (t == null) return 0F;
        long dt = System.currentTimeMillis() - t;
        if (dt > 3000) return 0F;
        return 1F - dt / 3000F;
    }
}
