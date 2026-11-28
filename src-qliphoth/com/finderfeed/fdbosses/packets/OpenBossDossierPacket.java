/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.Item
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerContextAssignable;
import com.finderfeed.fdbosses.content.entities.base.BossSpawnerEntity;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

@RegisterFDPacket(value="fdbosses:open_boss_dossier")
public class OpenBossDossierPacket
extends FDPacket {
    private static final NetworkCodec<List<Item>> ITEM_CODEC = NetworkCodec.listOf((NetworkCodec)NetworkCodec.registry(() -> ForgeRegistries.ITEMS));
    private List<Item> drops;
    private int bossSpawnerId;

    public OpenBossDossierPacket(FriendlyByteBuf buf) {
        this.drops = (List)ITEM_CODEC.fromNetwork(buf);
        this.bossSpawnerId = buf.readInt();
    }

    public OpenBossDossierPacket(BossSpawnerEntity bossSpawner) {
        EntityType<? extends BossSpawnerContextAssignable> bossEntityType = bossSpawner.getBossEntityType();
        this.bossSpawnerId = bossSpawner.m_19879_();
        LivingEntity fakeEntity = (LivingEntity)bossEntityType.m_20615_(bossSpawner.m_9236_());
        ResourceLocation lootTable = fakeEntity.m_5743_();
        MinecraftServer server = ((ServerLevel)bossSpawner.m_9236_()).m_7654_();
        List<Item> items = BossUtil.getItemsFromLootTable(server, lootTable);
        this.drops = items;
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        ITEM_CODEC.toNetwork(registryFriendlyByteBuf, this.drops);
        registryFriendlyByteBuf.writeInt(this.bossSpawnerId);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.openBossDossierScreen(this.bossSpawnerId, this.drops);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}

