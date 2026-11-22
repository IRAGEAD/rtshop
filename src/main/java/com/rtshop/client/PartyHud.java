package com.rtshop.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** 파티 표시: 화면 왼쪽에 파티원(나 제외)의 이름과 체력 막대. 파티가 없으면 그리지 않는다. */
public final class PartyHud {
    private PartyHud() {
    }

    private static volatile JsonArray members = new JsonArray();

    public static void set(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            members = o.has("members") ? o.getAsJsonArray("members") : new JsonArray();
        } catch (Exception e) {
            members = new JsonArray();
        }
    }

    public static int sizeForTest() {
        return members.size();
    }

    public static void render(GuiGraphics g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || members.size() <= 1 || !ClientConfig.get().partyHudEnabled || mc.screen instanceof HudEditScreen) return;
        String me = mc.player.getUUID().toString();
        int x = 4, y = Math.max(4, sh / 4), w = 92, rowH = 17;
        int n = 0;
        for (JsonElement e : members) if (!me.equals(e.getAsJsonObject().get("id").getAsString())) n++;
        if (n == 0) return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 150);
        g.fill(x - 2, y - 13, x + w + 2, y + n * rowH + 1, 0x70000000);
        RaidText.draw(g, mc.font, "파티 " + members.size(), RaidText.SANS, x + 1, y - 11, 0xFF9FE0F0);
        for (JsonElement e : members) {
            JsonObject m = e.getAsJsonObject();
            if (me.equals(m.get("id").getAsString())) continue;
            boolean on = m.get("online").getAsBoolean();
            float hp = m.get("hp").getAsFloat(), max = Math.max(1, m.get("max").getAsFloat());
            String name = (m.get("leader").getAsBoolean() ? "★ " : "") + m.get("name").getAsString();
            RaidText.draw(g, mc.font, RaidText.clip(mc.font, name, RaidText.SANS, w - 2), RaidText.SANS, x + 1, y + 1, on ? 0xFFFFFFFF : 0xFF8A8A8A);
            int bw = w - 2, by = y + 11;
            g.fill(x + 1, by, x + 1 + bw, by + 3, 0xFF301010);
            if (on) {
                float f = Math.max(0, Math.min(1, hp / max));
                int col = f > 0.5F ? 0xFF55D16A : f > 0.25F ? 0xFFE8C040 : 0xFFE84A3A;
                g.fill(x + 1, by, x + 1 + Math.round(bw * f), by + 3, col);
            }
            y += rowH;
        }
        g.pose().popPose();
    }
}
