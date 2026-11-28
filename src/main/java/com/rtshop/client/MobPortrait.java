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
 * 처음엔 안 보이는 몹(투명 · 나타나는 중)은 보이게 해서 다시 재고, 그래도 아무것도 그려지지 않거나 그리다 오류가 나는 몹은
 * 그 몹이 바탕으로 삼은 바닐라 몹(클래스, 없으면 이름 — Illusioner 의 유령 라바저 → 라바저)의 모습으로 대신 그린다. 그것도 없으면 알 아이콘.
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
    private static final float FILL = 0.9F;
    /** 자리를 잴 때 양 끝에서 덜어 내는 칠해진 양 (가늘게 삐져나온 꼬리 끝 · 수염 · 빛줄기 때문에 몸이 작아지지 않게). */
    private static final float TRIM = 0.015F;
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
        /** 대신 그리는 몹 (그 몹이 아무것도 그리지 않아 바탕이 된 바닐라 몹으로). null = 그 몹 그대로. */
        public final String drawId;
        /** 보이게 해서 잰 몹: 그릴 때 이 나이(틱)로 (0 = 그대로). */
        public final int shownTick;

        Fit(float probe, float w, float h, float cx, float cy, ItemStack icon, String why, int pixels) {
            this(probe, w, h, cx, cy, icon, why, pixels, null, 0);
        }

        Fit(float probe, float w, float h, float cx, float cy, ItemStack icon, String why, int pixels, String drawId, int shownTick) {
            this.probe = probe;
            this.w = w;
            this.h = h;
            this.cx = cx;
            this.cy = cy;
            this.icon = icon;
            this.why = why;
            this.pixels = pixels;
            this.drawId = drawId;
            this.shownTick = shownTick;
        }

        public boolean isIcon() {
            return !icon.isEmpty();
        }

        /** 이 잰 결과를 다른 몹(standIn)의 모습으로 쓴다. */
        Fit drawing(String standIn, String reason) {
            return new Fit(probe, w, h, cx, cy, icon, reason, pixels, standIn, shownTick);
        }
    }

    /** 처음엔 안 보이는 몹을 보이게 해서 잴 때 쓰는 나이(틱): 나타나는 데 걸리는 시간이 몹마다 달라 몇 가지로. */
    private static final int[] SHOW_TICKS = {100, 20, 40, 5, 200};

    private static final Map<String, LivingEntity> MODELS = new HashMap<>();
    private static final Map<String, Fit> FITS = new HashMap<>();
    private static TextureTarget target;
    private static Boolean pipelineOk;
    private static long lastFitNanos;
    /** 시험용: 마지막으로 화면 밖 그리기가 안 된 까닭. */
    static String pipelineInfo = "";
    /** 시험용: 화면 밖 그림이 늘 비는 컴퓨터처럼 (그만두고 예전 방식으로 돌아가는지 본다). */
    static boolean blankForTest;
    /**
     * 재는 칸을 지우는 빛깔 (3.79): 자홍(1,0,1)에 투명. 칠해진 픽셀 = 불투명하거나(알파 > 16) 빛깔이 자홍에서 멀어진 곳.
     * 그래픽 카드 · 셰이더 모드에 따라 알파를 적지 않는 경우에도 몹이 칠한 곳을 찾는다 (3.78 은 알파만 보았다).
     */
    private static final float KEY_R = 1F, KEY_G = 0F, KEY_B = 1F;

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

    /**
     * 칸(box×box) 안에 맞춰 천천히 도는 3D 모습. 아직 재지 않았으면 재고(한 프레임에 하나), 그릴 수 없는 몹은 알 아이콘.
     * 화면 밖 그림을 믿을 수 없는 컴퓨터(3.79 — 바닐라 몹 · 돼지까지 비게 나옴)에서는 모두 예전 방식(히트박스로 배율을 정한 3D)으로 그린다.
     */
    public static void draw(GuiGraphics g, String id, int x, int y, int box, float spin) {
        Fit f = fit(id, false);
        if (f != null && f.isIcon()) {
            drawIcon(g, f.icon, x, y, box);
            return;
        }
        LivingEntity e = model(f != null && f.drawId != null ? f.drawId : id);
        if (e == null) {
            drawIcon(g, iconOf(id), x, y, box);
            return;
        }
        if (f != null && f.shownTick > 0) {
            // 잴 때 보이게 한 몹 (차원이 바뀌어 새로 만든 몹에도 같게)
            e.setInvisible(false);
            e.tickCount = f.shownTick;
        }
        g.enableScissor(x, y, x + box, y + box);
        try {
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
        } catch (Throwable t) {
            // 그리다 오류가 나는 몹: 창이 멈추지 않게 이 몹만 알 아이콘으로 (다음부터는 다시 그리지 않는다)
            try {
                Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
            } catch (Throwable ignored) {
            }
            FITS.put(id, new Fit(0, 1, 1, 0, 0, iconOf(id), "draw error " + t, 0));
            RtShop.LOGGER.warn("[rtshop] 도감 그림: {} 을(를) 그리다 오류가 나서 알 아이콘으로 대신합니다: {}", id, t.toString());
        } finally {
            g.disableScissor();
        }
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
        drawing = e;
        try {
            RenderSystem.runAsFancy(() -> d.render(e, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, g.pose(), g.bufferSource(), 15728880));
            g.flush();
        } finally {
            drawing = null;
            d.setRenderShadow(true);
            g.pose().popPose();
            Lighting.setupFor3DItems();
        }
    }

    /** 지금 그림으로 그리는 몹 (그동안만). */
    private static volatile LivingEntity drawing;
    /** 시험용: 그림을 그릴 때 다른 모드가 모델을 바꿔 그리려던 것을 막은 횟수 (RenderLivingEvent.Pre 를 맨 앞에서 막고 맨 끝에서 풂). */
    static int vanillaLookForTest;

    /**
     * 그림은 몹의 원래(바닐라 · 그 모드의) 모델로 그린다 (3.79). Epic Fight 는 RenderLivingEvent.Pre 를 취소하고 자기 뼈대 모델로 바꿔 그리는데,
     * 셰이더(Oculus)를 켜면 창 안에서는 그것이 아무것도 그리지 않아 좀비 · 스켈레톤 · 크리퍼 · 거미 · 엔더맨 · 파괴수가 빈칸이 되었다 (CI 셰이더 시험,
     * 사용자 11-28 10:51 "도감과 현상금 게시판의 몹은 몹의 모습이 아니라 스폰 알로"). 그림을 그리는 동안만: 맨 앞(HIGHEST)에서 이벤트를 취소해 다른 모드가
     * 끼어들지 않게 하고, 맨 끝(LOWEST, 취소된 것도 받음)에서 취소를 풀어 원래 모델이 그려지게 한다.
     */
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = RtShop.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class VanillaLook {
        private VanillaLook() {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void first(net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> ev) {
            LivingEntity e = drawing;
            if (e != null && ev.getEntity() == e) ev.setCanceled(true);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST, receiveCanceled = true)
        public static void last(net.minecraftforge.client.event.RenderLivingEvent.Pre<?, ?> ev) {
            LivingEntity e = drawing;
            if (e != null && ev.getEntity() == e && ev.isCanceled()) {
                ev.setCanceled(false);
                vanillaLookForTest++;
            }
        }
    }

    /** 잰 결과 (now: 시험처럼 기다리지 않고 바로 잼). 화면 밖 그리기가 안 되거나 이번 프레임에 이미 하나를 쟀으면 null. */
    static Fit fit(String id, boolean now) {
        Fit f = FITS.get(id);
        if (f != null) return f;
        if (Boolean.FALSE.equals(pipelineOk) || !ClientConfig.get().measuredMobPortraits) return null;
        if (!now && System.nanoTime() - lastFitNanos < 30_000_000L) return null;
        if (pipelineOk == null) {
            pipelineOk = checkPipeline();
            if (!pipelineOk) return null;
        }
        Throwable err = null;
        try {
            f = measure(id);
        } catch (Throwable t) {
            err = t;
            try {
                f = standInOrIcon(id, model(id), "error " + t);
            } catch (Throwable t2) {
                f = new Fit(0, 1, 1, 0, 0, iconOf(id), "error " + t, 0);
            }
        }
        lastFitNanos = System.nanoTime();
        if (f.isIcon() && model(id) != null) {
            // 3.79 (사용자 11-28 10:51 "도감과 현상금 게시판의 몹이 몹의 모습이 아니라 스폰 알로"): 그릴 수 있는 몹이 비게 나왔다.
            // 컴퓨터 탓인지 몹 탓인지 가린다 — 지금 다시 그린 돼지 · 소가 비면 화면 밖 그리기가 망가진 것: 재기를 그만두고 모두 예전 방식으로.
            // 돼지 · 소는 잘 그려지면 그 몹이 이 화면에서 정말 아무것도 그리지 않는 것이라(예전 방식으로도 빈칸) 알 그대로.
            String canary = canaryProblem();
            if (canary != null) {
                giveUp(canary + " after " + id + " (" + f.why + ")");
                return null;
            }
        }
        if (err != null) {
            RtShop.LOGGER.warn("[rtshop] 도감 그림: {} 을(를) 그리다 오류가 나서 {} (으)로 대신합니다: {}", id, f.isIcon() ? "알 아이콘" : f.drawId, err.toString());
        } else if (f.isIcon()) {
            RtShop.LOGGER.info("[rtshop] 도감 그림: {} 은(는) 화면 밖에서 아무것도 그리지 않아 알 아이콘으로 ({})", id, f.why);
        }
        FITS.put(id, f);
        return f;
    }

    /** 화면 밖 재기를 그만두고, 이미 잰 것(알 아이콘 포함)도 버려 모두 예전 방식으로 그린다. 까닭은 한 번 로그에. */
    private static void giveUp(String why) {
        if (Boolean.FALSE.equals(pipelineOk)) return;
        pipelineOk = false;
        pipelineInfo = "gave up: " + why;
        FITS.clear();
        RtShop.LOGGER.warn("[rtshop] 도감 그림: 화면 밖에서 잰 그림이 비게 나와 예전 방식(3D 를 그대로 칸에 맞춤)으로 그립니다: {}", why);
    }

    /** 돼지와 소(다른 모드가 모델을 바꾸지 않는 순한 몹)를 지금 다시 그려 본다: 둘 다 칠해지면 null, 아니면 까닭. */
    private static String canaryProblem() {
        try {
            for (String c : new String[]{"minecraft:pig", "minecraft:cow"}) {
                LivingEntity e = model(c);
                if (e == null) continue;
                float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
                int[] u = probe(e, S * 0.35F / size, 30F);
                if (u[4] <= 50) return c + " blank now (" + u[4] + " px)";
            }
            return null;
        } catch (Throwable t) {
            return "canary error " + t;
        }
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
        if (e == null) return standInOrIcon(id, null, "no entity");
        Fit f = measureEntity(e);
        return f != null ? f : standInOrIcon(id, e, "blank");
    }

    /** 몹 하나를 잰다. 어떻게 해도 아무것도 그려지지 않으면 null. */
    private static Fit measureEntity(LivingEntity e) {
        float size = Math.max(0.5F, Math.max(e.getBbHeight(), e.getBbWidth()));
        float probe = S * 0.35F / size;
        int[] u = union(e, probe);
        String why = "ok";
        int shownTick = 0;
        if (u[4] == 0) {
            // 모델이 기준점에서 멀리 떨어져 그려지는 몹: 훨씬 넓게 보며 다시
            u = union(e, probe * 0.15F);
            if (u[4] > 0) {
                probe *= 0.15F;
                why = "far";
            }
        }
        if (u[4] == 0) {
            // 처음엔 안 보이는 몹 (투명 · 땅에서 나타나는 중 · 서서히 나타나는 유령): 보이게 하고 시간이 조금 흐른 것처럼 해서 다시
            boolean wasInvisible = e.isInvisible();
            int wasTick = e.tickCount;
            e.setInvisible(false);
            for (int tick : SHOW_TICKS) {
                e.tickCount = tick;
                u = union(e, probe);
                if (u[4] == 0) {
                    u = union(e, probe * 0.15F);
                    if (u[4] > 0) probe *= 0.15F;
                }
                if (u[4] > 0) {
                    why = "shown";
                    shownTick = tick;
                    break;
                }
            }
            if (u[4] == 0) {
                e.setInvisible(wasInvisible);
                e.tickCount = wasTick;
                return null;
            }
        }
        // 가장자리에 닿으면(재는 칸보다 큼) 작게 해서 다시 (여섯 번까지 — 모래벌레처럼 몸이 아주 긴 몹)
        for (int tries = 0; tries < 6 && touches(u); tries++) {
            probe *= 0.35F;
            int[] v = union(e, probe);
            if (v[4] == 0) break;
            u = v;
            if (why.equals("ok")) why = "big";
        }
        // 칸에 맞추는 것은 가는 끝을 뺀 몸 (끝이 칸 밖으로 조금 나가 잘려도 몸이 작아지지 않게)
        float w = u[7] - u[5] + 1, h = u[8] - u[6] + 1;
        float cx = (u[5] + u[7] + 1) / 2F - S / 2F, cy = (u[6] + u[8] + 1) / 2F - S * ANCHOR_Y;
        return new Fit(probe, w, h, cx, cy, ItemStack.EMPTY, why, u[4], null, shownTick);
    }

    /** 그리지 못하는 몹: 바탕이 된 바닐라 몹의 모습으로 대신, 그것도 안 되면 알(없으면 종이) 아이콘. */
    private static Fit standInOrIcon(String id, LivingEntity e, String why) {
        String sid = standIn(id, e);
        if (sid != null) {
            LivingEntity s = model(sid);
            Fit f = s == null ? null : measureEntity(s);
            if (f != null) return f.drawing(sid, "stand-in " + sid + " (" + why + ")");
            if (s != null) why = why + ", stand-in blank " + sid;
        }
        return new Fit(0, 1, 1, 0, 0, iconOf(id), why, 0);
    }

    /** 바닐라의 살아 있는 몹: 클래스 → ID (처음 부를 때 한 번 만든다). */
    private static Map<Class<?>, String> vanillaByClass;

    private static Map<Class<?>, String> vanillaByClass() {
        if (vanillaByClass != null) return vanillaByClass;
        Map<Class<?>, String> m = new HashMap<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<EntityType<?>>, EntityType<?>> en : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            ResourceLocation rl = en.getKey().location();
            if (!rl.getNamespace().equals("minecraft")) continue;
            LivingEntity v = model(rl.toString());
            if (v != null) m.putIfAbsent(v.getClass(), rl.toString());
        }
        if (Minecraft.getInstance().level != null) vanillaByClass = m;   // 세계가 없을 때 만든 빈 표는 기억하지 않는다
        return m;
    }

    /**
     * 아무것도 그리지 않는 몹 대신 그릴 바닐라 몹. 먼저 그 몹의 바탕 클래스(유령 라바저 → Ravager), 없으면 ID 의 낱말
     * (ghosted_ravager → ravager, pillagers_patrol → pillager, ice_spider → spider). 바닐라 몹이거나 찾지 못하면 null.
     */
    static String standIn(String id, LivingEntity e) {
        if (id == null || id.startsWith("minecraft:")) return null;
        Map<Class<?>, String> vanilla = vanillaByClass();
        if (e != null) {
            for (Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                String v = vanilla.get(c);
                if (v != null) return v;
            }
        }
        java.util.Set<String> ids = new java.util.HashSet<>(vanilla.values());
        String[] parts = id.substring(id.indexOf(':') + 1).split("[_/]");
        // 긴 이름부터 (cave_spider · polar_bear 처럼 두 낱말인 바닐라 몹)
        for (int len = parts.length; len >= 1; len--) {
            for (int i = 0; i + len <= parts.length; i++) {
                String name = String.join("_", java.util.Arrays.copyOfRange(parts, i, i + len));
                if (ids.contains("minecraft:" + name)) return "minecraft:" + name;
                if (name.endsWith("s") && ids.contains("minecraft:" + name.substring(0, name.length() - 1))) return "minecraft:" + name.substring(0, name.length() - 1);
            }
        }
        return null;
    }

    private static boolean touches(int[] u) {
        return u[0] <= 1 || u[1] <= 1 || u[2] >= S - 2 || u[3] >= S - 2;
    }

    /** 여러 각도로 그린 것을 합친 사각형 {minX, minY, maxX, maxY, 칠해진 수(가장 많은 각도), 가는 끝을 뺀 minX, minY, maxX, maxY}. */
    private static int[] union(LivingEntity e, float probe) {
        int[] u = {S, S, -1, -1, 0, S, S, -1, -1};
        for (float a : ANGLES) {
            int[] p = probe(e, probe, a);
            if (p[4] == 0) continue;
            for (int i = 0; i < 4; i++) {
                u[i] = i < 2 ? Math.min(u[i], p[i]) : Math.max(u[i], p[i]);
                u[5 + i] = i < 2 ? Math.min(u[5 + i], p[5 + i]) : Math.max(u[5 + i], p[5 + i]);
            }
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
            target.setClearColor(KEY_R, KEY_G, KEY_B, 0F);
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
        // {minX, minY, maxX, maxY, 수, 그리고 가는 끝(꼬리 끝 · 수염 · 더듬이 같은 칠해진 양의 TRIM 쯤)을 뺀 minX, minY, maxX, maxY}
        int[] r = {S, S, -1, -1, 0, S, S, -1, -1};
        int[] cols = new int[S], rows = new int[S];
        try (NativeImage img = new NativeImage(S, S, false)) {
            RenderSystem.bindTexture(target.getColorTextureId());
            img.downloadTexture(0, false);
            for (int row = 0; row < S; row++) {
                int y = S - 1 - row; // GL 의 아래가 0 → 화면의 위가 0
                for (int x = 0; x < S; x++) {
                    if (!blankForTest && painted(img.getPixelRGBA(x, row))) {
                        if (x < r[0]) r[0] = x;
                        if (y < r[1]) r[1] = y;
                        if (x > r[2]) r[2] = x;
                        if (y > r[3]) r[3] = y;
                        r[4]++;
                        cols[x]++;
                        rows[y]++;
                    }
                }
            }
        }
        if (r[4] > 0) {
            int cut = (int) (r[4] * TRIM);
            int[] tx = trim(cols, cut), ty = trim(rows, cut);
            r[5] = tx[0];
            r[6] = ty[0];
            r[7] = tx[1];
            r[8] = ty[1];
        }
        return r;
    }

    /** 칠해진 픽셀인가 (NativeImage 의 ABGR): 불투명하거나, 빛깔이 지운 빛깔(자홍)에서 멀어졌다. */
    static boolean painted(int abgr) {
        int a = (abgr >>> 24) & 0xFF, r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
        if (a > 16) return true;
        return Math.abs(r - (int) (KEY_R * 255)) + Math.abs(g - (int) (KEY_G * 255)) + Math.abs(b - (int) (KEY_B * 255)) > 60;
    }

    /** 양 끝에서 칠해진 양을 cut 만큼씩 덜어 낸 범위 {처음, 끝}. */
    private static int[] trim(int[] h, int cut) {
        int a = 0, sum = 0;
        while (a < h.length - 1 && sum + h[a] <= cut) sum += h[a++];
        int b = h.length - 1;
        sum = 0;
        while (b > a && sum + h[b] <= cut) sum += h[b--];
        return new int[]{a, b};
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

    /** 시험용: 처음처럼 (다시 돼지부터 재기). */
    static void resetForTest() {
        pipelineOk = null;
        pipelineInfo = "";
        FITS.clear();
        blankForTest = false;
    }

    /**
     * 시험용: 칸(box) 하나를 재는 칸 가운데에 두고, 예전 방식(legacy) 또는 새 방식으로 그렸을 때 {칸 안에 칠해진 수(가장 많은 각도), 칸 밖으로 넘친 픽셀 거리(가장 큰 값)}.
     * 칸 밖은 잘려 보이지 않는 부분이다.
     */
    static int[] boxCheckForTest(String id, int box, boolean legacy) {
        Fit f0 = legacy ? null : fit(id, true);
        LivingEntity e = model(f0 != null && f0.drawId != null ? f0.drawId : id);
        if (e == null) return new int[]{0, 0};
        if (f0 != null && f0.shownTick > 0) {
            e.setInvisible(false);
            e.tickCount = f0.shownTick;
        }
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
            // 넘친 거리는 가는 끝을 뺀 몸으로 잰다 (꼬리 끝 몇 픽셀이 잘리는 것은 괜찮다)
            over = Math.max(over, (int) Math.max(Math.max(bx - p[5], by - p[6]), Math.max(p[7] + 1 - (bx + box), p[8] + 1 - (by + box))));
        }
        return new int[]{inside, Math.max(0, over)};
    }
}
