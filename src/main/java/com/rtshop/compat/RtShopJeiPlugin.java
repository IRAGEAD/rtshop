package com.rtshop.compat;

import com.rtshop.RtShop;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI 연동: RT Shop 의 블록(강화 모루·룬 제단)을 쓰는 창에서는 JEI 의 아이템 목록·북마크가 뜨지 않게 한다.
 * JEI 는 '창 정보'를 돌려주지 않는 창에는 아무것도 그리지 않는다. JEI 가 없으면 이 클래스는 불리지 않는다.
 */
@JeiPlugin
public class RtShopJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(RtShop.MODID, "no_overlay");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(com.rtshop.client.EnhanceScreen.class, screen -> null);
        registration.addGuiScreenHandler(com.rtshop.client.MagicMenuScreen.class, screen -> null);
    }
}
