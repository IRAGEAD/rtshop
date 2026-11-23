package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtshop.RtShop;
import com.rtshop.quest.QuestDef;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 장소 조사의 화면 쪽: 서버가 알려 준 흔적을 세계 속에 그리고(바닥의 빛나는 원 + 떠 있는 그림 + 빛기둥),
 * 가까이서 바라보면 「살펴보기」 안내를 띄우고, 우클릭하면 서버에 알린다. 흔적은 그려지기만 하는 것이라 부술 수 없다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class SpotClient {
    private SpotClient() {
    }

    public record Spot(String id, BlockPos pos, int shape, String label) {
    }

    private static final ResourceLocation RING = new ResourceLocation("rtshop", "textures/gui/magic/ring_outer.png");
    private static final ResourceLocation GLOW = new ResourceLocation("rtshop", "textures/gui/magic/glow.png");
    private static final ResourceLocation ICONS = new ResourceLocation("rtshop", "textures/gui/spots.png");
    private static final List<Spot> SPOTS = new ArrayList<>();
    private static Spot aimed;
    private static long lastSent;
    public static final double REACH = 4.5;

    public static void receive(JsonObject o) {
        SPOTS.clear();
        aimed = null;
        if (!o.has("spots")) return;
        for (JsonElement e : o.getAsJsonArray("spots")) {
            JsonObject j = e.getAsJsonObject();
            int shape = Math.max(0, Arrays.asList(QuestDef.SHAPES).indexOf(j.get("shape").getAsString()));
            SPOTS.add(new Spot(j.get("id").getAsString(), new BlockPos(j.get("x").getAsInt(), j.get("y").getAsInt(), j.get("z").getAsInt()), shape,
                    j.has("label") ? j.get("label").getAsString() : ""));
        }
    }

    public static void clear() {
        SPOTS.clear();
        aimed = null;
    }

    public static int countForTest() {
        return SPOTS.size();
    }

    public static String aimedForTest() {
        return aimed == null ? "" : aimed.id;
    }

    private static Vec3 center(Spot s) {
        return new Vec3(s.pos.getX() + 0.5, s.pos.getY() + 1.0, s.pos.getZ() + 0.5);
    }

    /** 지금 바라보고 있는, 손이 닿는 흔적. */
    private static Spot pick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return null;
        Vec3 eye = mc.player.getEyePosition(), look = mc.player.getLookAngle();
        Spot best = null;
        double bestDot = 0.90;
        for (Spot s : SPOTS) {
            Vec3 to = center(s).subtract(eye);
            double d = to.length();
            if (d > REACH) continue;
            double dot = d < 1.2 ? 1 : to.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = s;
            }
        }
        return best;
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SPOTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        aimed = pick(mc);
        Camera cam = event.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float t = (mc.level.getGameTime() % 24000L) + event.getPartialTick();
        for (Spot s : SPOTS) {
            Vec3 c = center(s);
            double dist = c.distanceTo(cp);
            if (dist > 64) continue;
            boolean hot = s == aimed;
            float pulse = 0.75F + 0.25F * (float) Math.sin(t * 0.12F);
            float a = (hot ? 1F : 0.8F) * pulse;
            // 바닥의 원 (돌아간다)
            flat(ps, buf.getBuffer(RenderType.text(RING)), cp, new Vec3(c.x, s.pos.getY() + 0.03, c.z), 0.75F, t * 1.5F, 255, 214, 130, Math.round(200 * a));
            flat(ps, buf.getBuffer(RenderType.text(GLOW)), cp, new Vec3(c.x, s.pos.getY() + 0.02, c.z), 1.1F, 0, 255, 190, 90, Math.round(120 * a));
            // 떠 있는 그림 (카메라를 본다)
            float bob = (float) Math.sin(t * 0.07F) * 0.06F;
            Vec3 ic = c.add(0, 0.25 + bob, 0);
            board(buf.getBuffer(RenderType.text(GLOW)), ps, cam, ic, 0.55F, 0.55F, 0F, 1F, 255, 200, 110, Math.round(110 * a));
            board(buf.getBuffer(RenderType.text(ICONS)), ps, cam, ic, hot ? 0.34F : 0.28F, hot ? 0.34F : 0.28F, s.shape / 8F, (s.shape + 1) / 8F, 255, 244, 214, Math.round(255 * Math.min(1F, a + 0.2F)));
            // 멀리서도 보이는 가는 빛기둥
            if (dist > 6) board(buf.getBuffer(RenderType.text(GLOW)), ps, cam, c.add(0, 2.2, 0), 0.10F, 2.4F, 0.45F, 0.55F, 255, 214, 130, 90);
        }
        buf.endBatch();
    }

    private static void flat(PoseStack ps, VertexConsumer vc, Vec3 cp, Vec3 at, float half, float deg, int r, int g, int b, int a) {
        Matrix4f m = ps.last().pose();
        double rad = Math.toRadians(deg);
        float x = (float) (at.x - cp.x), y = (float) (at.y - cp.y), z = (float) (at.z - cp.z);
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int pass = 0; pass < 2; pass++) { // 위에서도 아래에서도 보이게
            for (int k = 0; k < 4; k++) {
                int i = pass == 0 ? k : 3 - k;
                double ang = rad + Math.PI / 4 + i * Math.PI / 2;
                float dx = (float) (Math.cos(ang) * half * 1.41421F), dz = (float) (Math.sin(ang) * half * 1.41421F);
                vc.vertex(m, x + dx, y, z + dz).color(r, g, b, Math.max(0, Math.min(255, a))).uv(uv[i][0], uv[i][1]).uv2(0xF000F0).endVertex();
            }
        }
    }

    private static void board(VertexConsumer vc, PoseStack ps, Camera cam, Vec3 at, float hw, float hh, float u0, float u1, int r, int g, int b, int a) {
        Vector3f l = cam.getLeftVector(), u = cam.getUpVector();
        Vec3 cp = cam.getPosition();
        Matrix4f m = ps.last().pose();
        float x = (float) (at.x - cp.x), y = (float) (at.y - cp.y), z = (float) (at.z - cp.z);
        float lx = l.x() * hw, ly = l.y() * hw, lz = l.z() * hw, ux = u.x() * hh, uy = u.y() * hh, uz = u.z() * hh;
        a = Math.max(0, Math.min(255, a));
        vc.vertex(m, x + lx + ux, y + ly + uy, z + lz + uz).color(r, g, b, a).uv(u0, 0f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x + lx - ux, y + ly - uy, z + lz - uz).color(r, g, b, a).uv(u0, 1f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x - lx - ux, y - ly - uy, z - lz - uz).color(r, g, b, a).uv(u1, 1f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x - lx + ux, y - ly + uy, z - lz + uz).color(r, g, b, a).uv(u1, 0f).uv2(0xF000F0).endVertex();
    }

    /** 「살펴보기」 안내 (조준점 아래). */
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Spot s = aimed;
        if (s == null || mc.screen != null || mc.options.hideGui || !SPOTS.contains(s)) return;
        GuiGraphics g = event.getGuiGraphics();
        int sw = g.guiWidth(), sh = g.guiHeight();
        String key = mc.options.keyUse.getTranslatedKeyMessage().getString();
        String name = s.label.isEmpty() ? Component.translatable("rtshop.spot.trace").getString() : s.label;
        String line = Component.translatable("rtshop.spot.prompt", key).getString();
        int w = Math.max(RaidText.w(mc.font, line, RaidText.SANS), RaidText.w(mc.font, name, RaidText.SERIF)) + 18;
        // 조준점 왼쪽: 아래쪽 가운데의 알림 상자, 오른쪽의 퀘스트 목록과 겹치지 않는다
        int x = sw / 2 - 14 - w, y = sh / 2 - 13, cx = x + w / 2;
        g.fill(x, y, x + w, y + 27, 0xB0120C06);
        g.fill(x, y, x + w, y + 1, 0xFFD8B060);
        g.fill(x, y + 26, x + w, y + 27, 0xFFD8B060);
        RaidText.center(g, mc.font, name, RaidText.SERIF, cx, y + 4, 0xFFFFE9B0);
        RaidText.center(g, mc.font, line, RaidText.SANS, cx, y + 15, 0xFFF2EADC);
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || mc.player == null) return;
        Spot s = pick(mc);
        if (s == null) return;
        event.setCanceled(true);
        event.setSwingHand(true);
        look(s.id);
    }

    public static void look(String id) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (mc.player == null || now - lastSent < 400) return;
        lastSent = now;
        mc.player.connection.sendCommand("rtshop spot look " + id);
    }
}
