package com.rtshop.pshop;

import com.rtshop.RtShop;
import com.rtshop.attendance.Attendance;
import com.rtshop.attendance.AttendanceConfig;
import com.rtshop.economy.Economy;
import com.rtshop.economy.EconomyConfig;
import com.rtshop.economy.EconomyData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 플레이어 상점 직원: 진열대를 놓으면 그 뒤에 직원(주민 모습)이 선다. 직원을 우클릭해도 진열대와 똑같은 화면이 열린다.
 * 직원 이름은 서버 전체에서 겹치지 않는다. 출석이 초기화되는 시각마다 하루 고용비(기본 1,000G)를 주인의 예금 → 지갑 순으로 낸다.
 * 모자라면 그날은 직원이 쉬어(손님이 살 수 없음) 주인에게 알리고, 주인이 [고용비 내기] 를 누르거나 다음 날 다시 낸다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class Employees {
    private Employees() {
    }

    public static final String TAG = "rtshop_pshop_emp";
    private static final String STAND = "rtshop_stand";
    private static final String STAND_DIM = "rtshop_stand_dim";

    /** 직원 이름 후보. 다 쓰면 뒤에 숫자를 붙인다. */
    static final String[] NAMES = {"민준", "서연", "도윤", "하은", "시우", "지우", "예준", "서윤", "주원", "지유", "하준", "채원", "지호", "수아",
            "준우", "지민", "건우", "다은", "현우", "은서", "우진", "예린", "선우", "소율", "연우", "하린", "유준", "윤서", "정우", "민서", "승우",
            "수빈", "준서", "예은", "민재", "지원", "현준", "가은", "지환", "서아", "동현", "유나", "태윤", "나연", "재윤", "하율", "시윤", "다인",
            "은우", "채아", "로운", "아린", "이안", "소은", "지안", "하윤", "도현", "시아", "준혁", "예서"};

    /** 시험용: 마지막으로 주인에게 보낸 알림. */
    public static volatile Component lastNotice;

    // ------------------------------------------------------------------ 저장 데이터

    public static final class Entry {
        public String dim = "minecraft:overworld";
        public BlockPos pos = BlockPos.ZERO;
        public UUID owner = new UUID(0, 0);
        public String ownerName = "";
        public String name = "";
        public UUID entity = new UUID(0, 0);
        /** 고용비를 낸 마지막 출석일. 오늘 이상이면 오늘 일한다. */
        public long paidDay;

        String key() {
            return Employees.key(dim, pos);
        }
    }

    public static final class Data extends SavedData {
        static final String NAME = "rtshop_pshop_staff";
        public final Map<String, Entry> entries = new LinkedHashMap<>();
        /** 아직 접속하지 않은 주인에게 보낼 알림(JSON). */
        public final Map<UUID, List<String>> pending = new HashMap<>();
        public long lastDay = Long.MIN_VALUE;

        static Data load(CompoundTag tag) {
            Data d = new Data();
            d.lastDay = tag.contains("lastDay") ? tag.getLong("lastDay") : Long.MIN_VALUE;
            ListTag list = tag.getList("staff", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i);
                Entry e = new Entry();
                e.dim = c.getString("dim");
                e.pos = BlockPos.of(c.getLong("pos"));
                if (c.hasUUID("owner")) e.owner = c.getUUID("owner");
                e.ownerName = c.getString("ownerName");
                e.name = c.getString("name");
                if (c.hasUUID("entity")) e.entity = c.getUUID("entity");
                e.paidDay = c.getLong("paidDay");
                d.entries.put(e.key(), e);
            }
            ListTag pl = tag.getList("pending", Tag.TAG_COMPOUND);
            for (int i = 0; i < pl.size(); i++) {
                CompoundTag c = pl.getCompound(i);
                List<String> msgs = new ArrayList<>();
                ListTag ml = c.getList("msgs", Tag.TAG_STRING);
                for (int k = 0; k < ml.size(); k++) msgs.add(ml.getString(k));
                if (c.hasUUID("id")) d.pending.put(c.getUUID("id"), msgs);
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putLong("lastDay", lastDay);
            ListTag list = new ListTag();
            for (Entry e : entries.values()) {
                CompoundTag c = new CompoundTag();
                c.putString("dim", e.dim);
                c.putLong("pos", e.pos.asLong());
                c.putUUID("owner", e.owner);
                c.putString("ownerName", e.ownerName);
                c.putString("name", e.name);
                c.putUUID("entity", e.entity);
                c.putLong("paidDay", e.paidDay);
                list.add(c);
            }
            tag.put("staff", list);
            ListTag pl = new ListTag();
            for (Map.Entry<UUID, List<String>> e : pending.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putUUID("id", e.getKey());
                ListTag ml = new ListTag();
                for (String m : e.getValue()) ml.add(StringTag.valueOf(m));
                c.put("msgs", ml);
                pl.add(c);
            }
            tag.put("pending", pl);
            return tag;
        }
    }

    public static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, Data.NAME);
    }

    static String key(String dim, BlockPos pos) {
        return dim + "|" + pos.asLong();
    }

    static String dimOf(Level level) {
        return level.dimension().location().toString();
    }

    public static long today() {
        AttendanceConfig c = AttendanceConfig.get();
        return Attendance.dayOf(Attendance.clock.getAsLong(), c);
    }

    public static long wage() {
        return EconomyConfig.get().employeeWage;
    }

    public static Entry entry(ServerLevel level, BlockPos pos) {
        return data(level.getServer()).entries.get(key(dimOf(level), pos));
    }

    /** 오늘 손님을 받는가 (고용비를 냈거나 고용비가 0). 직원이 없던 예전 진열대도 연다. */
    public static boolean working(ServerLevel level, BlockPos pos) {
        Entry e = entry(level, pos);
        return e == null || wage() <= 0 || e.paidDay >= today();
    }

    // ------------------------------------------------------------------ 고용 / 해고

    /** 진열대에 주인이 있으면 직원을 둔다 (이미 있으면 그대로). 진열대를 놓을 때, 그리고 예전 진열대를 열 때 부른다. */
    public static Entry hire(ServerLevel level, BlockPos pos, PlayerShopBlockEntity be) {
        if (!be.hasOwner()) return null;
        Data d = data(level.getServer());
        String k = key(dimOf(level), pos);
        Entry e = d.entries.get(k);
        if (e == null) {
            e = new Entry();
            e.dim = dimOf(level);
            e.pos = pos.immutable();
            e.owner = be.owner;
            e.ownerName = be.ownerName;
            e.name = newName(d);
            e.paidDay = today(); // 고용한 날은 고용비 없이 일한다
            d.entries.put(k, e);
            d.setDirty();
            RtShop.LOGGER.info("[rtshop] {} 의 플레이어 상점({}) 직원 {} 고용", be.ownerName, pos.toShortString(), e.name);
        } else if (!e.owner.equals(be.owner) || !e.ownerName.equals(be.ownerName)) {
            e.owner = be.owner;
            e.ownerName = be.ownerName;
            d.setDirty();
            if (level.getEntity(e.entity) instanceof Villager v) v.setCustomName(label(e));
        }
        if (!(level.getEntity(e.entity) instanceof Villager)) {
            spawn(level, e);
        }
        return e;
    }

    /** 진열대가 없어질 때: 직원도 떠난다. */
    public static void fire(ServerLevel level, BlockPos pos) {
        Data d = data(level.getServer());
        Entry e = d.entries.remove(key(dimOf(level), pos));
        if (e == null) return;
        d.setDirty();
        Entity v = level.getEntity(e.entity);
        if (v != null) v.discard();
        RtShop.LOGGER.info("[rtshop] {} 의 플레이어 상점({}) 직원 {} 퇴사", e.ownerName, pos.toShortString(), e.name);
    }

    /** 서버 전체에서 아직 아무 직원도 쓰지 않은 이름. */
    static String newName(Data d) {
        Set<String> used = new HashSet<>();
        for (Entry e : d.entries.values()) used.add(e.name);
        for (int round = 1; ; round++) {
            List<String> free = new ArrayList<>();
            for (String n : NAMES) {
                String cand = round == 1 ? n : n + round;
                if (!used.contains(cand)) free.add(cand);
            }
            if (!free.isEmpty()) {
                return free.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(free.size()));
            }
        }
    }

    static MutableComponent label(Entry e) {
        return Component.translatable("rtshop.pshop.emp_label", e.name, e.ownerName);
    }

    /** 직원이 설 자리: 진열대 뒤(손님 반대쪽) → 옆 → 앞 → 진열대 위. */
    static Vec3[] spot(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        Direction f = st.hasProperty(PlayerShopBlock.FACING) ? st.getValue(PlayerShopBlock.FACING) : Direction.NORTH;
        Direction[] order = {f.getOpposite(), f.getClockWise(), f.getCounterClockWise(), f};
        for (Direction d : order) {
            BlockPos q = pos.relative(d);
            if (free(level, q) && free(level, q.above())) {
                float yaw = d == f ? d.getOpposite().toYRot() : f.toYRot();
                return new Vec3[]{new Vec3(q.getX() + 0.5, q.getY(), q.getZ() + 0.5), new Vec3(yaw, 0, 0)};
            }
        }
        return new Vec3[]{new Vec3(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), new Vec3(f.toYRot(), 0, 0)};
    }

    private static boolean free(ServerLevel level, BlockPos q) {
        BlockState s = level.getBlockState(q);
        return s.getCollisionShape(level, q).isEmpty() && s.getFluidState().isEmpty();
    }

    static Villager spawn(ServerLevel level, Entry e) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) return null;
        Vec3[] at = spot(level, e.pos);
        float yaw = (float) at[1].x;
        v.moveTo(at[0].x, at[0].y, at[0].z, yaw, 0.0F);
        v.setYHeadRot(yaw);
        v.setYBodyRot(yaw);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.addTag(TAG);
        v.getPersistentData().putLong(STAND, e.pos.asLong());
        v.getPersistentData().putString(STAND_DIM, e.dim);
        v.setCustomName(label(e));
        v.setCustomNameVisible(true);
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.CARTOGRAPHER, 2));
        e.entity = v.getUUID();
        data(level.getServer()).setDirty();
        level.addFreshEntity(v);
        return v;
    }

    public static boolean isEmployee(Entity e) {
        return e != null && e.getTags().contains(TAG);
    }

    // ------------------------------------------------------------------ 고용비

    /** 출석 초기화 시각이 지나면 모든 직원의 고용비를 낸다. */
    public static void payDay(MinecraftServer server) {
        Data d = data(server);
        long today = today();
        if (d.lastDay == Long.MIN_VALUE) {
            d.lastDay = today;
            d.setDirty();
            return;
        }
        if (today <= d.lastDay) return;
        d.lastDay = today;
        d.setDirty();
        long wage = wage();
        Map<UUID, List<Entry>> byOwner = new LinkedHashMap<>();
        for (Entry e : d.entries.values()) {
            if (e.paidDay < today) byOwner.computeIfAbsent(e.owner, x -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<UUID, List<Entry>> o : byOwner.entrySet()) {
            if (wage <= 0) {
                for (Entry e : o.getValue()) e.paidDay = today;
                continue;
            }
            List<Entry> paid = new ArrayList<>();
            List<Entry> unpaid = new ArrayList<>();
            long fromBank = 0, fromWallet = 0;
            for (Entry e : o.getValue()) {
                long[] took = charge(server, o.getKey(), wage);
                if (took == null) {
                    unpaid.add(e);
                } else {
                    e.paidDay = today;
                    paid.add(e);
                    fromBank += took[0];
                    fromWallet += took[1];
                }
            }
            if (!paid.isEmpty()) {
                notify(server, o.getKey(), Component.translatable("rtshop.pshop.wage_paid", names(paid), num(wage * paid.size()), num(fromBank), num(fromWallet))
                        .withStyle(ChatFormatting.GOLD));
            }
            if (!unpaid.isEmpty()) {
                notify(server, o.getKey(), Component.translatable("rtshop.pshop.wage_short", names(unpaid), num(wage * unpaid.size()))
                        .withStyle(ChatFormatting.RED));
            }
        }
    }

    /** 예금 → 지갑 순으로 amount 를 뺀다. 모자라면 아무것도 빼지 않고 null. 성공하면 {예금에서, 지갑에서}. */
    static long[] charge(MinecraftServer server, UUID owner, long amount) {
        EconomyData ed = EconomyData.get(server);
        long bank = ed.bank(owner), wallet = ed.wallet(owner);
        if (bank + wallet < amount) return null;
        long b = Math.min(bank, amount);
        long w = amount - b;
        ed.setBank(owner, bank - b);
        ed.setWallet(owner, wallet - w);
        ServerPlayer op = server.getPlayerList().getPlayer(owner);
        if (op != null) Economy.sync(op);
        return new long[]{b, w};
    }

    /** 주인이 [고용비 내기]: 쉬는 직원을 바로 다시 일하게 한다. */
    public static Component payNow(ServerPlayer p, ServerLevel level, BlockPos pos) {
        Entry e = entry(level, pos);
        if (e == null || wage() <= 0 || e.paidDay >= today()) return null;
        long[] took = charge(p.server, e.owner, wage());
        if (took == null) {
            return Component.translatable("rtshop.pshop.wage_cant", num(wage())).withStyle(ChatFormatting.RED);
        }
        e.paidDay = today();
        data(p.server).setDirty();
        return Component.translatable("rtshop.pshop.wage_paid_now", e.name, num(wage())).withStyle(ChatFormatting.GREEN);
    }

    private static String names(List<Entry> es) {
        StringBuilder b = new StringBuilder();
        for (Entry e : es) {
            if (b.length() > 0) b.append(", ");
            b.append(e.name);
        }
        return b.toString();
    }

    private static String num(long v) {
        return String.format("%,d", v);
    }

    static void notify(MinecraftServer server, UUID owner, Component msg) {
        lastNotice = msg;
        ServerPlayer op = server.getPlayerList().getPlayer(owner);
        if (op != null) {
            com.rtshop.notice.Notice.send(op, msg);
        } else {
            Data d = data(server);
            List<String> l = d.pending.computeIfAbsent(owner, x -> new ArrayList<>());
            l.add(Component.Serializer.toJson(msg));
            while (l.size() > 20) l.remove(0);
            d.setDirty();
        }
    }

    // ------------------------------------------------------------------ 이벤트

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        int t = server.getTickCount();
        if (t % 40 == 0) {
            try {
                payDay(server);
            } catch (Exception ex) {
                RtShop.LOGGER.error("[rtshop] 직원 고용비를 처리하지 못했습니다", ex);
            }
        }
        if (t % 100 == 50) {
            checkStaff(server);
        }
    }

    /** 불러온 곳의 진열대마다 직원이 있는지 본다: 진열대가 사라졌으면 내보내고, 직원이 없어졌으면 다시 세운다. */
    public static void checkStaff(MinecraftServer server) {
        checkStaff(server, false);
    }

    /** force: 시험용. 서버가 막 켜져 엔티티를 아직 불러오는 중이어도 확인한다. */
    public static void checkStaff(MinecraftServer server, boolean force) {
        Data d = data(server);
        for (Entry e : new ArrayList<>(d.entries.values())) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(e.dim)));
            if (level == null || !level.isLoaded(e.pos) || !force && !level.areEntitiesLoaded(ChunkPos.asLong(e.pos))) continue;
            if (!(level.getBlockEntity(e.pos) instanceof PlayerShopBlockEntity be)) {
                fire(level, e.pos);
                continue;
            }
            if (!(level.getEntity(e.entity) instanceof Villager)) {
                spawn(level, e);
            } else if (be.hasOwner() && !be.owner.equals(e.owner)) {
                hire(level, e.pos, be);
            }
        }
    }

    /** 진열대가 없는 직원(진열대가 다른 방법으로 사라짐)이나 겹쳐 생긴 직원은 내보낸다. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        Entity v = event.getEntity();
        if (v.level().isClientSide() || v.tickCount % 100 != 7 || !isEmployee(v) || !(v.level() instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.of(v.getPersistentData().getLong(STAND));
        Entry e = data(level.getServer()).entries.get(key(dimOf(level), pos));
        if (e == null || !e.entity.equals(v.getUUID()) || (level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof PlayerShopBlockEntity))) {
            v.discard();
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isEmployee(event.getEntity()) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = BlockPos.of(event.getEntity().getPersistentData().getLong(STAND));
        Entry e = data(level.getServer()).entries.get(key(dimOf(level), pos));
        if (e == null || !e.entity.equals(event.getEntity().getUUID())) {
            event.setCanceled(true); // 해고된 직원 / 겹쳐 생긴 직원
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !isEmployee(event.getTarget())) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer sp)) return;
        BlockPos pos = BlockPos.of(event.getTarget().getPersistentData().getLong(STAND));
        PlayerShops.open(sp, pos);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        Data d = data(sp.server);
        List<String> msgs = d.pending.remove(sp.getUUID());
        if (msgs == null) return;
        d.setDirty();
        for (String m : msgs) {
            try {
                Component c = Component.Serializer.fromJson(m);
                if (c != null) com.rtshop.notice.Notice.send(sp, c);
            } catch (Exception ignored) {
            }
        }
    }
}
