package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.net.MagicActionPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 마법 뽑기 창 (별빛 수정 구체): [분야 선택 뽑기] [무작위 뽑기] [인챈트 재설정].
 * 갈색·크림 중세 틀에 보라빛 장식. 뽑으면 「룬석 소환진」 연출(MagicFx) 뒤에 결과 목록을 보여 준다.
 */
public class MagicGachaScreen extends Screen {
    public static final String[] CATS = {"attack", "guard", "dodge", "passive", "mover"};
    static final String[] GRADES = {"common", "uncommon", "rare", "epic", "legendary", "yamato"};
    private static final int PURPLE = 0xFF6A4AA8, PURPLE_HI = 0xFF9C7AE0;

    private JsonObject data;
    private int tab = 0; // 0 선택, 1 무작위, 2 재설정
    private int cat = 0;
    private final MagicFx fx = new MagicFx();
    private List<MagicFx.Card> shown = new ArrayList<>();
    private String rerollText = "";
    private String rerollGrade = "";
    private Component status;
    private boolean statusOk;
    private int statusTicks;
    private int fx0, fy0, W, H;
    private final List<Object[]> hits = new ArrayList<>(); // {x, y, w, h, Runnable, enabled, label, kind}

    public MagicGachaScreen(JsonObject data) {
        super(Component.translatable("rtshop.magic.title"));
        this.data = data;
    }

    // ------------------------------------------------------------------ 데이터

    public void update(JsonObject o, boolean result) {
        this.data = o;
        if (result) {
            List<MagicFx.Card> cards = new ArrayList<>();
            if (o.has("results")) {
                for (JsonElement e : o.getAsJsonArray("results")) cards.add(card(e.getAsJsonObject()));
            }
            if (o.has("reroll")) {
                JsonObject r = o.getAsJsonObject("reroll");
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                cards.add(new MagicFx.Card(book, r.get("name").getAsString(), r.get("grade").getAsString()));
                rerollText = r.get("name").getAsString();
                rerollGrade = r.get("grade").getAsString();
            }
            shown = cards;
            fx.start(cards);
            if (minecraft != null && minecraft.level != null && o.has("x")) {
                MagicClient.FLARE.put(new net.minecraft.core.BlockPos(o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()),
                        System.currentTimeMillis());
            }
        }
    }

    static ItemStack stackOf(JsonObject e) {
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(e.has("item") ? e.get("item").getAsString() : ""));
        ItemStack s = new ItemStack(item == null ? Items.BOOK : item);
        if (e.has("nbt") && !e.get("nbt").getAsString().isEmpty()) {
            try {
                s.getOrCreateTag().merge(TagParser.parseTag(e.get("nbt").getAsString()));
            } catch (Exception ignored) {
            }
        }
        return s;
    }

    /** 스킬 이름: Epic Fight 번역 키 skill.모드.이름 이 있으면 그것, 없으면 아이템 이름. */
    static String skillName(JsonObject e, ItemStack s) {
        String sk = e.has("skill") ? e.get("skill").getAsString() : "";
        if (!sk.isEmpty() && sk.contains(":")) {
            String key = "skill." + sk.replace(':', '.');
            if (net.minecraft.client.resources.language.I18n.exists(key)) {
                return stripCodes(Component.translatable(key).getString());
            }
            return sk.substring(sk.indexOf(':') + 1).replace('_', ' ');
        }
        return e.has("name") ? e.get("name").getAsString() : s.getHoverName().getString();
    }

    /** 다른 모드 번역에 섞인 색 코드(§)와 장식 괄호를 걷어 낸다 (창 글꼴에서 깨지지 않게). */
    static String stripCodes(String s) {
        return s.replaceAll("§.", "").replace("『", "").replace("』", "").trim();
    }

    private MagicFx.Card card(JsonObject e) {
        ItemStack s = stackOf(e);
        return new MagicFx.Card(s, skillName(e, s), e.has("grade") ? e.get("grade").getAsString() : "common");
    }

    private int tickets(String k) {
        return data.has("tickets") ? data.getAsJsonObject("tickets").get(k).getAsInt() : 0;
    }

    private int cost(String k) {
        return data.has("costs") ? data.getAsJsonObject("costs").get(k).getAsInt() : 1;
    }

    private double[] chances() {
        JsonArray a;
        if (tab == 0) a = data.getAsJsonObject("cats").getAsJsonObject(CATS[cat]).getAsJsonArray("chances");
        else if (tab == 1) a = data.getAsJsonObject("random").getAsJsonArray("chances");
        else a = data.getAsJsonArray("enchantChances");
        double[] d = new double[GRADES.length];
        for (int i = 0; i < d.length && i < a.size(); i++) d[i] = a.get(i).getAsDouble();
        return d;
    }

    private static String tr(String k, Object... a) {
        return Component.translatable(k, a).getString();
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 160;
    }

    // ------------------------------------------------------------------ 점검용

    public boolean fxActiveForTest() {
        return fx.active();
    }

    public int shownForTest() {
        return shown.size();
    }

    public void tabForTest(int t) {
        tab = t;
    }

    public void catForTest(int c) {
        cat = c;
    }

    public void pullForTest(int n) {
        pull(n);
    }

    public void rerollForTest() {
        reroll();
    }

    public String rerollTextForTest() {
        return rerollText;
    }

    public JsonObject dataForTest() {
        return data;
    }

    // ------------------------------------------------------------------ 동작

    private void pull(int n) {
        JsonObject a = new JsonObject();
        a.addProperty("mode", tab == 0 ? "select" : "random");
        a.addProperty("cat", CATS[cat]);
        a.addProperty("n", n);
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.PULL, a.toString()));
    }

    private void reroll() {
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.REROLL, ""));
    }

    @Override
    protected void init() {
        W = Math.min(width - 8, 430);
        H = Math.min(height - 8, 252);
        fx0 = (width - W) / 2;
        fy0 = (height - H) / 2;
        ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.REFRESH, ""));
    }

    @Override
    public void tick() {
        if (statusTicks > 0 && --statusTicks == 0) status = null;
    }

    private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean enabled, Runnable r) {
        boolean hov = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        Ui.button(g, font, x, y, w, h, label, hov, enabled);
        hits.add(new Object[]{x, y, w, h, r, enabled});
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        hits.clear();
        boolean fxOn = fx.active();
        int x0 = fx0, y0 = fy0;
        Ui.woodFrame(g, x0, y0, W, H);
        // 제목 띠 (보랏빛 장식선)
        g.fill(x0 + 8, y0 + 6, x0 + W - 8, y0 + 22, 0xFF2A1A3E);
        g.fill(x0 + 8, y0 + 22, x0 + W - 8, y0 + 23, PURPLE_HI);
        g.drawString(font, title, x0 + 14, y0 + 10, Ui.C_GOLD, false);
        // 티켓 수
        int tx = x0 + W - 12;
        String[][] tk = {{"reroll", "rtshop:enchant_scroll"}, {"random", "rtshop:magic_ticket"}, {"select", "rtshop:magic_ticket_select"}};
        for (String[] k : tk) {
            String n = String.valueOf(tickets(k[0]));
            tx -= font.width(n);
            g.drawString(font, n, tx, y0 + 10, Ui.C_CREAM, false);
            tx -= 18;
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(k[1]));
            if (it != null) g.renderItem(new ItemStack(it), tx, y0 + 6);
            tx -= 8;
        }
        // 탭
        String[] tabs = {tr("rtshop.magic.tab.select"), tr("rtshop.magic.tab.random"), tr("rtshop.magic.tab.reroll")};
        int tw = (W - 24) / 3;
        for (int i = 0; i < 3; i++) {
            final int k = i;
            int bx = x0 + 12 + i * tw;
            boolean sel = tab == i;
            g.fill(bx, y0 + 28, bx + tw - 4, y0 + 44, sel ? PURPLE : 0xFF3E2512);
            g.fill(bx, y0 + 43, bx + tw - 4, y0 + 44, sel ? PURPLE_HI : 0xFF5C3A1E);
            g.drawString(font, Ui.clip(font, tabs[i], tw - 12), bx + (tw - 4 - font.width(Ui.clip(font, tabs[i], tw - 12))) / 2, y0 + 32,
                    sel ? 0xFFF4ECFF : Ui.C_CREAM, false);
            hits.add(new Object[]{bx, y0 + 28, tw - 4, 16, (Runnable) () -> tab = k, true});
        }
        // 양피지 몸통
        int px = x0 + 12, py = y0 + 50, pw = W - 24, ph = H - 86;
        g.fill(px, py, px + pw, py + ph, Ui.C_PARCH);
        g.fill(px, py, px + pw, py + 1, Ui.C_PARCH_SHADE);
        int left = px + 8, top = py + 8;
        if (tab == 0) {
            // 분야 고르기
            for (int i = 0; i < CATS.length; i++) {
                final int k = i;
                int by = top + i * 22;
                boolean sel = cat == i;
                int count = data.getAsJsonObject("cats").getAsJsonObject(CATS[i]).get("count").getAsInt();
                g.fill(left, by, left + 112, by + 18, sel ? 0xFF4A3470 : 0xFFE0CDA4);
                g.fill(left, by, left + 3, by + 18, sel ? PURPLE_HI : Ui.C_PARCH_SHADE);
                g.drawString(font, tr("rtshop.magic.cat." + CATS[i]), left + 8, by + 5, sel ? 0xFFF4ECFF : Ui.C_INK, false);
                String c = String.valueOf(count);
                g.drawString(font, c, left + 106 - font.width(c), by + 5, sel ? 0xFFD8C8F8 : Ui.C_INK_SOFT, false);
                hits.add(new Object[]{left, by, 112, 18, (Runnable) () -> cat = k, true});
            }
            g.drawString(font, Ui.clip(font, tr("rtshop.magic.select_hint"), 112), left, top + 5 * 22 + 4, Ui.C_INK_SOFT, false);
            oddsPanel(g, left + 124, top, pw - 140, tr("rtshop.magic.cat." + CATS[cat]));
            pullButtons(g, mx, my, left + 124, py + ph - 26, pw - 140, "select");
        } else if (tab == 1) {
            int count = data.getAsJsonObject("random").get("count").getAsInt();
            g.drawString(font, Ui.clip(font, tr("rtshop.magic.random_desc", count), pw - 16), left, top, Ui.C_INK, false);
            oddsPanel(g, left, top + 16, pw - 16, tr("rtshop.magic.tab.random"));
            pullButtons(g, mx, my, left, py + ph - 26, pw - 16, "random");
        } else {
            rerollPanel(g, mx, my, left, top, pw - 16, py + ph);
        }
        // 아래 단추
        int by = y0 + H - 30;
        button(g, mx, my, x0 + 12, by, 96, 18, tr("rtshop.gacha.buy_tickets"), !fxOn,
                () -> ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.SHOP, "")));
        if (data.has("canEdit") && data.get("canEdit").getAsBoolean()) {
            button(g, mx, my, x0 + W - 150, by, 64, 18, tr("rtshop.magic.edit"), !fxOn,
                    () -> ShopNet.CHANNEL.sendToServer(new MagicActionPacket(MagicActionPacket.EDIT, "")));
        }
        button(g, mx, my, x0 + W - 80, by, 68, 18, tr("rtshop.enhodds.close"), !fxOn, this::onClose);
        if (status != null) {
            String s = Ui.clip(font, status.getString(), W - 260);
            g.drawString(font, s, x0 + 116, by + 5, statusOk ? 0xFF8FD67A : 0xFFE88070, false);
        }
        if (data.has("error")) g.drawString(font, Ui.clip(font, tr("rtshop.gacha.broken"), W - 30), x0 + 14, y0 + H - 46, 0xFFE06050, false);
        // 결과 목록 (연출이 끝난 뒤)
        if (!fxOn && !shown.isEmpty()) resultPanel(g, mx, my);
        super.render(g, mx, my, partial);
        fx.render(g, font, width, height);
    }

    private void oddsPanel(GuiGraphics g, int x, int y, int w, String head) {
        g.drawString(font, Ui.clip(font, tr("rtshop.magic.odds_of", head), w), x, y, Ui.C_INK, false);
        double[] d = chances();
        int row = 0;
        for (int i = 0; i < GRADES.length; i++) {
            if (d[i] <= 0) continue;
            int yy = y + 14 + row * 14;
            String gname = tr("rtshop.gacha.grade." + GRADES[i]);
            g.fill(x, yy + 1, x + 3, yy + 10, GachaColors.argb(GRADES[i]));
            g.drawString(font, gname, x + 7, yy + 2, GachaColors.dark(GRADES[i]), false);
            int bx = x + 62, bw = Math.max(10, w - 120);
            g.fill(bx, yy + 3, bx + bw, yy + 8, 0xFFD2BE94);
            g.fill(bx, yy + 3, bx + Math.max(1, (int) (bw * Math.min(1, d[i]))), yy + 8, GachaColors.argb(GRADES[i]));
            String p = GachaScreen.pct(d[i]);
            g.drawString(font, p, x + w - font.width(p), yy + 2, Ui.C_INK, false);
            row++;
        }
        if (row == 0) g.drawString(font, tr("rtshop.magic.empty"), x, y + 16, 0xFFA03020, false);
        String gu = data.has("tenGuarantee") ? data.get("tenGuarantee").getAsString() : "";
        if (!gu.isEmpty() && tab != 2) {
            g.drawString(font, Ui.clip(font, tr("rtshop.gacha.guarantee_short", tr("rtshop.gacha.grade." + gu)), w), x, y + 18 + 6 * 14,
                    Ui.C_INK_SOFT, false);
        }
    }

    private void pullButtons(GuiGraphics g, int mx, int my, int x, int y, int w, String mode) {
        int c = cost(mode);
        int have = tickets(mode);
        boolean idle = !fx.active();
        String t1 = tr("rtshop.magic.pull_n", 1, c);
        String t10 = tr("rtshop.magic.pull_n", 10, c * 10);
        int bw = (w - 6) / 2;
        button(g, mx, my, x, y, bw, 20, t1, idle && have >= c, () -> pull(1));
        button(g, mx, my, x + bw + 6, y, bw, 20, t10, idle && have >= c * 10, () -> pull(10));
    }

    private void rerollPanel(GuiGraphics g, int mx, int my, int x, int y, int w, int bottom) {
        String held = data.has("held") ? data.get("held").getAsString() : "";
        boolean ok = data.has("heldOk") && data.get("heldOk").getAsBoolean();
        ItemStack s = minecraft != null && minecraft.player != null ? minecraft.player.getMainHandItem() : ItemStack.EMPTY;
        g.drawString(font, Ui.clip(font, tr("rtshop.magic.reroll_desc"), w), x, y, Ui.C_INK, false);
        int iy = y + 16;
        g.fill(x, iy, x + 24, iy + 24, 0xFFD9C592);
        if (!s.isEmpty()) g.renderItem(s, x + 4, iy + 4);
        if (held.isEmpty() || s.isEmpty()) {
            g.drawString(font, Ui.clip(font, tr("rtshop.magic.reroll_hold"), w - 32), x + 30, iy + 8, Ui.C_INK_SOFT, false);
        } else {
            g.drawString(font, Ui.clip(font, s.getHoverName().getString(), w - 32), x + 30, iy + 2, Ui.C_INK, false);
            var ench = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(s);
            String now = ench.isEmpty() ? tr("rtshop.magic.no_enchant")
                    : String.join(", ", ench.entrySet().stream().map(en -> en.getKey().getFullname(en.getValue()).getString()).toList());
            g.drawString(font, Ui.clip(font, tr("rtshop.magic.current", now), w - 32), x + 30, iy + 13, ok ? 0xFF5A3E9A : 0xFFA03020, false);
        }
        if (!ok && !held.isEmpty()) g.drawString(font, Ui.clip(font, tr("rtshop.magic.reroll_bad_item"), w), x, iy + 28, 0xFFA03020, false);
        if (!rerollText.isEmpty()) {
            g.drawString(font, Ui.clip(font, tr("rtshop.magic.reroll_got", rerollText), w), x, iy + 28, GachaColors.dark(rerollGrade), false);
        }
        oddsPanel(g, x, iy + 42, w, tr("rtshop.magic.tab.reroll"));
        int c = cost("reroll");
        button(g, mx, my, x, bottom - 26, w, 20, tr("rtshop.magic.reroll_n", c), !fx.active() && ok && tickets("reroll") >= c, this::reroll);
    }

    private void resultPanel(GuiGraphics g, int mx, int my) {
        int n = shown.size();
        int cols = n > 5 ? 5 : n;
        int rows = (n + cols - 1) / cols;
        int cw = 78, ch = 30;
        int w = cols * (cw + 4) + 12, h = rows * (ch + 4) + 30;
        int x = (width - w) / 2, y = (height - h) / 2;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(0, 0, width, height, 0x88000000);
        Ui.woodFrame(g, x - 4, y - 4, w + 8, h + 8);
        g.drawString(font, tr("rtshop.magic.results"), x + 6, y + 4, Ui.C_GOLD, false);
        for (int i = 0; i < n; i++) {
            MagicFx.Card c = shown.get(i);
            int cx = x + 6 + (i % cols) * (cw + 4), cy = y + 18 + (i / cols) * (ch + 4);
            g.fill(cx, cy, cx + cw, cy + ch, 0xFF22183A);
            g.fill(cx, cy, cx + cw, cy + 2, GachaColors.argb(c.grade));
            g.renderItem(c.icon, cx + 3, cy + 8);
            g.drawString(font, Ui.clip(font, c.name, cw - 24), cx + 22, cy + 6, 0xFFEDE4FF, false);
            g.drawString(font, Ui.clip(font, tr("rtshop.gacha.grade." + c.grade), cw - 24), cx + 22, cy + 17, GachaColors.argb(c.grade), false);
        }
        String close = tr("rtshop.magic.click_close");
        g.drawString(font, close, x + w - 6 - font.width(close), y + h - 10, 0xFFB8A8D8, false);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (fx.active()) {
            fx.skip();
            return true;
        }
        if (!shown.isEmpty()) {
            shown = new ArrayList<>();
            return true;
        }
        for (Object[] h : hits) {
            int x = (int) h[0], y = (int) h[1], w = (int) h[2], hh = (int) h[3];
            if ((boolean) h[5] && mx >= x && mx < x + w && my >= y && my < y + hh) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1F));
                ((Runnable) h[4]).run();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (fx.active() && key != 256) {
            fx.skip();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        fx.skip();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
