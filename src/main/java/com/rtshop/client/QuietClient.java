package com.rtshop.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.rtshop.RtShop;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 3.84 (사용자 11-29): 화면에 저절로 뜨던 것 두 가지를 끈다. client.json 에서 끌 수 있다.
 * <ul>
 *   <li>"spiral abyss의 `단축키를 눌렀을 때 나오는 창 기능을 삭제" (noAbyssJournal): 그 모드의 일지 창(AbyssJournalScreen)을 열지 않고,
 *       그 키(key.spiral_abyss.menu, 기본 `)를 한 번 풀어 준다. 가방 창의 일지 단추는 3.78 부터 숨겨 두었다 (hideAbyssInventoryButton).</li>
 *   <li>"바닐라 마인크래프트의 튜토리얼(우상단)표시 표시되지 않게" (hideVanillaTutorial): 튜토리얼 단계를 끝난 것(NONE)으로 두고,
 *       처음 서버에 들어갈 때의 "P: 소셜 상호작용" 안내도 띄우지 않는다.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RtShop.MODID, value = Dist.CLIENT)
public final class QuietClient {
    private QuietClient() {
    }

    public static final String ABYSS_KEY = "key.spiral_abyss.menu";
    public static final String ABYSS_JOURNAL = "com.pla.spiral_abyss.client.screen.AbyssJournalScreen";
    private static boolean keyDone;
    /** 점검용: 키를 어떻게 했는가 · 막은 일지 창 수. */
    public static volatile String keyForTest = "";
    public static volatile int journalBlockedForTest;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        ClientConfig cc = ClientConfig.get();
        try {
            if (cc.hideVanillaTutorial) quietTutorial(mc);
        } catch (Throwable ignored) {
        }
        if (keyDone) return;
        keyDone = true;
        try {
            keyForTest = clearAbyssKey(mc, cc);
        } catch (Throwable t) {
            keyForTest = "error " + t;
        }
    }

    /** 튜토리얼을 끝난 것으로 둔다 (이미 그렇다면 아무것도 하지 않는다). */
    public static boolean quietTutorial(Minecraft mc) {
        boolean changed = false;
        if (mc.options.tutorialStep != TutorialSteps.NONE) {
            mc.getTutorial().setStep(TutorialSteps.NONE);   // 설정 파일에도 적는다
            changed = true;
        }
        if (!mc.options.joinedFirstServer) {
            mc.options.joinedFirstServer = true;   // 처음 서버에 들어갈 때의 "소셜 상호작용(P)" 안내
            mc.options.save();
            changed = true;
        }
        return changed;
    }

    /** Spiral Abyss 의 일지 키를 한 번만 푼다 (그 뒤에 다시 묶어도 창은 열리지 않는다). */
    static String clearAbyssKey(Minecraft mc, ClientConfig cc) {
        if (!cc.noAbyssJournal) return "kept";
        if (cc.abyssKeyCleared) return "already";
        for (KeyMapping km : mc.options.keyMappings) {
            if (!ABYSS_KEY.equals(km.getName())) continue;
            if (!km.isUnbound()) {
                km.setKey(InputConstants.UNKNOWN);
                KeyMapping.resetMapping();
                mc.options.save();
                RtShop.LOGGER.info("[rtshop] Spiral Abyss 의 일지 키(`)를 풀었습니다. 일지 창은 열리지 않습니다 (client.json 의 noAbyssJournal).");
            }
            cc.abyssKeyCleared = true;
            cc.save();
            return "unbound";
        }
        return "absent";
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onOpen(ScreenEvent.Opening event) {
        try {
            if (event.getNewScreen() != null && ClientConfig.get().noAbyssJournal && ABYSS_JOURNAL.equals(event.getNewScreen().getClass().getName())) {
                event.setCanceled(true);
                journalBlockedForTest++;
            }
        } catch (Throwable ignored) {
        }
    }
}
