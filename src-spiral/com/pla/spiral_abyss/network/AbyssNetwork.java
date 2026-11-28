/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  dev.architectury.platform.Platform
 *  dev.architectury.utils.Env
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 */
package com.pla.spiral_abyss.network;

import com.mojang.logging.LogUtils;
import com.pla.spiral_abyss.client.handler.JournalClientHandler;
import com.pla.spiral_abyss.client.overlay.AbyssEncounterOverlay;
import com.pla.spiral_abyss.client.overlay.RareRewardOverlay;
import com.pla.spiral_abyss.client.portal.PortalEntryAnimation;
import com.pla.spiral_abyss.client.screen.FloorSelectionScreen;
import com.pla.spiral_abyss.encounter.AbyssRejection;
import com.pla.spiral_abyss.encounter.FloorProgress;
import com.pla.spiral_abyss.encounter.JournalPreviews;
import com.pla.spiral_abyss.encounter.RewardPreviews;
import com.pla.spiral_abyss.encounter.data.BossOptions;
import com.pla.spiral_abyss.encounter.data.FloorCatalog;
import com.pla.spiral_abyss.encounter.data.FloorDefinition;
import com.pla.spiral_abyss.encounter.data.MobCatalog;
import com.pla.spiral_abyss.network.AbyssChannel;
import com.pla.spiral_abyss.world.AbyssDimension;
import com.pla.spiral_abyss.world.AbyssParties;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class AbyssNetwork {
    private static final String VERSION = "25";
    private static final Map<ServerPlayer, Integer> JOURNAL_REQUESTS = new WeakHashMap<ServerPlayer, Integer>();
    private static final AbyssChannel CHANNEL = AbyssChannel.create("25");

    private AbyssNetwork() {
    }

    private static void client(Runnable action) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            action.run();
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(SectionStatus.class, 8, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder((message, buffer) -> {
            buffer.m_130077_(message.token());
            AbyssNetwork.writeSections(buffer, message.sections());
            buffer.m_130130_(message.selectable());
        }).decoder(buffer -> new SectionStatus(buffer.m_130259_(), AbyssNetwork.readSections(buffer), buffer.m_130242_())).consumerMainThread((message, context) -> AbyssNetwork.client(() -> FloorSelectionScreen.updateSections(message))).add();
        CHANNEL.messageBuilder(OpenFloors.class, 0, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder(AbyssNetwork::writeFloors).decoder(AbyssNetwork::readFloors).consumerMainThread((message, context) -> AbyssNetwork.client(() -> PortalEntryAnimation.open(message))).add();
        CHANNEL.messageBuilder(SelectFloor.class, 1, AbyssChannel.Direction.PLAY_TO_SERVER).encoder((message, buffer) -> {
            buffer.m_130077_(message.token);
            buffer.m_130130_(message.floor);
        }).decoder(buffer -> new SelectFloor(buffer.m_130259_(), buffer.m_130242_())).consumerMainThread((message, context) -> {
            ServerPlayer sender = ((AbyssChannel.Context)context.get()).getSender();
            if (sender != null && message.floor >= -1 && message.floor <= 7) {
                AbyssParties.get(sender.f_8924_).portals().choose(sender, message.token, message.floor);
            }
        }).add();
        CHANNEL.messageBuilder(EncounterStatus.class, 2, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder((message, buffer) -> {
            buffer.writeBoolean(message.active);
            buffer.writeBoolean(message.preparing);
            buffer.m_130130_(message.floor);
            buffer.m_130130_(message.wave);
            buffer.m_130130_(message.defeated);
            buffer.m_130130_(message.total);
            buffer.m_130130_(message.seconds);
            buffer.m_130130_(message.countdownSeconds);
            AbyssNetwork.writeChallenges(buffer, message.challenges);
            buffer.m_236828_(message.rejectedBosses, FriendlyByteBuf::m_130085_);
        }).decoder(buffer -> new EncounterStatus(buffer.readBoolean(), buffer.readBoolean(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), AbyssNetwork.readChallenges(buffer), AbyssNetwork.readRejectedBosses(buffer))).consumerMainThread((message, context) -> AbyssNetwork.client(() -> AbyssEncounterOverlay.update(message))).add();
        CHANNEL.messageBuilder(CloseFloors.class, 3, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder((message, buffer) -> {}).decoder(buffer -> new CloseFloors()).consumerMainThread((message, context) -> AbyssNetwork.client(() -> FloorSelectionScreen.closeForReset())).add();
        CHANNEL.messageBuilder(KeepFloorsOpen.class, 4, AbyssChannel.Direction.PLAY_TO_SERVER).encoder((message, buffer) -> buffer.m_130077_(message.token)).decoder(buffer -> new KeepFloorsOpen(buffer.m_130259_())).consumerMainThread((message, context) -> {
            ServerPlayer sender = ((AbyssChannel.Context)context.get()).getSender();
            if (sender != null) {
                AbyssParties.get(sender.f_8924_).portals().keepOpen(sender, message.token);
            }
        }).add();
        CHANNEL.messageBuilder(RareReward.class, 5, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder((message, buffer) -> {
            buffer.m_130083_(message.player);
            buffer.m_130055_(message.item);
        }).decoder(buffer -> new RareReward(buffer.m_130238_(), buffer.m_130267_())).consumerMainThread((message, context) -> AbyssNetwork.client(() -> RareRewardOverlay.enqueue(message.player, message.item))).add();
        CHANNEL.messageBuilder(RequestJournal.class, 6, AbyssChannel.Direction.PLAY_TO_SERVER).encoder((message, buffer) -> {}).decoder(buffer -> new RequestJournal()).consumerMainThread((message, context) -> {
            ServerPlayer sender = ((AbyssChannel.Context)context.get()).getSender();
            if (sender == null || !sender.m_6084_() || sender.m_9232_()) {
                return;
            }
            int now = sender.f_8924_.m_129921_();
            Integer last = JOURNAL_REQUESTS.get(sender);
            if (last != null && now - last < 5) {
                return;
            }
            JOURNAL_REQUESTS.put(sender, now);
            AbyssParties parties = AbyssParties.get(sender.f_8924_);
            if (sender.m_9236_().m_46472_().equals(AbyssDimension.LEVEL)) {
                parties.portals().openArenaMenu(sender);
                return;
            }
            CHANNEL.sendToPlayer(sender, new OpenJournal(parties.resetRemainingMillis(), parties.cycle().bannerPoseSeed(), JournalPreviews.bosses(sender)));
        }).add();
        CHANNEL.messageBuilder(OpenJournal.class, 7, AbyssChannel.Direction.PLAY_TO_CLIENT).encoder(AbyssNetwork::writeJournal).decoder(AbyssNetwork::readJournal).consumerMainThread((message, context) -> AbyssNetwork.client(() -> JournalClientHandler.open(message))).add();
    }

    public static void sectionStatus(ServerPlayer player, UUID token, List<SectionView> sections, int selectable) {
        CHANNEL.sendToPlayer(player, new SectionStatus(token, sections, selectable));
    }

    private static void writeSections(FriendlyByteBuf buffer, List<SectionView> sections) {
        buffer.m_130130_(sections.size());
        for (SectionView section : sections) {
            buffer.m_130130_(section.floor());
            buffer.m_130130_(section.status());
            buffer.m_130130_(section.players().size());
            for (SectionPlayer player : section.players()) {
                buffer.m_130077_(player.id());
                buffer.m_130072_(player.name(), 16);
            }
        }
    }

    private static List<SectionView> readSections(FriendlyByteBuf buffer) {
        ArrayList<SectionView> sections = new ArrayList<SectionView>();
        for (int count = AbyssNetwork.bounded(buffer, 7); count > 0; --count) {
            int floor = AbyssNetwork.bounded(buffer, 7);
            int status = AbyssNetwork.bounded(buffer, 2);
            ArrayList<SectionPlayer> players = new ArrayList<SectionPlayer>();
            for (int people = AbyssNetwork.bounded(buffer, 80); people > 0; --people) {
                players.add(new SectionPlayer(buffer.m_130259_(), buffer.m_130136_(16)));
            }
            sections.add(new SectionView(floor, status, players));
        }
        return List.copyOf(sections);
    }

    private static void writeFloors(OpenFloors message, FriendlyByteBuf buffer) {
        buffer.m_130077_(message.token);
        buffer.m_130130_(message.claimed);
        buffer.m_130130_(message.unlocked);
        buffer.m_130130_(message.selectable);
        buffer.m_130130_(message.visible);
        buffer.writeBoolean(message.inside);
        buffer.writeLong(message.resetRemainingMillis);
        AbyssNetwork.writePreviews(buffer, message.floors);
        buffer.m_236821_((Object)message.entryPortal, FriendlyByteBuf::m_130064_);
        buffer.m_130130_(message.endlessWave);
        AbyssNetwork.writeSections(buffer, message.sections);
    }

    private static OpenFloors readFloors(FriendlyByteBuf buffer) {
        return new OpenFloors(buffer.m_130259_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.m_130242_(), buffer.readBoolean(), buffer.readLong(), AbyssNetwork.readPreviews(buffer), (BlockPos)buffer.m_236868_(FriendlyByteBuf::m_130135_), buffer.m_130242_(), AbyssNetwork.readSections(buffer));
    }

    private static void writeJournal(OpenJournal message, FriendlyByteBuf buffer) {
        buffer.writeLong(message.resetRemainingMillis());
        buffer.writeLong(message.poseSeed());
        buffer.m_130130_(message.bosses().size());
        for (BannerBoss boss : message.bosses()) {
            buffer.m_130130_(boss.floor());
            buffer.m_130130_(boss.wave());
            buffer.m_130085_(boss.id());
            AbyssNetwork.writeAnimation(buffer, boss.animation());
            buffer.m_130130_(boss.rewards().size());
            for (RewardPreview reward : boss.rewards()) {
                buffer.m_130085_(reward.id());
                buffer.m_130055_(reward.icon());
                buffer.writeDouble(reward.displayChance());
            }
        }
    }

    private static OpenJournal readJournal(FriendlyByteBuf buffer) {
        long remaining = buffer.readLong();
        long poseSeed = buffer.readLong();
        ArrayList<BannerBoss> bosses = new ArrayList<BannerBoss>();
        for (int b = AbyssNetwork.bounded(buffer, 4); b > 0; --b) {
            int floor = AbyssNetwork.bounded(buffer, 6);
            int wave = AbyssNetwork.bounded(buffer, 3);
            ResourceLocation id = buffer.m_130281_();
            BossOptions.PreviewAnimation animation = AbyssNetwork.readAnimation(buffer);
            ArrayList<RewardPreview> rewards = new ArrayList<RewardPreview>();
            for (int r = AbyssNetwork.bounded(buffer, 128); r > 0; --r) {
                rewards.add(new RewardPreview(buffer.m_130281_(), false, true, List.of(), false, buffer.m_130267_(), buffer.readDouble()));
            }
            bosses.add(new BannerBoss(floor, wave, id, rewards, animation));
        }
        return new OpenJournal(remaining, poseSeed, bosses);
    }

    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside) {
        AbyssNetwork.openFloors(player, token, claimed, selectable, visible, inside, null);
    }

    public static void openFloors(ServerPlayer player, UUID token, int claimed, int selectable, int visible, boolean inside, BlockPos entryPortal) {
        FloorProgress progress = AbyssParties.get((MinecraftServer)player.f_8924_).profile((UUID)player.m_20148_()).progress;
        int unlocked = 128;
        for (int floor = 1; floor <= 6; ++floor) {
            if (!progress.unlocked(floor)) continue;
            unlocked |= 1 << floor;
        }
        CHANNEL.sendToPlayer(player, new OpenFloors(token, claimed, unlocked, selectable, visible, inside, AbyssParties.get(player.f_8924_).resetRemainingMillis(), AbyssNetwork.previews(player), entryPortal, AbyssParties.get((MinecraftServer)player.f_8924_).profile((UUID)player.m_20148_()).endlessWave, AbyssParties.get(player.f_8924_).portals().sectionViews(player)));
    }

    private static List<FloorPreview> previews(ServerPlayer player) {
        AbyssParties parties = AbyssParties.get(player.f_8924_);
        FloorProgress progress = parties.profile((UUID)player.m_20148_()).progress;
        ArrayList<FloorPreview> floors = new ArrayList<FloorPreview>();
        RewardPreviews rewards = new RewardPreviews(player.f_8924_);
        for (FloorDefinition definition : new TreeMap<Integer, FloorDefinition>(FloorCatalog.snapshot().floors()).values()) {
            List<Object> challenges;
            ArrayList<WavePreview> waves = new ArrayList<WavePreview>();
            LinkedHashSet<ResourceLocation> floorEnemies = new LinkedHashSet<ResourceLocation>();
            try {
                challenges = parties.challenges(definition.number());
            }
            catch (IllegalArgumentException ex) {
                challenges = List.of();
            }
            int[] stars = progress.waveStars(definition.number());
            for (int index = 0; index < definition.waves().size(); ++index) {
                LinkedHashMap counts = new LinkedHashMap();
                try {
                    parties.cycleLineup(definition.number(), index + 1, definition.waves().get(index)).forEach(enemy -> counts.merge(enemy.id(), enemy.count(), Integer::sum));
                }
                catch (IllegalArgumentException ex) {
                    LogUtils.getLogger().warn("Cannot preview Abyss floor {} wave {}: {}", new Object[]{definition.number(), index + 1, ex.getMessage()});
                }
                int wavePopulation = counts.values().stream().mapToInt(Integer::intValue).sum();
                floorEnemies.addAll(counts.keySet());
                List<MutableComponent> objectives = challenges.isEmpty() ? List.of(Component.m_237115_((String)"message.spiral_abyss.challenges_unavailable")) : challenges.stream().map(challenge -> challenge.description(wavePopulation)).toList();
                waves.add(new WavePreview(index + 1, stars[index], counts.entrySet().stream().map(entry -> new EnemyPreview((ResourceLocation)entry.getKey(), (Integer)entry.getValue(), AbyssRejection.isRejected(player, definition.number(), (ResourceLocation)entry.getKey()), MobCatalog.bossOptions((ResourceLocation)entry.getKey()).previewAnimation())).toList(), objectives));
            }
            floors.add(new FloorPreview(definition.number(), waves, rewards.floor(definition.rewards(), floorEnemies)));
        }
        return floors;
    }

    private static void writePreviews(FriendlyByteBuf buffer, List<FloorPreview> floors) {
        buffer.m_130130_(floors.size());
        for (FloorPreview floor : floors) {
            buffer.m_130130_(floor.floor());
            buffer.m_130130_(floor.waves().size());
            for (WavePreview wave : floor.waves()) {
                buffer.m_130130_(wave.wave());
                buffer.m_130130_(wave.stars());
                buffer.m_130130_(wave.challenges().size());
                wave.challenges().forEach(arg_0 -> ((FriendlyByteBuf)buffer).m_130083_(arg_0));
                buffer.m_130130_(wave.enemies().size());
                for (EnemyPreview enemy : wave.enemies()) {
                    buffer.m_130085_(enemy.id());
                    buffer.m_130130_(enemy.count());
                    buffer.writeBoolean(enemy.rejected());
                    AbyssNetwork.writeAnimation(buffer, enemy.animation());
                }
            }
            buffer.m_130130_(floor.rewards().size());
            for (RewardPreview reward : floor.rewards()) {
                buffer.m_130085_(reward.id());
                buffer.writeBoolean(reward.table());
                buffer.writeBoolean(reward.rare());
                buffer.writeBoolean(reward.incomplete());
                buffer.writeDouble(reward.displayChance());
                buffer.m_130130_(reward.contents().size());
                reward.contents().forEach(arg_0 -> ((FriendlyByteBuf)buffer).m_130085_(arg_0));
                if (reward.table()) continue;
                buffer.m_130055_(reward.icon());
            }
        }
    }

    private static void writeChallenges(FriendlyByteBuf buffer, List<ChallengeView> challenges) {
        buffer.m_130130_(challenges.size());
        for (ChallengeView challenge : challenges) {
            buffer.m_130083_(challenge.description());
            buffer.m_130130_(challenge.progress());
            buffer.m_130130_(challenge.target());
            buffer.writeBoolean(challenge.satisfied());
            buffer.writeBoolean(challenge.failed());
        }
    }

    private static void writeAnimation(FriendlyByteBuf buffer, BossOptions.PreviewAnimation animation) {
        buffer.m_130072_(animation.name(), 128);
        buffer.writeDouble(animation.timeSeconds());
    }

    private static BossOptions.PreviewAnimation readAnimation(FriendlyByteBuf buffer) {
        return new BossOptions.PreviewAnimation(buffer.m_130136_(128), buffer.readDouble());
    }

    private static List<ChallengeView> readChallenges(FriendlyByteBuf buffer) {
        ArrayList<ChallengeView> result = new ArrayList<ChallengeView>();
        for (int c = AbyssNetwork.bounded(buffer, 3); c > 0; --c) {
            result.add(new ChallengeView(buffer.m_130238_(), AbyssNetwork.bounded(buffer, 1000000), AbyssNetwork.bounded(buffer, 1000000), buffer.readBoolean(), buffer.readBoolean()));
        }
        return result;
    }

    private static List<ResourceLocation> readRejectedBosses(FriendlyByteBuf buffer) {
        ArrayList<ResourceLocation> result = new ArrayList<ResourceLocation>();
        for (int count = AbyssNetwork.bounded(buffer, 128); count > 0; --count) {
            result.add(buffer.m_130281_());
        }
        return result;
    }

    private static int bounded(FriendlyByteBuf buffer, int max) {
        int number = buffer.m_130242_();
        if (number < 0 || number > max) {
            throw new IllegalArgumentException("Invalid Abyss preview size");
        }
        return number;
    }

    private static List<FloorPreview> readPreviews(FriendlyByteBuf buffer) {
        ArrayList<FloorPreview> floors = new ArrayList<FloorPreview>();
        for (int f = AbyssNetwork.bounded(buffer, 6); f > 0; --f) {
            int floor = AbyssNetwork.bounded(buffer, 6);
            ArrayList<WavePreview> waves = new ArrayList<WavePreview>();
            for (int w = AbyssNetwork.bounded(buffer, 3); w > 0; --w) {
                int wave = AbyssNetwork.bounded(buffer, 3);
                int stars = AbyssNetwork.bounded(buffer, 3);
                ArrayList<Component> challenges = new ArrayList<Component>();
                for (int c = AbyssNetwork.bounded(buffer, 3); c > 0; --c) {
                    challenges.add(buffer.m_130238_());
                }
                ArrayList<EnemyPreview> enemies = new ArrayList<EnemyPreview>();
                for (int e = AbyssNetwork.bounded(buffer, 128); e > 0; --e) {
                    enemies.add(new EnemyPreview(buffer.m_130281_(), AbyssNetwork.bounded(buffer, 128), buffer.readBoolean(), AbyssNetwork.readAnimation(buffer)));
                }
                waves.add(new WavePreview(wave, stars, enemies, challenges));
            }
            ArrayList<RewardPreview> rewards = new ArrayList<RewardPreview>();
            for (int r = AbyssNetwork.bounded(buffer, 128); r > 0; --r) {
                ResourceLocation id = buffer.m_130281_();
                boolean table = buffer.readBoolean();
                boolean rare = buffer.readBoolean();
                boolean incomplete = buffer.readBoolean();
                double displayChance = buffer.readDouble();
                ArrayList<ResourceLocation> contents = new ArrayList<ResourceLocation>();
                for (int i = AbyssNetwork.bounded(buffer, 128); i > 0; --i) {
                    contents.add(buffer.m_130281_());
                }
                ItemStack icon = table ? ItemStack.f_41583_ : buffer.m_130267_();
                rewards.add(new RewardPreview(id, table, rare, contents, incomplete, icon, displayChance));
            }
            floors.add(new FloorPreview(floor, waves, rewards));
        }
        return floors;
    }

    public static void select(UUID token, int floor) {
        CHANNEL.sendToServer(new SelectFloor(token, floor));
    }

    public static void requestJournal() {
        CHANNEL.sendToServer(new RequestJournal());
    }

    public static void broadcastRareReward(Component player, ItemStack item) {
        CHANNEL.broadcast(new RareReward(player, item));
    }

    public static void keepFloorsOpen(UUID token) {
        CHANNEL.sendToServer(new KeepFloorsOpen(token));
    }

    public static void encounterStatus(ServerPlayer player, EncounterStatus status) {
        CHANNEL.sendToPlayer(player, status);
    }

    public static void closeFloors(ServerPlayer player) {
        CHANNEL.sendToPlayer(player, new CloseFloors());
    }

    public record SectionStatus(UUID token, List<SectionView> sections, int selectable) {
    }

    public record OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside, long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal, int endlessWave, List<SectionView> sections) {
        public OpenFloors {
            sections = List.copyOf(sections);
            floors = List.copyOf(floors);
            endlessWave = Math.max(1, endlessWave);
        }

        public OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside, long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal, int endlessWave) {
            this(token, claimed, unlocked, selectable, visible, inside, resetRemainingMillis, floors, entryPortal, endlessWave, List.of());
        }

        public OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside, long resetRemainingMillis, List<FloorPreview> floors, BlockPos entryPortal) {
            this(token, claimed, unlocked, selectable, visible, inside, resetRemainingMillis, floors, entryPortal, 1);
        }

        public OpenFloors(UUID token, int claimed, int unlocked, int selectable, int visible, boolean inside, long resetRemainingMillis, List<FloorPreview> floors) {
            this(token, claimed, unlocked, selectable, visible, inside, resetRemainingMillis, floors, null);
        }
    }

    public record SelectFloor(UUID token, int floor) {
    }

    public record EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total, int seconds, int countdownSeconds, List<ChallengeView> challenges, List<ResourceLocation> rejectedBosses) {
        public static final EncounterStatus HIDDEN = new EncounterStatus(false, false, 0, 0, 0, 0, 0, 0, List.of());

        public EncounterStatus {
            challenges = List.copyOf(challenges);
            rejectedBosses = List.copyOf(rejectedBosses);
        }

        public EncounterStatus(boolean active, boolean preparing, int floor, int wave, int defeated, int total, int seconds, int countdownSeconds, List<ChallengeView> challenges) {
            this(active, preparing, floor, wave, defeated, total, seconds, countdownSeconds, challenges, List.of());
        }
    }

    public record CloseFloors() {
    }

    public record KeepFloorsOpen(UUID token) {
    }

    public record RareReward(Component player, ItemStack item) {
        public RareReward {
            player = player.m_6881_();
            item = item.m_41777_();
        }
    }

    public record RequestJournal() {
    }

    public record OpenJournal(long resetRemainingMillis, long poseSeed, List<BannerBoss> bosses) {
        public OpenJournal {
            bosses = List.copyOf(bosses);
        }
    }

    public record SectionView(int floor, int status, List<SectionPlayer> players) {
        public SectionView {
            players = List.copyOf(players);
        }
    }

    public record SectionPlayer(UUID id, String name) {
    }

    public record BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards, BossOptions.PreviewAnimation animation) {
        public BannerBoss {
            rewards = List.copyOf(rewards);
        }

        public BannerBoss(int floor, int wave, ResourceLocation id, List<RewardPreview> rewards) {
            this(floor, wave, id, rewards, BossOptions.PreviewAnimation.DEFAULT);
        }
    }

    public record RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents, boolean incomplete, ItemStack icon, double displayChance) {
        public RewardPreview {
            contents = List.copyOf(contents);
            icon = icon.m_41777_();
            if (!Double.isFinite(displayChance) || displayChance < 0.0 || displayChance > 1.0) {
                throw new IllegalArgumentException("Invalid Abyss reward preview chance");
            }
        }

        public RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents, boolean incomplete, ItemStack icon) {
            this(id, table, rare, contents, incomplete, icon, rare ? 0.05 : 0.0);
        }

        public RewardPreview(ResourceLocation id, boolean table, boolean rare, List<ResourceLocation> contents, boolean incomplete) {
            this(id, table, rare, contents, incomplete, ItemStack.f_41583_);
        }
    }

    public record WavePreview(int wave, int stars, List<EnemyPreview> enemies, List<Component> challenges) {
        public WavePreview {
            enemies = List.copyOf(enemies);
            challenges = List.copyOf(challenges);
        }
    }

    public record FloorPreview(int floor, List<WavePreview> waves, List<RewardPreview> rewards) {
        public FloorPreview {
            waves = List.copyOf(waves);
            rewards = List.copyOf(rewards);
        }

        public FloorPreview(int floor, List<WavePreview> waves) {
            this(floor, waves, List.of());
        }
    }

    public record EnemyPreview(ResourceLocation id, int count, boolean rejected, BossOptions.PreviewAnimation animation) {
        public EnemyPreview(ResourceLocation id, int count, boolean rejected) {
            this(id, count, rejected, BossOptions.PreviewAnimation.DEFAULT);
        }

        public EnemyPreview(ResourceLocation id, int count) {
            this(id, count, false);
        }
    }

    public record ChallengeView(Component description, int progress, int target, boolean satisfied, boolean failed) {
    }
}

