package com.rtshop;

import com.mojang.logging.LogUtils;
import com.rtshop.net.ShopNet;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * RT Shop - 중세풍 NPC 상점 모드.
 * 이 모드는 믹스인/코어모드/기존 모드 API를 전혀 사용하지 않으며,
 * 자체 네임스페이스(rtshop)만 사용하므로 다른 모드와 충돌하지 않습니다.
 */
@Mod(RtShop.MODID)
public class RtShop {
    public static final String MODID = "rtshop";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RtShop() {
        ShopNet.register();
    }
}
