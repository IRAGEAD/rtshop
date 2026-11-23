package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rtshop.net.QuestEditActionPacket;
import com.rtshop.net.QuestEditorPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 퀘스트 편집기 (OP 전용, /rtshop quest edit).
 * 왼쪽: 퀘스트 / NPC 목록. 오른쪽: 고르거나 새로 만든 항목의 입력 칸. 저장하면 서버가 파일에 쓰고 바로 적용한다.
 */
public class QuestEditScreen extends Screen {
    private static final int LIST_W = 108;
    private static final int LIST_ROW = 12;
    private static final int MAX_OBJ = 4;
    private static final int MAX_ITEMS = 3;
    private static final int MAX_CHOICES = 3;
    private static final String[] PROFESSIONS = {"librarian", "farmer", "fisherman", "weaponsmith", "armorer", "toolsmith",
            "cleric", "cartographer", "butcher", "shepherd", "mason", "fletcher", "leatherworker"};
    private static final QuestDef.Type[] TYPES = QuestDef.Type.values();

    // ------------------------------------------------------------------ 편집 중인 내용

    private static final class ObjDraft {
        QuestDef.Type type = QuestDef.Type.DELIVER;
        String target = "";
        String count = "1";
        String text = "";
        String dim = "minecraft:overworld";
        String mod = "";
    }

    private static final class ItemDraft {
        String item = "";
        String count = "1";
        String mod = "";
    }

    private static final class ChoiceDraft {
        String text = "";
        String flags = "";
    }

    private static final class QDraft {
        String id = "", title = "", chain = "", order = "0", giver = "", turnIn = "";
        String requires = "", reqFlags = "", blockFlags = "";
        String intro = "", progress = "", done = "";
        String gold = "0", rewardFlags = "";
        final List<ObjDraft> objs = new ArrayList<>();
        final List<ItemDraft> items = new ArrayList<>();
        final List<ChoiceDraft> choices = new ArrayList<>();
    }

    private static final class NDraft {
        String id = "", name = "", profession = "librarian", greeting = "";
    }

    private record Hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled, Consumer<Integer> action,
                       Supplier<ItemStack> icon) {
    }

    private QuestEditorPacket data;
    private boolean npcMode = false;
    /** 목록에서 고른 번호. -1 = 새로 만드는 중 또는 아무것도 안 고름. */
    private int selected = -1;
    private boolean creating = false;
    private int listScroll = 0;
    private int tab = 0;
    private QDraft q;
    private NDraft n;
    /** 고른 항목을 편집할 수 없는 이유 (형식 오류 등). */
    private String brokenReason = null;
    private boolean dirty = false;
    private String origId = "";
    private String pendingSelectId = null;
    private int confirmIndex = Integer.MIN_VALUE;
    private boolean confirmClose = false;
    private boolean confirmDelete = false;
    private Component status = null;
    private boolean statusOk = true;
    private int statusTicks = 0;

    private final List<Hit> hits = new ArrayList<>();
    private int fx, fy, W, H;

    public QuestEditScreen(QuestEditorPacket data) {
        super(Component.translatable("rtshop.qedit.title"));
        this.data = data;
    }

    // ------------------------------------------------------------------ 서버에서 온 내용

    public void update(QuestEditorPacket packet) {
        String keepId = selectedId();
        this.data = packet;
        selected = indexOf(keepId);
        if (!dirty && !creating) {
            loadSelected();
        }
        rebuild();
    }

    public void showResult(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 200;
        if (ok) {
            String id = pendingSelectId;
            pendingSelectId = null;
            creating = false;
            dirty = false;
            selected = id == null ? -1 : indexOf(id);
            loadSelected();
            rebuild();
        } else {
            pendingSelectId = null;
        }
    }

    private List<QuestEditorPacket.Entry> entries() {
        return npcMode ? data.npcs : data.quests;
    }

    private String selectedId() {
        List<QuestEditorPacket.Entry> l = entries();
        return selected >= 0 && selected < l.size() ? l.get(selected).id() : null;
    }

    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        List<QuestEditorPacket.Entry> l = entries();
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static String join(List<String> l) {
        return String.join(", ", l);
    }

    private static List<String> split(String s) {
        List<String> out = new ArrayList<>();
        for (String p : s.split("[,\\s]+")) {
            if (!p.isBlank()) {
                out.add(p.trim());
            }
        }
        return out;
    }

    /** 고른 항목을 편집 칸으로 불러온다. */
    private void loadSelected() {
        q = null;
        n = null;
        brokenReason = null;
        origId = "";
        confirmDelete = false;
        List<QuestEditorPacket.Entry> l = entries();
        if (selected < 0 || selected >= l.size()) {
            return;
        }
        QuestEditorPacket.Entry e = l.get(selected);
        origId = e.id();
        try {
            JsonObject o = JsonParser.parseString(e.json()).getAsJsonObject();
            if (npcMode) {
                n = new NDraft();
                n.id = e.id();
                n.name = o.has("name") ? o.get("name").getAsString() : "";
                n.profession = o.has("profession") ? o.get("profession").getAsString() : "librarian";
                n.greeting = o.has("greeting") ? o.get("greeting").getAsString() : "";
                return;
            }
            QuestDef d = QuestDef.parse(o);
            q = new QDraft();
            q.id = d.id;
            q.title = d.title;
            q.chain = d.chain;
            q.order = Integer.toString(d.order);
            q.giver = d.giver;
            q.turnIn = d.turnIn;
            q.requires = join(d.requires);
            q.reqFlags = join(d.requiresFlags);
            q.blockFlags = join(d.blockedByFlags);
            q.intro = d.intro;
            q.progress = d.progress;
            q.done = d.done;
            q.gold = Long.toString(d.rewardGold);
            q.rewardFlags = join(d.rewardFlags);
            for (QuestDef.Objective ob : d.objectives) {
                ObjDraft od = new ObjDraft();
                od.type = ob.type;
                od.count = Integer.toString(ob.type == QuestDef.Type.VISIT ? ob.radius : ob.count);
                od.text = ob.text;
                od.mod = ob.mod;
                if (ob.type == QuestDef.Type.VISIT) {
                    od.dim = ob.target;
                    od.target = ob.x + " " + ob.y + " " + ob.z;
                } else if (ob.type != QuestDef.Type.PAY) {
                    od.target = ob.target;
                }
                if (ob.type == QuestDef.Type.PAY) {
                    od.count = Integer.toString(ob.count);
                }
                q.objs.add(od);
            }
            for (QuestDef.ItemReward ir : d.rewardItems) {
                ItemDraft id = new ItemDraft();
                id.item = ir.item;
                id.count = Integer.toString(ir.count);
                id.mod = ir.mod;
                q.items.add(id);
            }
            for (QuestDef.Choice c : d.choices) {
                ChoiceDraft cd = new ChoiceDraft();
                cd.text = c.text;
                cd.flags = join(c.flags);
                q.choices.add(cd);
            }
        } catch (Exception ex) {
            q = null;
            n = null;
            brokenReason = e.status().isEmpty() ? String.valueOf(ex.getMessage()) : e.status();
        }
    }

    private void startNew() {
        creating = true;
        dirty = true;
        selected = -1;
        origId = "";
        brokenReason = null;
        confirmDelete = false;
        tab = 0;
        if (npcMode) {
            q = null;
            n = new NDraft();
            n.id = uniqueId("npc_");
            n.name = Component.translatable("rtshop.qedit.new_npc_name").getString();
        } else {
            n = null;
            q = new QDraft();
            q.id = uniqueId("quest_");
            q.title = Component.translatable("rtshop.qedit.new_quest_title").getString();
            q.giver = defaultGiver();
        }
        rebuild();
    }

    private String uniqueId(String prefix) {
        for (int i = 1; ; i++) {
            String id = prefix + i;
            if (indexOf(id) < 0) {
                return id;
            }
        }
    }

    private String defaultGiver() {
        for (String[] k : data.npcKeys) {
            if (k[0].startsWith("quest_")) {
                return k[0];
            }
        }
        return data.npcKeys.isEmpty() ? "bank" : data.npcKeys.get(0)[0];
    }

    private String npcLabel(String key) {
        if (key == null || key.isEmpty()) {
            return "-";
        }
        for (String[] k : data.npcKeys) {
            if (k[0].equals(key)) {
                return k[1];
            }
        }
        return key;
    }

    private ChoicePopup popup;

    private void openPopup(int x, int y, int w, int h, List<ChoicePopup.Option> options, String current, Consumer<String> pick) {
        popup = new ChoicePopup(x, y, w, h, this.height, this.font, options, current, v -> {
            pick.accept(v);
            popup = null;
        });
    }

    private List<ChoicePopup.Option> npcOptions(boolean allowEmpty) {
        List<ChoicePopup.Option> l = new ArrayList<>();
        if (allowEmpty) {
            l.add(new ChoicePopup.Option("", tr("rtshop.qedit.same_as_giver")));
        }
        for (String[] k : data.npcKeys) {
            l.add(new ChoicePopup.Option(k[0], k[1]));
        }
        return l;
    }

    /** 모드 조건 버튼에 보일 글. */
    private static String condLabel(String cond) {
        if (cond == null || cond.isBlank()) {
            return tr("rtshop.qedit.cond_always");
        }
        boolean not = cond.startsWith("!");
        String id = not ? cond.substring(1) : cond;
        return tr(not ? "rtshop.qedit.cond_without" : "rtshop.qedit.cond_with", QuestText.modName(id));
    }

    /** 모드 조건 목록: 항상 / (고른 모드가 있으면) 있을 때·없을 때 / 모든 모드 (있을 때). */
    private static List<ChoicePopup.Option> condOptions(String cur) {
        List<ChoicePopup.Option> l = new ArrayList<>();
        l.add(new ChoicePopup.Option("", tr("rtshop.qedit.cond_always")));
        String id = cur == null ? "" : (cur.startsWith("!") ? cur.substring(1) : cur);
        if (!id.isBlank()) {
            l.add(new ChoicePopup.Option(id, condLabel(id)));
            l.add(new ChoicePopup.Option("!" + id, condLabel("!" + id)));
        }
        List<net.minecraftforge.forgespi.language.IModInfo> mods = new ArrayList<>(net.minecraftforge.fml.ModList.get().getMods());
        mods.sort(java.util.Comparator.comparing(m -> m.getDisplayName().toLowerCase(java.util.Locale.ROOT)));
        for (net.minecraftforge.forgespi.language.IModInfo m : mods) {
            String mid = m.getModId();
            if (mid.equals("minecraft") || mid.equals("forge") || mid.equals(id)) {
                continue;
            }
            l.add(new ChoicePopup.Option(mid, m.getDisplayName() + "  (" + mid + ")"));
        }
        return l;
    }

    /** 키 넣기 목록: 모든 조작 키 (이름 [지금 키]). */
    private static List<ChoicePopup.Option> keyOptions() {
        List<ChoicePopup.Option> l = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.KeyMapping[] all = mc.options.keyMappings.clone();
        java.util.Arrays.sort(all);
        for (net.minecraft.client.KeyMapping k : all) {
            l.add(new ChoicePopup.Option(k.getName(), net.minecraft.client.resources.language.I18n.get(k.getName()) + "  [" + k.getTranslatedKeyMessage().getString() + "]"));
        }
        return l;
    }

    /** 모드 도움말 줄 넣기 목록: 설치된 모드. */
    private static List<ChoicePopup.Option> modTipOptions() {
        List<ChoicePopup.Option> l = condOptions("");
        l.remove(0);
        return l;
    }

    private List<ChoicePopup.Option> typeOptions() {
        List<ChoicePopup.Option> l = new ArrayList<>();
        for (QuestDef.Type t : TYPES) {
            l.add(new ChoicePopup.Option(t.name(), tr(typeKey(t))));
        }
        return l;
    }

    /** 점검용 */
    public ChoicePopup popupForTest() {
        return popup;
    }

    /** 점검용: 이야기 탭에서 "키 넣기" 목록을 연다. */
    public void openKeyPopupForTest() {
        int kx = rx() + rw() - 4 - 62;
        openPopup(kx, formTop() + 1, 62, 12, keyOptions(), "", v -> insertAtCursor(0, "{key:" + v + "}"));
    }

    /** 점검용: 지금 퀘스트의 "받기 전 이야기". */
    public String introForTest() {
        return q == null ? "" : q.intro;
    }

    /** 점검용: 목표 탭에서 첫 목표의 종류 팝업을 연다. */
    public void openTypePopupForTest() {
        if (q != null && !q.objs.isEmpty()) {
            ObjDraft od = q.objs.get(0);
            openPopup(rx() + 4, formTop() + 1, 46, 14, typeOptions(), od.type.name(), v -> od.type = QuestDef.Type.valueOf(v));
        }
    }

    private String cycleNpc(String key, int dir, boolean allowEmpty) {
        List<String> keys = new ArrayList<>();
        if (allowEmpty) {
            keys.add("");
        }
        for (String[] k : data.npcKeys) {
            keys.add(k[0]);
        }
        if (keys.isEmpty()) {
            return key;
        }
        int i = keys.indexOf(key);
        i = i < 0 ? 0 : Math.floorMod(i + dir, keys.size());
        return keys.get(i);
    }

    // ------------------------------------------------------------------ 저장용 JSON

    private static int num(String s, String what) {
        try {
            return Integer.parseInt(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(Component.translatable("rtshop.qedit.err_number", what).getString());
        }
    }

    private JsonObject questJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", q.id.trim());
        o.addProperty("chain", q.chain);
        o.addProperty("order", num(q.order.isBlank() ? "0" : q.order, tr("rtshop.qedit.f_order")));
        o.addProperty("title", q.title);
        o.addProperty("giver", q.giver);
        o.addProperty("turnIn", q.turnIn);
        o.add("requires", arr(split(q.requires)));
        o.add("requiresFlags", arr(split(q.reqFlags)));
        o.add("blockedByFlags", arr(split(q.blockFlags)));
        o.addProperty("intro", q.intro);
        o.addProperty("progress", q.progress);
        o.addProperty("done", q.done);
        JsonArray objs = new JsonArray();
        int i = 0;
        for (ObjDraft od : q.objs) {
            i++;
            String what = tr("rtshop.qedit.objective_n", i);
            JsonObject jo = new JsonObject();
            jo.addProperty("type", od.type.name());
            int count = num(od.count.isBlank() ? "1" : od.count, what);
            if (count < 1) {
                throw new IllegalArgumentException(tr("rtshop.qedit.err_count", what));
            }
            switch (od.type) {
                case VISIT -> {
                    String[] p = od.target.trim().split("[,\\s]+");
                    if (p.length != 3) {
                        throw new IllegalArgumentException(tr("rtshop.qedit.err_visit", what));
                    }
                    jo.addProperty("target", od.dim);
                    jo.addProperty("x", num(p[0], what));
                    jo.addProperty("y", num(p[1], what));
                    jo.addProperty("z", num(p[2], what));
                    jo.addProperty("radius", count);
                    jo.addProperty("count", 1);
                }
                case PAY -> jo.addProperty("count", count);
                default -> {
                    if (od.target.isBlank()) {
                        throw new IllegalArgumentException(tr("rtshop.qedit.err_target", what));
                    }
                    jo.addProperty("target", od.target.trim());
                    jo.addProperty("count", count);
                }
            }
            if (!od.text.isBlank()) {
                jo.addProperty("text", od.text);
            }
            if (!od.mod.isBlank()) {
                jo.addProperty("mod", od.mod.trim());
            }
            objs.add(jo);
        }
        o.add("objectives", objs);
        JsonObject r = new JsonObject();
        r.addProperty("gold", Math.max(0, num(q.gold.isBlank() ? "0" : q.gold, tr("rtshop.qedit.f_gold"))));
        JsonArray items = new JsonArray();
        for (ItemDraft id : q.items) {
            if (id.item.isBlank()) {
                continue;
            }
            JsonObject jo = new JsonObject();
            jo.addProperty("item", id.item.trim());
            jo.addProperty("count", Math.max(1, num(id.count.isBlank() ? "1" : id.count, tr("rtshop.qedit.f_items"))));
            if (!id.mod.isBlank()) {
                jo.addProperty("mod", id.mod.trim());
            }
            items.add(jo);
        }
        r.add("items", items);
        r.add("flags", arr(split(q.rewardFlags)));
        o.add("reward", r);
        JsonArray ch = new JsonArray();
        for (ChoiceDraft cd : q.choices) {
            if (cd.text.isBlank()) {
                continue;
            }
            JsonObject jo = new JsonObject();
            jo.addProperty("text", cd.text);
            jo.add("flags", arr(split(cd.flags)));
            ch.add(jo);
        }
        o.add("choices", ch);
        // 서버와 같은 규칙으로 미리 확인 (틀린 곳을 바로 알려 주려고)
        QuestDef.parse(o);
        return o;
    }

    private static JsonArray arr(List<String> l) {
        JsonArray a = new JsonArray();
        for (String s : l) {
            a.add(s);
        }
        return a;
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private void save() {
        String json;
        int action;
        String id;
        try {
            if (npcMode) {
                if (n == null) {
                    return;
                }
                JsonObject o = new JsonObject();
                o.addProperty("id", n.id.trim());
                o.addProperty("name", n.name.trim());
                o.addProperty("profession", n.profession);
                o.addProperty("greeting", n.greeting);
                json = o.toString();
                action = QuestEditor.SAVE_NPC;
                id = n.id.trim();
            } else {
                if (q == null) {
                    return;
                }
                json = questJson().toString();
                action = QuestEditor.SAVE_QUEST;
                id = q.id.trim();
            }
        } catch (Exception e) {
            showLocal(Component.literal(e.getMessage()), false);
            return;
        }
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > QuestEditActionPacket.MAX_JSON - 100) {
            showLocal(Component.translatable("rtshop.qedit.err_too_long"), false);
            return;
        }
        pendingSelectId = id;
        ShopNet.CHANNEL.sendToServer(new QuestEditActionPacket(action, creating ? "" : origId, json));
    }

    private void delete() {
        if (creating) {
            creating = false;
            dirty = false;
            q = null;
            n = null;
            rebuild();
            return;
        }
        if (origId.isEmpty()) {
            return;
        }
        if (!confirmDelete) {
            confirmDelete = true;
            showLocal(Component.translatable("rtshop.qedit.confirm_delete"), false);
            return;
        }
        confirmDelete = false;
        pendingSelectId = null;
        ShopNet.CHANNEL.sendToServer(new QuestEditActionPacket(npcMode ? QuestEditor.DELETE_NPC : QuestEditor.DELETE_QUEST, origId, ""));
    }

    private void revert() {
        if (creating) {
            delete();
            return;
        }
        dirty = false;
        loadSelected();
        rebuild();
    }

    private void showLocal(Component msg, boolean ok) {
        status = msg;
        statusOk = ok;
        statusTicks = 160;
    }

    private void markDirty() {
        dirty = true;
        confirmClose = false;
    }

    // ------------------------------------------------------------------ 배치

    @Override
    protected void init() {
        W = Math.min(this.width - 8, 480);
        H = Math.min(this.height - 8, 270);
        fx = (this.width - W) / 2;
        fy = (this.height - H) / 2;
        if (q == null && n == null && brokenReason == null && selected >= 0) {
            loadSelected();
        }
        rebuild();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int listX() {
        return fx + 6;
    }

    private int listY() {
        return fy + 44;
    }

    private int listH() {
        return H - 44 - 28;
    }

    private int rx() {
        return fx + 6 + LIST_W + 6;
    }

    private int rw() {
        return W - LIST_W - 18;
    }

    private int formTop() {
        return fy + 44;
    }

    private int formH() {
        return H - 44 - 28;
    }

    private int bottomY() {
        return fy + H - 23;
    }

    private void hit(int x, int y, int w, int h, Supplier<String> label, BooleanSupplier enabled, Consumer<Integer> action) {
        hits.add(new Hit(x, y, w, h, label, enabled, action, null));
    }

    /** 아이콘이 붙은 버튼 (고른 아이템·몬스터를 보여 주고, 누르면 선택 목록을 연다). */
    private void iconHit(int x, int y, int w, int h, Supplier<String> label, Supplier<ItemStack> icon, Consumer<Integer> action) {
        hits.add(new Hit(x, y, w, h, label, () -> true, action, icon));
    }

    private void hit(int x, int y, int w, int h, String label, Runnable action) {
        hits.add(new Hit(x, y, w, h, () -> label, () -> true, b -> action.run(), null));
    }

    private EditBox box(int x, int y, int w, String value, int max, Consumer<String> onChange) {
        EditBox b = new EditBox(this.font, x, y, w, 12, Component.empty());
        b.setMaxLength(max);
        b.setValue(value == null ? "" : value);
        b.moveCursorToStart();
        b.setResponder(v -> {
            onChange.accept(v);
            markDirty();
        });
        return this.addRenderableWidget(b);
    }

    private MultiLineEditBox area(int x, int y, int w, int h, String value, Consumer<String> onChange) {
        MultiLineEditBox b = new MultiLineEditBox(this.font, x, y, w, h, Component.empty(), Component.empty());
        b.setValue(value == null ? "" : value);
        b.setValueListener(v -> {
            onChange.accept(v);
            markDirty();
        });
        return this.addRenderableWidget(b);
    }

    /** 칸 이름 (그리기용). */
    private record Label(int x, int y, String text, int color) {
    }

    private final List<Label> labels = new ArrayList<>();

    private void label(int x, int y, String key) {
        labels.add(new Label(x, y, tr(key), Ui.C_INK));
    }

    private void rebuild() {
        clearWidgets();
        hits.clear();
        labels.clear();
        // 맨 위: 퀘스트 / NPC 전환
        hit(listX(), fy + 27, 52, 14, () -> tr("rtshop.qedit.mode_quests"), () -> npcMode, b -> switchMode(false));
        hit(listX() + 56, fy + 27, 52, 14, () -> tr("rtshop.qedit.mode_npcs"), () -> !npcMode, b -> switchMode(true));
        // 목록 아래: 새로 만들기
        hit(listX(), bottomY(), LIST_W, 16, () -> tr(npcMode ? "rtshop.qedit.new_npc" : "rtshop.qedit.new_quest"),
                () -> !creating, b -> {
                    if (dirty && confirmIndex != -2) {
                        confirmIndex = -2;
                        showLocal(Component.translatable("rtshop.qedit.confirm_discard"), false);
                        return;
                    }
                    confirmIndex = Integer.MIN_VALUE;
                    startNew();
                });

        int x = rx();
        int w = rw();
        if (q != null || n != null || brokenReason != null) {
            int bx = x + w;
            bx -= 50;
            hit(bx, bottomY(), 50, 16, () -> tr("rtshop.qedit.save"), () -> (q != null || n != null) && pendingSelectId == null, b -> save());
            bx -= 54;
            hit(bx, bottomY(), 50, 16, () -> tr(creating ? "rtshop.qedit.cancel" : "rtshop.qedit.delete"), () -> true, b -> delete());
            if (!creating && brokenReason == null) {
                bx -= 54;
                hit(bx, bottomY(), 50, 16, () -> tr("rtshop.qedit.revert"), () -> dirty, b -> revert());
            }
            if (npcMode && n != null && !creating) {
                bx -= 64;
                hit(bx, bottomY(), 60, 16, () -> tr("rtshop.qedit.spawn_here"), () -> !dirty, b -> {
                    if (minecraft != null && minecraft.player != null) {
                        minecraft.player.connection.sendCommand("rtshop npc quest_" + n.id);
                        showLocal(Component.translatable("rtshop.qedit.spawned"), true);
                    }
                });
            }
        }
        if (npcMode) {
            if (n != null) {
                buildNpc(x, w);
            }
            return;
        }
        if (q == null) {
            return;
        }
        String[] tabs = {"rtshop.qedit.tab_basic", "rtshop.qedit.tab_story", "rtshop.qedit.tab_goals", "rtshop.qedit.tab_rewards"};
        int tw = (w - 3 * 3) / 4;
        for (int i = 0; i < tabs.length; i++) {
            final int t = i;
            hit(x + i * (tw + 3), fy + 27, tw, 14, () -> tr(tabs[t]), () -> tab != t, b -> {
                tab = t;
                rebuild();
            });
        }
        switch (tab) {
            case 0 -> buildBasic(x, w);
            case 1 -> buildStory(x, w);
            case 2 -> buildGoals(x, w);
            default -> buildRewards(x, w);
        }
    }

    private void switchMode(boolean npc) {
        if (npc == npcMode) {
            return;
        }
        if (dirty && confirmIndex != -3) {
            confirmIndex = -3;
            showLocal(Component.translatable("rtshop.qedit.confirm_discard"), false);
            return;
        }
        confirmIndex = Integer.MIN_VALUE;
        npcMode = npc;
        creating = false;
        dirty = false;
        selected = -1;
        listScroll = 0;
        q = null;
        n = null;
        brokenReason = null;
        rebuild();
    }

    private static final int LW = 58;

    private void buildBasic(int x, int w) {
        int y = formTop() + 2;
        int r = 15;
        int fxw = x + 4 + LW;
        int fw = w - LW - 8;
        label(x + 4, y + 2, "rtshop.qedit.f_id");
        EditBox id = box(fxw, y, fw, q.id, 64, v -> q.id = v);
        id.setEditable(creating);
        if (!creating) {
            id.setTextColor(0xFFA0A0A0);
        }
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_title");
        box(fxw, y, fw, q.title, 80, v -> q.title = v);
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_chain");
        box(fxw, y, fw, q.chain, 80, v -> q.chain = v);
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_order");
        box(fxw, y, 50, q.order, 9, v -> q.order = v);
        labels.add(new Label(fxw + 56, y + 2, tr("rtshop.qedit.h_order"), Ui.C_INK_SOFT));
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_giver");
        final int gy = y;
        hit(fxw, y - 1, fw, 14, () -> npcLabel(q.giver) + "  ▼", () -> true, b ->
                openPopup(fxw, gy - 1, fw, 14, npcOptions(false), q.giver, v -> {
                    q.giver = v;
                    markDirty();
                }));
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_turnin");
        final int ty = y;
        hit(fxw, y - 1, fw, 14, () -> (q.turnIn.isEmpty() ? tr("rtshop.qedit.same_as_giver") : npcLabel(q.turnIn)) + "  ▼",
                () -> true, b -> openPopup(fxw, ty - 1, fw, 14, npcOptions(true), q.turnIn, v -> {
                    q.turnIn = v;
                    markDirty();
                }));
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_requires");
        box(fxw, y, fw, q.requires, 400, v -> q.requires = v);
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_req_flags");
        box(fxw, y, fw, q.reqFlags, 400, v -> q.reqFlags = v);
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_block_flags");
        box(fxw, y, fw, q.blockFlags, 400, v -> q.blockFlags = v);
    }

    private final MultiLineEditBox[] storyAreas = new MultiLineEditBox[3];

    private void buildStory(int x, int w) {
        int top = formTop() + 1;
        int each = (formH() - 2) / 3;
        int bh = each - 15;
        String[] keys = {"rtshop.qedit.f_intro", "rtshop.qedit.f_progress", "rtshop.qedit.f_done"};
        for (int i = 0; i < 3; i++) {
            int ty = top + i * each;
            final int idx = i;
            label(x + 4, ty + 3, keys[i]);
            int kw = 62;
            int mw = 74;
            int kx = x + w - 4 - kw;
            int mx = kx - 4 - mw;
            hit(mx, ty, mw, 12, () -> tr("rtshop.qedit.insert_tip"), () -> true, b -> openPopup(mx, ty, mw, 12, modTipOptions(), "",
                    v -> appendTipLine(idx, v)));
            hit(kx, ty, kw, 12, () -> tr("rtshop.qedit.insert_key"), () -> true, b -> openPopup(kx, ty, kw, 12, keyOptions(), "",
                    v -> insertAtCursor(idx, "{key:" + v + "}")));
            String value = i == 0 ? q.intro : i == 1 ? q.progress : q.done;
            storyAreas[i] = area(x + 4, ty + 14, w - 8, bh, value, v -> {
                if (idx == 0) {
                    q.intro = v;
                } else if (idx == 1) {
                    q.progress = v;
                } else {
                    q.done = v;
                }
            });
        }
    }

    /** 커서 자리에 글을 넣는다 (키 표시처럼 한 줄짜리). */
    private void insertAtCursor(int idx, String text) {
        MultiLineEditBox b = storyAreas[idx];
        if (b == null) {
            return;
        }
        this.setFocused(b);
        b.setFocused(true);
        for (char c : text.toCharArray()) {
            b.charTyped(c, 0);
        }
    }

    /** 글 맨 끝에 "[모드ID] " 로 시작하는 도움말 줄을 새로 붙인다 (그 모드가 있을 때만 보인다). */
    private void appendTipLine(int idx, String mod) {
        MultiLineEditBox b = storyAreas[idx];
        if (b == null) {
            return;
        }
        String v = b.getValue();
        b.setValue(v + (v.isEmpty() || v.endsWith("\n") ? "" : "\n") + "[" + mod + "] ");
        this.setFocused(b);
        b.setFocused(true);
    }

    private static String typeKey(QuestDef.Type t) {
        return "rtshop.qedit.type_" + t.name().toLowerCase(java.util.Locale.ROOT);
    }

    private void buildGoals(int x, int w) {
        int y = formTop() + 2;
        int typeW = 46;
        int helpW = 34;
        for (int i = 0; i < q.objs.size(); i++) {
            ObjDraft od = q.objs.get(i);
            final int idx = i;
            // 1줄: 종류 / 대상 / 도우미
            final int oy = y;
            hit(x + 4, y - 1, typeW, 14, () -> tr(typeKey(od.type)) + "▼", () -> true, b -> openPopup(x + 4, oy - 1, typeW, 14, typeOptions(),
                    od.type.name(), v -> {
                QuestDef.Type nt = QuestDef.Type.valueOf(v);
                if (nt == od.type) {
                    return;
                }
                od.type = nt;
                od.target = "";
                od.count = "1";
                if (od.type == QuestDef.Type.TALK) {
                    od.target = defaultGiver();
                } else if (od.type == QuestDef.Type.KILL) {
                    od.target = "*";
                } else if (od.type == QuestDef.Type.VISIT) {
                    od.count = "8";
                } else if (od.type == QuestDef.Type.PAY) {
                    od.count = "1000";
                }
                markDirty();
                rebuild();
            }));
            int tx = x + 4 + typeW + 4;
            int tw = w - 8 - typeW - 4 - helpW - 4;
            switch (od.type) {
                case DELIVER -> iconHit(tx, y - 1, tw, 14, () -> pickLabel(itemName(od.target), od.target), () -> itemIcon(od.target),
                        b -> openPicker(PickerScreen.Mode.ITEM, v -> {
                            od.target = v;
                            od.text = "";
                        }));
                case KILL -> iconHit(tx, y - 1, tw, 14,
                        () -> pickLabel(od.target.equals("*") ? tr("rtshop.quest.any_monster") : entityName(od.target), od.target),
                        () -> entityIcon(od.target), b -> openPicker(PickerScreen.Mode.ENTITY, v -> {
                            od.target = v;
                            od.text = "";
                        }));
                case TALK -> hit(tx, y - 1, tw, 14, () -> npcLabel(od.target) + "  ▼", () -> true, b ->
                        openPopup(tx, oy - 1, tw, 14, npcOptions(false), od.target, v -> {
                            od.target = v;
                            markDirty();
                        }));
                case VISIT, STRUCTURE, ADVANCEMENT, DIMENSION, FLAG -> box(tx, y, tw, od.target, 200, v -> od.target = v);
                default -> labels.add(new Label(tx + 2, y + 2, tr("rtshop.qedit.pay_note"), Ui.C_INK_SOFT));
            }
            String helper = switch (od.type) {
                case DELIVER -> "rtshop.qedit.help_hand";
                case KILL -> "rtshop.qedit.help_near";
                case VISIT -> "rtshop.qedit.help_here";
                default -> null;
            };
            if (helper != null) {
                hit(x + w - 4 - helpW, y - 1, helpW, 14, () -> tr(helper), () -> true, b -> {
                    String before = od.target;
                    helperFor(od, b);
                    if (!od.target.equals(before) && od.type != QuestDef.Type.VISIT) {
                        od.text = "";
                    }
                    markDirty();
                    rebuild();
                });
            }
            y += 15;
            // 2줄: 수량(반경·금액) / 설명 / 삭제
            String countKey = switch (od.type) {
                case VISIT -> "rtshop.qedit.f_radius";
                case PAY -> "rtshop.qedit.f_amount";
                default -> "rtshop.qedit.f_count";
            };
            labels.add(new Label(x + 8, y + 2, tr(countKey), Ui.C_INK_SOFT));
            box(x + 40, y, 40, od.count, 9, v -> od.count = v);
            labels.add(new Label(x + 86, y + 2, tr("rtshop.qedit.f_text"), Ui.C_INK_SOFT));
            box(x + 112, y, w - 112 - 22, od.text, 120, v -> od.text = v);
            hit(x + w - 18, y - 1, 14, 14, () -> "X", () -> true, b -> {
                q.objs.remove(idx);
                markDirty();
                rebuild();
            });
            y += 15;
            // 이 목표가 무엇을 뜻하는지 한 줄 안내 + 모드 조건
            int cw = 104;
            labels.add(new Label(x + 8, y + 1, Ui.clip(this.font, goalHint(od), w - 16 - cw - 4), Ui.C_INK_SOFT));
            final int cy = y;
            hit(x + w - 4 - cw, y - 1, cw, 12, () -> Ui.clip(this.font, condLabel(od.mod), cw - 12) + "▼", () -> true,
                    b -> openPopup(x + w - 4 - cw, cy - 1, cw, 12, condOptions(od.mod), od.mod, v -> {
                        od.mod = v;
                        markDirty();
                    }));
            y += 13;
        }
        if (q.objs.size() < MAX_OBJ) {
            hit(x + 4, y, 90, 14, tr("rtshop.qedit.add_goal"), () -> {
                ObjDraft od = new ObjDraft();
                q.objs.add(od);
                markDirty();
                rebuild();
            });
        } else {
            labels.add(new Label(x + 4, y + 3, tr("rtshop.qedit.max_goals", MAX_OBJ), Ui.C_INK_SOFT));
        }
    }

    private String goalHint(ObjDraft od) {
        return switch (od.type) {
            case DELIVER -> tr("rtshop.qedit.hint_deliver", od.target.isBlank() ? "?" : itemName(od.target));
            case KILL -> tr("rtshop.qedit.hint_kill", od.target.equals("*") ? tr("rtshop.quest.any_monster") : entityName(od.target));
            case VISIT -> tr("rtshop.qedit.hint_visit", od.dim);
            case TALK -> tr("rtshop.qedit.hint_talk", npcLabel(od.target));
            case STRUCTURE -> tr("rtshop.qedit.hint_structure");
            case ADVANCEMENT -> tr("rtshop.qedit.hint_advancement");
            case DIMENSION -> tr("rtshop.qedit.hint_dimension");
            case FLAG -> tr("rtshop.qedit.hint_flag");
            default -> tr("rtshop.qedit.hint_pay");
        };
    }

    /** 선택 목록 화면을 열고, 고르면 그 값을 넣은 뒤 이 화면으로 돌아온다. */
    private void openPicker(PickerScreen.Mode mode, Consumer<String> set) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new PickerScreen(this, mode, v -> {
            set.accept(v);
            markDirty();
        }));
    }

    private static String pickLabel(String name, String id) {
        return id == null || id.isBlank() ? tr("rtshop.qedit.pick") : name;
    }

    private static ItemStack itemIcon(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id.trim());
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(ForgeRegistries.ITEMS.getValue(rl));
    }

    private static ItemStack entityIcon(String id) {
        if ("*".equals(id)) {
            return new ItemStack(net.minecraft.world.item.Items.ZOMBIE_HEAD);
        }
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id.trim());
        if (rl == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
            return ItemStack.EMPTY;
        }
        return PickerScreen.eggFor(ForgeRegistries.ENTITY_TYPES.getValue(rl));
    }

    private static String itemName(String id) {
        if (id.startsWith("#")) {
            return id;
        }
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null || !ForgeRegistries.ITEMS.containsKey(rl)) {
            return tr("rtshop.qedit.unknown_item", id);
        }
        return new ItemStack(ForgeRegistries.ITEMS.getValue(rl)).getHoverName().getString();
    }

    private static String entityName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id.trim());
        if (rl == null || !ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
            return tr("rtshop.qedit.unknown_entity", id);
        }
        return ForgeRegistries.ENTITY_TYPES.getValue(rl).getDescription().getString();
    }

    private void helperFor(ObjDraft od, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        switch (od.type) {
            case DELIVER -> {
                ItemStack s = mc.player.getMainHandItem();
                if (s.isEmpty()) {
                    showLocal(Component.translatable("rtshop.qedit.hand_empty"), false);
                } else {
                    od.target = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
                }
            }
            case KILL -> {
                Entity best = nearestMob(mc);
                if (best == null) {
                    od.target = "*";
                    showLocal(Component.translatable("rtshop.qedit.no_mob"), true);
                } else {
                    od.target = String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(best.getType()));
                }
            }
            case VISIT -> {
                BlockPos p = mc.player.blockPosition();
                od.target = p.getX() + " " + p.getY() + " " + p.getZ();
                od.dim = mc.player.level().dimension().location().toString();
            }
            case TALK -> od.target = cycleNpc(od.target, button == 1 ? -1 : 1, false);
            default -> {
            }
        }
    }

    private static Entity nearestMob(Minecraft mc) {
        Player me = mc.player;
        List<LivingEntity> all = mc.level.getEntitiesOfClass(LivingEntity.class, me.getBoundingBox().inflate(24),
                e -> e != me && !(e instanceof Player) && e.isAlive());
        Comparator<LivingEntity> byDist = Comparator.comparingDouble(e -> e.distanceToSqr(me));
        return all.stream().filter(e -> e instanceof Enemy).min(byDist)
                .map(e -> (Entity) e).orElse(all.stream().min(byDist).orElse(null));
    }

    private void buildRewards(int x, int w) {
        int y = formTop() + 2;
        int r = 14;
        label(x + 4, y + 2, "rtshop.qedit.f_gold");
        box(x + 4 + 34, y, 64, q.gold, 12, v -> q.gold = v);
        labels.add(new Label(x + 108, y + 2, tr("rtshop.qedit.f_reward_flags"), Ui.C_INK));
        box(x + 140, y, w - 144, q.rewardFlags, 400, v -> q.rewardFlags = v);
        y += r + 2;
        label(x + 4, y + 2, "rtshop.qedit.f_items");
        y += r - 2;
        for (int i = 0; i < q.items.size(); i++) {
            ItemDraft id = q.items.get(i);
            final int idx = i;
            int cw = 70;
            iconHit(x + 8, y - 1, w - 8 - 4 - 36 - 4 - 24 - 4 - 18 - cw - 4, 14, () -> pickLabel(itemName(id.item), id.item),
                    () -> itemIcon(id.item), b -> openPicker(PickerScreen.Mode.ITEM, v -> id.item = v));
            final int cx = x + w - 18 - 4 - 24 - 4 - 36 - 4 - cw;
            final int cy = y;
            hit(cx, y - 1, cw, 14, () -> Ui.clip(this.font, condLabel(id.mod), cw - 12) + "▼", () -> true,
                    b -> openPopup(cx, cy - 1, cw, 14, condOptions(id.mod), id.mod, v -> {
                        id.mod = v;
                        markDirty();
                    }));
            box(x + w - 18 - 4 - 24 - 4 - 36, y, 36, id.count, 6, v -> id.count = v);
            hit(x + w - 18 - 4 - 24, y - 1, 24, 14, () -> tr("rtshop.qedit.help_hand"), () -> true, b -> {
                Minecraft mc = Minecraft.getInstance();
                ItemStack s = mc.player == null ? ItemStack.EMPTY : mc.player.getMainHandItem();
                if (s.isEmpty()) {
                    showLocal(Component.translatable("rtshop.qedit.hand_empty"), false);
                    return;
                }
                id.item = String.valueOf(ForgeRegistries.ITEMS.getKey(s.getItem()));
                id.count = Integer.toString(s.getCount());
                markDirty();
                rebuild();
            });
            hit(x + w - 18, y - 1, 14, 14, () -> "X", () -> true, b -> {
                q.items.remove(idx);
                markDirty();
                rebuild();
            });
            y += r;
        }
        if (q.items.size() < MAX_ITEMS) {
            hit(x + 8, y - 1, 80, 13, tr("rtshop.qedit.add_item"), () -> {
                ItemDraft added = new ItemDraft();
                q.items.add(added);
                markDirty();
                openPicker(PickerScreen.Mode.ITEM, v -> added.item = v);
            });
            y += r;
        }
        y += 2;
        label(x + 4, y + 2, "rtshop.qedit.f_choices");
        y += r - 2;
        for (int i = 0; i < q.choices.size(); i++) {
            ChoiceDraft cd = q.choices.get(i);
            final int idx = i;
            box(x + 8, y, w - 8 - 4 - 70 - 4 - 18, cd.text, 200, v -> cd.text = v);
            box(x + w - 18 - 4 - 70, y, 70, cd.flags, 200, v -> cd.flags = v);
            hit(x + w - 18, y - 1, 14, 14, () -> "X", () -> true, b -> {
                q.choices.remove(idx);
                markDirty();
                rebuild();
            });
            y += r;
        }
        if (q.choices.size() < MAX_CHOICES) {
            hit(x + 8, y - 1, 80, 13, tr("rtshop.qedit.add_choice"), () -> {
                q.choices.add(new ChoiceDraft());
                markDirty();
                rebuild();
            });
        }
    }

    private void buildNpc(int x, int w) {
        int y = formTop() + 2;
        int r = 15;
        int fxw = x + 4 + LW;
        int fw = w - LW - 8;
        label(x + 4, y + 2, "rtshop.qedit.f_id");
        EditBox id = box(fxw, y, fw, n.id, 64, v -> n.id = v);
        id.setEditable(creating);
        if (!creating) {
            id.setTextColor(0xFFA0A0A0);
        }
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_npc_name");
        box(fxw, y, fw, n.name, 64, v -> n.name = v);
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_profession");
        final int py = y;
        hit(fxw, y - 1, fw, 14, () -> tr("entity.minecraft.villager." + n.profession) + "  ▼", () -> true, b -> {
            List<ChoicePopup.Option> opts = new ArrayList<>();
            for (String pr : PROFESSIONS) {
                opts.add(new ChoicePopup.Option(pr, tr("entity.minecraft.villager." + pr)));
            }
            openPopup(fxw, py - 1, fw, 14, opts, n.profession, v -> {
                n.profession = v;
                markDirty();
            });
        });
        y += r;
        label(x + 4, y + 2, "rtshop.qedit.f_greeting");
        y += 11;
        area(x + 4, y, w - 8, formTop() + formH() - y - 14, n.greeting, v -> n.greeting = v);
        labels.add(new Label(x + 4, formTop() + formH() - 11, Ui.clip(this.font, tr("rtshop.qedit.npc_key", "quest_" + n.id), w - 8), Ui.C_INK_SOFT));
    }

    // ------------------------------------------------------------------ 그리기

    @Override
    public void tick() {
        super.tick();
        if (statusTicks > 0 && --statusTicks == 0) {
            status = null;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        Ui.woodFrame(g, fx, fy, W, H);
        Ui.titleBand(g, fx + 5, fy + 4, fx + W - 5, fy + 23);
        g.fill(fx + 5, fy + 22, fx + W - 5, fy + 23, Ui.C_GOLD_DARK);
        String t = this.title.getString() + (dirty ? " *" : "");
        g.drawString(this.font, t, fx + 10, fy + 9, Ui.C_GOLD, true);
        String help = Ui.clip(this.font, tr("rtshop.qedit.help_line"), W - 30 - this.font.width(t));
        g.drawString(this.font, help, fx + W - 10 - this.font.width(help), fy + 9, Ui.C_PARCH_SHADE, false);

        renderList(g, mx, my);

        int x = rx();
        int w = rw();
        Ui.bevel(g, x, formTop(), w, formH(), Ui.C_PARCH, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
        if (q == null && n == null) {
            renderEmpty(g, x, w);
        } else {
            renderInfoLine(g, x, w);
        }
        for (Label l : labels) {
            g.drawString(this.font, l.text(), l.x(), l.y(), l.color(), false);
        }
        for (Hit h : hits) {
            boolean hov = Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h());
            if (h.icon() == null) {
                Ui.button(g, this.font, h.x(), h.y(), h.w(), h.h(), h.label().get(), hov, h.enabled().getAsBoolean());
                continue;
            }
            // 아이콘 + 이름, 왼쪽 정렬 (누르면 목록이 열린다는 걸 ▼ 로 표시)
            Ui.bevel(g, h.x(), h.y(), h.w(), h.h(), hov ? Ui.C_PARCH_HI : Ui.C_CREAM, Ui.C_PARCH_HI, Ui.C_PARCH_SHADE);
            ItemStack icon = h.icon().get();
            int tx = h.x() + 4;
            if (!icon.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(h.x() + 2, h.y() + 1, 0);
                g.pose().scale(0.75F, 0.75F, 1.0F);
                g.renderItem(icon, 0, 0);
                g.pose().popPose();
                tx = h.x() + 17;
            }
            g.drawString(this.font, Ui.clip(this.font, h.label().get(), h.x() + h.w() - tx - 12), tx, h.y() + (h.h() - 8) / 2, Ui.C_INK, false);
            g.drawString(this.font, "▼", h.x() + h.w() - 10, h.y() + (h.h() - 8) / 2, Ui.C_INK_SOFT, false);
        }
        super.render(g, mx, my, partial);
        renderStatus(g, mx, my);
        if (popup != null) {
            popup.render(g, this.font, mx, my);
        }
    }

    private void renderList(GuiGraphics g, int mx, int my) {
        int x = listX();
        int y = listY();
        int h = listH();
        Ui.bevel(g, x, y, LIST_W, h, Ui.C_PARCH_DARK, Ui.C_PARCH_SHADE, Ui.C_SHADOW);
        List<QuestEditorPacket.Entry> l = entries();
        int rows = (h - 4) / LIST_ROW;
        listScroll = Math.max(0, Math.min(Math.max(0, l.size() - rows), listScroll));
        String tip = null;
        for (int r = 0; r < rows; r++) {
            int i = listScroll + r;
            if (i >= l.size()) {
                break;
            }
            QuestEditorPacket.Entry e = l.get(i);
            int ry = y + 2 + r * LIST_ROW;
            boolean sel = i == selected && !creating;
            boolean hover = Ui.inside(mx, my, x + 2, ry, LIST_W - 4, LIST_ROW);
            if (sel) {
                g.fill(x + 2, ry, x + LIST_W - 2, ry + LIST_ROW, 0x55A87A22);
            } else if (hover) {
                g.fill(x + 2, ry, x + LIST_W - 2, ry + LIST_ROW, 0x22000000);
            }
            String name = displayName(e);
            int color = e.status().isEmpty() ? Ui.C_INK : Ui.C_RED;
            g.drawString(this.font, Ui.clip(this.font, name, LIST_W - 10), x + 5, ry + 2, color, false);
            if (hover) {
                tip = e.id() + "  (" + e.file() + ")" + (e.status().isEmpty() ? "" : "\n" + e.status());
            }
        }
        if (creating) {
            g.drawString(this.font, Ui.clip(this.font, "+ " + tr(npcMode ? "rtshop.qedit.new_npc" : "rtshop.qedit.new_quest"), LIST_W - 10),
                    x + 5, y + h - 11, Ui.C_GREEN, false);
        }
        if (tip != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (String s : tip.split("\n")) {
                lines.addAll(this.font.split(Component.literal(s), 220));
            }
            pendingTooltip = lines;
            tipX = mx;
            tipY = my;
        }
    }

    private List<FormattedCharSequence> pendingTooltip = null;
    private int tipX, tipY;

    private static String displayName(QuestEditorPacket.Entry e) {
        try {
            JsonObject o = JsonParser.parseString(e.json()).getAsJsonObject();
            String key = o.has("title") ? "title" : (o.has("name") ? "name" : null);
            if (key != null) {
                return o.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return e.id().isEmpty() ? e.file() : e.id();
    }

    private void renderEmpty(GuiGraphics g, int x, int w) {
        int y = formTop() + 8;
        List<String> lines = new ArrayList<>();
        if (brokenReason != null) {
            lines.add(tr("rtshop.qedit.broken", brokenReason));
        } else {
            lines.add(tr(npcMode ? "rtshop.qedit.intro_npc1" : "rtshop.qedit.intro1"));
            lines.add(tr("rtshop.qedit.intro2"));
            lines.add(tr("rtshop.qedit.intro3"));
            lines.add(tr("rtshop.qedit.intro4"));
            for (String p : data.problems) {
                lines.add("! " + p);
            }
        }
        for (String s : lines) {
            for (FormattedCharSequence line : this.font.split(Component.literal(s), w - 16)) {
                if (y > formTop() + formH() - 12) {
                    return;
                }
                g.drawString(this.font, line, x + 8, y, s.startsWith("! ") || brokenReason != null ? Ui.C_RED : Ui.C_INK, false);
                y += 10;
            }
            y += 4;
        }
    }

    /** 고른 퀘스트가 쓰이지 않는 이유가 있으면 맨 아래에 빨간 글로 보여 준다 (기본 탭). */
    private void renderInfoLine(GuiGraphics g, int x, int w) {
        if (npcMode || tab != 0 || creating) {
            return;
        }
        List<QuestEditorPacket.Entry> l = entries();
        if (selected < 0 || selected >= l.size()) {
            return;
        }
        QuestEditorPacket.Entry e = l.get(selected);
        String s = e.status().isEmpty() ? tr("rtshop.qedit.file_ok", e.file()) : tr("rtshop.qedit.file_unused", e.status());
        g.drawString(this.font, Ui.clip(this.font, s, w - 12), x + 6, formTop() + formH() - 11,
                e.status().isEmpty() ? Ui.C_INK_SOFT : Ui.C_RED, false);
    }

    private void renderStatus(GuiGraphics g, int mx, int my) {
        if (status != null) {
            int sx = listX() + LIST_W + 6;
            int left = Integer.MAX_VALUE;
            for (Hit h : hits) {
                if (h.y() == bottomY() && h.x() > sx) {
                    left = Math.min(left, h.x());
                }
            }
            int sw = (left == Integer.MAX_VALUE ? fx + W - 8 : left - 4) - sx;
            List<FormattedCharSequence> lines = this.font.split(status, Math.max(40, sw));
            int y = bottomY() - (lines.size() > 1 ? 1 : -4);
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                g.drawString(this.font, lines.get(i), sx, y + i * 9, statusOk ? 0xFFB8F0A0 : 0xFFFFB0A0, true);
            }
            if (lines.size() > 2 && Ui.inside(mx, my, sx, bottomY() - 2, sw, 20)) {
                g.renderTooltip(this.font, this.font.split(status, 240), mx, my);
            }
        }
        if (pendingTooltip != null) {
            g.renderTooltip(this.font, pendingTooltip, tipX, tipY);
            pendingTooltip = null;
        }
    }

    // ------------------------------------------------------------------ 입력

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (popup != null) {
            if (popup.click(mx, my)) {
                popup = null;
            }
            return true;
        }
        if (button == 0 || button == 1) {
            for (Hit h : new ArrayList<>(hits)) {
                if (Ui.inside(mx, my, h.x(), h.y(), h.w(), h.h()) && h.enabled().getAsBoolean()) {
                    click();
                    setFocused(null);
                    h.action().accept(button);
                    return true;
                }
            }
        }
        if (button == 0 && Ui.inside(mx, my, listX() + 2, listY() + 2, LIST_W - 4, listH() - 4)) {
            int i = listScroll + (int) ((my - listY() - 2) / LIST_ROW);
            if (i >= 0 && i < entries().size()) {
                if (i == selected && !creating) {
                    return true;
                }
                if (dirty && confirmIndex != i) {
                    confirmIndex = i;
                    showLocal(Component.translatable("rtshop.qedit.confirm_discard"), false);
                    return true;
                }
                confirmIndex = Integer.MIN_VALUE;
                click();
                creating = false;
                dirty = false;
                selected = i;
                tab = 0;
                loadSelected();
                rebuild();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (popup != null) {
            popup.scroll(delta);
            return true;
        }
        if (Ui.inside(mx, my, listX(), listY(), LIST_W, listH())) {
            listScroll -= (int) Math.signum(delta) * 2;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void onClose() {
        if (popup != null) {
            popup = null;
            return;
        }
        if (dirty && !confirmClose) {
            confirmClose = true;
            showLocal(Component.translatable("rtshop.qedit.confirm_close"), false);
            return;
        }
        super.onClose();
    }

    // ------------------------------------------------------------------ 점검용

    /** 클라이언트 점검: 지금 편집 중인 퀘스트의 탭을 바꾼다. */
    public void selectForTest(String id, int tabIndex) {
        npcMode = false;
        creating = false;
        dirty = false;
        selected = indexOf(id);
        tab = tabIndex;
        loadSelected();
        rebuild();
    }

    /** 클라이언트 점검: 제목을 바꿔 실제 저장 버튼과 같은 길로 서버에 보낸다. */
    public void saveTitleForTest(String title) {
        if (q != null) {
            q.title = title;
            dirty = true;
            save();
        }
    }

    /** 클라이언트 점검: 지금 퀘스트의 첫 목표(납품/처치)의 선택 목록을 연다. */
    public void openFirstGoalPickerForTest() {
        if (q != null && !q.objs.isEmpty()) {
            ObjDraft od = q.objs.get(0);
            openPicker(od.type == QuestDef.Type.KILL ? PickerScreen.Mode.ENTITY : PickerScreen.Mode.ITEM, v -> od.target = v);
        }
    }

    public String firstGoalTargetForTest() {
        return q == null || q.objs.isEmpty() ? null : q.objs.get(0).target;
    }

    public boolean isDirtyForTest() {
        return dirty;
    }

    public boolean hasDraftForTest() {
        return q != null;
    }
}
