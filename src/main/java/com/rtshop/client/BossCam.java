package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 보스 등장의 3D 카메라 연출 (3.61). 보스 등장 연출(Cinematic 의 boss)이 시작될 때 서버가 보스의 엔티티 번호를 함께 보내면,
 * 연출 시간 동안 카메라가 플레이어의 눈을 떠나 보스 곁에서 움직인다:
 * <ol>
 * <li>발치의 낮은 자리에서 머리 쪽으로 올려다보며 다가서고</li>
 * <li>보스 둘레를 천천히 돌고</li>
 * <li>끝에 플레이어의 눈으로 돌아온다.</li>
 * </ol>
 * 움직임은 보스마다 다르다 (서버가 보낸 방식 이름, story/BossIntros.CAMS):
 * orbit 둘레를 돎 · rise 발치에서 몸을 타고 올라감 · descend 높은 데서 내려옴 · pushin 멀리서 얼굴까지 곧게 다가감 ·
 * sweep 멀찍이 크게 휩쓸며 돎 · low 땅바닥 높이에서 올려다봄 · spiral 돌면서 솟아오름 · behind 보스의 어깨 너머로 플레이어를 봄.
 * 카메라는 화면 쪽에만 있는 표식 엔티티(Marker)를 따라간다 (세계에는 넣지 않는다). 벽이 가리면 벽 앞까지만 물러난다.
 * 몸은 그동안에도 움직일 수 있고, 서버가 등장 연출 동안 피해를 막아 준다. client.json 의 bossCamera 로 끈다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class BossCam {
    private BossCam() {
    }

    /** 이보다 먼 보스에게는 카메라를 보내지 않는다. */
    private static final double MAX_DISTANCE = 48.0;

    private static Marker cam;
    private static int bossId = -1;
    private static long startAt;
    private static float seconds;
    private static double startAngle;
    private static String style = "orbit";
    public static final java.util.List<String> STYLES = java.util.List.of("orbit", "rise", "descend", "pushin", "sweep", "low", "spiral", "behind");
    public static String styleForTest() {
        return style;
    }
    /** 점검용: 마지막으로 놓인 카메라 자리와, 지금까지 시작된 횟수. */
    public static Vec3 lastForTest = Vec3.ZERO;
    public static int startsForTest;

    public static boolean active() {
        return cam != null;
    }

    /** 보스 등장 연출과 함께 부른다. 보스가 화면 쪽 세계에 없거나 너무 멀면 아무것도 하지 않는다 (2D 연출만 나온다). */
    public static void start(int entityId, float duration) {
        start(entityId, duration, "orbit");
    }

    public static void start(int entityId, float duration, String how) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || duration < 1.5F || !ClientConfig.get().bossCamera) return;
        Entity boss = mc.level.getEntity(entityId);
        if (boss == null || boss == mc.player || boss.distanceTo(mc.player) > MAX_DISTANCE) return;
        stop();
        cam = new Marker(EntityType.MARKER, mc.level);
        bossId = entityId;
        style = STYLES.contains(how) ? how : "orbit";
        seconds = duration;
        startAt = System.currentTimeMillis();
        startAngle = Math.atan2(mc.player.getZ() - boss.getZ(), mc.player.getX() - boss.getX());
        startsForTest++;
        place(0F);
        mc.setCameraEntity(cam);
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (cam != null && mc.player != null && mc.getCameraEntity() == cam) mc.setCameraEntity(mc.player);
        cam = null;
        bossId = -1;
    }

    private static float ease(float v) {
        float c = Mth.clamp(v, 0F, 1F);
        return c * c * (3F - 2F * c);
    }

    /** t(0~1)의 카메라 자리와 바라보는 곳을 정해 표식 엔티티를 옮긴다. 보스가 사라졌으면 false. */
    private static boolean place(float t) {
        Minecraft mc = Minecraft.getInstance();
        if (cam == null || mc.level == null || mc.player == null) return false;
        Entity boss = mc.level.getEntity(bossId);
        if (boss == null || !boss.isAlive()) return false;
        double h = Math.max(1.0, boss.getBbHeight()), w = Math.max(0.6, boss.getBbWidth());
        double bx = boss.getX(), by = boss.getY(), bz = boss.getZ();
        double radius = Mth.clamp(w * 1.2 + 4.5, 5.0, 10.0);
        float rise = ease(t / 0.35F);
        float whole = ease(t / 0.82F);
        // 방식마다: 도는 각(시작 + 길이), 거리(처음 → 끝), 카메라 높이(처음 → 끝), 바라보는 높이(처음 → 끝)
        double a0 = 0.35, arc = 1.9 * ease((t - 0.25F) / 0.55F), d0 = 0.75, d1 = 1.0, y0 = 0.3, y1 = h * 0.55 + 0.8, l0 = h * 0.2, l1 = h * 0.6;
        float dk = ease(t / 0.3F), yk = rise, lk = rise;
        boolean lookAtPlayer = false;
        switch (style) {
            case "rise" -> { a0 = 0.5; arc = 0.5 * whole; d0 = 0.55; d1 = 0.7; y0 = 0.2; y1 = h * 1.1 + 1.0; l0 = h * 0.1; l1 = h * 0.9; yk = whole; lk = whole; }
            case "descend" -> { a0 = 0.2; arc = 1.2 * whole; d0 = 1.2; d1 = 0.9; y0 = h + 9.0; y1 = h * 0.6 + 0.5; l0 = h * 0.7; l1 = h * 0.7; yk = whole; dk = whole; }
            case "pushin" -> { a0 = 0.0; arc = 0.12 * whole; d0 = 1.7; d1 = 0.45; y0 = h * 0.7; y1 = h * 0.8; l0 = h * 0.8; l1 = h * 0.88; yk = whole; dk = whole; lk = whole; }
            case "sweep" -> { a0 = -0.6; arc = 3.2 * whole; d0 = 1.5; d1 = 1.35; y0 = h * 0.3 + 1.0; y1 = h * 0.5 + 1.5; l0 = h * 0.5; l1 = h * 0.5; yk = whole; }
            case "low" -> { a0 = 0.45; arc = 0.9 * whole; d0 = 0.95; d1 = 0.8; y0 = 0.25; y1 = 0.35; l0 = h * 0.45; l1 = h * 0.75; yk = whole; lk = whole; }
            case "spiral" -> { a0 = 0.0; arc = 3.6 * whole; d0 = 0.85; d1 = 1.1; y0 = 0.4; y1 = h + 2.0; l0 = h * 0.3; l1 = h * 0.7; yk = whole; lk = whole; dk = whole; }
            case "behind" -> { a0 = Math.PI - 0.45; arc = 0.9 * whole; d0 = 0.55; d1 = 0.75; y0 = h * 0.85 + 0.4; y1 = h * 0.95 + 0.8; l0 = h * 0.7; l1 = h * 0.7; yk = whole; dk = whole; lookAtPlayer = true; }
            default -> { }
        }
        double angle = startAngle + a0 + arc;
        double dist = radius * Mth.lerp(dk, d0, d1);
        double lookY = by + Mth.lerp(lk, l0, l1);
        Vec3 look = new Vec3(bx, lookY, bz);
        Vec3 pos = new Vec3(bx + Math.cos(angle) * dist, by + Mth.lerp(yk, y0, y1), bz + Math.sin(angle) * dist);
        // 벽이 가리면 벽 앞까지만
        BlockHitResult hit = mc.level.clip(new ClipContext(look, pos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, cam));
        if (hit.getType() != HitResult.Type.MISS) {
            Vec3 back = look.subtract(hit.getLocation());
            double len = back.length();
            pos = len > 0.6 ? hit.getLocation().add(back.scale(0.4 / len)) : look.add(0, 0.5, 0);
        }
        // 끝에는 플레이어의 눈으로 돌아온다
        float home = ease((t - 0.82F) / 0.18F);
        if (home > 0F) {
            Vec3 eye = mc.player.getEyePosition();
            pos = new Vec3(Mth.lerp(home, pos.x, eye.x), Mth.lerp(home, pos.y, eye.y), Mth.lerp(home, pos.z, eye.z));
        }
        if (lookAtPlayer) {
            // 어깨 너머: 보스 뒤에서 플레이어를 본다. 끝에 가까워지면 보스 쪽으로 시선을 돌린다.
            Vec3 eye = mc.player.getEyePosition();
            float back = ease((t - 0.6F) / 0.25F);
            look = new Vec3(Mth.lerp(back, eye.x, look.x), Mth.lerp(back, eye.y, look.y), Mth.lerp(back, eye.z, look.z));
        }
        double dx = look.x - pos.x, dy = look.y - pos.y, dz = look.z - pos.z;
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        cam.setPos(pos.x, pos.y, pos.z);
        cam.xo = pos.x;
        cam.yo = pos.y;
        cam.zo = pos.z;
        cam.xOld = pos.x;
        cam.yOld = pos.y;
        cam.zOld = pos.z;
        cam.setYRot(yaw);
        cam.setXRot(pitch);
        cam.yRotO = yaw;
        cam.xRotO = pitch;
        lastForTest = pos;
        return true;
    }

    /** 3.71: 연출 건너뛰기 키 (보스 등장 중에만, 창이 열려 있지 않을 때). */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            while (ClientModEvents.SKIP_KEY.consumeClick()) {
                if (Minecraft.getInstance().screen == null && Cinematic.skippable()) Cinematic.skip();
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || cam == null) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            float t = (System.currentTimeMillis() - startAt) / 1000F / Math.max(0.1F, seconds);
            if (mc.level == null || mc.player == null || t >= 1F || !Cinematic.active() || mc.getCameraEntity() != cam || !place(t)) stop();
        } catch (Throwable ex) {
            cam = null;
            bossId = -1;
        }
    }
}
