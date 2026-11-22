package com.rtshop.land;

import com.rtshop.RtShop;
import com.rtshop.item.DeedItem;
import com.rtshop.land.LandData.Claim;
import com.rtshop.land.LandData.Land;
import com.rtshop.net.ContainerInfoPacket;
import com.rtshop.net.PinPromptPacket;
import com.rtshop.net.ShopNet;
import com.rtshop.shop.ShopServer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 토지 보호: 권한 없는 플레이어의 블록 파괴·설치·상호작용을 막는다. 모든 판정은 서버에서 한다. */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class LandEvents {
    private LandEvents() {
    }

    private static final Map<UUID, Long> LAST_MSG = new HashMap<>();
    /** 마지막으로 우클릭한 보관함: 플레이어 -> {x, y, z, 게임 시각}. */
    private static final Map<UUID, long[]> LAST_STORAGE = new HashMap<>();

    private static void deny(ServerPlayer p, Land land) {
        long now = p.level().getGameTime();
        Long last = LAST_MSG.get(p.getUUID());
        if (last == null || now - last >= 20 || now < last) {
            LAST_MSG.put(p.getUUID(), now);
            Component msg = land == LandManager.ZONE ? Component.translatable("rtshop.castle.zone_denied")
                    : land != null && land.forSale() && !land.group.isEmpty()
                    ? Component.translatable("rtshop.plot.denied_starter", land.name)
                    : land != null && land.forSale()
                    ? Component.translatable("rtshop.plot.denied_sale", land.name, String.format("%,d", land.price))
                    : Component.translatable("rtshop.land.denied", land == null ? "?" : land.ownerName);
            p.displayClientMessage(msg.copy().withStyle(ChatFormatting.RED), true);
        }
    }

    private static Land landAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) {
            return null;
        }
        return LandManager.guardAt(sl, pos);
    }

    /** 성 영역(산 토지 밖)의 문·다락문·울타리 문·버튼은 누구나 쓸 수 있다. */
    public static boolean zoneDoor(Land land, BlockState st) {
        if (land != LandManager.ZONE) return false;
        net.minecraft.world.level.block.Block b = st.getBlock();
        return b instanceof net.minecraft.world.level.block.DoorBlock || b instanceof net.minecraft.world.level.block.TrapDoorBlock
                || b instanceof net.minecraft.world.level.block.FenceGateBlock || b instanceof net.minecraft.world.level.block.ButtonBlock
                || st.is(net.minecraft.tags.BlockTags.BUTTONS) || st.is(net.minecraft.tags.BlockTags.DOORS) || st.is(net.minecraft.tags.BlockTags.TRAPDOORS)
                || st.is(net.minecraft.tags.BlockTags.FENCE_GATES);
    }

    private static boolean allowed(ServerPlayer p, Land land, int perm) {
        return land == null || (LandManager.perms(p, land) & perm) != 0;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LandManager.clearRuntime();
        LAST_MSG.clear();
        LAST_STORAGE.clear();
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        Borders.forget(id);
        LAST_MSG.remove(id);
        LAST_STORAGE.remove(id);
        LandManager.SHOW_BORDER.remove(id);
        LandManager.forgetBorder(id);
    }

    // ------------------------------------------------------------------ 블록 파괴 / 설치

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        Land land = LandManager.guardAt(level, pos);
        if (land == null) {
            return;
        }
        if (!allowed(sp, land, LandData.BREAK)) {
            event.setCanceled(true);
            deny(sp, land);
            return;
        }
        if (LandManager.hasPin(level, pos)) {
            if (!LandManager.isOwner(sp, land) && !LandManager.bypass(sp)) {
                event.setCanceled(true);
                sp.displayClientMessage(Component.translatable("rtshop.pin.break_denied").withStyle(ChatFormatting.RED), true);
                return;
            }
            LandManager.clearPin(level, pos);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean denied = false;
        Land deniedLand = null;
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snap : multi.getReplacedBlockSnapshots()) {
                Land land = LandManager.guardAt(level, snap.getPos());
                if (!allowed(sp, land, LandData.PLACE)) {
                    denied = true;
                    deniedLand = land;
                    break;
                }
            }
        } else {
            Land land = LandManager.guardAt(level, event.getPos());
            if (!allowed(sp, land, LandData.PLACE)) {
                denied = true;
                deniedLand = land;
            }
        }
        // 남의 보관함 옆에 호퍼를 붙여 아이템을 빼가는 것을 막는다
        if (!denied && event.getPlacedBlock().getBlock() instanceof HopperBlock) {
            for (Direction dir : Direction.values()) {
                BlockPos n = event.getPos().relative(dir);
                if (!LandManager.isStorage(level, n)) {
                    continue;
                }
                Land land = LandManager.guardAt(level, n);
                if (land == null) {
                    continue;
                }
                boolean owner = LandManager.isOwner(sp, land) || LandManager.bypass(sp);
                if (!allowed(sp, land, LandData.CONTAINER) || (!owner && LandManager.hasPin(level, n))) {
                    denied = true;
                    deniedLand = land;
                    break;
                }
            }
        }
        // 넓은 범위에 작용하는 블록(자동 수확 상자 등)은 남의 토지 근처에 놓을 수 없다
        if (!denied) {
            try {
                ResourceLocation id = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
                Integer range = id == null ? null : LandConfig.get().rangedBlocks.get(id.toString());
                if (range != null && range > 0) {
                    BlockPos p = event.getPos();
                    LandData data = LandManager.data(sp);
                    for (Claim o : data.claimsNear(LandManager.dim(level), p.getX(), p.getY(), p.getZ(),
                            p.getX(), p.getY(), p.getZ(), range)) {
                        Land land = data.land(o.landId);
                        if (land != null && !allowed(sp, land, LandData.BREAK)) {
                            denied = true;
                            deniedLand = land;
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                RtShop.LOGGER.debug("[rtshop] ranged block check failed", t);
            }
        }
        if (denied) {
            event.setCanceled(true);
            deny(sp, deniedLand);
            sp.inventoryMenu.sendAllDataToRemote();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Land land = LandManager.guardAt(level, event.getPos());
            if (land != null && !(event.getEntity() instanceof ServerPlayer sp && allowed(sp, land, LandData.BREAK))) {
                event.setCanceled(true);
            }
        }
    }

    // ------------------------------------------------------------------ 우클릭

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getItemStack().is(com.rtshop.item.ModItems.PLOT_WAND.get()) && sp.hasPermissions(2)) {
            return; // 구획 지팡이(OP)는 토지 위에서도 칸을 고를 수 있어야 한다
        }
        BlockPos pos = event.getPos();
        if (level.getBlockState(pos).getBlock() instanceof com.rtshop.pshop.PlayerShopBlock && !sp.isSecondaryUseActive()) {
            return; // 플레이어 상점은 남의 토지 위에 있어도 누구나 열어 살 수 있다 (관리·부수기는 상점이 주인만 허락)
        }
        Land land = LandManager.guardAt(level, pos);
        boolean storage = LandManager.isStorage(level, pos);
        // 아이템 사용(블록 설치, 양동이, 부싯돌 등): 대상 칸이나 그 앞 칸이 남의 토지면 막는다
        Direction face = event.getFace();
        BlockPos front = face == null ? pos : pos.relative(face);
        Land frontLand = LandManager.guardAt(level, front);
        boolean itemOk = allowed(sp, land, LandData.PLACE) && allowed(sp, frontLand, LandData.PLACE);
        if (land != null) {
            int perm = storage ? LandData.CONTAINER : LandData.USE;
            if (!allowed(sp, land, perm) && !zoneDoor(land, level.getBlockState(pos))) {
                BlockState st = level.getBlockState(pos);
                if (storage || st.getMenuProvider(level, pos) != null || interactive(st)) {
                    deny(sp, land);
                }
                if (!itemOk) {
                    // 블록 사용도 아이템 사용도 안 되면 이벤트 자체를 취소한다 (다른 모드의 우클릭 기능까지 함께 막기 위함)
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.PASS);
                    if (!event.getItemStack().isEmpty()) {
                        sp.inventoryMenu.sendAllDataToRemote();
                    }
                    LAST_STORAGE.remove(sp.getUUID());
                    return;
                }
                event.setUseBlock(Event.Result.DENY);
            } else if (storage && LandManager.needsPin(sp, level, pos)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new PinPromptPacket(pos));
                return;
            }
        }
        if (!itemOk && !event.getItemStack().isEmpty()) {
            event.setUseItem(Event.Result.DENY);
        }
        if (storage && land != null) {
            LAST_STORAGE.put(sp.getUUID(), new long[]{pos.getX(), pos.getY(), pos.getZ(), level.getGameTime()});
        } else {
            LAST_STORAGE.remove(sp.getUUID());
        }
    }

    /** 문·스위치처럼 눌러서 쓰는 블록인가 (거부 안내를 띄울지 판단하는 용도). */
    private static boolean interactive(BlockState st) {
        Block b = st.getBlock();
        return b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock || b instanceof ButtonBlock
                || b instanceof LeverBlock || b instanceof BedBlock;
    }

    /** 양동이는 블록을 직접 겨누지 않아도 액체를 놓을 수 있어 따로 검사한다. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BucketItem bucket)) {
            return;
        }
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(6.0));
        boolean empty = bucket.getFluid() == Fluids.EMPTY;
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                empty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, sp));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockPos front = pos.relative(hit.getDirection());
        int perm = empty ? LandData.BREAK : LandData.PLACE;
        Land a = LandManager.guardAt(level, pos);
        Land b = LandManager.guardAt(level, front);
        if (!allowed(sp, a, perm) || !allowed(sp, b, perm)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            deny(sp, a != null ? a : b);
            sp.inventoryMenu.sendAllDataToRemote();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFillBucket(FillBucketEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getTarget() instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            Land land = LandManager.guardAt(level, hit.getBlockPos());
            if (!allowed(sp, land, LandData.BREAK)) {
                event.setCanceled(true);
                deny(sp, land);
            }
        }
    }

    // ------------------------------------------------------------------ 엔티티

    private static boolean protectedEntity(Entity target) {
        return !(target instanceof Player) && !(target instanceof Enemy) && !ShopServer.isNpc(target);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getLevel() instanceof ServerLevel level
                && protectedEntity(event.getTarget())) {
            Land land = LandManager.guardAt(level, event.getTarget().blockPosition());
            if (!allowed(sp, land, LandData.ENTITY)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                deny(sp, land);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getLevel() instanceof ServerLevel level
                && protectedEntity(event.getTarget())) {
            Land land = LandManager.guardAt(level, event.getTarget().blockPosition());
            if (!allowed(sp, land, LandData.ENTITY)) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.FAIL);
                deny(sp, land);
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && sp.level() instanceof ServerLevel level
                && protectedEntity(event.getTarget())) {
            Land land = LandManager.guardAt(level, event.getTarget().blockPosition());
            if (!allowed(sp, land, LandData.ENTITY)) {
                event.setCanceled(true);
                deny(sp, land);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PlotBrowser.tickJobs();
        }
    }

    // ------------------------------------------------------------------ 성 영역: 몬스터 스폰 금지

    private static boolean monster(net.minecraft.world.entity.EntityType<?> type) {
        return type.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    @SubscribeEvent
    public static void onSpawnCheck(net.minecraftforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck event) {
        if (monster(event.getEntityType()) && LandManager.inCastleZone(event.getLevel().getLevel(), event.getPos())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        var mob = event.getEntity();
        if (!(mob instanceof Enemy) && !monster(mob.getType())) {
            return;
        }
        var type = event.getSpawnType();
        if (type == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG || type == net.minecraft.world.entity.MobSpawnType.COMMAND
                || type == net.minecraft.world.entity.MobSpawnType.BUCKET) {
            return;
        }
        if (LandManager.inCastleZone(event.getLevel().getLevel(), mob.blockPosition())) {
            event.setSpawnCancelled(true);
        }
    }

    // ------------------------------------------------------------------ 폭발 / 피스톤

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!LandConfig.get().protectFromExplosions || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        event.getAffectedBlocks().removeIf(pos -> LandManager.guardAt(level, pos) != null);
        event.getAffectedEntities().removeIf(e -> protectedEntity(e) && !(e instanceof net.minecraft.world.entity.LivingEntity)
                && LandManager.guardAt(level, e.blockPosition()) != null);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!LandConfig.get().protectFromPistons) {
            return;
        }
        LevelAccessor acc = event.getLevel();
        if (!(acc instanceof ServerLevel level)) {
            return;
        }
        try {
            Land own = LandManager.guardAt(level, event.getPos());
            UUID owner = own == null ? null : own.owner;
            var helper = event.getStructureHelper();
            if (helper == null) {
                return;
            }
            helper.resolve();
            Direction dir = event.getPistonMoveType().isExtend ? event.getDirection() : event.getDirection().getOpposite();
            List<BlockPos> push = helper.getToPush();
            List<BlockPos> destroy = helper.getToDestroy();
            if (crosses(level, owner, event.getFaceOffsetPos()) || crossesAll(level, owner, push, null)
                    || crossesAll(level, owner, push, dir) || crossesAll(level, owner, destroy, null)) {
                event.setCanceled(true);
            }
        } catch (Throwable t) {
            RtShop.LOGGER.debug("[rtshop] piston check failed", t);
        }
    }

    private static boolean crosses(ServerLevel level, UUID pistonOwner, BlockPos pos) {
        Land land = LandManager.guardAt(level, pos);
        return land != null && !land.owner.equals(pistonOwner);
    }

    private static boolean crossesAll(ServerLevel level, UUID pistonOwner, List<BlockPos> list, Direction offset) {
        for (BlockPos p : list) {
            if (crosses(level, pistonOwner, offset == null ? p : p.relative(offset))) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ 보관함 PIN 버튼 정보

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long[] last = LAST_STORAGE.get(sp.getUUID());
        if (last == null || Math.abs(level.getGameTime() - last[3]) > 5) {
            return;
        }
        BlockPos pos = new BlockPos((int) last[0], (int) last[1], (int) last[2]);
        Land land = LandManager.guardAt(level, pos);
        if (land == null) {
            return;
        }
        boolean owner = LandManager.isOwner(sp, land) || LandManager.bypass(sp);
        ShopNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                new ContainerInfoPacket(event.getContainer().containerId, pos, owner, LandManager.hasPin(level, pos)));
    }

    // ------------------------------------------------------------------ 경계 표시 (입자)

    private static final Vector3f GREEN = new Vector3f(0.3F, 1.0F, 0.3F);
    private static final Vector3f RED = new Vector3f(1.0F, 0.25F, 0.2F);
    private static final Vector3f AQUA = new Vector3f(0.3F, 0.8F, 1.0F);
    private static final Vector3f ORANGE = new Vector3f(1.0F, 0.6F, 0.1F);
    private static final Vector3f WHITE = new Vector3f(1.0F, 1.0F, 1.0F);

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)
                || !(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if ((now + sp.getId()) % 10 != 0) {
            return;
        }
        try {
            tickBorders(sp, level, now);
        } finally {
            Borders.flush(sp);
        }
    }

    private static void tickBorders(ServerPlayer sp, ServerLevel level, long now) {
        if (PlotManager.tick(sp, level, now)) {
            return; // 구획 지팡이를 든 OP: 선택 영역을 그렸음
        }
        DeedItem deed = null;
        if (sp.getMainHandItem().getItem() instanceof DeedItem d) {
            deed = d;
        } else if (sp.getOffhandItem().getItem() instanceof DeedItem d) {
            deed = d;
        }
        LandData data = LandManager.data(sp);
        String dim = LandManager.dim(level);
        if (deed != null) {
            LandManager.Candidate c = LandManager.candidate(sp, deed.type);
            Borders.box(sp, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, Borders.rgb(c.error == null ? GREEN : RED));
            int shown = 0;
            for (Claim o : data.claimsNear(dim, c.x0, c.y0, c.z0, c.x1, c.y1, c.z1, 24)) {
                Land l = data.land(o.landId);
                Borders.claim(sp, o, Borders.rgb(l != null && l.owner.equals(sp.getUUID()) ? AQUA : ORANGE));
                if (++shown >= 6) {
                    break;
                }
            }
            return;
        }
        if (LandManager.borderOn(sp)) {
            // 명령어로 켠 경계 표시: 지금 서 있는 토지·농경지의 둘레 (작물 위에 서 있으면 발 위치가 한 칸 위일 수 있다)
            Land here = data.landAt(dim, sp.blockPosition());
            if (here == null) here = data.landAt(dim, sp.blockPosition().below());
            if (here != null) {
                Vector3f col = here.owner.equals(sp.getUUID()) ? AQUA : here.forSale() ? WHITE : ORANGE;
                int shown = 0;
                for (Claim o : here.claims) {
                    if (!o.dim.equals(dim) || (o.mask == null
                            && sp.distanceToSqr((o.x0 + o.x1) / 2.0, sp.getY(), (o.z0 + o.z1) / 2.0) > 128 * 128)) {
                        continue;
                    }
                    Borders.claim(sp, o, Borders.rgb(col));
                    if (++shown >= 8) {
                        break;
                    }
                }
            }
        }
        long[] show = LandManager.SHOW_BORDER.get(sp.getUUID());
        if (show != null) {
            if (show[1] < now) {
                LandManager.SHOW_BORDER.remove(sp.getUUID());
            } else {
                Land land = data.land((int) show[0]);
                if (land != null) {
                    int shown = 0;
                    for (Claim o : land.claims) {
                        if (!o.dim.equals(dim) || (o.mask == null
                                && sp.distanceToSqr((o.x0 + o.x1) / 2.0, sp.getY(), (o.z0 + o.z1) / 2.0) > 96 * 96)) {
                            continue;
                        }
                        Borders.claim(sp, o, Borders.rgb(AQUA));
                        if (++shown >= 6) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /** 구역 경계를 그린다. 칸 모양 구역은 바닥 둘레를 지면 높이를 따라 그린다. */
    static void drawClaim(ServerLevel level, ServerPlayer sp, Claim o, Vector3f color, int step) {
        if (o.mask != null) {
            PlotManager.drawOutline(level, sp, o::hasColumn, o.x0, o.z0, o.x1, o.z1, color, 1200);
        } else {
            drawBox(level, sp, o.x0, o.y0, o.z0, o.x1, o.y1, o.z1, color, step);
        }
    }

    private static void drawBox(ServerLevel level, ServerPlayer sp, int x0, int y0, int z0, int x1, int y1, int z1,
                                Vector3f color, int step) {
        DustParticleOptions dust = new DustParticleOptions(color, 1.5F);
        double ax = x0, az = z0, bx = x1 + 1.0, bz = z1 + 1.0;
        // 토지가 하늘 끝까지 이어지므로 위아래는 플레이어 근처만 그린다 (입자가 너무 많아지지 않게)
        double ay = Math.max(y0, Math.floor(sp.getY()) - 12);
        double by = Math.min(y1 + 1.0, Math.floor(sp.getY()) + 20);
        if (by <= ay) {
            return;
        }
        for (double x = ax; x <= bx + 0.01; x += step) {
            dot(level, sp, dust, x, ay, az);
            dot(level, sp, dust, x, ay, bz);
            dot(level, sp, dust, x, by, az);
            dot(level, sp, dust, x, by, bz);
        }
        for (double y = ay; y <= by + 0.01; y += step) {
            dot(level, sp, dust, ax, y, az);
            dot(level, sp, dust, ax, y, bz);
            dot(level, sp, dust, bx, y, az);
            dot(level, sp, dust, bx, y, bz);
        }
        for (double z = az; z <= bz + 0.01; z += step) {
            dot(level, sp, dust, ax, ay, z);
            dot(level, sp, dust, ax, by, z);
            dot(level, sp, dust, bx, ay, z);
            dot(level, sp, dust, bx, by, z);
        }
    }

    private static void dot(ServerLevel level, ServerPlayer sp, DustParticleOptions dust, double x, double y, double z) {
        level.sendParticles(sp, dust, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
