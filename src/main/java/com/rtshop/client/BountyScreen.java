package com.rtshop.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** 현상금 게시판: 게시판에 붙은 토벌 의뢰 중에서 골라 맡는다 (하루 몇 개, 한 주 몇 개까지). */
public class BountyScreen extends Screen {
    private static final int W = 300, H = 222, ROW = 26;
    private JsonObject data;
    private final List<JsonObject> rows = new ArrayList<>();
    private int fx, fy, scroll;

    public BountyScreen(JsonObject data) {
        super(Component.translatable("rtshop.hunt.board_screen"));
        set(data);
    }

    public static void open(JsonObject o) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BountyScreen b) {
            b.set(o);
        } else {
            mc.setScreen(new BountyScreen(o));
        }
    }

    private void set(JsonObject o) {
        data = o;
        rows.clear();
        // 맡을 수 있는 의뢰가 위, 맡은 의뢰·끝난 의뢰가 아래
        for (int pass = 0; pass < 3; pass++) {
            for (JsonElement e : o.getAsJsonArray("list")) {
                JsonObject r = e.getAsJsonObject();
                boolean acc = r.get("accepted").getAsBoolean(), done = r.get("done").getAsBoolean();
                int group = done ? 2 : acc ? 1 : 0;
                if (group == pass) rows.add(r);
            }
        }
    }

    public int rowsForTest() {
        return rows.size();
    }

    public int takenForTest() {
        return data.get("dailyTaken").getAsInt() + data.get("weeklyTaken").getAsInt();
    }

    /** 점검용: 첫 번째로 맡을 수 있는 일일 의뢰를 맡는다. */
    public boolean takeFirstForTest() {
        for (JsonObject r : rows) {
            if (!r.get("accepted").getAsBoolean() && !r.get("weekly").getAsBoolean()) {
                take(r);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void init() {
        fx = (width - W) / 2;
        fy = (height - H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int visible() {
        return (H - 62) / ROW;
    }

    private boolean full(boolean weekly) {
        return weekly ? data.get("weeklyTaken").getAsInt() >= data.get("weeklyMax").getAsInt()
                : data.get("dailyTaken").getAsInt() >= data.get("dailyMax").getAsInt();
    }

    private ItemStack icon(String entity) {
        EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(entity));
        SpawnEggItem egg = t == null ? null : SpawnEggItem.byId(t);
        return egg == null ? new ItemStack(net.minecraft.world.item.Items.PAPER) : new ItemStack(egg);
    }

    /** 몹 모습 (3D 모델을 작게). 만들 수 없는 몹은 null. */
    private static final java.util.Map<String, net.minecraft.world.entity.LivingEntity> MODELS = new java.util.HashMap<>();

    static net.minecraft.world.entity.LivingEntity model(String entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        net.minecraft.world.entity.LivingEntity e = MODELS.get(entity);
        if (e != null && e.level() == mc.level) return e;
        try {
            EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(ResourceLocation.tryParse(entity));
            net.minecraft.world.entity.Entity made = t == null ? null : t.create(mc.level);
            e = made instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
        } catch (Throwable ex) {
            e = null;
        }
        MODELS.put(entity, e);
        return e;
    }

    /** 몹 그림: 칸(box×box) 안에 맞춰 천천히 도는 3D 모습. 없으면 알 아이콘. */
    static void drawMob(GuiGraphics g, String entity, int x, int y, int box, float spin) {
        net.minecraft.world.entity.LivingEntity e = model(entity);
        if (e == null) {
            g.renderItem(new ItemStack(net.minecraft.world.item.Items.PAPER), x + (box - 16) / 2, y + (box - 16) / 2);
            return;
        }
        float size = Math.max(e.getBbHeight(), e.getBbWidth());
        int scale = Math.max(2, (int) (box * 0.8F / Math.max(0.5F, size)));
        g.enableScissor(x, y, x + box, y + box);
        e.yBodyRot = 180F + spin;
        e.yBodyRotO = e.yBodyRot;
        e.setYRot(e.yBodyRot);
        e.yHeadRot = e.yBodyRot;
        e.yHeadRotO = e.yBodyRot;
        e.setXRot(0);
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, x + box / 2, y + box - 2, scale,
                new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateX(-0.25F), null, e);
        g.disableScissor();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = title.getString();
        g.drawString(font, t, fx + (W - font.width(t)) / 2, fy + 9, Ui.C_GOLD, true);
        // 맡은 수
        String quota = Component.translatable("rtshop.hunt.board_quota", data.get("dailyTaken").getAsInt(), data.get("dailyMax").getAsInt(),
                data.get("weeklyTaken").getAsInt(), data.get("weeklyMax").getAsInt()).getString();
        g.drawString(font, quota, fx + 10, fy + 28, Ui.C_CREAM, false);
        int lx = fx + 8, ly = fy + 40, lw = W - 16, lh = H - 50;
        Ui.bevel(g, lx, ly, lw, lh, Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("rtshop.hunt.none"), lx + 8, ly + 8, Ui.C_INK_SOFT, false);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - visible())));
        for (int k = 0; k < visible() && scroll + k < rows.size(); k++) {
            JsonObject r = rows.get(scroll + k);
            int ry = ly + 3 + k * ROW;
            boolean acc = r.get("accepted").getAsBoolean(), done = r.get("done").getAsBoolean(), weekly = r.get("weekly").getAsBoolean();
            Ui.bevel(g, lx + 3, ry, lw - 6, ROW - 2, acc ? Ui.C_PARCH_HI : Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            if (acc && !done) g.fill(lx + 3, ry, lx + 5, ry + ROW - 2, 0xFFA02C1E); // 붉은 책갈피: 맡은 의뢰
            g.fill(lx + 6, ry + 1, lx + 28, ry + ROW - 3, 0x30000000);
            drawMob(g, r.get("entity").getAsString(), lx + 6, ry + 1, 22, (System.currentTimeMillis() % 7200) / 20F);
            String tag = Component.translatable(weekly ? "rtshop.hunt.tag_weekly" : "rtshop.hunt.tag_daily").getString();
            String name = Component.translatable("rtshop.hunt.goal_kill", r.get("name").getAsString(), r.get("need").getAsInt()).getString();
            g.drawString(font, tag, lx + 32, ry + 3, weekly ? 0xFF2F5E8A : 0xFF6A4A1E, false);
            g.drawString(font, Ui.clip(font, name, lw - 134), lx + 32 + font.width(tag) + 4, ry + 3, done ? Ui.C_INK_SOFT : Ui.C_INK, false);
            String sub = done ? Component.translatable("rtshop.hunt.row_done").getString()
                    : acc ? Component.translatable("rtshop.hunt.row_progress", r.get("have").getAsInt(), r.get("need").getAsInt()).getString()
                    : Component.translatable("rtshop.hunt.row_offer").getString();
            if (!acc && !done) {
                // 아직 안 맡은 의뢰: 난이도 별
                int st = r.has("stars") ? Math.max(1, Math.min(5, r.get("stars").getAsInt())) : 1;
                String dl = Component.translatable("rtshop.hunt.difficulty").getString() + " ";
                Ui.scaledText(g, font, dl, lx + 32, ry + 14, 0.8F, Ui.C_INK_SOFT, false);
                int sx = lx + 32 + (int) (font.width(dl) * 0.8F);
                int col = st >= 5 ? 0xFFB0281C : st >= 4 ? 0xFFC0601C : 0xFFB8861E;
                Ui.scaledText(g, font, "★".repeat(st), sx, ry + 14, 0.8F, col, false);
                Ui.scaledText(g, font, "☆".repeat(5 - st), sx + (int) (font.width("★".repeat(st)) * 0.8F), ry + 14, 0.8F, 0x80604020, false);
            } else {
                Ui.scaledText(g, font, sub, lx + 32, ry + 14, 0.8F, done ? Ui.C_GREEN : Ui.C_INK_SOFT, false);
            }
            String gold = Ui.num(r.get("gold").getAsLong()) + " G";
            int bx = lx + lw - 64;
            g.drawString(font, gold, bx - 6 - font.width(gold), ry + 8, 0xFF8A5A10, false);
            if (!acc && !done) {
                boolean can = !full(weekly);
                Ui.button(g, font, bx, ry + 3, 56, 16, Component.translatable("rtshop.hunt.take").getString(), can && Ui.inside(mx, my, bx, ry + 3, 56, 16), can);
            } else if (done) {
                g.drawString(font, "✔", bx + 24, ry + 8, Ui.C_GREEN, false);
            }
        }
        super.render(g, mx, my, partial);
    }

    private void take(JsonObject r) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("bounty take " + data.get("entity").getAsInt() + " " + r.get("i").getAsInt());
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int lx = fx + 8, ly = fy + 40, lw = W - 16;
        for (int k = 0; k < visible() && scroll + k < rows.size(); k++) {
            JsonObject r = rows.get(scroll + k);
            int ry = ly + 3 + k * ROW, bx = lx + lw - 64;
            if (!r.get("accepted").getAsBoolean() && !r.get("done").getAsBoolean() && !full(r.get("weekly").getAsBoolean())
                    && Ui.inside(mx, my, bx, ry + 3, 56, 16)) {
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                take(r);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll -= (int) Math.signum(delta);
        return true;
    }

    net.minecraft.client.gui.Font getFontForTest() {
        return this.font;
    }
}
