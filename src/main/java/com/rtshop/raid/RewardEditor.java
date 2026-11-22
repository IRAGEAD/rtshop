package com.rtshop.raid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 게임 안 보상·수치·문구 편집기 (OP)의 서버 쪽: 편집할 대상마다 "화면에 보낼 내용"과 "받은 내용을 적용하는 법"을 등록한다.
 * 화면(RewardEditScreen)은 대상이 무엇이든 같다: [수치] [문구] [보상] 세 갈래.
 */
public final class RewardEditor {
    private RewardEditor() {
    }

    public interface Target {
        JsonObject model(ServerPlayer p);

        void apply(ServerPlayer p, JsonObject edited);

        void add(ServerPlayer p, Reward r);
    }

    public static final Map<String, Target> TARGETS = new LinkedHashMap<>();

    public static JsonObject field(String key, String label, boolean number, String value) {
        JsonObject o = new JsonObject();
        o.addProperty("k", key);
        o.addProperty("label", label);
        o.addProperty("num", number);
        o.addProperty("v", value);
        return o;
    }

    public static JsonObject group(String id, String label, boolean grades, boolean chance) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("label", label);
        o.addProperty("grades", grades);
        o.addProperty("chance", chance);
        return o;
    }

    public static String num(double v) {
        return Math.abs(v - Math.rint(v)) < 1e-9 ? Long.toString((long) Math.rint(v)) : Double.toString(v);
    }

    public static Map<String, String> fields(JsonObject edited) {
        Map<String, String> out = new LinkedHashMap<>();
        if (edited.has("fields") && edited.get("fields").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : edited.getAsJsonObject("fields").entrySet()) {
                out.put(e.getKey(), e.getValue().getAsString());
            }
        }
        return out;
    }

    public static List<Reward> rewards(JsonObject edited) {
        List<Reward> out = new ArrayList<>();
        if (edited.has("rewards") && edited.get("rewards").isJsonArray()) {
            for (JsonElement e : edited.getAsJsonArray("rewards")) {
                if (e.isJsonObject()) out.add(Reward.parse(e.getAsJsonObject()));
            }
        }
        return out;
    }

    public static double dbl(Map<String, String> f, String key, double def) {
        try {
            return f.containsKey(key) ? Double.parseDouble(f.get(key).trim()) : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static JsonArray rewardsJson(List<Reward> list) {
        JsonArray a = new JsonArray();
        for (Reward r : list) a.add(r.toJson());
        return a;
    }

    static {
        TARGETS.put("raid", new Target() {
            private final String[] gradeNames = {"일반", "고급", "희귀", "영웅", "전설", "[???]"};

            @Override
            public JsonObject model(ServerPlayer p) {
                RaidConfig c = RaidConfig.get();
                JsonObject o = new JsonObject();
                o.addProperty("id", "raid");
                o.addProperty("title", "주간 레이드 설정");
                JsonArray f = new JsonArray();
                f.add(field("healthMultiplier", "보스 체력 배율 (일반 보스의 몇 배)", true, num(c.healthMultiplier)));
                f.add(field("damageMultiplier", "보스 공격력 배율", true, num(c.damageMultiplier)));
                f.add(field("requireKills", "입장 조건: 일반 보스 처치 수", true, num(c.requireKills)));
                f.add(field("returnSeconds", "성공 뒤 마을로 돌아가기까지 (초)", true, num(c.returnSeconds)));
                f.add(field("failReturnSeconds", "실패 뒤 자동으로 돌려보내기까지 (초)", true, num(c.failReturnSeconds)));
                f.add(field("weeklyGold", "주간 보상 Gold", true, num(c.weeklyGold)));
                f.add(field("rollsFresh", "드랍 굴림 횟수 (이번 주 주간 보상을 아직 안 받은 사람)", true, num(c.rollsFresh)));
                f.add(field("rollsRepeat", "드랍 굴림 횟수 (이미 받은 사람)", true, num(c.rollsRepeat)));
                for (int i = 0; i < 6; i++) {
                    f.add(field("chanceFresh" + i, gradeNames[i] + " 등급 확률 % (주간 보상 전)", true, num(c.chanceFresh[i])));
                }
                for (int i = 0; i < 6; i++) {
                    f.add(field("chanceRepeat" + i, gradeNames[i] + " 등급 확률 % (주간 보상을 받은 뒤)", true, num(c.chanceRepeat[i])));
                }
                f.add(field("resetDay", "주간 초기화 요일 (1 = 월요일 … 7 = 일요일)", true, num(c.resetDay)));
                f.add(field("resetHour", "주간 초기화 시각 (0~23시)", true, num(c.resetHour)));
                f.add(field("boss", "레이드 보스 (엔티티 ID)", false, c.boss));
                f.add(field("requireKill", "입장 조건 몬스터 (엔티티 ID, 비우면 조건 없음)", false, c.requireKill));
                for (String k : RaidConfig.defaultTexts().keySet()) {
                    f.add(field("text." + k, RaidConfig.label(k), false, c.text(k)));
                }
                o.add("fields", f);
                JsonArray g = new JsonArray();
                g.add(group(RaidConfig.G_WEEKLY, "주간 보상 (매주 첫 성공 때 모두 지급)", false, false));
                g.add(group(RaidConfig.G_DROP, "보스 드랍 (등급 굴림)", true, false));
                o.add("groups", g);
                o.add("rewards", rewardsJson(c.rewards));
                return o;
            }

            @Override
            public void apply(ServerPlayer p, JsonObject edited) {
                RaidConfig c = RaidConfig.get();
                Map<String, String> f = fields(edited);
                JsonObject j = c.toJson();
                for (String k : new String[]{"healthMultiplier", "damageMultiplier", "requireKills", "returnSeconds", "failReturnSeconds", "weeklyGold",
                        "rollsFresh", "rollsRepeat", "resetDay", "resetHour"}) {
                    if (f.containsKey(k)) {
                        double v = dbl(f, k, j.get(k).getAsDouble());
                        if (k.endsWith("Multiplier")) j.addProperty(k, v);
                        else j.addProperty(k, (long) Math.rint(v));
                    }
                }
                JsonArray cf = new JsonArray(), cr = new JsonArray();
                for (int i = 0; i < 6; i++) {
                    cf.add(dbl(f, "chanceFresh" + i, c.chanceFresh[i]));
                    cr.add(dbl(f, "chanceRepeat" + i, c.chanceRepeat[i]));
                }
                j.add("chanceFresh", cf);
                j.add("chanceRepeat", cr);
                if (f.containsKey("boss")) j.addProperty("boss", f.get("boss").trim());
                if (f.containsKey("requireKill")) j.addProperty("requireKill", f.get("requireKill").trim());
                JsonObject t = j.getAsJsonObject("texts");
                for (Map.Entry<String, String> e : f.entrySet()) {
                    if (e.getKey().startsWith("text.")) t.addProperty(e.getKey().substring(5), e.getValue());
                }
                if (edited.has("rewards")) j.add("rewards", rewardsJson(rewards(edited)));
                RaidConfig next = RaidConfig.parse(j);
                next.save();
                RaidConfig.reload();
            }

            @Override
            public void add(ServerPlayer p, Reward r) {
                RaidConfig c = RaidConfig.get();
                c.rewards.add(r);
                c.save();
            }
        });
    }
}
