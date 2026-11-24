package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import com.rtshop.quest.QuestDef;
import com.rtshop.quest.QuestManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 장소 조사: 조사 목표(INSPECT)가 있는 플레이어가 그 구조물에 닿으면 근처에 '흔적'이 나타난다.
 * 흔적은 세계에 놓이는 블록이 아니라 그 플레이어의 화면에만 그려지는 표식이라서 부수거나 밀 수 없고, 다른 사람에게는 보이지 않는다.
 * 다가가 우클릭하면 대화 상자 틀로 관찰한 내용이 나오고, 끝까지 보면 목표가 채워진다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Spots {
    private Spots() {
    }

    private static final String KEY = "rtshop_spots";
    public static final double LOOK_RANGE = 6.0;
    private static final Map<UUID, String> SENT = new HashMap<>();
    private static final java.util.Random RNG = new java.util.Random();

    private static CompoundTag store(ServerPlayer p, boolean create) {
        CompoundTag root = p.getPersistentData();
        CompoundTag pers = root.getCompound(Player.PERSISTED_NBT_TAG);
        if (create && !root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, pers);
        CompoundTag s = pers.getCompound(KEY);
        if (create && !pers.contains(KEY)) pers.put(KEY, s);
        return s;
    }

    /** 이 흔적이 놓인 자리 (아직 안 놓였거나 다른 차원이면 null). */
    public static BlockPos placed(ServerPlayer p, String id) {
        CompoundTag s = store(p, false);
        if (!s.contains(id)) return null;
        if (!p.level().dimension().location().toString().equals(s.getString(id + "@"))) return null;
        return BlockPos.of(s.getLong(id));
    }

    private static void place(ServerPlayer p, String id, BlockPos pos) {
        CompoundTag s = store(p, true);
        s.putLong(id, pos.asLong());
        s.putString(id + "@", p.level().dimension().location().toString());
    }

    private static void forget(ServerPlayer p, String id) {
        CompoundTag s = store(p, false);
        s.remove(id);
        s.remove(id + "@");
    }

    private static boolean standable(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    /** 흔적을 놓을 자리: 플레이어에게서 5~12칸, 발 디딜 수 있는 바닥, 이미 놓인 다른 흔적과 6칸 이상 떨어진 곳. */
    static BlockPos choose(ServerPlayer p, List<BlockPos> others) {
        ServerLevel level = p.serverLevel();
        BlockPos base = p.blockPosition();
        BlockPos best = null;
        for (int tries = 0; tries < 80; tries++) {
            double a = RNG.nextDouble() * Math.PI * 2, r = 5 + RNG.nextDouble() * (7 + tries / 10.0);
            int x = base.getX() + (int) Math.round(Math.cos(a) * r), z = base.getZ() + (int) Math.round(Math.sin(a) * r);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos c = new BlockPos(x, base.getY() + dy, z);
                if (!level.isLoaded(c) || !standable(level, c)) continue;
                boolean far = true;
                for (BlockPos o : others) if (o.distSqr(c) < 36) far = false;
                if (!far) break;
                // 하늘이 뚫린 곳보다 지붕 아래(구조물 안)를 먼저 고른다
                if (!level.canSeeSky(c)) return c;
                if (best == null) best = c;
                break;
            }
        }
        if (best != null) return best;
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos c = base.offset(2, dy, 1);
            if (standable(level, c)) return c;
        }
        return base;
    }

    private static String label(ServerPlayer p, QuestDef.Objective ob) {
        return ob.text.isEmpty() ? "" : StoryText.t(p, ob.text); // 비어 있으면 화면 쪽에서 '흔적'이라고 적는다
    }

    /** 매초: 흔적을 놓을 때가 됐는지 보고, 화면에 그릴 목록을 맞춘다. */
    public static void update(ServerPlayer p) {
        List<Object[]> pend = QuestManager.pendingInspects(p);
        if (pend.isEmpty() && !SENT.containsKey(p.getUUID())) return;
        List<BlockPos> have = new ArrayList<>();
        for (Object[] o : pend) {
            BlockPos at = placed(p, ((QuestDef.Objective) o[1]).target);
            if (at != null) have.add(at);
        }
        JsonArray arr = new JsonArray();
        for (Object[] o : pend) {
            QuestDef.Objective ob = (QuestDef.Objective) o[1];
            BlockPos at = placed(p, ob.target);
            if (at == null && !store(p, false).contains(ob.target) && QuestManager.atStructure(p, ob.at)) {
                at = choose(p, have);
                place(p, ob.target, at);
                have.add(at);
                String lb = label(p, ob);
                com.rtshop.notice.Notice.send(p, lb.isEmpty() ? Component.translatable("rtshop.spot.appeared_plain")
                        : Component.translatable("rtshop.spot.appeared", lb));
                Companions.onSpotNear(p);
            }
            if (at == null) continue;
            JsonObject j = new JsonObject();
            j.addProperty("id", ob.target);
            j.addProperty("x", at.getX());
            j.addProperty("y", at.getY());
            j.addProperty("z", at.getZ());
            j.addProperty("shape", ob.shape.isEmpty() ? "circle" : ob.shape);
            j.addProperty("label", label(p, ob));
            arr.add(j);
        }
        String sig = p.level().dimension().location() + arr.toString();
        if (!sig.equals(SENT.get(p.getUUID()))) {
            SENT.put(p.getUUID(), sig);
            JsonObject out = new JsonObject();
            out.add("spots", arr);
            ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.SPOTS, out.toString()));
        }
        if (pend.isEmpty()) SENT.remove(p.getUUID());
    }

    private static QuestDef.Objective find(ServerPlayer p, String id) {
        for (Object[] o : QuestManager.pendingInspects(p)) {
            QuestDef.Objective ob = (QuestDef.Objective) o[1];
            if (ob.target.equals(id)) return ob;
        }
        return null;
    }

    private static boolean inReach(ServerPlayer p, String id) {
        BlockPos at = placed(p, id);
        return at != null && p.distanceToSqr(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= LOOK_RANGE * LOOK_RANGE;
    }

    /** 흔적을 살펴본다: 글이 있으면 대화 상자로 보여 주고, 없으면 바로 채운다. */
    public static boolean look(ServerPlayer p, String id) {
        QuestDef.Objective ob = find(p, id);
        if (ob == null || !inReach(p, id)) return false;
        if (ob.lines == null || ob.lines.size() == 0) return done(p, id);
        JsonObject out = new JsonObject();
        out.addProperty("npc", "_spot");
        out.addProperty("entity", -1);
        out.addProperty("name", label(p, ob));
        out.addProperty("mode", "scene");
        out.addProperty("spot", id);
        JsonArray lines = new JsonArray();
        for (JsonElement e : ob.lines) lines.add(Loc.pick(p, e));
        out.add("lines", lines);
        out.add("topics", new JsonArray());
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.DIALOGUE, StoryText.json(p, out).toString()));
        return true;
    }

    /** 끝까지 살펴봤다: 목표를 채우고 흔적을 지운다. 본 것은 표식(spot_ID)으로 남아 조사 수첩에 적힌다. */
    public static boolean done(ServerPlayer p, String id) {
        if (find(p, id) == null || !inReach(p, id)) return false;
        boolean ok = QuestManager.completeInspect(p, id);
        if (ok) {
            forget(p, id);
            QuestManager.setFlag(p, "spot_" + id, true);
            p.playNotifySound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.9F);
            update(p);
        }
        return ok;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (event.player instanceof ServerPlayer sp && sp.tickCount % 20 == 7) update(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** 조사 수첩: 지금까지 살펴본 흔적(표식 spot_ID)을 퀘스트 순서대로 모아 보낸다. */
    public static JsonObject notebook(ServerPlayer p) {
        JsonArray arr = new JsonArray();
        java.util.Set<String> seen = new java.util.HashSet<>();
        List<QuestDef> all = new ArrayList<>(QuestManager.loaded().quests.values());
        all.sort(java.util.Comparator.comparingInt(q -> q.order));
        for (QuestDef q : all) {
            for (QuestDef.Objective ob : q.objectives) {
                if (ob.type != QuestDef.Type.INSPECT || !QuestManager.hasFlag(p, "spot_" + ob.target)) continue;
                if (!ob.mod.isEmpty() && !QuestDef.modOk(ob.mod)) continue;
                if (!seen.add(ob.target)) continue;
                JsonObject j = new JsonObject();
                String chap = StoryText.t(p, q.chain).replaceAll("\\s*\\(\\d+/\\d+\\)$", "");
                j.addProperty("chapter", chap.isEmpty() ? StoryText.t(p, q.title) : chap);
                j.addProperty("quest", StoryText.t(p, q.title));
                j.addProperty("label", ob.text.isEmpty() ? ob.target : StoryText.t(p, ob.text));
                JsonArray lines = new JsonArray();
                if (ob.lines != null) for (JsonElement e : ob.lines) lines.add(StoryText.t(p, Loc.pick(p, e)));
                j.add("lines", lines);
                arr.add(j);
            }
        }
        JsonObject out = new JsonObject();
        out.add("entries", arr);
        return out;
    }

    public static void openNotebook(ServerPlayer p) {
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.NOTEBOOK, notebook(p).toString()));
    }

    /** 점검용: 흔적을 바로 이 자리에 놓는다. */
    public static void placeForTest(ServerPlayer p, String id, BlockPos pos) {
        place(p, id, pos);
    }
}
