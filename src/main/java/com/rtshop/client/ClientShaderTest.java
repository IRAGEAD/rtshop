package com.rtshop.client;

import com.rtshop.RtShop;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.rtshop.client.ClientSelfTest.check;
import static com.rtshop.client.ClientSelfTest.onServer;
import static com.rtshop.client.ClientSelfTest.shot;

/**
 * 셰이더를 켠 화면에서 찍는 짧은 점검 (3.64.2). JVM 옵션 -Drtshop.clienttest=true -Drtshop.clienttest.only=shader 일 때만 돈다
 * (CI 의 모드팩 전체 job 이 화면 점검 뒤에, 모드팩에 든 Photon 을 Oculus 로 켜고 한 번 더 클라이언트를 띄운다).
 * 사용자가 Photon 에서 본 문제를 사람 대신 그림으로 확인하려는 것: 강화 모루의 빛 · 흔적 표시 · 룬 제단의 구슬과 그 빛.
 * 결과는 rtshop-shadertest.txt, 사진은 screenshots/150_<팩>_*.png (팩 이름은 -Drtshop.shadertest.label, 기본 photon).
 * CI 의 소프트웨어 그리기(llvmpipe)는 계산 셰이더의 공유 메모리가 32KB 뿐이라 Photon 의 계산 패스 하나가 안 만들어진다:
 * CI 는 그 패스만 끈 Photon 을 먼저 써 보고, 그래도 켜지지 않으면 다른 셰이더 팩(같은 Oculus 경로)으로 찍는다.
 */
final class ClientShaderTest {
    private ClientShaderTest() {
    }

    interface Step {
        void run(Minecraft mc);
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static volatile BlockPos base;
    private static volatile BlockPos anvil;
    private static volatile BlockPos orb;
    private static long anvilDrawn, anvilAdd, anvilSh;
    private static long spotsBefore;
    /** 사진 이름과 결과에 쓰는 셰이더 팩 이름 (photon, complementary, bsl ...). */
    static final String LABEL = System.getProperty("rtshop.shadertest.label", "photon").replaceAll("[^a-z0-9]", "");

    static int count() {
        return STEPS.size();
    }

    /** k 번째 걸음. 목록이 끝나면 결과를 적고 (조금 기다렸다가) 끈다. */
    static void run(Minecraft mc, int k) {
        if (k < STEPS.size()) {
            STEPS.get(k).run(mc);
        } else if (k == STEPS.size()) {
            ClientSelfTest.finish(mc);
        }
    }

    private static void waitTicks(int n) {
        ClientSelfTest.wait = n;
    }

    /** 지금 셰이더가 켜져 있는가와 셰이더 묶음의 이름 (Oculus 의 공개 API 를 리플렉션으로). */
    static String shaderState() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            boolean inUse = (Boolean) api.getMethod("isShaderPackInUse").invoke(inst);
            String pack = "?";
            for (String cn : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
                try {
                    Object name = Class.forName(cn).getMethod("getCurrentPackName").invoke(null);
                    pack = String.valueOf(name);
                    break;
                } catch (Throwable ignored) {
                }
            }
            return (inUse ? "on " : "off ") + pack;
        } catch (Throwable t) {
            return "no shader mod (" + t.getClass().getSimpleName() + ")";
        }
    }

    private static void add(Step s) {
        STEPS.add(s);
    }

    static {
        // 세계가 뜨고 셰이더가 다 만들어질 때까지 (소프트웨어 그리기라 오래 걸린다)
        add(mc -> waitTicks(300));
        // 둘레를 평평하게 고른다: 9×9 돌 바닥, 그 위 5칸은 비움. 한낮 · 맑음 · 몬스터 없음 · 무적
        add(mc -> {
            onServer(mc, sp -> {
                sp.serverLevel().setDayTime(6000);
                sp.serverLevel().setWeatherParameters(6000, 0, false, false);
                sp.server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, sp.server);
                sp.server.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, sp.server);
                sp.getAbilities().invulnerable = true;
                sp.onUpdateAbilities();
                for (net.minecraft.world.entity.Entity e : sp.serverLevel().getEntities(sp, sp.getBoundingBox().inflate(48),
                        e -> e instanceof net.minecraft.world.entity.monster.Enemy)) {
                    e.discard();
                }
                BlockPos p = sp.blockPosition();
                for (BlockPos q : BlockPos.betweenClosed(p.offset(-5, -1, -5), p.offset(5, -1, 5))) {
                    sp.serverLevel().setBlockAndUpdate(q, Blocks.STONE.defaultBlockState());
                }
                for (BlockPos q : BlockPos.betweenClosed(p.offset(-5, 0, -5), p.offset(5, 5, 5))) {
                    if (!sp.serverLevel().getBlockState(q).isAir()) sp.serverLevel().setBlockAndUpdate(q, Blocks.AIR.defaultBlockState());
                }
                base = p.immutable();
                sp.getInventory().selected = 8;
                sp.getInventory().setItem(8, net.minecraft.world.item.ItemStack.EMPTY);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                sp.connection.teleport(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0F, 0F);
            });
            mc.setScreen(null);
            waitTicks(80);
        });
        // 셰이더가 정말 켜졌는가 (꺼져 있으면 아래 사진들은 셰이더 없는 모습이다)
        add(mc -> {
            String st = shaderState();
            RtShop.LOGGER.info("[rtshop-clienttest] shader state: {}", st);
            check("the shader pack is on for these pictures (Oculus with the pack's " + LABEL + ")", st.startsWith("on "), st);
            // 강화 모루: 남쪽(앞) 세 칸, 내려다본다
            onServer(mc, sp -> {
                if (base == null) return;
                anvil = base.offset(0, 0, 3);
                sp.serverLevel().setBlockAndUpdate(anvil, com.rtshop.enhance.ModEnhance.ANVIL.get().defaultBlockState());
                sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 33F);
            });
            anvilDrawn = EnhanceAnvilGlow.drawnForTest;
            anvilAdd = EnhanceAnvilGlow.addDrawnForTest;
            anvilSh = EnhanceAnvilGlow.shaderDrawnForTest;
            waitTicks(120);
        });
        add(mc -> {
            float lv = anvil == null ? -1F : EnhanceAnvilGlow.levelForTest(anvil);
            check("with the shader on, the enhance anvil's runes and gem light up when someone stands near it, and the glow is really drawn",
                    lv > 0.95F && EnhanceAnvilGlow.drawnForTest > anvilDrawn, lv + " drawn=" + (EnhanceAnvilGlow.drawnForTest - anvilDrawn));
            shot(mc, "150_" + LABEL + "_anvil");
            // 3.75 (W-16): 셰이더에서는 한 겹(룬 · 보석의 획)만 — 스스로 빛나는 불투명 겹은 까맣게 켜지고 툭 꺼지며 크게 번졌다
            check("3.75 with the shader on, the anvil glow is one see-through layer only (the opaque self-glowing layer that popped and bled is not drawn)",
                    EnhanceAnvilGlow.shaderDrawnForTest > anvilSh && EnhanceAnvilGlow.addDrawnForTest == anvilAdd,
                    "sh=" + (EnhanceAnvilGlow.shaderDrawnForTest - anvilSh) + " add=" + (EnhanceAnvilGlow.addDrawnForTest - anvilAdd));
            EnhanceAnvilGlow.holdForTest = 0.2F;
            waitTicks(40);
        });
        // 켜지는 도중의 모습 (밝기를 묶어 두고 찍는다): 20% · 45% · 70% — 매끈하게 밝아지는지 그림으로 본다
        add(mc -> {
            shot(mc, "150_" + LABEL + "_anvil_l20");
            EnhanceAnvilGlow.holdForTest = 0.45F;
            waitTicks(40);
        });
        add(mc -> {
            shot(mc, "150_" + LABEL + "_anvil_l45");
            EnhanceAnvilGlow.holdForTest = 0.7F;
            waitTicks(40);
        });
        add(mc -> {
            shot(mc, "150_" + LABEL + "_anvil_l70");
            EnhanceAnvilGlow.holdForTest = null;
            // 비스듬히 (룬 띠와 모루 옆면의 보석이 함께 보이게)
            onServer(mc, sp -> {
                if (base == null) return;
                sp.connection.teleport(base.getX() + 2.5, base.getY(), base.getZ() + 1.5, 45F, 24F);
            });
            waitTicks(60);
        });
        add(mc -> {
            shot(mc, "150_" + LABEL + "_anvil_side");
            waitTicks(20);
        });
        // 흔적 표시 (이야기의 "살펴보기"): 앞 세 칸 바닥
        add(mc -> {
            onServer(mc, sp -> {
                if (anvil != null) sp.serverLevel().setBlockAndUpdate(anvil, Blocks.AIR.defaultBlockState());
                if (base == null) return;
                com.rtshop.quest.QuestDef q = com.rtshop.quest.QuestDef.parse(com.google.gson.JsonParser.parseString(
                        "{\"id\":\"t_shader_spot\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"INSPECT\",\"target\":\"t_shader_circle\",\"shape\":\"circle\","
                                + "\"text\":\"그을린 마법진\",\"lines\":[\"바닥에 원이 그을려 있다.\"]}]}").getAsJsonObject());
                com.rtshop.quest.QuestManager.loaded().quests.put(q.id, q);
                com.rtshop.quest.QuestManager.accept(sp, "t_shader_spot", null);
                com.rtshop.story.Spots.placeForTest(sp, "t_shader_circle", base.offset(0, 0, 3));
                com.rtshop.story.Spots.update(sp);
                sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 18F);
            });
            spotsBefore = SpotClient.countForTest();
            waitTicks(80);
        });
        add(mc -> {
            check("with the shader on, the trace to look at is listed on the screen side", SpotClient.countForTest() >= 1, SpotClient.countForTest());
            shot(mc, "151_" + LABEL + "_spot");
            onServer(mc, sp -> {
                com.rtshop.quest.QuestManager.abandon(sp, "t_shader_spot");
                com.rtshop.quest.QuestManager.loaded().quests.remove("t_shader_spot");
            });
            waitTicks(30);
        });
        // 룬 제단(별빛 수정 구체): 받침 위에 가만히 있는 모습 → 장면 → 분야를 고른 모습
        add(mc -> {
            onServer(mc, sp -> {
                if (base == null) return;
                orb = base.offset(0, 0, 2);
                sp.serverLevel().setBlockAndUpdate(orb, com.rtshop.magic.MagicRegistry.ORB.get().defaultBlockState());
                sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get(), 5));
                // 마을의 제단은 하나뿐이다: 앞의 화면 점검에서 놓은 제단 대신 이 제단을 마을의 제단으로 (안 그러면 열기를 거절한다)
                com.rtshop.rules.WorldRules.setAltar(sp.serverLevel(), orb);
                sp.getInventory().selected = 8;
                sp.getInventory().setItem(8, net.minecraft.world.item.ItemStack.EMPTY);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                sp.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 0F, 6F);
            });
            waitTicks(80);
        });
        add(mc -> {
            check("with the shader on, the orb on its pedestal is drawn (orb, rim glow and rings) with the extra glowing layer the shader needs",
                    ArcaneOrbRenderer.orbDrawnForTest > 0 && ArcaneOrbRenderer.coreGlowForTest > 0,
                    ArcaneOrbRenderer.orbDrawnForTest + " core=" + ArcaneOrbRenderer.coreGlowForTest);
            shot(mc, "152_" + LABEL + "_orb");
            if (orb != null) {
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(orb).add(0, 0.5, 0), net.minecraft.core.Direction.UP, orb, false));
            }
            waitTicks(40);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            RtShop.LOGGER.info("[rtshop-clienttest] altar scene after the click: {}", CinematicOrb.phaseForTest());
            shot(mc, "153_" + LABEL + "_orb_scene_intro");
            waitTicks(40);
        });
        // 셰이더를 켠 소프트웨어 그리기는 아주 느리다: 장면이 펼쳐질 때까지(ACTIVE) 조금씩 기다린다
        for (int i = 0; i < 8; i++) {
            add(mc -> {
                if (!com.rtshop.magic.MagicRegistry.cinematic) return;
                String ph = CinematicOrb.phaseForTest();
                RtShop.LOGGER.info("[rtshop-clienttest] altar scene: {}", ph);
                waitTicks("ACTIVE".equals(ph) || "NONE".equals(ph) ? 1 : 40);
            });
        }
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("with the shader on, the rune altar scene draws the orb with its glow kept close around it",
                    st != null && ArcaneOrbRenderer.sceneOrbDrawnForTest > 0 && CinematicOrbStation.sceneRimForTest < CinematicOrbStation.ringForTest() * 0.65,
                    CinematicOrb.phaseForTest() + " rim=" + CinematicOrbStation.sceneRimForTest);
            shot(mc, "154_" + LABEL + "_orb_scene");
            if (st != null) st.chooseForTest(0);
            waitTicks(20);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            shot(mc, "155_" + LABEL + "_orb_chosen");
            waitTicks(60);   // 사진이 다 써질 때까지
        });
    }
}
