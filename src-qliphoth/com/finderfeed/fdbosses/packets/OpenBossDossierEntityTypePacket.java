/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.network.FDPacket
 *  com.finderfeed.fdlib.network.RegisterFDPacket
 *  com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.packets;

import com.finderfeed.fdbosses.BossClientPackets;
import com.finderfeed.fdbosses.BossUtil;
import com.finderfeed.fdlib.network.FDPacket;
import com.finderfeed.fdlib.network.RegisterFDPacket;
import com.finderfeed.fdlib.systems.stream_codecs.NetworkCodec;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

@RegisterFDPacket(value="fdbosses:open_boss_dossier_entity_type")
public class OpenBossDossierEntityTypePacket
extends FDPacket {
    private static final NetworkCodec<List<Item>> ITEM_CODEC = NetworkCodec.listOf((NetworkCodec)NetworkCodec.registry(() -> ForgeRegistries.ITEMS));
    private List<Item> drops;
    private EntityType<?> entityType;

    public OpenBossDossierEntityTypePacket(FriendlyByteBuf buf) {
        this.drops = (List)ITEM_CODEC.fromNetwork(buf);
        this.entityType = (EntityType)BuiltInRegistries.f_256780_.m_7745_(buf.m_130281_());
    }

    public OpenBossDossierEntityTypePacket(Level level, EntityType<?> bossEntityType) {
        LivingEntity fakeEntity = (LivingEntity)bossEntityType.m_20615_(level);
        ResourceLocation lootTable = fakeEntity.m_5743_();
        MinecraftServer server = ((ServerLevel)level).m_7654_();
        List<Item> items = BossUtil.getItemsFromLootTable(server, lootTable);
        this.entityType = bossEntityType;
        this.drops = items;
    }

    public void write(FriendlyByteBuf registryFriendlyByteBuf) {
        ITEM_CODEC.toNetwork(registryFriendlyByteBuf, this.drops);
        ResourceLocation key = BuiltInRegistries.f_256780_.m_7981_(this.entityType);
        registryFriendlyByteBuf.m_130085_(key);
    }

    public void clientAction(Supplier<NetworkEvent.Context> supplier) {
        BossClientPackets.openBossDossierScreenEntityType(this.entityType, this.drops);
    }

    public void serverAction(Supplier<NetworkEvent.Context> supplier) {
    }
}

