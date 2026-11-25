package com.rtshop.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rtshop.magic.MagicMenu;
import com.rtshop.net.MagicActionPacket;
import com.rtshop.net.ShopNet;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.EnchantFx;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 별빛 수정 구체의 Cinematic Interactions 장면 (인챈트 테이블 장면의 방식을 따른다).
 * <ul>
 * <li>구체 앞으로 걸어가 몸을 숙이고, 두 손이 받침대 위에 놓인다.</li>
 * <li>구체 둘레에 룬석 다섯 개가 오각으로 떠 있다: 만지면 그 분야를 고르고(선택 뽑기), 다시 만지면 풀린다(무작위 뽑기).</li>
 * <li>받침대 앞쪽의 소환 문양을 만지면 문양이 구체로 날아들어 뽑기 의식이 벌어진다.</li>
 * <li>인챈트 재설정: 화면 위 가방에서 무기·장비를 집어 받침대 오른쪽 앞의 푸른 원 위에 올려놓고, 그 옆의 푸른 문양을 만진다.</li>
 * <li>확률·티켓·천장·오늘의 룬은 화면 옆의 룬 제단 패널에 겹쳐 보인다 (자리·글꼴·크기는 [화면 설정]).</li>
 * </ul>
 */
public final class CinematicOrbStation extends AddonMenuStation<MagicMenu> {
    private static final ResourceLocation STONES = new ResourceLocation("rtshop", "textures/gui/magic/stones.png");
    private static final ResourceLocation STONES_ON = new ResourceLocation("rtshop", "textures/gui/magic/stones_on.png");
    private static final String[] CATS = MagicGachaScreen.CATS;
    private static final double RING = 0.37, STONE = 0.10, SIGIL = 0.085;
    public static final double RITUAL = 1.6, PEAK = 0.55;

    public enum Target {
        NONE, STONE, PULL1, REROLL, HUD, ITEM, PLACE
    }

    public final Vec3 top, orb;
    private final Minecraft mc = Minecraft.getInstance();
    private final RandomSource random = RandomSource.create();
    private final MagicGachaScreen panel;
    private JsonObject data;
    /** 고른 분야 (-1 = 고르지 않음 = 무작위 뽑기). */
    public int cat = -1;
    public Target target = Target.NONE;
    public int hover = -1;
    private int hudHover = -1;
    private int lastHoverKey = -1;
    private final int[][] hud = new int[2][4];
    private final boolean[] hudOn = new boolean[2];
    private double ritualAt = -10.0;
    private Target ritualKind = Target.NONE;
    private Vec3 ritualFrom = Vec3.ZERO;
    private boolean burstDone;
    private double burstAt = -10.0;
    private double refuseAt = -10.0;
    private Target refuseWhat = Target.NONE;
    private double glow;
    private final MagicFx fx = new MagicFx();
    private List<MagicFx.Card> shown = new ArrayList<>();
    private List<MagicFx.Card> waiting;
    private String bestGrade = "";
    private Component note;
    private boolean noteOk;
    private double noteAt = -10.0;

    public CinematicOrbStation(BlockPos pos, Direction side) {
        super(pos, side, MagicMenu.class, 1); // 0번 칸 = 제단 위 (재설정할 장비)
        this.top = new Vec3(pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5);
        this.orb = top.add(0.0, 0.45, 0.0);
        this.data = MagicClient.lastView != null ? MagicClient.lastView : new JsonObject();
        this.panel = new MagicGachaScreen(data);
    }

    // ------------------------------------------------------------------ 서버 답

    public void update(JsonObject o, boolean result) {
        data = o;
        panel.setData(o);
        if (!result) return;
        List<MagicFx.Card> cards = MagicGachaScreen.cardsOf(o);
        if (cards.isEmpty()) return;
        if (o.has("pityHit")) notice(Component.translatable("rtshop.magic.pity_hit"), true);
        if (ritualTime() >= 0.0 && !burstDone) waiting = cards; // 의식의 절정에서 펼친다
        else reveal(cards);
    }

    private void reveal(List<MagicFx.Card> cards) {
        waiting = null;
        shown = cards;
        bestGrade = MagicGachaScreen.bestGradeOf(cards);
        fx.start(cards);
        MagicClient.FLARE.put(pos, System.currentTimeMillis());
    }

    public void notice(Component msg, boolean ok) {
        note = msg;
        noteOk = ok;
        noteAt = Scene.clock();
        if (!ok) { // 서버가 거절: 의식을 멈춘다
            ritualAt = -10.0;
            waiting = null;
            refuseAt = Scene.clock();
            refuseWhat = ritualKind;
        }
    }

    // ------------------------------------------------------------------ 창 내용 읽기

    private boolean ready() {
        return data.has("cats");
    }

    private String mode() {
        return cat >= 0 ? "select" : "random";
    }

    private JsonObject source() {
        return cat >= 0 ? data.getAsJsonObject("cats").getAsJsonObject(CATS[cat]) : data.getAsJsonObject("random");
    }

    private int cost(Target t) {
        if (!ready()) return 1;
        if (t == Target.REROLL) return panel.cost("reroll");
        JsonObject s = source();
        return s.has("cost1") ? s.get("cost1").getAsInt() : panel.cost(mode());
    }

    /** 제단 위에 올려 둔 장비. */
    public ItemStack onAltar() {
        return menu() == null ? ItemStack.EMPTY : slot(MagicMenu.ALTAR);
    }

    private ItemStack carried() {
        MagicMenu m = menu();
        return m == null ? ItemStack.EMPTY : m.getCarried();
    }

    private int have(Target t) {
        return panel.tickets(t == Target.REROLL ? "reroll" : mode());
    }

    private boolean affordable(Target t) {
        if (!ready()) return false;
        if (t == Target.REROLL) return !onAltar().isEmpty() && data.has("heldOk") && data.get("heldOk").getAsBoolean() && have(t) >= cost(t);
        return have(t) >= cost(t) && source().get("count").getAsInt() > 0;
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    // ------------------------------------------------------------------ 자리

    @Override
    public Vec3 center() {
        return top;
    }

    @Override
    public SoundEvent touchSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    /** 화면 위 가방: 재설정할 장비를 집어 제단에 올린다 (의식·결과가 보이는 동안은 숨긴다). */
    @Override
    public boolean showsInventory() {
        return ritualTime() < 0.0 && !fx.active() && shown.isEmpty();
    }

    /** 제단 위 칸의 자리: 받침대 오른쪽 앞, 푸른 원 위에 떠 있다. */
    public Vec3 itemSpot() {
        double clock = Scene.clock();
        Vec3 at = top.add(side().scale(0.28)).add(right().scale(0.15)).add(0.0, 0.14 + 0.01 * Math.sin(clock * 1.4), 0.0);
        double t = ritualTime();
        if (t >= 0.0 && ritualKind == Target.REROLL) at = Anim.lerp(at, orb, 0.75 * Anim.hump(Anim.window(t, 0.0, 0.9)));
        return at;
    }

    /** 룬석 i 의 자리: 구체를 둘러싼 오각형 (플레이어 쪽을 보는 면). */
    public Vec3 stonePos(int i) {
        double a = Math.toRadians(-90 + 72 * i);
        double clock = Scene.clock();
        return orb.add(right().scale(Math.cos(a) * RING)).add(0.0, -Math.sin(a) * RING + 0.012 * Math.sin(clock * 1.3 + i * 2.1), 0.0)
                .add(side().scale(0.05));
    }

    /** 소환 문양의 자리: 소환은 받침대 왼쪽 앞, 재설정은 오른쪽 모서리 (제단 위 칸 옆). */
    public Vec3 sigilPos(Target t) {
        double clock = Scene.clock();
        double x = t == Target.PULL1 ? -0.17 : 0.36;
        double out = t == Target.REROLL ? 0.20 : 0.30;
        Vec3 at = top.add(side().scale(out)).add(right().scale(x)).add(0.0, 0.115 + 0.008 * Math.sin(clock * 1.6 + x * 9.0), 0.0);
        double d = clock - refuseAt;
        if (t == refuseWhat && d >= 0.0 && d < 0.35) at = at.add(right().scale(0.02 * Math.sin(d * 60.0) * (1.0 - d / 0.35)));
        return at;
    }

    @Override
    public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = new Vec3(top.x + side.getStepX() * 0.86, player.getY(), top.z + side.getStepZ() * 0.86);
        boolean walk = Math.hypot(stand.x - player.getX(), stand.z - player.getZ()) > 0.06;
        return walk ? List.of(stand) : List.of();
    }

    @Override
    public CameraRig.Pose lean() {
        Vec3 eye = top.add(side().scale(1.18)).add(0.0, 1.0, 0.0);
        Vec3 look = top.add(0.0, 0.42, 0.0).subtract(eye);
        // 오른쪽으로 조금 돌려, 장면이 화면 왼쪽에 놓이게 한다 (오른쪽에는 룬 제단 패널이 겹친다)
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z)) + (ClientConfig.get().magicPanelLeft ? -10.0f : 10.0f);
        float pitch = (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z)));
        return new CameraRig.Pose(eye, yaw, pitch, 0.0f);
    }

    @Override
    public double leanFov() {
        return 68.0;
    }

    @Override
    public float bodyPitch() {
        return 30.0f;
    }

    @Override
    public Vec3 rest(boolean right) {
        return top.add(side().scale(0.38)).add(right().scale(right ? 0.26 : -0.26)).add(0.0, 0.062, 0.0);
    }

    @Override
    protected Vec3 restPoint(int slot) {
        return top;
    }

    // ------------------------------------------------------------------ 가리키기 · 누르기

    private static boolean under(double[] mouse, Vec3 center, double radius) {
        double[] at = Picking.toGui(center);
        double[] edge = Picking.toGui(center.add(0.0, radius, 0.0));
        if (at == null || edge == null) return false;
        double r = Math.max(8.0, Math.hypot(at[0] - edge[0], at[1] - edge[1]));
        return Math.hypot(mouse[0] - at[0], mouse[1] - at[1]) < r;
    }

    @Override
    public void updateCursor(boolean overPanel) {
        clearCursor();
        if (menu() == null || !ready() || fx.active() || !shown.isEmpty()) return;
        double[] mouse = Picking.mouseGui();
        for (int i = 0; i < hud.length; i++) {
            if (hudOn[i] && mouse[0] >= hud[i][0] && mouse[0] < hud[i][0] + hud[i][2] && mouse[1] >= hud[i][1] && mouse[1] < hud[i][1] + hud[i][3]) {
                target = Target.HUD;
                hudHover = i;
                return;
            }
        }
        if (ritualTime() >= 0.0 || overPanel) return;
        // 제단 위 칸: 든 것이 있으면 올려놓기, 빈손이면 집어 들기
        if (under(mouse, itemSpot(), 0.11)) {
            boolean carrying = !carried().isEmpty();
            if (carrying || !onAltar().isEmpty()) {
                target = carrying ? Target.PLACE : Target.ITEM;
                aim = itemSpot();
                return;
            }
        }
        for (Target t : new Target[]{Target.PULL1, Target.REROLL}) {
            if (under(mouse, sigilPos(t), SIGIL * 1.25)) {
                target = t;
                aim = sigilPos(t);
                return;
            }
        }
        for (int i = 0; i < CATS.length; i++) {
            if (under(mouse, stonePos(i), STONE * 1.3)) {
                target = Target.STONE;
                hover = i;
                aim = stonePos(i);
                return;
            }
        }
    }

    @Override
    public void clearCursor() {
        super.clearCursor();
        target = Target.NONE;
        hover = -1;
        hudHover = -1;
    }

    @Override
    public Vec3 handTarget(Vec3 aim, Vec3 shoulder, boolean carrying, double tap) {
        Vec3 dir = aim.subtract(shoulder).normalize();
        return aim.subtract(dir.scale(0.14 - 0.05 * tap)).add(0.0, -0.03, 0.0);
    }

    @Override
    public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        Vec3 reach = new Vec3(hand.x - shoulder.x, 0.0, hand.z - shoulder.z).normalize();
        return hand.add(reach.scale(0.075)).add(0.0, -0.05, 0.0);
    }

    /** 의식 동안: 두 손을 구체 양옆으로 들어 올린다. */
    @Override
    public Vec3[] handsOverride() {
        double t = ritualTime();
        if (t < 0.0) return null;
        double raise = Anim.hump(Anim.window(t, 0.0, 0.85));
        Vec3 c = orb.add(side().scale(0.14)).add(0.0, -0.20 + 0.08 * raise, 0.0);
        double spread = 0.24 - 0.04 * raise;
        return new Vec3[]{c.add(right().scale(spread)), c.add(right().scale(-spread))};
    }

    private void sound(SoundEvent e, float volume, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(e, pitch, volume));
    }

    @Override
    protected void act(int button, boolean shift) {
    }

    @Override
    public void click(int button, boolean shift) {
        if (fx.active()) {
            fx.skip();
            return;
        }
        if (!shown.isEmpty()) {
            shown = new ArrayList<>();
            return;
        }
        if (menu() == null || !ready()) return;
        switch (target) {
            case HUD -> {
                if (hudHover < 0 || ritualTime() >= 0.0) return;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                if (hudHover == 0) CinematicOrb.afterLeaveClient = () -> mc.setScreen(new MagicUiScreen()); // 화면 설정은 장면에서 물러난 뒤에 뜬다
                else CinematicOrb.afterLeave = MagicActionPacket.EDIT;
                Scene.leave();
            }
            case ITEM, PLACE -> {
                if (ritualTime() >= 0.0) return;
                slotClick(MagicMenu.ALTAR, button, shift);
                sound(SoundEvents.AMETHYST_BLOCK_PLACE, 0.8f, target == Target.PLACE ? 1.0f : 1.3f);
            }
            case STONE -> {
                if (ritualTime() >= 0.0 || hover < 0) return;
                CinematicOrb.tap();
                cat = cat == hover ? -1 : hover;
                sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, cat < 0 ? 0.8f : 1.1f + hover * 0.08f);
                EnchantFx.sparkle(stonePos(hover), 0.12);
            }
            case PULL1, REROLL -> {
                if (ritualTime() >= 0.0) return;
                CinematicOrb.tap();
                if (affordable(target)) {
                    startRitual(target);
                } else {
                    refuseAt = Scene.clock();
                    refuseWhat = target;
                    sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.7f, 0.6f);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean keyPressed(int key) {
        if (key != 256 && fx.active()) {
            fx.skip();
            return true;
        }
        return false;
    }

    private void startRitual(Target t) {
        ritualKind = t;
        ritualFrom = sigilPos(t);
        ritualAt = Scene.clock();
        burstDone = false;
        waiting = null;
        if (t == Target.REROLL) {
            ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.REROLL, ""));
        } else {
            JsonObject a = new JsonObject();
            a.addProperty("mode", mode());
            a.addProperty("cat", cat >= 0 ? CATS[cat] : "");
            a.addProperty("n", 1);
            ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.PULL, a.toString()));
        }
        sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.45f, 1.7f);
        sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f, 0.9f);
    }

    /** 의식 진행도 0~1 (진행 중이 아니면 -1). */
    public double ritualTime() {
        double t = (Scene.clock() - ritualAt) / RITUAL;
        return t >= 0.0 && t <= 1.0 ? t : -1.0;
    }

    @Override
    public boolean busy() {
        return ritualTime() >= 0.0 || fx.active() || !shown.isEmpty();
    }

    @Override
    public double cameraKick() {
        double t = ritualTime();
        if (t < 0.0) return 0.0;
        double peak = PEAK / RITUAL;
        return t < peak ? 0.6 * Anim.smoother(t / peak) : Math.exp(-(t - peak) * RITUAL * 3.2);
    }

    @Override
    public double flash() {
        double d = Scene.clock() - burstAt;
        return d >= 0.0 && d < 0.5 ? Math.pow(1.0 - d / 0.5, 2.0) * 0.26 : 0.0;
    }

    @Override
    public int flashColor() {
        return 0xE8DCFF;
    }

    @Override
    public void onEnd() {
        fx.skip();
        super.onEnd();
    }

    private Vec3 afar() {
        double a = random.nextDouble() * Math.PI * 2.0;
        return top.add(Math.cos(a) * 1.6, 0.6 + random.nextDouble() * 0.8, Math.sin(a) * 1.6);
    }

    @Override
    public void tick() {
        if (menu() == null || mc.player == null) return;
        int key = target == Target.STONE ? hover : target == Target.NONE || target == Target.HUD ? -1 : 10 + target.ordinal();
        if (!onAltar().isEmpty() && mc.player.tickCount % 5 == 0) EnchantFx.sparkle(itemSpot(), 0.08);
        if (key != lastHoverKey && key >= 0) {
            sound(SoundEvents.AMETHYST_BLOCK_STEP, 0.5f, 0.9f + random.nextFloat() * 0.3f);
        }
        lastHoverKey = key;
        if (mc.player.tickCount % 3 == 0) EnchantFx.sparkle(orb, 0.2);
        if (cat >= 0 && mc.player.tickCount % 4 == 0) EnchantFx.stream(stonePos(cat), orb, 1);
        if (target == Target.PULL1 || target == Target.REROLL) {
            Vec3 sigil = sigilPos(target);
            EnchantFx.stream(afar(), sigil, 1);
            EnchantFx.stream(sigil, orb, 1);
        }
        double t = ritualTime();
        double sec = t * RITUAL;
        if (t >= 0.0 && sec < PEAK) {
            for (int i = 0; i < 3; i++) {
                EnchantFx.stream(afar(), orb, 1);
                EnchantFx.gather(orb, 0.55);
            }
            if (cat < 0) {
                for (int i = 0; i < CATS.length; i++) EnchantFx.stream(stonePos(i), orb, 1);
            }
        }
        if (t >= 0.0 && !burstDone && sec >= PEAK) {
            burstDone = true;
            burstAt = Scene.clock();
            EnchantFx.burst(orb, 1.4);
            sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, 1.3f);
            sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.7f);
            MagicClient.FLARE.put(pos, System.currentTimeMillis());
            if (waiting != null) reveal(waiting);
        }
    }

    // ------------------------------------------------------------------ 그리기 (세계)

    private double presence() {
        if (Scene.phase == Scene.Phase.INTRO) return Anim.smoother(Anim.window(Scene.introProgress(), 0.45, 1.0));
        if (Scene.phase == Scene.Phase.OUTRO) return 1.0 - Anim.smoother(Scene.time / 0.45);
        return Scene.phase == Scene.Phase.ACTIVE ? 1.0 : 0.0;
    }

    @Override
    public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        Vec3 cam = camera.getPosition();
        MagicClient.sceneAt(pos, (float) presence());
        double clock = Scene.clock();
        double t = ritualTime();
        boolean sigilHover = target == Target.PULL1 || target == Target.REROLL;
        double want = Scene.phase == Scene.Phase.INTRO ? 0.5 * Anim.smoother(Anim.window(Scene.introProgress(), 0.45, 1.0))
                : Scene.phase == Scene.Phase.ACTIVE ? 0.5 + (cat >= 0 ? 0.15 : 0.0) + (sigilHover ? 0.3 : 0.0) + (t >= 0.0 ? 1.4 * Anim.hump(Anim.window(t, 0.0, 0.75)) : 0.0)
                : 0.0;
        glow = Anim.approach(glow, want, want > glow ? 6.0 : 2.5, Scene.frameDt);
        double spin = clock * 0.25 + (t >= 0.0 ? 4.0 * Anim.smoother(t) : 0.0);
        float g = (float) glow;
        boolean refused = clock - refuseAt >= 0.0 && clock - refuseAt < 0.5;
        EnchantFx.CirclePalette pal = refused ? EnchantFx.CirclePalette.RED : EnchantFx.CirclePalette.VIOLET;
        // 받침대 위의 소환진 두 겹
        EnchantFx.circle(pose, buffers, cam, top.add(0.0, 0.012, 0.0), 0.47, spin, pal, 0.50f * g);
        EnchantFx.circle(pose, buffers, cam, top.add(0.0, 0.02, 0.0), 0.25, -spin * 1.7, pal, 0.32f * g);
        // 구체 둘레에 서 있는 큰 소환진 (룬석들이 놓인 원)
        EnchantFx.circleBillboard(pose, buffers, camera, orb.subtract(side().scale(0.03)), RING * 1.12, -spin * 0.6, pal, 0.26f * g);
        EnchantFx.billboard(pose, buffers, camera, orb, 0.26, clock * 0.4, EnchantFx.GLOW, 0.30f * g, 0.18f * g, 0.50f * g);
        if (t >= 0.0) {
            double peak = PEAK / RITUAL;
            double beam = t < peak ? 0.0 : 1.0 - Anim.smoother((t - peak) / (1.0 - peak));
            if (beam > 0.01) {
                float b = (float) (beam * 1.3);
                float[] c = gradeColor();
                EnchantFx.beam(pose, buffers, cam, new Vec3(orb.x, top.y, orb.z), 1.9, 0.5 + 0.3 * beam, clock * 0.9, c[0] * b, c[1] * b, c[2] * b);
            }
        }
    }

    /** 빛기둥 색: 언제나 보랏빛 (결과의 등급 색을 쓰면 카드가 뒤집히기 전에 등급이 드러난다). */
    private float[] gradeColor() {
        return new float[]{0.7f, 0.5f, 1.0f};
    }

    @Override
    public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        Camera camera = mc.gameRenderer.getMainCamera();
        double p = presence();
        if (p <= 0.01 || !ready()) return;
        double clock = Scene.clock();
        double t = ritualTime();
        float appear = (float) Anim.outBack(Anim.clamp01(p), 1.6);
        // 룬석 다섯: 고르지 않은 룬석도 어두워지지 않는다 (고른 것만 조금 커지고 금빛 고리와 빛으로 표시)
        for (int i = 0; i < CATS.length; i++) {
            Vec3 at = stonePos(i);
            boolean chosen = cat == i;
            boolean lit = chosen || cat < 0;
            boolean hov = target == Target.STONE && hover == i;
            float size = (float) (STONE * appear * (hov ? 1.18 : 1.0) * (chosen ? 1.16 + 0.03 * Math.sin(clock * 4.0) : cat >= 0 ? 0.92 : 1.0));
            float b = (float) p;
            if (chosen || hov) EnchantFx.billboard(pose, buffers, camera, at, size * 2.2, 0.0, EnchantFx.GLOW, 0.55f * b, 0.42f * b, 0.18f * b);
            if (chosen) EnchantFx.circleBillboard(pose, buffers, camera, at, size * 1.25, clock * 0.9, EnchantFx.CirclePalette.VIOLET, 0.9f * b);
            float bright = lit || hov ? 1.0f : 0.86f;
            quad(pose, buffers, camera, STONES_ON, at, size * 0.8, size, i / 5f, (i + 1) / 5f, bright, (float) p);
            String name = tr("rtshop.magic.cat." + CATS[i]);
            label(pose, buffers, camera, at.add(0.0, -size - 0.03, 0.0), name, 0.0082f * appear, lit ? 0xFFFFE9B0 : 0xFFCFC2F0, (float) p);
        }
        // 제단 위 칸: 받침대의 푸른 원과, 그 위에 떠서 천천히 도는 장비
        Vec3 spot = itemSpot();
        ItemStack on = onAltar();
        boolean spotHover = target == Target.PLACE || target == Target.ITEM;
        Vec3 ring = top.add(side().scale(0.28)).add(right().scale(0.15)).add(0.0, 0.016, 0.0);
        EnchantFx.circle(pose, buffers, cam, ring, spotHover ? 0.105 : 0.09, clock * 0.6, EnchantFx.CirclePalette.LAPIS,
                (float) p * (spotHover ? 0.95f : on.isEmpty() ? 0.45f : 0.7f));
        if (!on.isEmpty()) {
            pose.pushPose();
            pose.translate(spot.x - cam.x, spot.y - cam.y, spot.z - cam.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float) ((clock * 40.0) % 360.0)));
            float sz = 0.3f * appear * (target == Target.ITEM ? 1.1f : 1.0f);
            pose.scale(sz, sz, sz);
            mc.getItemRenderer().renderStatic(on, net.minecraft.world.item.ItemDisplayContext.FIXED, 0xF000F0,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, pose, buffers, mc.level, 0);
            pose.popPose();
        } else if (target != Target.PLACE) {
            label(pose, buffers, camera, ring.add(0.0, 0.07, 0.0), tr("rtshop.magic.altar_slot"), 0.0058f * appear, 0xFF9CC8FF, (float) p * 0.85f);
        }
        // 소환 문양
        for (Target s : new Target[]{Target.PULL1, Target.REROLL}) {
            if (t >= 0.0 && s == ritualKind) continue;
            float dim = t >= 0.0 ? 0.4f : 1.0f;
            sigil(pose, buffers, camera, s, sigilPos(s), appear, target == s, affordable(s), dim * (float) p);
        }
        if (t >= 0.0) {
            double flight = Anim.inOutCubic(Anim.window(t * RITUAL, 0.0, 0.45));
            if (flight < 1.0) {
                Vec3 at = Anim.lerp(ritualFrom, orb, flight).add(0.0, 0.12 * Anim.hump(flight), 0.0);
                sigil(pose, buffers, camera, ritualKind, at, (float) (1.0 - 0.8 * flight), true, true, 1.6f);
            }
        }
    }

    private void sigil(PoseStack pose, MultiBufferSource buffers, Camera camera, Target s, Vec3 at, float appear, boolean hovered, boolean affordable, float brightness) {
        if (appear <= 0.01f) return;
        double clock = Scene.clock();
        float size = (float) (SIGIL * appear * (hovered ? 1.2 : 1.0));
        float b = brightness * (hovered ? 1.35f : 0.85f) * Math.min(1.0f, appear);
        boolean blue = s == Target.REROLL;
        float r = !affordable ? 0.5f : blue ? 0.25f : 0.55f, g = !affordable ? 0.12f : blue ? 0.55f : 0.45f, bl = !affordable ? 0.16f : 1.0f;
        EnchantFx.billboard(pose, buffers, camera, at, size * 1.8, 0.0, EnchantFx.GLOW, r * b * 0.35f, g * b * 0.35f, bl * b * 0.35f);
        double turn = clock * (hovered ? 1.8 : 0.5);
        EnchantFx.circleBillboard(pose, buffers, camera, at, size, turn,
                !affordable ? EnchantFx.CirclePalette.RED : blue ? EnchantFx.CirclePalette.LAPIS : EnchantFx.CirclePalette.VIOLET, b);
        float alpha = Math.min(1.0f, brightness) * Math.min(1.0f, appear);
        Vector3f look = camera.getLookVector();
        Vec3 text = at.subtract(look.x() * 0.01, look.y() * 0.01, look.z() * 0.01);
        String head = s == Target.REROLL ? tr("rtshop.magic.sigil.reroll") : tr("rtshop.magic.sigil.times");
        label(pose, buffers, camera, text.add(0.0, 0.018, 0.0), head, 0.0088f * appear * (hovered ? 1.2f : 1.0f), affordable ? 0xFFF4EEFF : 0xFFFF6B6B, alpha);
        String sub = "×" + cost(s);
        label(pose, buffers, camera, text.add(0.0, -0.046, 0.0), sub, 0.0062f * appear, affordable ? (blue ? 0xFF9CC8FF : 0xFFB9A6FF) : 0xFF7A4A55, alpha);
    }

    /** 카메라를 보는 사각형 (룬석 그림의 한 칸). */
    private static void quad(PoseStack pose, MultiBufferSource buffers, Camera camera, ResourceLocation tex, Vec3 at, double hw, double hh,
                             float u0, float u1, float bright, float alpha) {
        Vector3f l = camera.getLeftVector(), u = camera.getUpVector();
        Vec3 cam = camera.getPosition();
        float x = (float) (at.x - cam.x), y = (float) (at.y - cam.y), z = (float) (at.z - cam.z);
        float lx = l.x() * (float) hw, ly = l.y() * (float) hw, lz = l.z() * (float) hw;
        float ux = u.x() * (float) hh, uy = u.y() * (float) hh, uz = u.z() * (float) hh;
        VertexConsumer vc = buffers.getBuffer(RenderType.text(tex));
        Matrix4f m = pose.last().pose();
        int c = Math.max(0, Math.min(255, Math.round(bright * 255))), a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        vc.vertex(m, x + lx + ux, y + ly + uy, z + lz + uz).color(c, c, c, a).uv(u0, 0f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x + lx - ux, y + ly - uy, z + lz - uz).color(c, c, c, a).uv(u0, 1f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x - lx - ux, y - ly - uy, z - lz - uz).color(c, c, c, a).uv(u1, 1f).uv2(0xF000F0).endVertex();
        vc.vertex(m, x - lx + ux, y - ly + uy, z - lz + uz).color(c, c, c, a).uv(u1, 0f).uv2(0xF000F0).endVertex();
    }

    /** 카메라를 보는 글자 (가운데 정렬). */
    private void label(PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 at, String s, float scale, int color, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        if (a < 12 || scale <= 0.0001f) return;
        Vec3 cam = camera.getPosition();
        Font font = mc.font;
        Component c = MagicUi.c(s, MagicUi.body());
        pose.pushPose();
        pose.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
        pose.mulPose(camera.rotation());
        pose.scale(-scale, -scale, scale);
        // 글자 그림자는 화면 설정에서 켠 경우에만
        font.drawInBatch(c, -font.width(c) / 2f, -4.5f, (a << 24) | (color & 0xFFFFFF), MagicUi.shadow(), pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
    }

    // ------------------------------------------------------------------ 그리기 (화면)

    @Override
    public void renderScreen(GuiGraphics g, Font font, float alpha) {
        int w = g.guiWidth(), h = g.guiHeight();
        panel.prepare(mc, w, h);
        boolean reroll = target == Target.REROLL || target == Target.ITEM || target == Target.PLACE || !onAltar().isEmpty()
                || (ritualTime() >= 0.0 && ritualKind == Target.REROLL);
        panel.tabForTest(reroll ? 2 : cat >= 0 ? 0 : 1);
        if (cat >= 0) panel.catForTest(cat);
        hudOn[0] = hudOn[1] = false;
        boolean fxOn = fx.active();
        if (Scene.phase == Scene.Phase.ACTIVE && alpha > 0.6f && !fxOn && shown.isEmpty() && ready()) {
            // 패널: 크기 배율과 자리(오른쪽·왼쪽)는 화면 설정을 따른다
            float sc = MagicUi.scale();
            int pw = Math.min(170, Math.round(w * 0.3f / sc));
            int real = Math.round(pw * sc);
            int px = ClientConfig.get().magicPanelLeft ? 10 : w - real - 10, py = 30;
            boolean edit = data.has("canEdit") && data.get("canEdit").getAsBoolean();
            int bw = edit ? (pw - 6) / 2 : pw;
            int by = MagicGachaScreen.HUD_H + 8;
            g.pose().pushPose();
            g.pose().translate(px, py, 0);
            g.pose().scale(sc, sc, 1f);
            panel.renderHud(g, 0, 0, pw);
            panel.hudButton(g, 0, by, bw, 16, tr("rtshop.magic.ui_settings"), hudHover == 0);
            if (edit) panel.hudButton(g, bw + 6, by, bw, 16, tr("rtshop.magic.edit"), hudHover == 1);
            if (note != null && Scene.clock() - noteAt < 5.0) {
                panel.hudLine(g, 0, by + 24, pw, note.getString(), noteOk ? 0xFF9FE88A : 0xFFFF9080);
            }
            g.pose().popPose();
            hud[0] = new int[]{px, py + Math.round(by * sc), Math.round(bw * sc), Math.round(16 * sc)};
            hudOn[0] = true;
            if (edit) {
                hud[1] = new int[]{px + Math.round((bw + 6) * sc), py + Math.round(by * sc), Math.round(bw * sc), Math.round(16 * sc)};
                hudOn[1] = true;
            }
        }
        if (!fxOn && !shown.isEmpty()) panel.drawResults(g, shown, w, h);
        fx.render(g, font, w, h);
    }

    @Override
    public List<Component> tooltip(ItemStack carried) {
        if (!ready() || fx.active() || !shown.isEmpty()) return null;
        List<Component> lines = new ArrayList<>();
        switch (target) {
            case STONE -> {
                if (hover < 0) return null;
                int n = data.getAsJsonObject("cats").getAsJsonObject(CATS[hover]).get("count").getAsInt();
                lines.add(Component.translatable("rtshop.magic.cat." + CATS[hover]).append(" · ").append(Component.translatable("rtshop.magic.kinds", n))
                        .withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable(cat == hover ? "rtshop.magic.tip.unchoose" : "rtshop.magic.tip.choose").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case ITEM -> {
                lines.add(onAltar().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("rtshop.magic.take_tip").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case PLACE -> {
                lines.add(Component.translatable("rtshop.magic.altar_slot").withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("rtshop.magic.place_tip").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case PULL1 -> {
                lines.add(Component.translatable("rtshop.magic.times").withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable(cat >= 0 ? "rtshop.magic.tip.from" : "rtshop.magic.tip.from_all",
                        cat >= 0 ? Component.translatable("rtshop.magic.cat." + CATS[cat]) : Component.empty()).withStyle(ChatFormatting.GRAY));
                boolean ok = have(target) >= cost(target);
                lines.add(Component.translatable("rtshop.magic.cost." + mode(), cost(target)).append(" (" + have(target) + ")")
                        .withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
                if (affordable(target)) lines.add(Component.translatable("rtshop.magic.tip.touch").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            case REROLL -> {
                lines.add(Component.translatable("rtshop.magic.reroll_btn").withStyle(ChatFormatting.WHITE));
                boolean held = data.has("heldOk") && data.get("heldOk").getAsBoolean();
                if (onAltar().isEmpty()) {
                    lines.add(Component.translatable("rtshop.magic.reroll_hold").withStyle(ChatFormatting.RED));
                } else if (!held) {
                    lines.add(Component.translatable("rtshop.magic.reroll_bad_item").withStyle(ChatFormatting.RED));
                } else {
                    lines.add(Component.translatable("rtshop.magic.reroll_desc").withStyle(ChatFormatting.GRAY));
                }
                boolean ok = have(target) >= cost(target);
                lines.add(Component.translatable("rtshop.magic.cost.reroll", cost(target)).append(" (" + have(target) + ")")
                        .withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
                if (affordable(target)) lines.add(Component.translatable("rtshop.magic.tip.touch").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            default -> {
                return null;
            }
        }
        return lines;
    }

    @Override
    public Component hint() {
        return Component.translatable("rtshop.magic.hint");
    }

    // ------------------------------------------------------------------ 점검용

    public void chooseForTest(int c) {
        cat = c;
    }

    public boolean pullForTest() {
        if (!affordable(Target.PULL1) || ritualTime() >= 0.0) return false;
        startRitual(Target.PULL1);
        return true;
    }

    /** 가방 칸(0~35)의 물건을 제단 위로 올린다 (가방에서 Shift 클릭한 것과 같다). */
    public void placeForTest(int inventoryIndex) {
        MagicMenu m = menu();
        if (m == null || mc.gameMode == null) return;
        mc.gameMode.handleInventoryMouseClick(m.containerId, MagicMenu.slotOfInventory(inventoryIndex), 0,
                net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
    }

    public boolean inventoryShownForTest() {
        return showsInventory();
    }

    /** 제단 위 칸이 화면 안에 들어오는가 (화면 좌표). */
    public double[] guiOfItemSpotForTest() {
        return Picking.toGui(itemSpot());
    }

    public boolean rerollForTest() {
        if (!affordable(Target.REROLL) || ritualTime() >= 0.0) return false;
        startRitual(Target.REROLL);
        return true;
    }

    public boolean fxActiveForTest() {
        return fx.active();
    }

    public int shownForTest() {
        return shown.size();
    }

    public void dismissForTest() {
        fx.skip();
        shown = new ArrayList<>();
    }

    public boolean readyForTest() {
        return ready() && menu() != null;
    }

    /** 룬석·문양이 화면 안에 들어오는가 (화면 좌표). */
    public double[] guiOfForTest(int stone) {
        return Picking.toGui(stone >= 0 ? stonePos(stone) : sigilPos(Target.PULL1));
    }
}
