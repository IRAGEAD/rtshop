package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.function.Function;

/**
 * RT Shop 창(상점·퀘스트·게시판·대화 등)의 글꼴. 설정: 고딕(Noto Sans KR) / 명조(Noto Serif KR) / 게임 기본 픽셀 글씨,
 * 크기 작게·보통·크게. 게임 기본 글꼴 자리에 고른 글꼴을 넣은 Font 를 만들어 RT Shop 창에만 쓴다
 * (다른 모드·바닐라 화면은 그대로).
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class UiFont {
    private UiFont() {
    }

    public static final String[] FACES = {"sans", "serif", "pixel"};
    private static Font cached;
    private static String cachedKey = "";
    private static Field fontsField, screenFontField;
    private static boolean broken;

    /** 고른 글꼴의 ID (픽셀이면 null) */
    public static ResourceLocation id() {
        ClientConfig c = ClientConfig.get();
        if ("pixel".equals(c.uiFont)) return null;
        String face = "serif".equals(c.uiFont) ? "serif" : "sans";
        String size = c.uiFontSize <= 0 ? "_s" : c.uiFontSize >= 2 ? "_l" : "";
        return new ResourceLocation(RtShop.MODID, "ui_" + face + size);
    }

    /** RT Shop 창에 쓸 Font. */
    @SuppressWarnings("unchecked")
    public static Font get() {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation id = id();
        if (id == null || broken || mc.font == null) return mc.font;
        String key = id + "@" + System.identityHashCode(mc.font);
        if (cached != null && key.equals(cachedKey)) return cached;
        try {
            if (fontsField == null) {
                for (Field f : Font.class.getDeclaredFields()) {
                    if (Function.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        fontsField = f;
                        break;
                    }
                }
            }
            Function<ResourceLocation, FontSet> base = (Function<ResourceLocation, FontSet>) fontsField.get(mc.font);
            cached = new Font(rl -> base.apply(Style.DEFAULT_FONT.equals(rl) ? id : rl), false);
            cachedKey = key;
            return cached;
        } catch (Exception ex) {
            broken = true;
            RtShop.LOGGER.warn("[rtshop] 창 글꼴을 만들지 못해 기본 글꼴을 씁니다: {}", ex.toString());
            return mc.font;
        }
    }

    /** 설정을 바꾸면 다시 만든다. */
    public static void changed() {
        cached = null;
        cachedKey = "";
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && ours(mc.screen)) {
            mc.screen.init(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }

    static boolean ours(Screen s) {
        return s.getClass().getName().startsWith("com.rtshop.client.");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInit(ScreenEvent.Init.Pre event) {
        Screen s = event.getScreen();
        if (!ours(s)) return;
        try {
            if (screenFontField == null) {
                for (Field f : Screen.class.getDeclaredFields()) {
                    if (f.getType() == Font.class) {
                        f.setAccessible(true);
                        screenFontField = f;
                        break;
                    }
                }
            }
            screenFontField.set(s, get());
        } catch (Exception ex) {
            RtShop.LOGGER.warn("[rtshop] 창 글꼴을 넣지 못했습니다: {}", ex.toString());
        }
    }
}
