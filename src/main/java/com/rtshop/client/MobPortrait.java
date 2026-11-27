package com.rtshop.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.Map;

/**
 * 몹 그림 (도감 · 현상금 게시판). 3.78 (사용자 "도감에서 몹들의 3D 모습이 일부 보이지 않는 문제"):
 * 예전에는 히트박스 크기로만 배율을 정해, 모델이 히트박스보다 훨씬 큰 몹은 칸 밖으로 나가 잘리거나 엉뚱한 데 그려져 빈칸으로 보였다.
 * 이제 몹마다 한 번, 화면 밖 버퍼에 여러 각도로 그려 실제로 칠해지는 자리를 재고 칸에 꼭 맞는 배율 · 위치를 기억한다.
 * 처음엔 안 보이는 몹(투명 · 나타나는 중)은 보이게 해서 다시 재고, 그래도 아무것도 그려지지 않거나 그리다 오류가 나는 몹은 알 아이콘으로 대신한다.
 * 화면 밖 그리기가 이 컴퓨터에서 안 되면(돼지를 그려 확인) 예전 방식 그대로 그린다.
 */
public final class MobPortrait {
    private MobPortrait() {
    }

    /** 화면 밖에서 재는 칸의 크기(픽셀). */
    static final int S = 128;
    /** 재는 칸에서 몹의 발이 놓이는 높이 (위에서부터의 비율). */
    private static final float ANCHOR_Y = 0.72F;
    /** 칸을 이만큼 채운다 (가장자리에 조금 틈). */
    private static final float FILL = 0.86F;
    /** 칸 안에서 천천히 도는 몹을 재는 각도. */
    private static final float[] ANGLES = {0F, 60F, 120F, 180F, 240F, 300F};
    private static final Quaternionf TILT = new Quaternionf().rotateZ((float) Math.PI).rotateX(-0.25F);

    /** 잰 결과. icon 이 비어 있지 않으면 3D 대신 그 아이템을 그린다. */
    public static final class Fit {
        /** 잰 배율, 칠해진 사각형의 너비 · 높이, 기준점(발)에서 본 사각형 가운데 (모두 잰 칸의 픽셀). */
        public final float probe, w, h, cx, cy;
        public final ItemStack icon;
        public final String why;
        public final int pixels;

        Fit(float probe, float w, float h, float cx, float cy, ItemStack icon, String why, int pixels) {
            this.probe = probe;
            this.w = w;
            this.h = h;
            this.cx = cx;
            this.cy = cy;
            this.icon = icon;
            this.why = why;
            this.pixels = pixels;
        }

        public boolean isIcon() {
            return !icon.isEmpty();
        }
    }

    private static final Map<String, LivingEntity> MODELS = new HashMap<>();
    private static final Map<String, Fit> FITS = new HashMap<>();
    private static TextureTarget target;
    private static Boolean pipelineOk;
    private static long lastFitNanos;
    /** 시험용: 마지막으로 화면 밖 그리기가 안 된 까닭. */
    static String pipelineInfo = "";

    /** 그림에 쓸 몹 (차원마다 한 번 만든다). 만들 수 없거나 살아 있는 몹이 아니면 null. */
    public static LivingEntity model(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        LivingEntity e = MODELS.get(id);
        if (e != null && e.level() == mc.level) return e;
        try {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
            Entity made = t == null ? null : t.create(mc.level);
            e = made instanceof LivingEntity l ? l : null;
        } catch (Throwable ex) {
            e = null;
        }
        MODELS.put(id, e);
        return e;
    }

    /** 3D 대신 그릴 아이템: 그 몹의 알, 없으면 종이. */
    public static ItemStack iconOf(String id) {
        try {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(id));
            SpawnEggItem egg = t == null ? null : SpawnEggItem.byId(t);
            if (egg != null) return new ItemStack(egg);
        } catch (Throwable ignored) {
        }
        return new ItemStack(Items.PAPER);
    }

    /** 칸(box×box) 안에 맞춰 천천히 도는 3D 모습. 아직 재지 않았으면 재고(한 프레임에 하나), 그릴 수 없는 몹은 알 아이콘. */
    public static void draw(GuiGraphics g, String id, int x, int y, int box, float spin) {
        Fit f = fit(id, false);
        if (f != null && f.isIcon()) {
            drawIcon(g, f.icon, x, y, box);
            return;
        }
        LivingEntity e = model(id);
        if (e == null) {
            drawIcon(g, iconOf(id), x, y, box);
            return;
        }
        if (f != null && f.why.equals("shown")) {
            // 잴 때 보이게 한 몹 (차원이 바뀌어 새로 만든 몹에도 같게)
            e.setInvisible(false);
            e.tickCount = Math.max(e.tickCount, 100);
        }
        g.enableScissor(x, y, x + box, y + box);
        pose(e, spin);
        if (f == null) {
            // 아직 재지 못함 (이번 프레임에 이미 하나를 쟀거나, 화면 밖 그리기가 안 되는 컴퓨터): 예전 방식
            float size = Math.max(e.getBbHeight(), e.getBbWidth());
            float scale = Math.max(2, (int) (box * 0.8F / Math.max(0.5F, size)));
            renderAt(g, x + box / 2F, y + box - 2, scale, e);
        } else {
            float k = Math.min(FILL * box / f.w, FILL * box / f.h);
            renderAt(g, x + box / 2F - f.cx * k, y + box / 2F - f.cy * k, f.probe * k, e);
        }
        g.disableScissor();
    }

    static void drawIcon(GuiGraphics g, ItemStack icon, int x, int y, int box) {
        float s = Math.max(1F, Math.min(3F, (box - 4) / 16F));
        g.pose().pushPose();
        g.pose().translate(x + (box - 16 * s) / 2F, y + (box - 16 * s) / 2F, 0);
        g.pose().scale(s, s, 1F);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
    }

    private static void pose(LivingEntity e, float spin) {
        e.yBodyRot = 180F + spin;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(e.yBodyRot);
        e.yRotO = e.yBodyRot;
        e.yHeadRot = e.yBodyRot;
        e.yHeadRotO = e.yBodyRot;
        e.setXRot(0);
        e.xRotO = 0;
    }

    /** InventoryScreen.renderEntityInInventory 와 같되 배율을 소수로 (작은 칸에서도 꼭 맞게). (x, y) = 몹의 발. */
    static void renderAt(GuiGraphics g, float x, float y, float scale, LivingEntity e) {
        g.pose().pushPose();
        g.pose().translate(x, y, 50.0F);
        g.pose().mulPoseMatrix(new Matrix4f().scaling(scale, scale, -scale));
        g.pose().mulPose(TILT);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
        d.setRenderShadow(false);
        try {
            RenderSystem.runAsFancy(() -> d.render(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, g.pose(), g.bufferSource(), 15728880));
            g.flush();
        } finally {
            d.setRenderShadow(true);
            g.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

    /** 잰 결과 (now: 시험처럼 기다리지 않고 바로 잼). 화면 밖 그리기가 안 되거나 이번 프레임에 이미 하나를 쟀으면 null. */
    static Fit fit(String id, boolean now) {
        Fit f = FITS.get(id);
        if (f != null) return f;
        if (Boolean.FALSE.equals(pipelineOk)) return null;
        if (!now && System.nanoTime() - lastFitNanos < 30_000_000L) return null;
        if (pipelineOk == null) {
            pipelineOk = checkPipeline();
            if (!pipelineOk) return null;
        }
        try {
            f = measure(id);
        } catch (Throwable t) {
            f = new Fit(0, 1, 1, 0, 0, iconOf(id), "error " + t, 0);
            RtShop.LOGGER.warn("[rtshop] 도감 그림: {} 을(를) 그리지 못해 알 아이콘으로 대신합니다: {}", id, t.toString());
        }
        FITS.put(id, f);
        lastFitNanos = System.nanoTime();
        return f;
    }

    /** 화면 밖 그리기가 되는가: 돼지를 그려 칠해진 픽셀이 있는지 본다. */
    private static boolean checkPipeline() {
        try {
            LivingEntity pig = model("minecraft:pig");
            if (pig == null) {
                pipelineInfo = "no pig";
                return false;
            }
            int[] u = probe(pig, S * 0.35F / 0.9F, 30F);
            pipelineInfo = "pig pixels " + u[4];
            return u[4] > 50;
        } catch (Throwable t) {
            pipelineInfo = String.valueOf(t);
            RtShop.LOGGER.warn("[rtshop] 도감 그림: 화면 밖 그리기를 쓸 수 없어 예전 방식으로 그립니다: {}", t.toString());
            return false;
        }
    }

    private static Fit measure(String id) {
        LivingEntity e = model(id);
        if (e == null) return new Fit(0, 1, 1, 0, 0, iconOf(id), "no entity", 0);
        float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
        float probe = S * 0.35F / size;
        int[] u = union(e, probe);
        String why = "ok";
        if (u[4] == 0) {
            // 모델이 기준점에서 멀리 떨어져 그려지는 몹: 훨씬 넓게 보며 다시
            probe *= 0.15F;
            u = union(e, probe);
            why = "far";
        }
        if (u[4] == 0) {
            // 처음엔 안 보이는 몹 (투명 · 땅에서 나타나는 중): 보이게 하고 시간이 조금 흐른 것처럼 해서 다시
            e.setInvisible(false);
            e.tickCount = 100;
            probe /= 0.15F;
            u = union(e, probe);
            if (u[4] == 0) {
                probe *= 0.15F;
                u = union(e, probe);
            }
            why = "shown";
        }
        if (u[4] == 0) return new Fit(0, 1, 1, 0, 0, iconOf(id), "blank", 0);
        // 가장자리에 닿으면(재는 칸보다 큼) 작게 해서 다시 (세 번까지)
        for (int tries = 0; tries < 3 && touches(u); tries++) {
            probe *= 0.4F;
            int[] v = union(e, probe);
            if (v[4] == 0) break;
            u = v;
            if (why.equals("ok")) why = "big";
        }
        float w = u[2] - u[0] + 1, h = u[3] - u[1] + 1;
        float cx = (u[0] + u[2] + 1) / 2F - S / 2F, cy = (u[1] + u[3] + 1) / 2F - S * ANCHOR_Y;
        return new Fit(probe, w, h, cx, cy, ItemStack.EMPTY, why, u[4]);
    }

    private static boolean touches(int[] u) {
        return u[0] <= 1 || u[1] <= 1 || u[2] >= S - 2 || u[3] >= S - 2;
    }

    /** 여러 각도로 그린 것을 합친 사각형 {minX, minY, maxX, maxY, 칠해진 수(가장 많은 각도)}. */
    private static int[] union(LivingEntity e, float probe) {
        int[] u = {S, S, -1, -1, 0};
        for (float a : ANGLES) {
            int[] p = probe(e, probe, a);
            if (p[4] == 0) continue;
            u[0] = Math.min(u[0], p[0]);
            u[1] = Math.min(u[1], p[1]);
            u[2] = Math.max(u[2], p[2]);
            u[3] = Math.max(u[3], p[3]);
            u[4] = Math.max(u[4], p[4]);
        }
        return u;
    }

    /** 화면 밖 S×S 칸에 기준점 (S/2, S*ANCHOR_Y), 배율 scale, 각도 spin 으로 그려 칠해진 사각형 {minX, minY, maxX, maxY, 수} (위가 0). */
    static int[] probe(LivingEntity e, float scale, float spin) {
        return probeAt(e, S / 2F, S * ANCHOR_Y, scale, spin);
    }

    static int[] probeAt(LivingEntity e, float ax, float ay, float scale, float spin) {
        RenderSystem.assertOnRenderThread();
        Minecraft mc = Minecraft.getInstance();
        // 지금 그리던 곳(프레임버퍼 · 보이는 범위 · 잘라 내기 · 투영)을 먼저 적어 두고 (화면 밖 버퍼를 만들 때도 바뀌므로), 끝나면 그대로 되돌린다
        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] vp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] sb = new int[4];
        if (scissor) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, sb);
        Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        PoseStack mv = RenderSystem.getModelViewStack();
        boolean pushed = false;
        Throwable err = null;
        try {
            if (scissor) RenderSystem.disableScissor();
            if (target == null) {
                target = new TextureTarget(S, S, true, Minecraft.ON_OSX);
            }
            target.setClearColor(0F, 0F, 0F, 0F);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            float far = ForgeHooksClient.getGuiFarPlane();
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0F, S, S, 0F, 1000F, far), VertexSorting.ORTHOGRAPHIC_Z);
            mv.pushPose();
            pushed = true;
            mv.setIdentity();
            mv.translate(0.0F, 0.0F, 1000F - far);
            RenderSystem.applyModelViewMatrix();
            GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            pose(e, spin);
            renderAt(g, ax, ay, scale, e);
        } catch (Throwable t) {
            err = t;
            try {
                mc.renderBuffers().bufferSource().endBatch();
            } catch (Throwable ignored) {
            }
        } finally {
            if (pushed) mv.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(proj, sorting);
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            RenderSystem.viewport(vp[0], vp[1], vp[2], vp[3]);
            if (scissor) RenderSystem.enableScissor(sb[0], sb[1], sb[2], sb[3]);
        }
        if (err != null) {
            if (err instanceof RuntimeException re) throw re;
            if (err instanceof Error er) throw er;
            throw new RuntimeException(err);
        }
        int[] r = {S, S, -1, -1, 0};
        try (NativeImage img = new NativeImage(S, S, false)) {
            RenderSystem.bindTexture(target.getColorTextureId());
            img.downloadTexture(0, false);
            for (int row = 0; row < S; row++) {
                int y = S - 1 - row; // GL 의 아래가 0 → 화면의 위가 0
                for (int x = 0; x < S; x++) {
                    if (((img.getPixelRGBA(x, row) >>> 24) & 0xFF) > 16) {
                        if (x < r[0]) r[0] = x;
                        if (y < r[1]) r[1] = y;
                        if (x > r[2]) r[2] = x;
                        if (y > r[3]) r[3] = y;
                        r[4]++;
                    }
                }
            }
        }
        return r;
    }

    // ---- 시험용 ----

    /** 시험용: 예전 방식(히트박스로만 배율)으로 그린다 — 비교 사진. */
    static void drawLegacyForTest(GuiGraphics g, String id, int x, int y, int box, float spin) {
        LivingEntity e = model(id);
        if (e == null) {
            drawIcon(g, new ItemStack(Items.PAPER), x, y, box);
            return;
        }
        g.enableScissor(x, y, x + box, y + box);
        pose(e, spin);
        float size = Math.max(e.getBbHeight(), e.getBbWidth());
        renderAt(g, x + box / 2F, y + box - 2, Math.max(2, (int) (box * 0.8F / Math.max(0.5F, size))), e);
        g.disableScissor();
    }

    /** 시험용: 기다리지 않고 바로 잰 결과 (화면 밖 그리기가 안 되면 null). */
    static Fit fitNowForTest(String id) {
        return fit(id, true);
    }

    static boolean pipelineOkForTest() {
        return Boolean.TRUE.equals(pipelineOk);
    }

    /**
     * 시험용: 칸(box) 하나를 재는 칸 가운데에 두고, 예전 방식(legacy) 또는 새 방식으로 그렸을 때 {칸 안에 칠해진 수(가장 많은 각도), 칸 밖으로 넘친 픽셀 거리(가장 큰 값)}.
     * 칸 밖은 잘려 보이지 않는 부분이다.
     */
    static int[] boxCheckForTest(String id, int box, boolean legacy) {
        LivingEntity e = model(id);
        if (e == null) return new int[]{0, 0};
        float bx = (S - box) / 2F, by = (S - box) / 2F;
        float ax, ay, scale;
        if (legacy) {
            float size = Math.max(e.getBbHeight(), e.getBbWidth());
            scale = Math.max(2, (int) (box * 0.8F / Math.max(0.5F, size)));
            ax = bx + box / 2F;
            ay = by + box - 2;
        } else {
            Fit f = fit(id, true);
            if (f == null || f.isIcon()) return new int[]{-1, 0};
            float k = Math.min(FILL * box / f.w, FILL * box / f.h);
            scale = f.probe * k;
            ax = bx + box / 2F - f.cx * k;
            ay = by + box / 2F - f.cy * k;
        }
        int inside = 0, over = 0;
        for (float a : new float[]{0F, 90F, 180F, 270F}) {
            int[] p = probeAt(e, ax, ay, scale, a);
            if (p[4] == 0) continue;
            // 칸 안에 든 픽셀 수는 사각형만으로는 알 수 없으므로, 사각형이 칸과 겹치는 넓이에 비례해 어림한다
            float ix0 = Math.max(p[0], bx), iy0 = Math.max(p[1], by), ix1 = Math.min(p[2] + 1, bx + box), iy1 = Math.min(p[3] + 1, by + box);
            float area = (float) (p[2] - p[0] + 1) * (p[3] - p[1] + 1);
            float in = ix1 > ix0 && iy1 > iy0 ? (ix1 - ix0) * (iy1 - iy0) / area : 0F;
            inside = Math.max(inside, (int) (p[4] * in));
            over = Math.max(over, (int) Math.max(Math.max(bx - p[0], by - p[1]), Math.max(p[2] + 1 - (bx + box), p[3] + 1 - (by + box))));
        }
        return new int[]{inside, Math.max(0, over)};
    }
}
