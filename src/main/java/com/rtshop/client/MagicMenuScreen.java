package com.rtshop.client;

import com.google.gson.JsonObject;
import com.rtshop.magic.MagicMenu;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Inventory;

/**
 * 구체가 '메뉴'로 열렸을 때의 룬 제단 창 (Cinematic Interactions 가 있지만 장면을 꺼 둔 경우 등).
 * 모습과 동작은 MagicGachaScreen 그대로이고, 닫힐 때 서버의 메뉴도 닫는다.
 */
public class MagicMenuScreen extends MagicGachaScreen implements MenuAccess<MagicMenu> {
    private final MagicMenu menu;

    public MagicMenuScreen(MagicMenu menu, Inventory inv, Component title) {
        super(MagicClient.lastView != null ? MagicClient.lastView : new JsonObject());
        this.menu = menu;
    }

    @Override
    public MagicMenu getMenu() {
        return menu;
    }

    @Override
    public void removed() {
        super.removed();
        if (minecraft != null && minecraft.player != null && minecraft.player.containerMenu == menu) {
            minecraft.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
            minecraft.player.containerMenu = minecraft.player.inventoryMenu;
        }
    }
}
