package com.rtshop.story;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rtshop.RtShop;
import com.rtshop.net.ShopNet;
import com.rtshop.net.StoryPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 보스 등장 연출: story.json 의 보스가 처음으로 플레이어 가까이(기본 36칸)에 나타나면, 근처 사람들에게 보스 고유의 등장 연출을 보여 주고
 * 연출 동안(기본 5초) 피해를 막아 준다. 같은 보스(같은 개체)에게는 한 번만.
 * 보스가 떨어뜨린 물건에는 그 보스의 이야기가 붙는다 (마우스를 올리면 기울임꼴로 보인다).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class BossIntros {
    private BossIntros() {
    }

    public static final String SEEN_TAG = "rtshop_intro_seen";
    public static final String LORE_TAG = "rtshop_lore";
    private static final Set<UUID> SHOWN = new HashSet<>();

    public static String id(Entity e) {
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        return rl == null ? "" : rl.toString();
    }

    public static JsonObject cinema(StoryConfig.Intro in) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", "boss");
        o.addProperty("name", in.name);
        o.addProperty("epithet", in.epithet);
        o.addProperty("theme", in.theme);
        o.addProperty("color", in.color);
        o.addProperty("sound", in.sound);
        o.addProperty("quote", in.quote);
        o.addProperty("shake", in.shake);
        return o;
    }

    public static void send(ServerPlayer p, JsonObject o) {
        if (p instanceof FakePlayer || p.connection == null) return;
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.CINEMA, StoryText.json(p, o).toString()));
    }

    /** 이 보스의 등장 연출을 보여 준다 (근처 사람 모두). 보여 줬으면 true. */
    public static boolean show(LivingEntity boss) {
        StoryConfig c = StoryConfig.get();
        StoryConfig.Intro in = c.intros.get(id(boss));
        if (in == null || !(boss.level() instanceof ServerLevel level)) return false;
        boss.getPersistentData().putBoolean(SEEN_TAG, true);
        SHOWN.add(boss.getUUID());
        JsonObject o = cinema(in);
        o.addProperty("entity", boss.getId());   // 화면 쪽의 3D 카메라 연출(client/BossCam)이 이 보스를 따라간다
        double r = c.introRadius + 12;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(r))) {
            send(p, o);
            if (c.introShieldSeconds > 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, c.introShieldSeconds * 20, 4, false, false, true));
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        MinecraftServer server = event.getServer();
        if (event.phase != TickEvent.Phase.END || server == null || server.getTickCount() % 10 != 5) return;
        StoryConfig c = StoryConfig.get();
        if (!c.introEnabled || c.intros.isEmpty()) return;
        try {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.isSpectator()) continue;
                AABB box = p.getBoundingBox().inflate(c.introRadius);
                List<LivingEntity> near = p.level().getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && !SHOWN.contains(e.getUUID()) && c.intros.containsKey(id(e)) && !e.getPersistentData().getBoolean(SEEN_TAG));
                for (LivingEntity boss : near) {
                    StorySeals.Seal seal = StorySeals.ofBoss(boss);
                    if (seal == null || StorySeals.passes(p, seal)) show(boss); // 결계에 막힌 사람에게는 아직 등장하지 않는다
                }
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] boss intro", t);
        }
    }

    /** 보스가 떨어뜨린 물건에 그 보스의 이야기를 붙인다. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) return;
        String id = id(e);
        StoryConfig.Intro in = StoryConfig.get().intros.get(id);
        if (in == null || in.lore.isEmpty()) return;
        for (ItemEntity ie : event.getDrops()) {
            ItemStack s = ie.getItem();
            if (s.isEmpty()) continue;
            s.getOrCreateTag().putString(LORE_TAG, id);
            ie.setItem(s);
        }
        SHOWN.remove(e.getUUID());
    }

    /** 접속할 때 보스 이야기 목록을 보낸다 (물건에 붙은 표식 → 이야기 줄). */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            sendLore(p);
            StoryGates.sync(p);
        }
    }

    /** 물건 ID → 설명 아래에 붙는 이야기. */
    public static final Map<String, String[]> ITEM_LORE = new java.util.LinkedHashMap<>();

    static {
        ITEM_LORE.put("efn:meen_spear", new String[]{
                "— 황혼의 불꽃 —",
                "최초의 불꽃이 모든 것을 집어 삼키기 이전,",
                "아직 기억을 지키던 때의 불빛.",
                "잿빛 해가 오기 전날 밤, 왕국에서 제련으로는 따를 자가 없던",
                "대장장이가 소문으로만 전해지던 이름 없는 금속 안에",
                "불 한 줌을 식혀 가두었다.",
                "대장장이는 창 어디에도 자기 이름을 새기지 않았다.",
                "불이 처음 삼키는 것이 만든 이의 이름이어서는 안 되었기에.",
                "이 불꽃은 자신을 받아들인 이의 이름을 불타게 두지 않는다.",
                "…그래서 그 대장장이의 이름만은, 아무도 모른다."});
    }

    public static void sendLore(ServerPlayer p) {
        if (p instanceof FakePlayer || p.connection == null) return;
        JsonObject o = new JsonObject();
        for (Map.Entry<String, StoryConfig.Intro> e : StoryConfig.get().intros.entrySet()) {
            if (e.getValue().lore.isEmpty()) continue;
            JsonArray a = new JsonArray();
            a.add("— " + StoryText.t(p, e.getValue().name) + ", " + StoryText.t(p, e.getValue().epithet) + " —");
            for (String l : e.getValue().lore) a.add(l);
            o.add("boss:" + e.getKey(), a);
        }
        // 이야기가 얽힌 물건 (물건 ID 로 붙는다)
        for (Map.Entry<String, String[]> e : ITEM_LORE.entrySet()) {
            JsonArray a = new JsonArray();
            for (String l : e.getValue()) a.add(l);
            o.add(e.getKey(), a);
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new StoryPacket(StoryPacket.LORE, StoryText.json(p, o).toString()));
    }

    /** 점검용 */
    public static boolean seen(Entity e) {
        return e.getPersistentData().getBoolean(SEEN_TAG);
    }

    public static String loreOf(ItemStack s) {
        CompoundTag t = s.getTag();
        return t == null ? "" : t.getString(LORE_TAG);
    }
}
