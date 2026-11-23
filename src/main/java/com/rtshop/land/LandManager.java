package com.rtshop.land;

import com.mojang.authlib.GameProfile;
import com.rtshop.RtShop;
import com.rtshop.item.DeedItem;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import com.rtshop.land.LandData.Member;
import com.rtshop.net.LandInfoPacket;
import com.rtshop.net.ShopNet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** 토지 시스템의 핵심 로직: 문서 사용, 권한 판정, 설정 변경, 보관함 PIN. */
public final class LandManager {
    private LandManager() {
    }

    // 설정 화면에서 오는 동작 번호
    public static final int ACT_SET_DEFAULT = 0;
    public static final int ACT_ADD_MEMBER = 1;
    public static final int ACT_SET_MEMBER = 2;
    public static final int ACT_REMOVE_MEMBER = 3;
    public static final int ACT_SHOW_BORDER = 4;
    public static final int ACT_ABANDON = 5;
    public static final int ACT_RENAME = 6;
    public static final int ACT_REFRESH = 7;

    private static final Set<UUID> BYPASS = new HashSet<>();
    /** 경계 표시 요청: 플레이어 -> {토지ID, 끝나는 게임 시각}. */
    static final Map<UUID, long[]> SHOW_BORDER = new HashMap<>();
    /** /rtshop border 로 경계 표시를 켠 플레이어: 지금 서 있는 토지·농경지의 둘레를 보여 준다 (기본은 꺼짐). */
    private static final Set<UUID> BORDER_ON = new HashSet<>();

    public static boolean borderOn(ServerPlayer p) {
        return BORDER_ON.contains(p.getUUID());
    }

    /** 경계 표시를 켜거나 끈다. on == null 이면 반대로. 켜졌는지 돌려준다. */
    public static boolean setBorder(ServerPlayer p, Boolean on) {
        boolean now = on != null ? on : !BORDER_ON.contains(p.getUUID());
        if (now) {
            BORDER_ON.add(p.getUUID());
            Land here = landAt((ServerLevel) p.level(), p.blockPosition());
            com.rtshop.notice.Notice.send(p, Component.translatable(here != null ? "rtshop.land.border_on" : "rtshop.land.border_on_outside")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            BORDER_ON.remove(p.getUUID());
            SHOW_BORDER.remove(p.getUUID());
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.border_off").withStyle(ChatFormatting.GRAY));
        }
        return now;
    }

    static void forgetBorder(UUID id) {
        BORDER_ON.remove(id);
    }
    private static final Map<UUID, Map<String, Long>> UNLOCKED = new HashMap<>();
    private static final Map<UUID, long[]> PIN_FAILS = new HashMap<>();

    public static String dim(Level level) {
        return level.dimension().location().toString();
    }

    public static LandData data(ServerPlayer p) {
        return LandData.get(p.server);
    }

    public static void clearRuntime() {
        BYPASS.clear();
        SHOW_BORDER.clear();
        BORDER_ON.clear();
        UNLOCKED.clear();
        PIN_FAILS.clear();
    }

    // ------------------------------------------------------------------ 권한

    public static boolean toggleBypass(ServerPlayer p) {
        if (!BYPASS.remove(p.getUUID())) {
            BYPASS.add(p.getUUID());
            return true;
        }
        return false;
    }

    public static boolean bypass(ServerPlayer p) {
        return BYPASS.contains(p.getUUID());
    }

    public static boolean isOwner(ServerPlayer p, Land land) {
        return land.owner.equals(p.getUUID());
    }

    public static int perms(ServerPlayer p, Land land) {
        if (isOwner(p, land) || bypass(p)) {
            return LandData.ALL;
        }
        Member m = land.members.get(p.getUUID());
        return m != null ? m.perms : land.defaultPerms;
    }

    /** 이 위치에서 해당 행동을 할 수 있는가. 토지가 아니면 항상 가능. */
    public static boolean can(ServerPlayer p, Level level, BlockPos pos, int perm) {
        Land land = data(p).landAt(dim(level), pos);
        return land == null || (perms(p, land) & perm) != 0;
    }

    /** 성 영역의 가짜 토지: 주인 없음, 아무 권한 없음 (OP 우회만 가능). */
    public static final Land ZONE = zoneLand();

    private static Land zoneLand() {
        Land l = new Land();
        l.id = -1;
        l.owner = LandData.NO_OWNER;
        l.ownerName = "";
        l.name = "성 영역";
        l.defaultPerms = 0;
        return l;
    }

    /** 보호 검사용: 토지, 또는 토지가 아닌 성 영역(아모노스 맵)이면 ZONE. */
    public static Land guardAt(ServerLevel level, BlockPos pos) {
        Land l = landAt(level, pos);
        if (l != null) {
            return l;
        }
        return inCastleZone(level, pos) ? ZONE : null;
    }

    public static boolean inCastleZone(ServerLevel level, BlockPos pos) {
        return CastlePlots.inZone(pos.getX(), pos.getZ()) && dim(level).equals(CastlePlots.def().dim)
                && CastlePlots.zoneActive(level.getServer());
    }

    public static Land landAt(ServerLevel level, BlockPos pos) {
        return LandData.get(level.getServer()).landAt(dim(level), pos);
    }

    // ------------------------------------------------------------------ 토지 문서

    public static final class Candidate {
        public int x0, y0, z0, x1, y1, z1;
        public Component error;
    }

    /** 지금 이 자리에서 문서를 쓰면 생길 구역을 계산한다. */
    public static Candidate candidate(ServerPlayer p, DeedItem.Type type) {
        LandConfig cfg = LandConfig.get();
        LandData data = data(p);
        Level level = p.level();
        String dim = dim(level);
        int size = type.size;
        BlockPos feet = p.blockPosition();
        Candidate c = new Candidate();
        c.x0 = Math.floorDiv(feet.getX(), size) * size;
        c.z0 = Math.floorDiv(feet.getZ(), size) * size;
        c.x1 = c.x0 + size - 1;
        c.z1 = c.z0 + size - 1;
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        // 가로·세로는 문서 크기 그대로, 아래로는 발밑 깊이만큼, 위로는 월드 꼭대기까지
        c.y0 = Math.max(minY, feet.getY() - cfg.depthBelowFeet * (size / 16));
        c.y1 = maxY;

        // 같은 자리에 내 토지가 이미 있으면 위/아래로 붙여서 확장한다
        List<Claim> column = new ArrayList<>();
        for (Claim o : data.claimsNear(dim, c.x0, minY, c.z0, c.x1, maxY, c.z1, 0)) {
            Land l = data.land(o.landId);
            if (l != null && l.owner.equals(p.getUUID())) {
                column.add(o);
            }
        }
        for (Claim o : column) {
            Land l = data.land(o.landId);
            if (l != null && l.plot && o.intersects(c.x0, minY, c.z0, c.x1, maxY, c.z1)) {
                // 구획은 OP 가 정한 모양 그대로 쓴다 (문서로 넓히거나 붙일 수 없음)
                c.error = Component.translatable("rtshop.plot.err_deed");
                return c;
            }
        }
        boolean overlapsOwn = false;
        for (Claim o : column) {
            if (o.intersects(c.x0, c.y0, c.z0, c.x1, c.y1, c.z1)) {
                overlapsOwn = true;
                break;
            }
        }
        if (overlapsOwn) {
            int top = Integer.MIN_VALUE;
            int bottom = Integer.MAX_VALUE;
            for (Claim o : column) {
                top = Math.max(top, o.y1);
                bottom = Math.min(bottom, o.y0);
            }
            float pitch = p.getXRot();
            if (pitch <= -30.0F) {
                // 토지는 이미 하늘 끝까지 덮고 있다
                c.error = Component.translatable(top >= maxY ? "rtshop.land.err_top" : "rtshop.land.err_own");
                return c;
            } else if (pitch >= 30.0F) {
                c.y1 = bottom - 1;
                c.y0 = c.y1 - size + 1;
            } else {
                c.error = Component.translatable("rtshop.land.err_own");
                return c;
            }
        }
        if (c.y0 < minY) {
            if (overlapsOwn) {
                c.error = Component.translatable("rtshop.land.err_height");
                return c;
            }
            c.y0 = minY;
            c.y1 = c.y0 + size - 1;
        }
        if (c.y1 > maxY) {
            c.y1 = maxY;
        }

        for (Claim o : data.claimsNear(dim, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, 0)) {
            Land l = data.land(o.landId);
            if (l != null && l.owner.equals(p.getUUID())) {
                c.error = Component.translatable("rtshop.land.err_own");
            } else if (l != null && l.plot) {
                c.error = Component.translatable("rtshop.plot.err_overlap", l.name);
            } else {
                c.error = Component.translatable("rtshop.land.err_other", l == null ? "?" : l.ownerName);
            }
            return c;
        }
        if (cfg.minDistanceToOthers > 0) {
            for (Claim o : data.claimsNear(dim, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, cfg.minDistanceToOthers)) {
                Land l = data.land(o.landId);
                if (l != null && !l.owner.equals(p.getUUID()) && !l.forSale()) {
                    c.error = Component.translatable("rtshop.land.err_near", l.ownerName, cfg.minDistanceToOthers);
                    return c;
                }
            }
        }
        if (cfg.maxClaimsPerPlayer > 0 && data.claimCount(p.getUUID()) >= cfg.maxClaimsPerPlayer) {
            c.error = Component.translatable("rtshop.land.err_limit", cfg.maxClaimsPerPlayer);
        }
        return c;
    }

    public static void useDeed(ServerPlayer p, ItemStack stack, DeedItem.Type type) {
        Candidate c = candidate(p, type);
        if (c.error != null) {
            com.rtshop.notice.Notice.send(p, c.error.copy().withStyle(ChatFormatting.RED));
            return;
        }
        LandData data = data(p);
        String dim = dim(p.level());
        Claim claim = new Claim();
        claim.dim = dim;
        claim.x0 = c.x0;
        claim.y0 = c.y0;
        claim.z0 = c.z0;
        claim.x1 = c.x1;
        claim.y1 = c.y1;
        claim.z1 = c.z1;
        claim.type = type.id;

        // 맞닿은 내 토지가 있으면 거기에 합친다 (권한 설정을 함께 쓴다)
        Land land = null;
        for (Claim o : data.claimsNear(dim, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, 1)) {
            Land l = data.land(o.landId);
            if (l == null || !l.owner.equals(p.getUUID()) || !o.touches(claim)) {
                continue;
            }
            if (land == null) {
                land = l;
            } else if (land != l) {
                data.merge(land, l);
            }
        }
        boolean expanded = land != null;
        if (land == null) {
            land = data.createLand(p.getUUID(), p.getGameProfile().getName());
        }
        land.ownerName = p.getGameProfile().getName();
        data.addClaim(land, claim);
        if (!p.isCreative()) {
            stack.shrink(1);
        }
        com.rtshop.notice.Notice.send(p, Component.translatable(expanded ? "rtshop.land.expanded" : "rtshop.land.claimed",
                c.x0 + "~" + c.x1, c.y0 + "~" + c.y1, c.z0 + "~" + c.z1).withStyle(ChatFormatting.GREEN));
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.claimed_hint").withStyle(ChatFormatting.GRAY));
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.border_hint").withStyle(ChatFormatting.GRAY));
    }

    /**
     * RT Shop 3.3 부터 토지는 위로 월드 꼭대기까지 덮는다. 예전에 만든 토지도 (가로·세로·아래쪽은 그대로) 위쪽만 늘린다.
     * 위쪽에 다른 토지가 있으면 그 바로 아래까지만 늘린다. 서버가 켜질 때마다 해도 결과가 같다.
     */
    public static int extendClaimsToSky(net.minecraft.server.MinecraftServer server) {
        LandData data = LandData.get(server);
        int changed = 0;
        for (Land land : data.lands()) {
            for (Claim c : land.claims) {
                Level level = null;
                for (net.minecraft.server.level.ServerLevel l : server.getAllLevels()) {
                    if (dim(l).equals(c.dim)) {
                        level = l;
                        break;
                    }
                }
                if (level == null) {
                    continue;
                }
                int maxY = level.getMaxBuildHeight() - 1;
                if (c.y1 >= maxY) {
                    continue;
                }
                int limit = maxY;
                for (Claim o : data.claimsNear(c.dim, c.x0, c.y1 + 1, c.z0, c.x1, maxY, c.z1, 0)) {
                    if (o != c && o.y0 > c.y1) {
                        limit = Math.min(limit, o.y0 - 1);
                    }
                }
                if (limit > c.y1) {
                    c.y1 = limit;
                    changed++;
                }
            }
        }
        if (changed > 0) {
            data.setDirty();
            RtShop.LOGGER.info("[rtshop] 예전 토지 {}구역의 높이를 월드 꼭대기까지 늘렸습니다.", changed);
        }
        return changed;
    }

    public static boolean firstDeedUsed(ServerPlayer p) {
        return data(p).firstDeedUsed(p.getUUID());
    }

    public static void markFirstDeed(ServerPlayer p) {
        data(p).markFirstDeed(p.getUUID());
    }

    // ------------------------------------------------------------------ 설정 화면

    public static void openScreen(ServerPlayer p) {
        LandData data = data(p);
        Land here = data.landAt(dim(p.level()), p.blockPosition());
        List<Land> list = new ArrayList<>(data.landsOf(p.getUUID()));
        if (here != null && !list.contains(here)) {
            list.add(here); // 남의 토지에 서 있으면 내 권한을 볼 수 있게 읽기 전용으로 포함
        }
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                new LandInfoPacket(LandInfoPacket.views(list, p), here == null ? -1 : here.id));
    }

    public static void handleAction(ServerPlayer p, int landId, int action, String sArg, int iArg) {
        LandData data = data(p);
        Land land = data.land(landId);
        if (action == ACT_REFRESH) {
            openScreen(p);
            return;
        }
        if (land == null || (!isOwner(p, land) && !bypass(p))) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.not_owner").withStyle(ChatFormatting.RED));
            openScreen(p);
            return;
        }
        switch (action) {
            case ACT_SET_DEFAULT -> {
                land.defaultPerms = iArg & LandData.ALL;
                data.touch();
            }
            case ACT_ADD_MEMBER -> addMember(p, land, sArg.trim());
            case ACT_SET_MEMBER -> {
                try {
                    Member m = land.members.get(UUID.fromString(sArg));
                    if (m != null) {
                        m.perms = iArg & LandData.ALL;
                        data.touch();
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            case ACT_REMOVE_MEMBER -> {
                try {
                    if (land.members.remove(UUID.fromString(sArg)) != null) {
                        data.touch();
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            case ACT_SHOW_BORDER -> {
                SHOW_BORDER.put(p.getUUID(), new long[]{land.id, p.level().getGameTime() + 400});
                com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.border_shown"));
                return; // 화면을 다시 열지 않는다 (입자가 보이도록 클라이언트가 화면을 닫음)
            }
            case ACT_RENAME -> {
                String n = sArg.trim();
                if (!n.isEmpty()) {
                    land.name = n.length() > 24 ? n.substring(0, 24) : n;
                    data.touch();
                }
            }
            case ACT_ABANDON -> abandon(p, land);
            default -> {
            }
        }
        openScreen(p);
    }

    private static void addMember(ServerPlayer p, Land land, String name) {
        if (name.isEmpty()) {
            return;
        }
        UUID id = null;
        String realName = name;
        ServerPlayer online = p.server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            id = online.getUUID();
            realName = online.getGameProfile().getName();
        } else {
            try {
                Optional<GameProfile> prof = p.server.getProfileCache() == null ? Optional.empty() : p.server.getProfileCache().get(name);
                if (prof.isPresent()) {
                    id = prof.get().getId();
                    realName = prof.get().getName();
                }
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] profile lookup failed", t);
            }
        }
        if (id == null) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.member_unknown", name).withStyle(ChatFormatting.RED));
            return;
        }
        if (id.equals(land.owner)) {
            return;
        }
        Member m = land.members.get(id);
        if (m == null) {
            m = new Member();
            m.id = id;
            m.perms = LandData.ALL;
            land.members.put(id, m);
        }
        m.name = realName;
        data(p).touch();
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.member_added", realName).withStyle(ChatFormatting.GREEN));
    }

    /** 토지를 반환하고 구역마다 토지 문서를 돌려준다. */
    private static void abandon(ServerPlayer p, Land land) {
        if (land.plot) {
            PlotManager.giveBack(p, land);
            return;
        }
        List<ItemStack> deeds = new ArrayList<>();
        for (Claim c : land.claims) {
            deeds.add(new ItemStack(DeedItem.Type.byId(c.type).item()));
        }
        int free = 0;
        for (ItemStack s : p.getInventory().items) {
            if (s.isEmpty()) {
                free++;
            }
        }
        if (free < deeds.size()) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.abandon_space", deeds.size()).withStyle(ChatFormatting.RED));
            return;
        }
        data(p).removeLand(land);
        for (ItemStack d : deeds) {
            p.getInventory().add(d);
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.land.abandoned", deeds.size()).withStyle(ChatFormatting.YELLOW));
    }

    // ------------------------------------------------------------------ 보관함 PIN

    public static boolean isStorage(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false;
        }
        if (be instanceof Container) {
            return true;
        }
        try {
            return be.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 큰 상자의 나머지 반쪽 위치. 없으면 null. */
    public static BlockPos otherHalf(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof ChestBlock && st.hasProperty(ChestBlock.TYPE) && st.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(st));
        }
        return null;
    }

    private static String hash(String salt, String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + ":" + pin).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString((salt + ":" + pin).hashCode());
        }
    }

    public static boolean hasPin(Level level, BlockPos pos) {
        LandData data = LandData.get(level.getServer());
        String dim = dim(level);
        if (data.pin(dim, pos) != null) {
            return true;
        }
        BlockPos other = otherHalf(level, pos);
        return other != null && data.pin(dim, other) != null;
    }

    private static boolean validPin(String pin) {
        LandConfig cfg = LandConfig.get();
        return pin != null && pin.matches("[0-9]+") && pin.length() >= cfg.pinMinLength && pin.length() <= cfg.pinMaxLength;
    }

    /** 소유자가 자기 토지 안의 보관함에 PIN 을 건다. pin 이 비어 있으면 해제. */
    public static void setPin(ServerPlayer p, BlockPos pos, String pin) {
        Level level = p.level();
        Land land = data(p).landAt(dim(level), pos);
        if (land == null || (!isOwner(p, land) && !bypass(p))) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.owner_only").withStyle(ChatFormatting.RED));
            return;
        }
        if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 100 || !isStorage(level, pos)) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.not_storage").withStyle(ChatFormatting.RED));
            return;
        }
        String dim = dim(level);
        BlockPos other = otherHalf(level, pos);
        if (pin == null || pin.isEmpty()) {
            data(p).setPin(dim, pos, null);
            if (other != null) {
                data(p).setPin(dim, other, null);
            }
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.removed").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!validPin(pin)) {
            LandConfig cfg = LandConfig.get();
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.invalid", cfg.pinMinLength, cfg.pinMaxLength).withStyle(ChatFormatting.RED));
            return;
        }
        LandData.Pin entry = new LandData.Pin();
        entry.salt = UUID.randomUUID().toString();
        entry.hash = hash(entry.salt, pin);
        data(p).setPin(dim, pos, entry);
        if (other != null) {
            data(p).setPin(dim, other, entry);
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.set").withStyle(ChatFormatting.GREEN));
    }

    /** 이 플레이어가 PIN 을 입력해야 열 수 있는 보관함인가. */
    public static boolean needsPin(ServerPlayer p, Level level, BlockPos pos) {
        if (!hasPin(level, pos)) {
            return false;
        }
        Land land = data(p).landAt(dim(level), pos);
        if (land == null || isOwner(p, land) || bypass(p)) {
            return false;
        }
        Map<String, Long> m = UNLOCKED.get(p.getUUID());
        if (m != null) {
            Long until = m.get(LandData.pinKey(dim(level), pos));
            if (until != null && until > level.getGameTime()) {
                return false;
            }
            BlockPos other = otherHalf(level, pos);
            if (other != null) {
                until = m.get(LandData.pinKey(dim(level), other));
                return until == null || until <= level.getGameTime();
            }
        }
        return true;
    }

    /** PIN 입력 시도. 결과 메시지를 직접 보낸다. */
    public static boolean tryPin(ServerPlayer p, BlockPos pos, String pin) {
        Level level = p.level();
        LandConfig cfg = LandConfig.get();
        long now = level.getGameTime();
        long[] fails = PIN_FAILS.computeIfAbsent(p.getUUID(), k -> new long[]{0, 0});
        if (fails[1] > now) {
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.locked_out", (fails[1] - now) / 20 + 1).withStyle(ChatFormatting.RED));
            return false;
        }
        String dim = dim(level);
        LandData.Pin entry = data(p).pin(dim, pos);
        if (entry == null) {
            BlockPos other = otherHalf(level, pos);
            entry = other == null ? null : data(p).pin(dim, other);
        }
        if (entry == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 100) {
            return false;
        }
        if (pin != null && entry.hash.equals(hash(entry.salt, pin))) {
            fails[0] = 0;
            long until = now + cfg.pinUnlockMinutes * 1200L + 40L;
            Map<String, Long> m = UNLOCKED.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
            m.put(LandData.pinKey(dim, pos), until);
            BlockPos other = otherHalf(level, pos);
            if (other != null) {
                m.put(LandData.pinKey(dim, other), until);
            }
            com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.ok").withStyle(ChatFormatting.GREEN));
            return true;
        }
        fails[0]++;
        if (fails[0] >= cfg.pinMaxAttempts) {
            fails[0] = 0;
            fails[1] = now + cfg.pinLockoutSeconds * 20L;
        }
        com.rtshop.notice.Notice.send(p, Component.translatable("rtshop.pin.wrong").withStyle(ChatFormatting.RED));
        Land land = data(p).landAt(dim, pos);
        if (land != null) {
            ServerPlayer owner = p.server.getPlayerList().getPlayer(land.owner);
            if (owner != null && owner != p) {
                com.rtshop.notice.Notice.send(owner, Component.translatable("rtshop.pin.alert", p.getGameProfile().getName(),
                        pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).withStyle(ChatFormatting.YELLOW));
            }
        }
        return false;
    }

    /** PIN 을 맞힌 직후 보관함을 대신 열어 준다 (다시 우클릭하지 않아도 되게). 일반 우클릭과 같은 경로를 탄다. */
    public static void openStorage(ServerPlayer p, BlockPos pos) {
        Level level = p.level();
        try {
            if (!isStorage(level, pos) || !can(p, level, pos, LandData.CONTAINER) || needsPin(p, level, pos)
                    || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
                return;
            }
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit);
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] could not open storage after PIN", t);
        }
    }

    /** 보관함이 부서졌을 때 PIN 기록을 지운다. */
    public static void clearPin(Level level, BlockPos pos) {
        LandData data = LandData.get(level.getServer());
        if (data.pin(dim(level), pos) != null) {
            data.setPin(dim(level), pos, null);
        }
    }
}
