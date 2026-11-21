package com.rtshop.net;

import com.rtshop.RtShop;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** rtshop 전용 네트워크 채널 (다른 모드와 채널 이름이 겹치지 않음). */
public final class ShopNet {
    private ShopNet() {
    }

    private static final String PROTOCOL = "12";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RtShop.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenShopPacket.class, OpenShopPacket::encode, OpenShopPacket::decode, OpenShopPacket::handle);
        CHANNEL.registerMessage(id++, ResultPacket.class, ResultPacket::encode, ResultPacket::decode, ResultPacket::handle);
        CHANNEL.registerMessage(id++, BalancePacket.class, BalancePacket::encode, BalancePacket::decode, BalancePacket::handle);
        CHANNEL.registerMessage(id++, OpenBankPacket.class, OpenBankPacket::encode, OpenBankPacket::decode, OpenBankPacket::handle);
        CHANNEL.registerMessage(id++, BankPacket.class, BankPacket::encode, BankPacket::decode, BankPacket::handle);
        CHANNEL.registerMessage(id++, TransactionPacket.class, TransactionPacket::encode, TransactionPacket::decode, TransactionPacket::handle);
        CHANNEL.registerMessage(id++, ClosePacket.class, ClosePacket::encode, ClosePacket::decode, ClosePacket::handle);
        CHANNEL.registerMessage(id++, SellAllPacket.class, SellAllPacket::encode, SellAllPacket::decode, SellAllPacket::handle);
        CHANNEL.registerMessage(id++, OpenHudEditorPacket.class, OpenHudEditorPacket::encode, OpenHudEditorPacket::decode, OpenHudEditorPacket::handle);
        CHANNEL.registerMessage(id++, LandInfoPacket.class, LandInfoPacket::encode, LandInfoPacket::decode, LandInfoPacket::handle);
        CHANNEL.registerMessage(id++, LandActionPacket.class, LandActionPacket::encode, LandActionPacket::decode, LandActionPacket::handle);
        CHANNEL.registerMessage(id++, ContainerInfoPacket.class, ContainerInfoPacket::encode, ContainerInfoPacket::decode, ContainerInfoPacket::handle);
        CHANNEL.registerMessage(id++, PinPromptPacket.class, PinPromptPacket::encode, PinPromptPacket::decode, PinPromptPacket::handle);
        CHANNEL.registerMessage(id++, PinActionPacket.class, PinActionPacket::encode, PinActionPacket::decode, PinActionPacket::handle);
        CHANNEL.registerMessage(id++, QuestOpenPacket.class, QuestOpenPacket::encode, QuestOpenPacket::decode, QuestOpenPacket::handle);
        CHANNEL.registerMessage(id++, QuestActionPacket.class, QuestActionPacket::encode, QuestActionPacket::decode, QuestActionPacket::handle);
        CHANNEL.registerMessage(id++, QuestEditorPacket.class, QuestEditorPacket::encode, QuestEditorPacket::decode, QuestEditorPacket::handle);
        CHANNEL.registerMessage(id++, QuestEditActionPacket.class, QuestEditActionPacket::encode, QuestEditActionPacket::decode, QuestEditActionPacket::handle);
        CHANNEL.registerMessage(id++, JobOpenPacket.class, JobOpenPacket::encode, JobOpenPacket::decode, JobOpenPacket::handle);
        CHANNEL.registerMessage(id++, JobChoosePacket.class, JobChoosePacket::encode, JobChoosePacket::decode, JobChoosePacket::handle);
        CHANNEL.registerMessage(id++, JobSyncPacket.class, JobSyncPacket::encode, JobSyncPacket::decode, JobSyncPacket::handle);
        CHANNEL.registerMessage(id++, JobEditorPacket.class, JobEditorPacket::encode, JobEditorPacket::decode, JobEditorPacket::handle);
        CHANNEL.registerMessage(id++, JobEditSavePacket.class, JobEditSavePacket::encode, JobEditSavePacket::decode, JobEditSavePacket::handle);
        CHANNEL.registerMessage(id++, QuestHudPacket.class, QuestHudPacket::encode, QuestHudPacket::decode, QuestHudPacket::handle);
        CHANNEL.registerMessage(id++, AttendancePacket.class, AttendancePacket::encode, AttendancePacket::decode, AttendancePacket::handle);
        CHANNEL.registerMessage(id++, AttendanceActionPacket.class, AttendanceActionPacket::encode, AttendanceActionPacket::decode, AttendanceActionPacket::handle);
        CHANNEL.registerMessage(id++, GachaPacket.class, GachaPacket::encode, GachaPacket::decode, GachaPacket::handle);
        CHANNEL.registerMessage(id++, GachaActionPacket.class, GachaActionPacket::encode, GachaActionPacket::decode, GachaActionPacket::handle);
        CHANNEL.registerMessage(id++, EnhancePacket.class, EnhancePacket::encode, EnhancePacket::decode, EnhancePacket::handle);
    }
}
