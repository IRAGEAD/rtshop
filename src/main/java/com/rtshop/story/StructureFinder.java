package com.rtshop.story;

import com.mojang.datafixers.util.Pair;
import com.rtshop.RtShop;
import com.rtshop.land.CastlePlots;
import com.rtshop.land.LandManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 실제로 생긴 건축물만 찾는다 (조사대 나침반과 /locate structure 공통).
 * 바닐라 /locate 는 "생길 수도 있는 자리"를 계산만 해서, 다른 모드·단계 잠금·지형 조건으로 생기지 않은 자리도 알려 준다.
 * 여기서는 후보 청크의 건축물 시작 정보를 실제로 만들어 보고(STRUCTURE_STARTS 단계까지만), 진짜로 시작점이 있는 자리만 돌려준다.
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID)
public final class StructureFinder {
    private StructureFinder() {
    }

    /** 확인해 볼 후보 수와 시간 한도 (서버 멈춤 방지). */
    static final int MAX_CHECKS = 48;
    static final long MAX_NANOS = 3_000_000_000L;

    public static final class Found {
        public final BlockPos pos;
        public final Holder<Structure> structure;

        Found(BlockPos pos, Holder<Structure> structure) {
            this.pos = pos;
            this.structure = structure;
        }
    }

    /** 테스트·로그용: 마지막 검색에서 확인한 후보 수와 걸러낸 수. */
    public static volatile int lastChecked, lastRejected, lastTownSkipped;
    public static volatile String lastInfo = "";

    /** 구조물 ID, #태그, "모드:*" → 구조물 목록. 모르는 대상이면 null. */
    public static List<Holder<Structure>> resolve(ServerLevel level, String target) {
        Registry<Structure> reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> out = new ArrayList<>();
        try {
            if (target.startsWith("#")) {
                var tag = reg.getTag(TagKey.create(Registries.STRUCTURE, new ResourceLocation(target.substring(1))));
                if (tag.isEmpty()) return null;
                for (Holder<Structure> h : tag.get()) out.add(h);
            } else if (target.endsWith(":*")) {
                String ns = target.substring(0, target.length() - 2);
                reg.holders().filter(h -> h.key().location().getNamespace().equals(ns)).forEach(out::add);
            } else {
                var h = reg.getHolder(ResourceKey.create(Registries.STRUCTURE, new ResourceLocation(target)));
                if (h.isEmpty()) return null;
                out.add(h.get());
            }
        } catch (Exception e) {
            return null;
        }
        return out.isEmpty() ? null : out;
    }

    /** 마을(성 영역) 가장자리에서의 거리. 성 영역이 없으면 월드 스폰 지점에서의 거리. */
    public static double townDistance(ServerLevel level, int x, int z) {
        CastlePlots.Def d = CastlePlots.def();
        if (CastlePlots.zoneKnown(level.getServer()) && LandManager.dim(level).equals(d.dim)) {
            double dx = Math.max(0, Math.max(d.zx0 - x, x - d.zx1));
            double dz = Math.max(0, Math.max(d.zz0 - z, z - d.zz1));
            return Math.sqrt(dx * dx + dz * dz);
        }
        if (level != level.getServer().overworld()) return Double.MAX_VALUE;
        BlockPos s = level.getSharedSpawnPos();
        double dx = x - s.getX(), dz = z - s.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private record Cand(Holder<Structure> h, StructurePlacement placement, int cx, int cz, double d2) {
    }

    /**
     * from 에서 가장 가까운, 실제로 생긴(또는 생기는) 건축물. 없으면 null.
     * radiusChunks 안만 본다. minTown > 0 이면 마을에서 그만큼 떨어진 자리만.
     */
    public static Found find(ServerLevel level, List<Holder<Structure>> set, BlockPos from, int radiusChunks, double minTown) {
        lastChecked = 0;
        lastRejected = 0;
        lastTownSkipped = 0;
        if (set == null || set.isEmpty()) return null;
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        long seed = state.getLevelSeed();
        int fx = from.getX() >> 4, fz = from.getZ() >> 4;
        List<Cand> cands = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        List<Holder<Structure>> other = new ArrayList<>();
        for (Holder<Structure> h : set) {
            String id = h.unwrapKey().map(k -> k.location().toString()).orElse("");
            if (StagedStructures.blocks(id)) continue; // 아직 풀리지 않은 이야기 건축물은 생기지 않는다
            List<StructurePlacement> pls = state.getPlacementsForStructure(h);
            if (pls.isEmpty()) continue; // 이 차원에서는 생기지 않음
            for (StructurePlacement pl : pls) {
                if (pl instanceof RandomSpreadStructurePlacement rs) {
                    int sp = Math.max(1, rs.spacing());
                    int r = radiusChunks / sp + 1;
                    int rx0 = Math.floorDiv(fx, sp), rz0 = Math.floorDiv(fz, sp);
                    for (int rx = rx0 - r; rx <= rx0 + r; rx++) {
                        for (int rz = rz0 - r; rz <= rz0 + r; rz++) {
                            ChunkPos c = rs.getPotentialStructureChunk(seed, rx, rz);
                            add(level, cands, seen, h, pl, c.x, c.z, fx, fz, radiusChunks, minTown);
                        }
                    }
                } else if (pl instanceof ConcentricRingsStructurePlacement cr) {
                    List<ChunkPos> ring = state.getRingPositionsFor(cr);
                    if (ring != null) for (ChunkPos c : ring) add(level, cands, seen, h, pl, c.x, c.z, fx, fz, radiusChunks, minTown);
                } else {
                    other.add(h);
                }
            }
        }
        cands.sort((a, b) -> Double.compare(a.d2, b.d2));
        int nPl = 0, staged = 0;
        for (Holder<Structure> h : set) {
            String id = h.unwrapKey().map(k -> k.location().toString()).orElse("");
            if (StagedStructures.blocks(id)) staged++;
            else nPl += state.getPlacementsForStructure(h).size();
        }
        int passed = 0;
        long t0 = System.nanoTime();
        Found best = null;
        for (Cand c : cands) {
            if (lastChecked >= MAX_CHECKS || System.nanoTime() - t0 > MAX_NANOS) break;
            if (!c.placement.isStructureChunk(state, c.cx, c.cz)) continue;
            passed++;
            lastChecked++;
            BlockPos at = verify(level, c.h, c.cx, c.cz);
            if (at == null) {
                lastRejected++;
                continue;
            }
            best = new Found(at, c.h);
            break;
        }
        lastInfo = "holders=" + set.size() + " staged=" + staged + " placements=" + nPl + " cands=" + cands.size() + " placementOk=" + passed
                + " other=" + other.size() + " genStructures=" + level.getServer().getWorldData().worldGenOptions().generateStructures();
        // 다른 방식으로 놓이는 모드 건축물: 바닐라 계산으로 찾은 뒤 실제로 있는지만 확인
        for (Holder<Structure> h : other) {
            if (lastChecked >= MAX_CHECKS) break;
            Pair<BlockPos, Holder<Structure>> r = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(h), from, Math.min(radiusChunks, 100), false);
            if (r == null) continue;
            int cx = r.getFirst().getX() >> 4, cz = r.getFirst().getZ() >> 4;
            if (minTown > 0 && townDistance(level, (cx << 4) + 8, (cz << 4) + 8) < minTown) {
                lastTownSkipped++;
                continue;
            }
            lastChecked++;
            BlockPos at = verify(level, h, cx, cz);
            if (at == null) {
                lastRejected++;
                continue;
            }
            if (best == null || at.distSqr(from) < best.pos.distSqr(from)) best = new Found(at, h);
        }
        return best;
    }

    private static void add(ServerLevel level, List<Cand> cands, Set<Long> seen, Holder<Structure> h, StructurePlacement pl,
                            int cx, int cz, int fx, int fz, int radiusChunks, double minTown) {
        long dx = cx - fx, dz = cz - fz;
        if (dx * dx + dz * dz > (long) radiusChunks * radiusChunks) return;
        if (minTown > 0 && townDistance(level, (cx << 4) + 8, (cz << 4) + 8) < minTown) {
            lastTownSkipped++;
            return;
        }
        long key = ChunkPos.asLong(cx, cz) * 31 + System.identityHashCode(h);
        if (!seen.add(key)) return;
        cands.add(new Cand(h, pl, cx, cz, dx * dx + dz * dz));
    }

    /** 그 청크에 이 건축물의 진짜 시작점이 있으면 그 가운데 자리. */
    public static BlockPos verify(ServerLevel level, Holder<Structure> h, int cx, int cz) {
        try {
            ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_STARTS, true);
            if (chunk == null) return null;
            StructureStart start = chunk.getStartForStructure(h.value());
            if (start == null || !start.isValid()) return null;
            BoundingBox bb = start.getBoundingBox();
            return new BlockPos(bb.getCenter().getX(), bb.getCenter().getY(), bb.getCenter().getZ());
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------ /locate structure 도 실제로 있는 곳만

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCommand(CommandEvent event) {
        try {
            String in = event.getParseResults().getReader().getString().trim();
            if (in.startsWith("/")) in = in.substring(1);
            String[] parts = in.split("\\s+");
            if (parts.length != 3 || !parts[0].equals("locate") || !parts[1].equals("structure")) return;
            CommandSourceStack src = event.getParseResults().getContext().getSource();
            if (!src.hasPermission(2)) return;
            ServerLevel level = src.getLevel();
            List<Holder<Structure>> set = resolve(level, parts[2]);
            if (set == null) return; // 모르는 대상은 바닐라가 오류를 알려 준다
            event.setCanceled(true);
            BlockPos from = BlockPos.containing(src.getPosition());
            Found f = find(level, set, from, 100, 0);
            if (f == null) {
                src.sendFailure(Component.translatable("commands.locate.structure.not_found", parts[2]));
                return;
            }
            int x = f.pos.getX(), z = f.pos.getZ();
            int dist = (int) Math.round(Math.sqrt(Math.pow(x - from.getX(), 2) + Math.pow(z - from.getZ(), 2)));
            String name = f.structure.unwrapKey().map(k -> k.location().toString()).orElse(parts[2]);
            Component coords = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", x, "~", z))
                    .withStyle(s -> s.withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + x + " ~ " + z))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
            src.sendSuccess(() -> Component.translatable("commands.locate.structure.success", name, coords, dist), false);
        } catch (Throwable t) {
            // 문제가 생기면 바닐라 /locate 에 맡긴다
        }
    }
}
