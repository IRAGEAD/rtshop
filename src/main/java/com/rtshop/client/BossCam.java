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
    /** 점검용: 마지막으로 놓인 카메라 자리와, 지금까지 시작된 횟수. */
    public static Vec3 lastForTest = Vec3.ZERO;
    public static int startsForTest;

    public static boolean active() {
        return cam != null;
    }

    /** 보스 등장 연출과 함께 부른다. 보스가 화면 쪽 세계에 없거나 너무 멀면 아무것도 하지 않는다 (2D 연출만 나온다). */
    public static void start(int entityId, float duration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || duration < 1.5F || !ClientConfig.get().bossCamera) return;
        Entity boss = mc.level.getEntity(entityId);
        if (boss == null || boss == mc.player || boss.distanceTo(mc.player) > MAX_DISTANCE) return;
        stop();
        cam = new Marker(EntityType.MARKER, mc.level);
        bossId = entityId;
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
        double angle = startAngle + 0.35 + 1.9 * ease((t - 0.25F) / 0.55F);
        double dist = radius * (0.75 + 0.25 * ease(t / 0.3F));
        double lookY = by + Mth.lerp(rise, h * 0.2, h * 0.6);
        Vec3 look = new Vec3(bx, lookY, bz);
        Vec3 pos = new Vec3(bx + Math.cos(angle) * dist, by + Mth.lerp(rise, 0.3, h * 0.55 + 0.8), bz + Math.sin(angle) * dist);
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
