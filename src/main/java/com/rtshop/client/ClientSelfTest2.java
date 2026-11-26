package com.rtshop.client;

import com.rtshop.enhance.Enhance;
import com.rtshop.land.LandData;
import com.rtshop.land.LandManager;
import com.rtshop.shop.ShopServer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static com.rtshop.client.ClientSelfTest.check;
import static com.rtshop.client.ClientSelfTest.onServer;
import static com.rtshop.client.ClientSelfTest.shot;

/**
 * 화면 자체 점검의 뒷부분 (3.19 부터 덧붙인 것들). 한 단계 = 목록의 한 칸이고, 각 단계가 다음 단계까지 기다릴 틱 수를 정한다.
 * ClientSelfTest 가 순서대로 부른다.
 */
final class ClientSelfTest2 {
    /** 읽기 화면 점검에서 잠깐 끝낸 것으로 적어 두는 퀘스트 (점검이 끝나면 되돌린다). */
    private static final String[] READ_QUESTS = {"story_1_04", "story_1_30", "story_2_25", "story_3_33", "story_3_36"};
    private static final java.util.Map<String, Boolean> READ_HAD = new java.util.HashMap<>();

    private ClientSelfTest2() {
    }

    interface Step {
        void run(Minecraft mc);
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static final java.util.List<Integer> LOOK_IDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static volatile long LOOK_DRAWN;
    private static volatile int npc = -1;
    static volatile int qnpc = -1;
    static volatile net.minecraft.core.BlockPos orbPos;
    static volatile int magicSkills;
    static volatile double[] magicFail;
    static volatile String magicBefore = "";
    static volatile int prevSel;
    static volatile ItemStack prevSlot8 = ItemStack.EMPTY;
    static final java.util.List<ItemStack> savedInv = new java.util.ArrayList<>();
    private static volatile int landId = -1;
    private static volatile int farmA = -1, farmB = -1;
    private static volatile int dishIdx = -1;
    private static volatile String lifeJob = "";
    static volatile String permResult = "";
    static volatile String stackResult = "";
    static volatile ItemStack stackKeep = ItemStack.EMPTY;

    static int count() {
        return STEPS.size();
    }

    static void run(Minecraft mc, int k) {
        STEPS.get(k).run(mc);
    }

    private static int MAP_FIRST = -9;

    private static net.minecraft.world.entity.Entity RADAR_NPC;

    private static void waitTicks(int n) {
        ClientSelfTest.wait = n;
    }

    private static void add(Step s) {
        STEPS.add(s);
    }

    /** 칸 모양으로 구획을 하나 만든다 (청크를 불러오지 않는다). */
    private static LandData.Land plot(net.minecraft.server.level.ServerPlayer sp, String name, long price, int x0, int z0, int w, int h,
                                      java.util.function.BiPredicate<Integer, Integer> in) {
        LandData data = LandData.get(sp.server);
        LandData.Claim c = new LandData.Claim();
        c.dim = LandManager.dim(sp.level());
        c.type = "plot";
        c.x0 = x0;
        c.z0 = z0;
        c.x1 = x0 + w - 1;
        c.z1 = z0 + h - 1;
        c.y0 = sp.level().getMinBuildHeight();
        c.y1 = sp.level().getMaxBuildHeight() - 1;
        BitSet m = new BitSet(w * h);
        for (int z = 0; z < h; z++) {
            for (int x = 0; x < w; x++) {
                if (in.test(x, z)) m.set(z * w + x);
            }
        }
        c.mask = m;
        LandData.Land l = data.createPlot(name, price);
        data.addClaim(l, c);
        return l;
    }

    /** 시험용 농경지 모양 (PlotBrowser.packMask 형식): 아모노스 맵 동쪽의 실제 밀밭 두 곳. */
    private static final String FIELD_A = "eNpl0sENgzAMhWFXHOihEhNUHSWjhdEySkbgmAOKC7b/iNKePsUP24GKiDxfYr9Vs6OoOqrqbNg0ak2jduIRJQ8dYV1AopQJKyf2WAHSwX5gIjTRaPbNovWJD6U8OjIj08fGa4Rt1kJ2joiN6LHG2ddgWZU7csz2CQP9esFx5RTv8IZ6DY9NfxqOERVsrNFABxoowLPHLRroQP9O2r1UQQMd6N8JkIEd8DjrZKmgXDA1x/u8dJKS7WoH/EMlWf2dJf6bH4d0sINtCVSwDswBmb5vFYrw";
    private static final String FIELD_B = "eNpl0rERAiEQBVAYggvpQFowNaIUS4HAPmyFDmzhSrhMZuRYT9j/dcaLXrCf5dg1Jht8K7VRzUKdEqiIQ5SqVJMFAfETmVpFovYSSQiIHt1FC4+AqlDbR3H2Eo30oTB6qcqUH71UleqUQIXSwHH/SjWVQ0BshhhIDEQGAgOBgYCAxI1qVIfugDyoJ/Wi9n8lHhd4nGMZ73SBribzXyELJQPF34cteBLI4rET5jRmUlGmwrDn4BvKpjzlKK7H3JkTyswit32ujI3mrEvmub1fuTeVp7Qz";

    private record Frame(int target, Enhance.Outcome outcome, long at, String shot) {
    }

    private static final Frame[] FRAMES = {
            new Frame(7, Enhance.Outcome.SUCCESS, 1480, "84d_fx1_forge_strike"),
            new Frame(7, Enhance.Outcome.SUCCESS, 2920, "84e_fx1_forge_hold"),
            new Frame(8, Enhance.Outcome.SUCCESS, 3000 + 520, "84f_fx1_success"),
            new Frame(8, Enhance.Outcome.MAINTAIN, 3000 + 300, "84g_fx1_fail"),
            new Frame(9, Enhance.Outcome.SUCCESS, 1000, "84h_fx2_rune_drawing"),
            new Frame(9, Enhance.Outcome.SUCCESS, 3100, "84i_fx2_rune_sealed"),
            new Frame(9, Enhance.Outcome.SUCCESS, 4000, "84j_fx2_blackout"),
            new Frame(10, Enhance.Outcome.SUCCESS, 4200 + 650, "84k_fx2_success"),
            new Frame(10, Enhance.Outcome.DAMAGE, 4200 + 420, "84l_fx2_shatter"),
            new Frame(11, Enhance.Outcome.SUCCESS, 500, "84m_fx3_closing"),
            new Frame(11, Enhance.Outcome.SUCCESS, 3000, "84n_fx3_stars"),
            new Frame(11, Enhance.Outcome.SUCCESS, 5700, "84o_fx3_eleventh_flicker"),
            new Frame(11, Enhance.Outcome.SUCCESS, 6700, "84p_fx3_collapse"),
            new Frame(11, Enhance.Outcome.SUCCESS, 7600, "84q_fx3_silence"),
            new Frame(11, Enhance.Outcome.SUCCESS, 8200 + 350, "84r_fx3_flash"),
            new Frame(11, Enhance.Outcome.SUCCESS, 8200 + 2600, "84s_fx3_success"),
            new Frame(11, Enhance.Outcome.DESTROY, 8200 + 420, "84t_fx3_destroy"),
            new Frame(11, Enhance.Outcome.MAINTAIN, 8200 + 700, "84u_fx3_maintain"),
    };

    static {
        // ------------------------------------------------------------ 퀘스트 창 → 상인 화면으로 돌아가기
        add(mc -> {
            mc.setScreen(null);
            npc = -1;
            onServer(mc, sp -> {
                net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                var v = ShopServer.spawnNpc(sp.serverLevel(), sp.position().add(look.x * 2, 0, look.z * 2), sp.getYRot() + 180, "farmer");
                npc = v == null ? -2 : v.getId();
            });
            waitTicks(20);
        });
        // 대화 먼저: 우클릭하면 대화 상자가 열리고, [상점] 선택지로 상점에 간다. 일상 이야기는 누를 때마다 바뀐다.
        add(mc -> {
            net.minecraft.world.entity.Entity e = npc >= 0 ? mc.level.getEntity(npc) : null;
            DialogueScreen.autoMainForTest = false;
            if (e != null) mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
            waitTicks(25);
        });
        add(mc -> {
            DialogueScreen ds = mc.screen instanceof DialogueScreen x ? x : null;
            check("right-clicking a merchant opens the dialogue box first, with the shop as a choice", ds != null && ds.hasMainForTest(), mc.screen);
            if (ds != null) {
                ds.skipForTest();
                ds.chatterForTest();
                String first = ds.lastChatForTest();
                ds.chatterForTest();
                String second = ds.lastChatForTest();
                check("small talk changes every time you click it", !first.isEmpty() && !second.isEmpty() && !first.equals(second), first + " / " + second);
                ds.skipForTest();
                shot(mc, "114c_dialogue_first");
                ds.chooseMainForTest();
            }
            waitTicks(25);
        });
        add(mc -> {
            check("choosing [Shop] in the dialogue opens the merchant's shop", mc.screen instanceof ShopScreen, mc.screen);
            mc.setScreen(null);
            DialogueScreen.autoMainForTest = true;
            waitTicks(5);
        });
        add(mc -> {
            net.minecraft.world.entity.Entity e = npc >= 0 ? mc.level.getEntity(npc) : null;
            check("the farmer NPC for the back-button test is there", e != null, npc);
            if (e != null) mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
            waitTicks(25);
        });
        add(mc -> {
            check("the farmer's shop opens with the [Quests] button", mc.screen instanceof ShopScreen && NpcButtons.questButtonForTest(mc.screen), mc.screen);
            if (mc.screen != null) NpcButtons.pressQuestForTest(mc.screen);
            waitTicks(25);
        });
        add(mc -> {
            boolean ok = mc.screen instanceof QuestTalkScreen qs && qs.hasBackForTest();
            check("the quest talk opened from a merchant has a [Back] choice", ok, mc.screen);
            shot(mc, "95_quest_back_button");
            if (mc.screen instanceof QuestTalkScreen qs && qs.hasBackForTest()) qs.goBack();
            waitTicks(25);
        });
        add(mc -> {
            check("[Back] returns to the merchant's shop window", mc.screen instanceof ShopScreen ss && "farmer".equals(ss.shopIdForEdit()), mc.screen);
            check("...and the [Quests] button is there again", mc.screen != null && NpcButtons.questButtonForTest(mc.screen));
            shot(mc, "95b_quest_back_to_shop");
            mc.setScreen(null);
            int id = npc;
            onServer(mc, sp -> {
                var e = sp.serverLevel().getEntity(id);
                if (e != null) e.discard();
            });
            waitTicks(10);
        });

        // ------------------------------------------------------------ 강화 연출 3종: 정해진 시각에서 멈춘 장면
        for (int i = 0; i <= FRAMES.length; i++) {
            final int idx = i;
            add(mc -> {
                if (idx > 0) {
                    Frame prev = FRAMES[idx - 1];
                    check("enhance scene frame renders: " + prev.shot, EnhanceGrandFx.running(), EnhanceGrandFx.last);
                    shot(mc, prev.shot);
                }
                if (idx < FRAMES.length) {
                    Frame f = FRAMES[idx];
                    ItemStack it = ClientSelfTest.leveled(Items.NETHERITE_SWORD, f.target - 1);
                    EnhanceGrandFx.freezeForTest(f.target, f.outcome, f.at, it);
                } else {
                    EnhanceGrandFx.stopForTest();
                }
                waitTicks(4);
            });
        }
        add(mc -> {
            check("each level band has its own scene (7-8 / 9-10 / 11)", Enhance.fxTier(7, 7) == 1 && Enhance.fxTier(8, 7) == 1 && Enhance.fxTier(9, 7) == 2
                    && Enhance.fxTier(10, 7) == 2 && Enhance.fxTier(11, 7) == 3 && Enhance.fxTier(6, 7) == 0);
            EnhanceFx.legend("Merchant_Kim", 11, ClientSelfTest.leveled(Items.NETHERITE_SWORD, 11));
            waitTicks(30);
        });
        add(mc -> {
            check("a +11 success by someone else shows a banner at the top of the screen", EnhanceFx.active() && EnhanceFx.lastLegend.equals("Merchant_Kim:11"),
                    EnhanceFx.lastLegend);
            shot(mc, "84v_fx3_server_banner");
            waitTicks(5);
        });

        // ------------------------------------------------------------ 토지 경계: 스스로 켰을 때만, 땅을 따라가는 낮은 테두리
        add(mc -> {
            landId = -1;
            onServer(mc, sp -> {
                int px = sp.getBlockX(), pz = sp.getBlockZ();
                // 둥근 밭에 한쪽 귀퉁이가 파인 모양
                LandData.Land l = plot(sp, "test wheat field", 30000, px - 9, pz - 9, 19, 19,
                        (x, z) -> (x - 9) * (x - 9) + (z - 9) * (z - 9) <= 81 && !(x > 12 && z > 12));
                landId = l.id;
            });
            waitTicks(40);
        });
        add(mc -> {
            check("standing in a lot for sale does not force the border display on", !BorderRenderer.activeForTest(), BorderRenderer.segmentsForTest());
            mc.player.connection.sendCommand("rtshop border on");
            waitTicks(30);
        });
        add(mc -> {
            check("/rtshop border on shows the border of the lot you stand in", BorderRenderer.activeForTest() && BorderRenderer.segmentsForTest() > 20,
                    BorderRenderer.segmentsForTest());
            // 시험용 토지는 무작위 지형에 놓인다: 발밑에서 잰 높이는 언덕만큼 커지므로(6.7 ~ 13.7), 기둥이 "그 자리의 땅" 위로 얼마나 솟는지로 본다
            check("the border is a low outline along the ground (no wall into the sky)",
                    BorderRenderer.cornersForTest() > 0 && BorderRenderer.maxRiseAboveGroundForTest() <= 2.0,
                    "above the ground " + BorderRenderer.maxRiseAboveGroundForTest() + " / above the feet " + BorderRenderer.maxRiseForTest()
                            + " / corners " + BorderRenderer.cornersForTest());
            mc.player.setXRot(28);
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "96_border_low_outline");
            mc.player.setXRot(-75);
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "96b_border_sky_is_clean");
            mc.player.setXRot(10);
            mc.player.connection.sendCommand("border");
            waitTicks(50);
        });
        add(mc -> {
            check("/border (again) turns the border display off", !BorderRenderer.activeForTest());
            int id = landId;
            onServer(mc, sp -> {
                LandData data = LandData.get(sp.server);
                LandData.Land l = data.land(id);
                if (l != null) data.removeLand(l);
            });
            waitTicks(10);
        });

        // ------------------------------------------------------------ 토지 상인 지도: 갈래별로만, 농경지는 실제 모양으로
        add(mc -> {
            farmA = farmB = -1;
            onServer(mc, sp -> {
                // 아모노스 맵의 실제 밀밭 두 곳을 구획 지팡이의 '밭 채우기'로 골랐을 때의 모양 그대로 (밭 + 가운데 빈칸 메우기 + 풀 테두리 한 줄)
                java.util.BitSet ma = com.rtshop.land.PlotBrowser.unpackMask(FIELD_A);
                java.util.BitSet mb = com.rtshop.land.PlotBrowser.unpackMask(FIELD_B);
                farmA = plot(sp, "밀밭 A", 125080, -24176, -5398, 72, 83, (x, z) -> ma.get(z * 72 + x)).id;
                farmB = plot(sp, "밀밭 B", 141240, -24148, -5292, 79, 76, (x, z) -> mb.get(z * 79 + x)).id;
                net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                var v = ShopServer.spawnNpc(sp.serverLevel(), sp.position().add(look.x * 2, 0, look.z * 2), sp.getYRot() + 180, "land");
                npc = v == null ? -2 : v.getId();
                com.rtshop.land.PlotBrowser.handle(sp, com.rtshop.land.PlotBrowser.OPEN, 0);
            });
            waitTicks(30);
        });
        add(mc -> {
            PlotBrowserScreen ps = mc.screen instanceof PlotBrowserScreen s ? s : null;
            check("the lot window lists the farmland lots under their own tab", ps != null && ps.countForTest("plot") >= 2, ps == null ? mc.screen : ps.countForTest("plot"));
            check("farmland lots arrive with their real shape (not a square)", PlotBrowserScreen.shapesForTest() >= 2, PlotBrowserScreen.shapesForTest());
            int[] sa = PlotBrowserScreen.shapeForTest(farmA), sb = PlotBrowserScreen.shapeForTest(farmB);
            check("the map outline is the lot's own cells: a real field keeps its winding border (every cell, many outline pieces)",
                    sa != null && sb != null && sa[0] == 3127 && sb[0] == 3531 && sa[1] > 60 && sb[1] > 60,
                    sa == null || sb == null ? null : sa[0] + "/" + sa[1] + " " + sb[0] + "/" + sb[1]);
            if (ps != null) {
                ps.showForTest(1, 0, -1);
                ps.zoomForTest(0.45);
            }
            waitTicks(10);
        });
        add(mc -> {
            shot(mc, "97_plots_farmland_shapes");
            PlotBrowserScreen ps = mc.screen instanceof PlotBrowserScreen s ? s : null;
            if (ps != null) ps.showForTest(0, 0, -1);
            waitTicks(10);
        });
        add(mc -> {
            shot(mc, "97b_plots_castle_tab_only");
            mc.setScreen(null);
            int a = farmA, b = farmB, nid = npc;
            onServer(mc, sp -> {
                var ne = sp.serverLevel().getEntity(nid);
                if (ne != null) ne.discard();
                LandData data = LandData.get(sp.server);
                for (int id : new int[]{a, b}) {
                    LandData.Land l = data.land(id);
                    if (l != null) data.removeLand(l);
                }
            });
            waitTicks(10);
        });

        // ------------------------------------------------------------ PvP 표시 (HUD) 와 HUD 편집 화면
        add(mc -> {
            mc.setScreen(null);
            onServer(mc, sp -> {
                com.rtshop.pvp.PvpConfig.get().outside = true; // 저장하지 않는다 (끝에서 파일을 다시 읽는다)
                com.rtshop.pvp.Pvp.forgetForTest(sp);
                com.rtshop.pvp.Pvp.sync(sp);
                com.rtshop.pvp.Pvp.set(sp, true, false);
            });
            waitTicks(20);
        });
        add(mc -> {
            check("the PvP indicator shows ON after /pvp on in a place where PvP is allowed",
                    PvpHud.systemOn && PvpHud.on && PvpHud.area == com.rtshop.pvp.Pvp.AREA_WILD, PvpHud.on + " " + PvpHud.area);
            shot(mc, "98_pvp_hud_on");
            onServer(mc, sp -> {
                com.rtshop.pvp.PvpConfig.get().outside = false;
                com.rtshop.pvp.Pvp.tick(sp); // PvP 를 할 수 없는 곳이 되었으므로 저절로 꺼진다
            });
            waitTicks(20);
        });
        add(mc -> {
            check("the PvP indicator turns OFF by itself where PvP is not allowed",
                    PvpHud.systemOn && !PvpHud.on && PvpHud.area == com.rtshop.pvp.Pvp.AREA_OFF, PvpHud.on + " " + PvpHud.area);
            shot(mc, "98b_pvp_hud_off");
            HudEditScreen ed = new HudEditScreen();
            mc.setScreen(ed);
            ed.select(HudEditScreen.PVP);
            waitTicks(10);
        });
        add(mc -> {
            check("the HUD editor can select the PvP indicator (third HUD)", mc.screen instanceof HudEditScreen ed && ed.selectedForTest() == HudEditScreen.PVP, mc.screen);
            int[] b = PvpHud.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            int[] gb = GoldHud.bounds(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            check("by default the PvP indicator sits just above the Gold box without overlapping it", b[1] + b[3] <= gb[1] && b[0] == gb[0], b[1] + "+" + b[3] + " vs " + gb[1]);
            shot(mc, "98c_hud_editor_pvp");
            mc.setScreen(null);
            onServer(mc, sp -> {
                com.rtshop.pvp.PvpConfig.reload();
                com.rtshop.pvp.Pvp.forgetForTest(sp);
                com.rtshop.pvp.Pvp.sync(sp);
            });
            waitTicks(10);
        });

        // ------------------------------------------------------------ 식당 주인: 빈 메뉴 → 요리사가 팔면 메뉴에 오른다
        add(mc -> {
            mc.setScreen(null);
            onServer(mc, sp -> {
                com.rtshop.shop.Restaurant.reset(sp.server);
                com.rtshop.shop.ShopData rest = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
                dishIdx = -1;
                if (rest != null) {
                    for (int i = 0; i < rest.entries.size() && dishIdx < 0; i++) {
                        if (rest.entries.get(i).sellUnit >= 2 && rest.entries.get(i).stack.getMaxStackSize() >= 8) dishIdx = i;
                    }
                }
                lifeJob = com.rtshop.job.Jobs.entry(sp).life;
                com.rtshop.job.Jobs.force(sp, com.rtshop.job.JobConfig.LIFE, "");
                if (dishIdx >= 0) {
                    ItemStack give = rest.entries.get(dishIdx).stack.copy();
                    give.setCount(6);
                    if (!sp.getInventory().add(give)) {
                        sp.getInventory().setItem(35, give);
                    }
                }
                ShopServer.openShop(sp, com.rtshop.shop.Restaurant.ID, -1);
            });
            waitTicks(20);
        });
        add(mc -> {
            ShopScreen s = mc.screen instanceof ShopScreen x ? x : null;
            check("the restaurant opens with an empty menu and explains how dishes are added",
                    s != null && dishIdx >= 0 && s.menuCountForTest() == 0 && s.rowsForTest() == 0, s == null ? mc.screen : s.menuCountForTest() + " idx " + dishIdx);
            shot(mc, "99_restaurant_empty_menu");
            if (s != null) s.debugView(true, true, false);
            waitTicks(5);
        });
        add(mc -> {
            ShopScreen s = mc.screen instanceof ShopScreen x ? x : null;
            check("someone who is not a cook sees that only cooks can sell here", s != null && s.sellLockedForTest() && s.rowsForTest() == 0);
            shot(mc, "99b_restaurant_cook_only");
            waitTicks(5);
        });
        add(mc -> {
            mc.setScreen(null); // 창을 닫으면 서버의 거래 세션도 닫힌다. 그 뒤에 다시 연다
            waitTicks(15);
        });
        add(mc -> {
            onServer(mc, sp -> {
                com.rtshop.job.Jobs.force(sp, com.rtshop.job.JobConfig.LIFE, "cook");
                ShopServer.openShop(sp, com.rtshop.shop.Restaurant.ID, -1);
            });
            waitTicks(20);
        });
        add(mc -> {
            ShopScreen s = mc.screen instanceof ShopScreen x ? x : null;
            if (s != null) s.debugView(true, true, false);
            check("a cook sees the dishes in the inventory on the sell tab", s != null && !s.sellLockedForTest() && s.rowsForTest() >= 1, s == null ? mc.screen : s.rowsForTest());
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "99c_restaurant_cook_sells");
            int idx = dishIdx;
            onServer(mc, sp -> {
                com.rtshop.shop.ShopData rest = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
                if (rest == null || idx < 0) return;
                com.rtshop.shop.ShopData.Entry e = rest.entries.get(idx);
                ShopServer.handleTransaction(sp, com.rtshop.shop.Restaurant.ID, rest.version, idx, true, 4, rest.variantKey(e, e.stack));
            });
            waitTicks(20);
        });
        add(mc -> {
            ShopScreen s = mc.screen instanceof ShopScreen x ? x : null;
            check("the dish the cook sold is added to the open window's menu right away", s != null && s.menuCountForTest() == 1, s == null ? mc.screen : s.menuCountForTest());
            if (s != null) s.debugView(false, true, false);
            waitTicks(5);
        });
        add(mc -> {
            ShopScreen s = mc.screen instanceof ShopScreen x ? x : null;
            check("the buy tab now lists exactly that dish", s != null && s.rowsForTest() == 1, s == null ? mc.screen : s.rowsForTest());
            shot(mc, "99d_restaurant_menu_one");
            waitTicks(5);
        });
        add(mc -> {
            mc.setScreen(null);
            int idx = dishIdx;
            String job = lifeJob;
            onServer(mc, sp -> {
                com.rtshop.shop.ShopData rest = ShopServer.shops().get(com.rtshop.shop.Restaurant.ID);
                if (rest != null && idx >= 0) {
                    net.minecraft.world.item.Item it = rest.entries.get(idx).stack.getItem();
                    sp.getInventory().clearOrCountMatchingItems(st -> st.is(it), -1, sp.inventoryMenu.getCraftSlots());
                }
                com.rtshop.shop.Restaurant.reset(sp.server);
                com.rtshop.job.Jobs.force(sp, com.rtshop.job.JobConfig.LIFE, job == null ? "" : job);
            });
            waitTicks(10);
        });

        // ------------------------------------------------------------ 주간 레이드: 안내인 대화 → 입장 화면 → 확인, 투표·실패 창, 편집기
        add(mc -> {
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.raid.Raid.open(sp, -1));
            waitTicks(20);
        });
        add(mc -> {
            RaidScreen rs = mc.screen instanceof RaidScreen x ? x : null;
            check("talking to the raid guide starts with a short conversation", rs != null && rs.phaseForTest() == 0, mc.screen);
            if (rs != null) rs.phaseForTest(1, false);
            waitTicks(8);
        });
        add(mc -> {
            shot(mc, "100_raid_dialogue");
            if (mc.screen instanceof RaidScreen rs) rs.phaseForTest(3, false);
            waitTicks(8);
        });
        add(mc -> {
            RaidScreen rs = mc.screen instanceof RaidScreen x ? x : null;
            check("then the boss entry screen lists the conditions and every reward (weekly + drops by grade)", rs != null && rs.rowsForTest() >= 10
                    && !rs.canEnterForTest(), rs == null ? mc.screen : rs.rowsForTest());
            check("the entry screen's illustration is in the mod", mc.getResourceManager().getResource(
                    new net.minecraft.resources.ResourceLocation("rtshop", "textures/gui/raid_lord.png")).isPresent());
            shot(mc, "100b_raid_entry");
            if (rs != null) rs.phaseForTest(3, true);
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "100c_raid_confirm");
            com.rtshop.raid.RaidConfig c = com.rtshop.raid.RaidConfig.fresh();
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("mode", "vote");
            o.addProperty("seconds", 30);
            o.addProperty("agreed", 1);
            o.addProperty("total", 3);
            o.addProperty("voted", false);
            com.google.gson.JsonObject t = new com.google.gson.JsonObject();
            for (String k : c.texts.keySet()) t.addProperty(k, c.text(k));
            o.add("texts", t);
            mc.setScreen(null);
            RaidClient.receive(com.rtshop.net.RaidPacket.PROMPT, o.toString());
            waitTicks(5);
        });
        add(mc -> {
            check("after a kill the 'leave now?' vote opens", mc.screen instanceof RaidPromptScreen ps && ps.voteForTest() && ps.agreedForTest() == 1, mc.screen);
            shot(mc, "100d_raid_vote");
            com.rtshop.raid.RaidConfig c = com.rtshop.raid.RaidConfig.fresh();
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("mode", "fail");
            o.addProperty("seconds", 60);
            o.addProperty("agreed", 0);
            o.addProperty("total", 3);
            o.addProperty("voted", false);
            com.google.gson.JsonObject t = new com.google.gson.JsonObject();
            for (String k : c.texts.keySet()) t.addProperty(k, c.text(k));
            o.add("texts", t);
            mc.setScreen(null);
            RaidClient.receive(com.rtshop.net.RaidPacket.PROMPT, o.toString());
            waitTicks(5);
        });
        add(mc -> {
            check("a failed raid shows the failure and asks before moving", mc.screen instanceof RaidPromptScreen ps && !ps.voteForTest(), mc.screen);
            shot(mc, "100e_raid_fail");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.raid.Raid.openEditor(sp, "raid"));
            waitTicks(20);
        });
        add(mc -> {
            RewardEditScreen es = mc.screen instanceof RewardEditScreen x ? x : null;
            check("the OP raid editor opens with numbers, texts and rewards", es != null && es.fieldsForTest(true) >= 20 && es.fieldsForTest(false) >= 30
                    && es.rewardsForTest() >= 10, es == null ? mc.screen : es.fieldsForTest(true) + "/" + es.fieldsForTest(false) + "/" + es.rewardsForTest());
            shot(mc, "101_raid_editor_numbers");
            if (es != null) es.tabForTest(1, 0);
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "101b_raid_editor_texts");
            if (mc.screen instanceof RewardEditScreen es) es.tabForTest(2, 1);
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "101c_raid_editor_rewards");
            mc.setScreen(null);
            waitTicks(5);
        });

        // ------------------------------------------------------------ 레이드 탈출권: 실제 우클릭으로 쓰면 들어온 자리로 돌아가고 게임이 멈추지 않는다
        final double[] raidFrom = new double[3];
        add(mc -> {
            mc.setScreen(null);
            raidFrom[0] = mc.player.getX();
            raidFrom[1] = mc.player.getY();
            raidFrom[2] = mc.player.getZ();
            onServer(mc, sp -> {
                com.rtshop.raid.Raid.resetForTest(sp.server);
                com.rtshop.raid.Raid.setKills(sp, 5);
                sp.getInventory().add(new net.minecraft.world.item.ItemStack(com.rtshop.item.ModItems.RAID_TICKET.get(), 1));
                com.rtshop.raid.Raid.enter(sp);
            });
            waitTicks(40);
        });
        add(mc -> {
            check("entering the raid moves the player into the arena", com.rtshop.raid.Raid.inArena(mc.player), mc.player.position());
            // 문 같은 블록을 눌러도 열리지 않는다 (구역 보호)
            onServer(mc, sp -> {
                net.minecraft.world.level.GameType gm = sp.gameMode.getGameModeForPlayer();
                sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                net.minecraft.core.BlockPos d = sp.blockPosition().north(2);
                sp.serverLevel().setBlock(d, net.minecraft.world.level.block.Blocks.OAK_TRAPDOOR.defaultBlockState(), 3);
                sp.gameMode.useItemOn(sp, sp.serverLevel(), net.minecraft.world.item.ItemStack.EMPTY, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(d), net.minecraft.core.Direction.UP, d, false));
                boolean opened = sp.serverLevel().getBlockState(d).getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN);
                sp.serverLevel().removeBlock(d, false);
                sp.setGameMode(gm);
                check("blocks inside the raid arena cannot be used (a trapdoor stays shut)", !opened);
            });
            int slot = -1;
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(com.rtshop.item.ModItems.RAID_ESCAPE.get())) slot = i;
            }
            if (slot < 0) {
                for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
                    if (mc.player.getInventory().items.get(i).is(com.rtshop.item.ModItems.RAID_ESCAPE.get())) {
                        final int from = i;
                        onServer(mc, sp -> {
                            net.minecraft.world.item.ItemStack st = sp.getInventory().items.get(from);
                            sp.getInventory().items.set(from, sp.getInventory().items.get(8));
                            sp.getInventory().items.set(8, st);
                            sp.inventoryMenu.broadcastChanges();
                        });
                        slot = 8;
                    }
                }
            }
            check("the escape ticket is handed out on entering", slot >= 0);
            if (slot >= 0) mc.player.getInventory().selected = slot;
            waitTicks(10);
        });
        add(mc -> {
            check("the escape ticket is in hand", mc.player.getMainHandItem().is(com.rtshop.item.ModItems.RAID_ESCAPE.get()), mc.player.getMainHandItem());
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            waitTicks(60);
        });
        add(mc -> {
            long t0 = mc.level.getGameTime();
            check("using the escape ticket (real right-click) brings the player back to where they entered, and the game keeps running",
                    !com.rtshop.raid.Raid.inArena(mc.player) && mc.player.distanceToSqr(raidFrom[0], raidFrom[1], raidFrom[2]) < 4 * 4
                            && !mc.player.getInventory().hasAnyMatching(s -> s.is(com.rtshop.item.ModItems.RAID_ESCAPE.get())), mc.player.position());
            shot(mc, "100f_raid_escaped");
            // 성공 투표 창이 열려 있다가 마을로 돌아오면 닫힌다
            com.rtshop.raid.RaidConfig c = com.rtshop.raid.RaidConfig.fresh();
            com.google.gson.JsonObject o = new com.google.gson.JsonObject();
            o.addProperty("mode", "vote");
            o.addProperty("seconds", 30);
            o.addProperty("agreed", 1);
            o.addProperty("total", 1);
            o.addProperty("voted", true);
            com.google.gson.JsonObject t = new com.google.gson.JsonObject();
            for (String k : c.texts.keySet()) t.addProperty(k, c.text(k));
            o.add("texts", t);
            RaidClient.receive(com.rtshop.net.RaidPacket.PROMPT, o.toString());
            com.google.gson.JsonObject close = new com.google.gson.JsonObject();
            close.addProperty("mode", "close");
            RaidClient.receive(com.rtshop.net.RaidPacket.PROMPT, close.toString());
            check("the 'leave now?' window closes once the player is back in town", !(mc.screen instanceof RaidPromptScreen), mc.screen);
            waitTicks(20);
            raidFrom[0] = t0;
        });
        add(mc -> {
            check("the world is still ticking after the escape", mc.level.getGameTime() > (long) raidFrom[0] + 10, mc.level.getGameTime());
            mc.setScreen(null);
        });

        // ------------------------------------------------------------ Spiral Abyss 주간 보상 창 (층 선택 화면 왼쪽 아래에 그려지는 것)
        add(mc -> {
            mc.setScreen(null);
            onServer(mc, sp -> {
                com.rtshop.raid.AbyssWeekly.resetPlayer(sp);
                // 주간 보상이 무한 모드에 걸려 있으면 첫 단계(endlessStep 웨이브)를, 아니면 1층 클리어를 흉내 낸다
                if (com.rtshop.raid.AbyssWeekly.config().endless) com.rtshop.raid.AbyssWeekly.endlessCleared(sp, com.rtshop.raid.AbyssWeekly.config().endlessStep);
                else com.rtshop.raid.AbyssWeekly.cleared(sp, 1);
                com.rtshop.raid.AbyssWeekly.sendView(sp);
            });
            waitTicks(20);
        });
        add(mc -> {
            check("the weekly abyss rewards arrive with the floor already cleared marked as received", RaidClient.abyss != null
                    && RaidClient.abyss.get("mask").getAsLong() == 1 && RaidClient.abyss.getAsJsonArray("rewards").size() >= 6, RaidClient.abyss);
            mc.setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("abyss panel")) {
                @Override
                public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float partial) {
                    g.fill(0, 0, this.width, this.height, 0xFF20182C);
                    AbyssPanel.render(g, this.width, this.height, 40, this.height - 60);
                }

                @Override
                public boolean isPauseScreen() {
                    return false;
                }
            });
            waitTicks(8);
        });
        add(mc -> {
            shot(mc, "102_abyss_weekly_panel");
            mc.setScreen(new AbyssRewardScreen(null));
            waitTicks(8);
        });
        add(mc -> {
            check("'details' opens the full weekly abyss reward window", mc.screen instanceof AbyssRewardScreen, mc.screen);
            shot(mc, "102c_abyss_weekly_detail");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.raid.Raid.openEditor(sp, "abyss"));
            waitTicks(20);
        });
        add(mc -> {
            RewardEditScreen es = mc.screen instanceof RewardEditScreen x ? x : null;
            check("the OP abyss reward editor opens (floors as groups, chance per reward)", es != null && es.id().equals("abyss") && es.rewardsForTest() >= 6, mc.screen);
            if (es != null) es.tabForTest(2, 2);
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "102b_abyss_editor_rewards");
            mc.setScreen(null);
            onServer(mc, com.rtshop.raid.AbyssWeekly::resetPlayer);
            waitTicks(5);
        });

        // ------------------------------------------------------------ NPC 대화 상자
        add(mc -> {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString("{\"npc\":\"quest_order_elder\",\"entity\":-1,\"name\":\"수도회 장로 에르윈\","
                    + "\"mode\":\"talk\",\"greet\":\"불을 지키고, 봉인을 지킨다. …견습이여, 오늘은 무엇을 배우러 왔나.\",\"quests\":1,"
                    + "\"topics\":[{\"id\":\"_chatter\",\"title\":\"요즘 어떠세요?\",\"lines\":[\"이 장갑 말인가? 오래된 화상 자국이 있어서 말일세.\"]},"
                    + "{\"id\":\"order\",\"title\":\"잿불 수도회에 대해\",\"new\":true,\"lines\":[\"잿불 수도회는 불을 지키는 자들이 세운 곳일세.\"]}]}").getAsJsonObject();
            DialogueScreen.open(o);
            waitTicks(30);
        });
        add(mc -> {
            check("the dialogue box opens and writes the greeting", mc.screen instanceof DialogueScreen d && !d.sceneForTest());
            shot(mc, "113_dialogue_greet");
            if (mc.screen instanceof DialogueScreen d) d.skipForTest();
            waitTicks(4);
        });
        add(mc -> {
            check("after the greeting the player picks a topic (chatter, lore, quests, goodbye)", mc.screen instanceof DialogueScreen d && d.choosingForTest());
            shot(mc, "114_dialogue_choices");
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString("{\"npc\":\"quest_order_archivist\",\"entity\":-1,\"name\":\"기록관 이사벨\","
                    + "\"mode\":\"scene\",\"greet\":\"\",\"topics\":[],\"lines\":[\"기록관 이사벨: 아, 장로님이 보내셨군요! 서약서는 여기 있어요.\","
                    + "\"탐험대장 로란: 나도 왔다.\"]}").getAsJsonObject();
            DialogueScreen.open(o);
            waitTicks(40);
        });
        add(mc -> {
            check("a quest scene plays in the dialogue box", mc.screen instanceof DialogueScreen d && d.sceneForTest());
            shot(mc, "115_dialogue_scene");
            mc.setScreen(null);
            // 스토리 대화의 내 대답: 하나뿐이어도 선택지로 직접 누른다
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString("{\"npc\":\"quest_order_archivist\",\"entity\":-1,\"name\":\"기록관 이사벨\","
                    + "\"mode\":\"scene\",\"greet\":\"\",\"topics\":[],\"lines\":[\"기록관 이사벨: 서약은 세 줄이에요. 셋째 줄은 지워져 있네요.\","
                    + "\"▷ 셋째 줄은 왜 지워졌나요?\",\"기록관 이사벨: 어머니가 베껴 쓴 거예요. 장로님께 가져가세요.\",\"▷ 알겠어요. 장로님께 가져갈게요.\"]}").getAsJsonObject();
            DialogueScreen.open(o);
            waitTicks(10);
        });
        add(mc -> {
            DialogueScreen d = mc.screen instanceof DialogueScreen x ? x : null;
            if (d != null) d.skipForTest();
            check("in a story scene the player's line waits as a choice to click, even when there is only one", d != null && d.replyingForTest()
                    && mc.screen == d, mc.screen);
            shot(mc, "115b_dialogue_reply");
            if (d != null) d.replyForTest();
            waitTicks(4);
        });
        add(mc -> {
            DialogueScreen d = mc.screen instanceof DialogueScreen x ? x : null;
            check("clicking the reply goes on to the next line", d != null && !d.replyingForTest(), mc.screen);
            if (d != null) d.skipForTest();
            check("the last reply is also a click (the scene does not close by itself)", d != null && d.replyingForTest() && mc.screen == d, mc.screen);
            if (d != null) d.replyForTest();
            waitTicks(4);
        });
        add(mc -> {
            check("after the last reply the scene ends", !(mc.screen instanceof DialogueScreen), mc.screen);
            mc.setScreen(null);
            waitTicks(2);
        });
        // ------------------------------------------------------------ 퀘스트 수첩 (어디서나 내용 보기)
        add(mc -> {
            mc.setScreen(null);
            mc.player.connection.sendCommand("rtshop quest");
            waitTicks(20);
        });
        add(mc -> {
            QuestScreen qs = mc.screen instanceof QuestScreen x ? x : null;
            check("the quest journal opens anywhere (/rtshop quest)", qs != null, mc.screen);
            if (qs != null) qs.tabForTest(1);
            waitTicks(4);
        });
        add(mc -> {
            QuestScreen qs = mc.screen instanceof QuestScreen x ? x : null;
            check("the journal lists quests the player can take now, read-only (go to the giver to accept)", qs != null && qs.shownForTest() > 0,
                    qs == null ? mc.screen : qs.shownForTest());
            shot(mc, "116_journal_available");
            if (qs != null) qs.tabForTest(2);
            waitTicks(4);
        });
        add(mc -> {
            shot(mc, "116b_journal_done");
            mc.setScreen(new FontCalibScreen());
            waitTicks(6);
        });
        // ------------------------------------------------------------ 마법 뽑기 (별빛 수정 구체)
        add(mc -> {
            mc.setScreen(null);
            orbPos = null;
            onServer(mc, sp -> {
                net.minecraft.core.BlockPos at = sp.blockPosition().relative(sp.getDirection(), 2);
                for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(at.offset(-1, 0, -1), at.offset(1, 2, 1))) {
                    sp.serverLevel().setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(at.offset(-3, -1, -3), at.offset(3, -1, 3))) {
                    sp.serverLevel().setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                }
                for (net.minecraft.core.BlockPos q : net.minecraft.core.BlockPos.betweenClosed(at.offset(-3, 0, -3), at.offset(3, 2, 3))) {
                    if (!sp.serverLevel().getBlockState(q).isAir()) sp.serverLevel().setBlockAndUpdate(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                sp.connection.teleport(sp.blockPosition().getX() + 0.5, at.getY(), sp.blockPosition().getZ() + 0.5, sp.getYRot(), sp.getXRot());
                sp.serverLevel().setBlockAndUpdate(at.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                sp.serverLevel().setBlockAndUpdate(at, com.rtshop.magic.MagicRegistry.ORB.get().defaultBlockState());
                com.rtshop.magic.MagicConfig c = com.rtshop.magic.MagicConfig.get();
                // 결과가 정해져 있어야 하므로 실패 확률을 잠시 0 으로 둔다 (끝나면 되돌린다; 실패는 아래에서 따로 본다)
                magicFail = new double[]{c.selectOdds[0], c.randomOdds[0], c.enchantOdds[0]};
                c.selectOdds[0] = c.randomOdds[0] = c.enchantOdds[0] = 0;
                magicSkills = 0;
                for (String cat : com.rtshop.magic.MagicConfig.CATS) magicSkills += c.pool(cat).size();
                // 가방을 잠시 비워 둔다 (뽑은 스킬북 10권이 들어갈 자리) — 끝나면 되돌린다
                savedInv.clear();
                for (int i = 0; i < sp.getInventory().items.size(); i++) savedInv.add(sp.getInventory().items.get(i).copy());
                sp.getInventory().items.replaceAll(x -> ItemStack.EMPTY);
                sp.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET_SELECT.get(), 30));
                sp.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.TICKET.get(), 30));
                sp.getInventory().add(new ItemStack(com.rtshop.magic.MagicRegistry.SCROLL.get(), 6));
                ItemStack sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
                sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 2);
                sword.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
                prevSel = sp.getInventory().selected;
                prevSlot8 = sp.getInventory().getItem(8).copy();
                sp.getInventory().selected = 8;
                sp.getInventory().setItem(8, sword);
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(8));
                orbPos = at;
            });
            waitTicks(60); // 앞의 창이 게임을 멈춰 세계를 저장하는 동안 서버가 늦게 깨어난다
        });
        add(mc -> {
            check("the Starlight Crystal Orb was placed and the magic gacha has skill books from this pack", orbPos != null
                    && mc.level.getBlockState(orbPos).is(com.rtshop.magic.MagicRegistry.ORB.get()) && magicSkills > 0, magicSkills);
            if (orbPos != null) {
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                        net.minecraft.world.phys.Vec3.atCenterOf(orbPos).add(0, 0.5, 0), net.minecraft.core.Direction.UP, orbPos, false));
            }
            waitTicks(com.rtshop.magic.MagicRegistry.cinematic ? 30 : 4);
        });
        // ---- Cinematic Interactions 가 있으면: 제단은 1인칭 장면으로 열린다 (걸어가 몸을 숙이고, 룬석·소환 문양을 손으로 만진다)
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            check("with Cinematic Interactions the orb starts a first-person scene instead of a window", CinematicOrb.sceneActive(), CinematicOrb.phaseForTest());
            shot(mc, "123_orb_scene_intro");
            waitTicks(130);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("the scene becomes active and the altar data has arrived", st != null && "ACTIVE".equals(CinematicOrb.phaseForTest()) && st.readyForTest(),
                    CinematicOrb.phaseForTest());
            double[] g0 = st == null ? null : st.guiOfForTest(0), g3 = st == null ? null : st.guiOfForTest(3), gs = st == null ? null : st.guiOfForTest(-1);
            double[] gi = st == null ? null : st.guiOfItemSpotForTest();
            int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
            boolean in = g0 != null && g3 != null && gs != null && g0[1] > 4 && g0[1] < sh && g3[1] < sh - 4 && gs[1] < sh - 4 && g3[0] > 4 && g3[0] < sw;
            check("the five rune stones and the summoning sigil are inside the view", in,
                    (g0 == null ? "?" : (int) g0[0] + "," + (int) g0[1]) + " " + (g3 == null ? "?" : (int) g3[0] + "," + (int) g3[1]) + " "
                            + (gs == null ? "?" : (int) gs[0] + "," + (int) gs[1]) + " of " + sw + "x" + sh);
            check("the place for gear on the altar is inside the view, and the bag is shown so gear can be picked up",
                    gi != null && gi[0] > 4 && gi[0] < sw - 4 && gi[1] > 4 && gi[1] < sh - 4 && st.inventoryShownForTest(),
                    gi == null ? "?" : (int) gi[0] + "," + (int) gi[1]);
            shot(mc, "123b_orb_scene");
            if (st != null) st.chooseForTest(0);
            waitTicks(14);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            shot(mc, "123c_orb_scene_attack");
            check("touching the summoning sigil starts the ritual (one draw at a time)", st != null && st.pullForTest());
            waitTicks(9);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            shot(mc, "123d_orb_ritual");
            waitTicks(50);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("at the ritual's peak the rune summoning card plays over the scene", st != null && (st.fxActiveForTest() || st.shownForTest() == 1),
                    st == null ? "no scene" : st.shownForTest());
            shot(mc, "123e_orb_fx");
            waitTicks(170);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("after the card the drawn skill book is shown (scene)", st != null && !st.fxActiveForTest() && st.shownForTest() == 1,
                    st == null ? "no scene" : st.shownForTest());
            shot(mc, "123f_orb_results");
            if (st != null) {
                st.dismissForTest();
                st.chooseForTest(-1);
                st.placeForTest(8); // 가방의 검을 제단 위로
            }
            waitTicks(14);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("gear picked from the bag lies on the altar (scene), not in the hand", st != null && st.onAltar().is(net.minecraft.world.item.Items.DIAMOND_SWORD)
                    && mc.player.getInventory().getItem(8).isEmpty(), st == null ? "no scene" : st.onAltar());
            shot(mc, "123g_orb_scene_gear");
            check("touching the blue sigil rerolls the enchantment of the gear on the altar (scene)", st != null && st.rerollForTest());
            waitTicks(90);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            int n = st == null ? -1 : net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(st.onAltar()).size();
            check("the scene reroll leaves exactly one enchantment on the gear", n == 1, n);
            shot(mc, "123h_orb_reroll");
            if (st != null) st.dismissForTest();
            onServer(mc, sp -> com.rtshop.magic.MagicConfig.get().enchantOdds[0] = 1e12); // 이번에는 반드시 실패
            waitTicks(10);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            magicBefore = st == null ? "" : String.valueOf(st.onAltar().getTag());
            check("the blue sigil can be touched again once the first ritual is over", st != null && st.rerollForTest());
            waitTicks(120);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            CinematicOrbStation st = CinematicOrb.station();
            check("a failed reroll shows the fail card and leaves the gear as it was (scene)", st != null && st.shownForTest() == 1
                    && magicBefore.equals(String.valueOf(st.onAltar().getTag()))
                    && mc.player.getInventory().countItem(com.rtshop.magic.MagicRegistry.SCROLL.get()) == 4, st == null ? "no scene" : st.shownForTest());
            shot(mc, "123i_orb_reroll_fail");
            if (st != null) st.dismissForTest();
            onServer(mc, sp -> com.rtshop.magic.MagicConfig.get().enchantOdds[0] = 0);
            CinematicOrb.leaveForTest();
            waitTicks(220);
        });
        add(mc -> {
            if (!com.rtshop.magic.MagicRegistry.cinematic) return;
            check("stepping back ends the scene and the gear left on the altar comes back to the bag", !CinematicOrb.sceneActive()
                    && mc.player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND_SWORD) == 1,
                    CinematicOrb.phaseForTest() + " / " + mc.player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND_SWORD));
        });
        // ---- 창으로 여는 길 (Cinematic 이 없거나 장면을 꺼 둔 경우): 같은 메뉴가 룬 제단 창으로 뜬다
        add(mc -> {
            onServer(mc, sp -> {
                if (orbPos != null) com.rtshop.magic.MagicGacha.open(sp, orbPos);
            });
            waitTicks(20);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            check("the rune altar window opens on its menu (one altar slot + the bag)", ms instanceof MagicMenuScreen
                    && mc.player.containerMenu instanceof com.rtshop.magic.MagicMenu, mc.screen);
            shot(mc, "122b_magic_gacha");
            if (ms != null) {
                ms.tabForTest(0);
                ms.catForTest(0);
                ms.pullForTest();
            }
            waitTicks(30);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            check("a chosen-field draw plays the rune summoning scene", ms != null && ms.fxActiveForTest(), mc.screen);
            shot(mc, "122c_magic_fx");
            waitTicks(40);
        });
        add(mc -> {
            shot(mc, "122d_magic_fx_card");
            waitTicks(120);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            check("after the scene the drawn skill book is shown", ms != null && !ms.fxActiveForTest() && ms.shownForTest() == 1,
                    ms == null ? mc.screen : ms.shownForTest());
            int books = 0;
            for (ItemStack st : mc.player.getInventory().items) if (com.rtshop.gacha.Gacha.marked(st)) books++;
            check("the drawn skill books are in the bag, marked like gacha items (can be dismantled at Lucky)", books > 0, books);
            shot(mc, "122e_magic_results");
            if (ms != null) {
                ms.mouseClicked(5, 5, 0);
                ms.tabForTest(2);
            }
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "122f_magic_reroll_tab");
            int at = -1;
            for (int i = 0; i < 36; i++) if (mc.player.getInventory().getItem(i).is(net.minecraft.world.item.Items.DIAMOND_SWORD)) at = i;
            if (mc.screen instanceof MagicGachaScreen ms && at >= 0) ms.placeForTest(at);
            waitTicks(10);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            check("shift-clicking gear in the bag lays it on the altar slot of the window", ms != null && ms.altarForTest().is(net.minecraft.world.item.Items.DIAMOND_SWORD)
                    && ms.dataForTest().get("heldOk").getAsBoolean(), ms == null ? mc.screen : ms.altarForTest());
            shot(mc, "122f2_magic_reroll_gear");
            if (ms != null) ms.rerollForTest();
            waitTicks(20);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            int n = ms == null ? -1 : net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(ms.altarForTest()).size();
            check("enchant reroll clears the enchantments of the gear on the altar and puts exactly one new one on it", n == 1
                    && !ms.rerollTextForTest().isEmpty(), n);
            waitTicks(100);
        });
        add(mc -> {
            shot(mc, "122g_magic_reroll_result");
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            if (ms != null) {
                ms.mouseClicked(5, 5, 0);
                ms.settingsForTest(true);
            }
            waitTicks(4);
        });
        add(mc -> {
            MagicGachaScreen ms = mc.screen instanceof MagicGachaScreen x ? x : null;
            check("the rune altar has its own display settings (font, size, shadow, animation, panel side)", ms != null && ms.settingsOpenForTest());
            shot(mc, "122g2_magic_settings");
            ClientConfig cfg = ClientConfig.get();
            String f0 = cfg.magicFont;
            double s0 = cfg.magicScale;
            MagicUiPanel.cycle(0);
            MagicUiPanel.cycle(1);
            MagicUiPanel.cycle(2);
            check("pressing a settings row moves it to the next value (sans -> serif, 100% -> 110%, shadow on) and the fonts follow",
                    f0.equals("sans") && cfg.magicFont.equals("serif") && s0 == 1.0 && cfg.magicScale == 1.1 && cfg.magicShadow
                            && MagicUi.body().getPath().equals("ui_serif") && MagicUi.shadow(), cfg.magicFont + " " + cfg.magicScale);
            waitTicks(4);
        });
        add(mc -> {
            shot(mc, "122g3_magic_settings_serif");
            ClientConfig.get().resetMagic();
            ClientConfig.get().save();
            check("the defaults are a clean sans font with no text shadow", MagicUi.body().getPath().equals("ui_sans") && !MagicUi.shadow() && MagicUi.scale() == 1F);
            if (mc.screen instanceof MagicGachaScreen ms) ms.settingsForTest(false);
            com.rtshop.net.ShopNet.CHANNEL.sendToServer(new com.rtshop.net.MagicActionPacket(com.rtshop.net.MagicActionPacket.EDIT, ""));
            waitTicks(20);
        });
        add(mc -> {
            MagicEditScreen es = mc.screen instanceof MagicEditScreen x ? x : null;
            check("the OP magic gacha editor opens with the attack-field list", es != null && es.rowsForTest() >= 0, mc.screen);
            shot(mc, "122h_magic_editor");
            if (es != null) es.tabForTest(6);
            waitTicks(4);
        });
        add(mc -> {
            shot(mc, "122i_magic_editor_odds");
            mc.setScreen(null);
            // 메뉴로 열리는 길 (Cinematic 은 있지만 장면을 꺼 둔 경우): 같은 룬 제단 창이 떠야 한다
            onServer(mc, sp -> {
                if (orbPos != null) {
                    var st = sp.serverLevel().getBlockState(orbPos);
                    var prov = st.getMenuProvider(sp.serverLevel(), orbPos);
                    if (prov != null) sp.openMenu(prov);
                }
            });
            waitTicks(12);
        });
        add(mc -> {
            check("opened as a menu (scene turned off) the orb shows the same rune altar window", mc.screen instanceof MagicMenuScreen
                    && mc.player.containerMenu instanceof com.rtshop.magic.MagicMenu, mc.screen);
            shot(mc, "122k_magic_menu_window");
            mc.setScreen(new FontCalibScreen());
            onServer(mc, sp -> {
                if (orbPos != null) sp.serverLevel().setBlockAndUpdate(orbPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                for (int i = 0; i < savedInv.size() && i < sp.getInventory().items.size(); i++) sp.getInventory().items.set(i, savedInv.get(i));
                if (magicFail != null) {
                    com.rtshop.magic.MagicConfig mcfg = com.rtshop.magic.MagicConfig.get();
                    mcfg.selectOdds[0] = magicFail[0];
                    mcfg.randomOdds[0] = magicFail[1];
                    mcfg.enchantOdds[0] = magicFail[2];
                }
                sp.getInventory().selected = prevSel;
                sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(prevSel));
            });
            waitTicks(5);
        });
        add(mc -> {
            shot(mc, "118_font_calib");
            mc.setScreen(null);
            // 퀘스트 NPC(기록관 이사벨)도 우클릭하면 대화 상자가 먼저 열린다
            qnpc = -1;
            DialogueScreen.autoMainForTest = false;
            onServer(mc, sp -> {
                net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                var v = com.rtshop.quest.QuestManager.spawnNpc(sp.serverLevel(), sp.position().add(look.x * 2, 0, look.z * 2), sp.getYRot() + 180,
                        "order_archivist");
                qnpc = v == null ? -2 : v.getId();
            });
            waitTicks(20);
        });
        add(mc -> {
            net.minecraft.world.entity.Entity e = qnpc >= 0 ? mc.level.getEntity(qnpc) : null;
            check("the archivist quest NPC for the dialogue-first test is there", e != null, qnpc);
            if (e != null) mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
            waitTicks(25);
        });
        add(mc -> {
            DialogueScreen ds = mc.screen instanceof DialogueScreen x ? x : null;
            check("right-clicking a story quest NPC opens the dialogue box first (not the quest window)", ds != null, mc.screen);
            if (ds != null) {
                ds.skipForTest();
                check("the story NPC's dialogue offers [Quests] as a choice", ds.hasQuestChoiceForTest());
                shot(mc, "119_quest_npc_dialogue");
                int lines = ds.openHistoryForTest();
                check("the dialogue can show the conversation history with this NPC", lines > 0, lines);
                int mid = ds.openHistoryMidTalkForTest();
                check("the conversation history also opens with H while talking, and the last line stays whole above the choices", mid > 0 && ds.lastLineRowsForTest() >= 1, mid);
            }
            // 연출 중에는 이 모드의 창이 그 위로 열리지 않고, 끝난 뒤에 열린다. 창을 닫은 직후 1초는 공격을 받지 않는다.
            ClientEvents.holdScreens = true;
            mc.setScreen(null);
            Cinematic.timeForTest = -1;
            Cinematic.play("{\"kind\":\"line\",\"speaker\":\"t\",\"text\":\"t\",\"closeScreen\":true,\"seconds\":1.2}");
            mc.setScreen(new FontCalibScreen());
            boolean held = mc.screen == null && ClientEvents.hasDeferredScreenForTest() && Cinematic.holdsScreens();
            check("while a quest's opening scene plays, windows wait instead of covering it", held, mc.screen);
            waitTicks(40);
        });
        add(mc -> {
            check("the waiting window opens once the scene is over, and for a second after a window closes attacks are ignored",
                    mc.screen instanceof FontCalibScreen && !ClientEvents.hasDeferredScreenForTest() && ClientEvents.attackGuard(), mc.screen);
            ClientEvents.holdScreens = false;
            mc.setScreen(null);
            waitTicks(4);
        });
        add(mc -> {
            shot(mc, "119b_dialogue_history");
            mc.setScreen(null);
            DialogueScreen.autoMainForTest = true;
            onServer(mc, sp -> {
                net.minecraft.world.entity.Entity e = qnpc >= 0 ? sp.serverLevel().getEntity(qnpc) : null;
                if (e != null) e.discard();
            });
            mc.player.connection.sendCommand("rtshop itemstats edit minecraft:diamond_sword");
            waitTicks(20);
        });
        // 장비 능력치 편집기
        add(mc -> {
            ItemStatsScreen is = mc.screen instanceof ItemStatsScreen x ? x : null;
            check("the equipment stats editor opens for a diamond sword", is != null && "minecraft:diamond_sword".equals(is.itemForTest()), mc.screen);
            if (is != null) {
                Double orig = is.originalForTest().get("minecraft:generic.attack_damage");
                check("the editor shows the sword's original damage (7) and speed (1.6)", orig != null && Math.abs(orig - 7) < 1e-6
                        && is.originalForTest().get("minecraft:generic.attack_speed") != null
                        && Math.abs(is.originalForTest().get("minecraft:generic.attack_speed") - 1.6) < 1e-6, is.originalForTest());
                shot(mc, "120_item_stats");
                is.setAndSaveForTest("minecraft:generic.attack_damage", "9");
            }
            waitTicks(25);
        });
        add(mc -> {
            var m = com.rtshop.stats.ItemStats.table(true).get("minecraft:diamond_sword");
            double dmg = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)
                    .getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
                    .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).stream().mapToDouble(x -> x.getAmount()).sum() + 1;
            check("saving in the editor reaches the client too (the tooltip shows damage 9)", m != null && Math.abs(dmg - 9) < 1e-6, dmg);
            shot(mc, "120b_item_stats_saved");
            onServer(mc, sp -> {
                com.rtshop.stats.ItemStats.put("minecraft:diamond_sword", null);
                com.rtshop.stats.ItemStats.save();
                com.rtshop.stats.ItemStats.syncAll(sp.server);
            });
            mc.setScreen(null);
            waitTicks(10);
        });
        // 알림 상자: 채팅 대신 핫바 위에, 창 글꼴로, 화면 밖으로 넘치지 않게
        add(mc -> {
            NoticeHud.clearForTest();
            mc.player.connection.sendCommand("rtshop balance");
            waitTicks(15);
        });
        add(mc -> {
            check("a server notice (/rtshop balance) shows in the notice box, not in chat", NoticeHud.activeForTest() > 0
                    && !NoticeHud.historyForTest().isEmpty(), NoticeHud.historyForTest());
            NoticeHud.push(net.minecraft.network.chat.Component.literal("「습격의 징조」 퀘스트를 받았습니다. 「조사대원」에게 가 보세요.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            NoticeHud.push(net.minecraft.network.chat.Component.literal("이 땅은 성 영역입니다. 산 토지 밖에서는 블록을 부수거나 놓을 수 없습니다. "
                    + "아주 긴 알림도 화면 폭에 맞춰 줄을 바꾸고, 세 줄이 넘으면 끝을 줄임표로 줄입니다. 글자가 깨지거나 화면 밖으로 나가지 않아야 합니다. "
                    + "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW").withStyle(net.minecraft.ChatFormatting.RED));
            NoticeHud.push(net.minecraft.network.chat.Component.literal("같은 알림은 묶어서 보여 줍니다"));
            NoticeHud.push(net.minecraft.network.chat.Component.literal("같은 알림은 묶어서 보여 줍니다"));
            waitTicks(12);
        });
        add(mc -> {
            check("notices stay inside the screen and long ones wrap to at most 2 lines each", NoticeHud.lastInsideForTest
                    && NoticeHud.lastLinesForTest > 0 && NoticeHud.lastLinesForTest <= NoticeHud.MAX_LINES * NoticeHud.MAX_VISIBLE, NoticeHud.lastLinesForTest);
            check("notices are thin strips at the top centre of the screen (under any boss bars), newest below, at most three at a time",
                    NoticeHud.lastTopForTest >= 4 && NoticeHud.lastTopForTest < mc.getWindow().getGuiScaledHeight() / 3
                            && NoticeHud.lastCountForTest >= 1 && NoticeHud.lastCountForTest <= 3, NoticeHud.lastTopForTest + " / " + NoticeHud.lastCountForTest);
            check("a notice takes its dot colour from the message (green success, red failure, gold otherwise)",
                    NoticeHud.tone(net.minecraft.network.chat.Component.literal("ok").withStyle(net.minecraft.ChatFormatting.GREEN)) == 0x55FF55
                            && NoticeHud.tone(net.minecraft.network.chat.Component.literal("plain")) == 0xE8B84A
                            && NoticeHud.tone(net.minecraft.network.chat.Component.literal("white").withStyle(net.minecraft.ChatFormatting.WHITE)) == 0xE8B84A);
            shot(mc, "121_notices");
            mc.player.connection.sendCommand("rtshop quest");
            waitTicks(25);
        });
        add(mc -> {
            NoticeHud.push(net.minecraft.network.chat.Component.literal("창이 열려 있어도 알림은 화면 아래에 보입니다").withStyle(net.minecraft.ChatFormatting.GREEN));
            waitTicks(10);
        });
        add(mc -> {
            check("with a window open the notice draws on top of it, still inside the screen", mc.screen != null && NoticeHud.lastInsideForTest, mc.screen);
            shot(mc, "121b_notice_over_window");
            mc.setScreen(null);
            NoticeHud.clearForTest();
            waitTicks(5);
        });
        // 무기를 바꿔도 특수기의 스택이 남는다: 검을 들었다가 다른 것을 들고, 다시 검을 들면 기억해 둔 스택이 돌아온다
        add(mc -> {
            stackResult = "";
            if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
            onServer(mc, sp -> {
                stackKeep = sp.getMainHandItem().copy();
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            });
            waitTicks(12);
        });
        add(mc -> {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
            onServer(mc, sp -> {
                String first = com.rtshop.compat.EfStacks.probe(sp);
                stackResult = first;
                String id = first.contains("|") ? first.substring(0, first.indexOf('|')) : "";
                if (!id.isEmpty()) com.rtshop.compat.EfStacks.rememberForTest(sp, id, 1);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
            });
            waitTicks(12);
        });
        add(mc -> {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
            onServer(mc, sp -> {
                String id = stackResult.contains("|") ? stackResult.substring(0, stackResult.indexOf('|')) : "";
                if (!id.isEmpty()) com.rtshop.compat.EfStacks.rememberForTest(sp, id, 1);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            });
            waitTicks(12);
        });
        add(mc -> {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
            onServer(mc, sp -> {
                stackResult = stackResult + " -> " + com.rtshop.compat.EfStacks.probe(sp);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stackKeep);
            });
            waitTicks(6);
        });
        add(mc -> {
            if (!net.minecraftforge.fml.ModList.get().isLoaded("epicfight")) return;
            String[] parts = stackResult.split(" -> ");
            boolean had = parts.length == 2 && parts[0].contains("|") && !parts[0].startsWith("|");
            boolean back = had && parts[1].startsWith(parts[0].substring(0, parts[0].indexOf('|')) + "|1|");
            check("switching weapons keeps the innate skill's stack: back on the sword, the remembered stack returns", had && back, stackResult);
        });
        // 운영자 기능은 크리에이티브일 때만: 서바이벌의 OP 는 OP 전용 명령어도 편집 창도 쓸 수 없다 (/gamemode 는 남는다)
        add(mc -> {
            permResult = "";
            onServer(mc, sp -> {
                com.rtshop.util.Perm.enforceForTest = true;
                try {
                    net.minecraft.world.level.GameType was = sp.gameMode.getGameModeForPlayer();
                    sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    boolean locked = com.rtshop.util.Perm.lockedOp(sp) && !com.rtshop.util.Perm.op(sp);
                    boolean timeOnly = com.rtshop.util.Perm.opOnly(sp, "time set day"), balanceFree = !com.rtshop.util.Perm.opOnly(sp, "rtshop balance");
                    sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    boolean open = com.rtshop.util.Perm.op(sp) && !com.rtshop.util.Perm.lockedOp(sp);
                    sp.setGameMode(was);
                    permResult = sp.hasPermissions(2) ? locked + "/" + timeOnly + "/" + balanceFree + "/" + open : "not-op";
                } finally {
                    com.rtshop.util.Perm.enforceForTest = false;
                }
            });
            waitTicks(10);
        });
        add(mc -> {
            check("an operator in survival is locked out of operator features (operator-only commands are told apart from ordinary ones), in creative they are back",
                    permResult.equals("true/true/true/true") || permResult.equals("not-op"), permResult);
            waitTicks(2);
        });
        // 직업 딱지: 가방 창 왼쪽의 [직업] 에 마우스를 올리면 전투·생활 직업의 패시브가 보인다
        add(mc -> {
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            waitTicks(10);
        });
        add(mc -> {
            java.util.List<net.minecraft.network.chat.Component> jl = JobBadge.lines();
            boolean named = com.rtshop.job.ClientJobCache.combat.isEmpty() || jl.get(0).getString().contains(com.rtshop.job.ClientJobCache.combat);
            if (net.minecraftforge.fml.ModList.get().isLoaded("ftblibrary")) {
                boolean left = false;
                StringBuilder kinds = new StringBuilder();
                for (net.minecraft.client.gui.components.events.GuiEventListener l : mc.screen.children()) {
                    left |= ClientEvents.sidebarButton(l);
                    kinds.append(l.getClass().getName()).append(' ');
                }
                check("the function buttons other mods put at the top-left of the inventory (FTB sidebar) are gone (at least one was removed)",
                        !left && ClientEvents.sidebarRemovedForTest >= 1, ClientEvents.sidebarRemovedForTest + " | " + kinds);
            }
            check("the job badge lists the combat and life job with their passives (no job = 'none', never empty)",
                    mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen && jl.size() >= 4 && named
                            && !jl.get(0).getString().isBlank(), jl.size() + " / " + jl.get(0).getString());
            shot(mc, "122_job_badge");
            mc.setScreen(null);
            waitTicks(5);
        });
        // 보스 음악: 곡 파일이 아직 없으면 조용히 아무것도 틀지 않는다 (오류 없이)
        add(mc -> {
            com.google.gson.JsonObject mo = new com.google.gson.JsonObject();
            mo.addProperty("track", "no_such_track_for_test");
            mo.addProperty("entity", -1);
            BossMusicClient.receive(mo);
            waitTicks(5);
        });
        add(mc -> {
            check("boss music: a track with no sound file plays nothing and breaks nothing",
                    BossMusicClient.playingForTest().isEmpty() && !BossMusicClient.has("no_such_track_for_test"), BossMusicClient.playingForTest());
            // 3.63: 음악이 겹치지 않게 — 0 그대로 틀기 · 1 틀지 않기 · 2 틀고 먼저 흐르던 배경 음악 멈추기
            String m363 = "" + BossMusicClient.decideForTest("cataclysm:ignis_music", "MUSIC", false, false, false, true)      // 첫 보스 음악: 틀고 배경은 멈춤
                    + BossMusicClient.decideForTest("mowziesmobs:music.frostmaw_theme", "MUSIC", false, false, true, true)        // 보스 음악이 이미 흐름: 둘째는 틀지 않음
                    + BossMusicClient.decideForTest("somemod:ambient_music.cave", "MUSIC", true, false, true, true)              // 보스 음악 중의 배경 음악: 틀지 않음
                    + BossMusicClient.decideForTest("somemod:ambient_music.cave", "MUSIC", true, false, false, true)             // 평소의 배경 음악: 틀고 앞의 배경은 멈춤
                    + BossMusicClient.decideForTest("cataclysm:ignis_music", "MUSIC", false, true, false, true)                  // 우리 곡이 흐름: 틀지 않음
                    + BossMusicClient.decideForTest("cataclysm:ignis_music", "MUSIC", false, false, true, false)                 // 설정을 끔: 손대지 않음
                    + BossMusicClient.decideForTest("cataclysm:music_disc_x", "RECORDS", true, false, true, true)                // 주크박스: 손대지 않음
                    + BossMusicClient.decideForTest("minecraft:music.game", "MUSIC", true, false, true, true)                    // 게임 본래의 음악: 여기서는 손대지 않음 (음악 관리자를 쉬게 한다)
                    + BossMusicClient.decideForTest("cataclysm:ignis_roar", "HOSTILE", false, false, true, true);                // 음악이 아닌 소리
            check("3.63 boss music never overlaps: one boss theme at a time (the first one keeps playing), background music gives way to it, jukeboxes and ordinary sounds are left alone, and client.json can switch it off",
                    m363.equals("211210000") && ClientConfig.get().oneBossMusic, m363);
            com.google.gson.JsonObject mo = new com.google.gson.JsonObject();
            mo.addProperty("track", "");
            BossMusicClient.receive(mo);
            waitTicks(2);
        });
        // 무기 설명서: 펼치면 그 무기의 연속 공격과 특수기가 나온다
        add(mc -> {
            ClientHooks.openWeaponManual(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            waitTicks(10);
        });
        add(mc -> {
            boolean ef = net.minecraftforge.fml.ModList.get().isLoaded("epicfight");
            if (mc.screen instanceof WeaponManualScreen w) {
                check("the weapon manual opens and has text", w.linesForTest() >= 1, w.linesForTest());
                if (ef) {
                    check("the iron sword's manual names its type, its combo and its special skill (read from Epic Fight)",
                            w.categoryForTest().equals("sword") && !w.skillForTest().isEmpty() && w.comboForTest() >= 1 && w.linesForTest() >= 10,
                            w.categoryForTest() + " / " + w.skillForTest() + " / " + w.comboForTest() + " / " + w.linesForTest());
                }
            } else {
                check("the weapon manual opens and has text", false, mc.screen);
            }
            shot(mc, "123_weapon_manual");
            mc.setScreen(null);
            waitTicks(5);
        });
        // 이야기 물건 읽기: 연대기 페이지를 펼치면 기록관이 적은 이름들이 쪽마다 나온다
        add(mc -> {
            onServer(mc, sp -> {
                for (String q : READ_QUESTS) READ_HAD.put(q, com.rtshop.quest.QuestManager.setCompletedForTest(sp, q, true));
                com.rtshop.story.Readables.open(sp, new ItemStack(com.rtshop.item.ModItems.CHRONICLE_PAGE.get()));
            });
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof ReadScreen r) {
                boolean fits = true;
                int n = r.pagesForTest();
                for (int i = 0; i < n; i++) {
                    fits &= r.fitsForTest();
                    r.turnForTest(1);
                }
                boolean end = r.pageForTest() == n - 1;
                for (int i = 0; i < n; i++) r.turnForTest(-1);
                boolean start = r.pageForTest() == 0;
                r.turnForTest(1);
                check("a chronicle page opens as a sheet to read: one page per name written, every page fits, pages turn", n >= 3 && r.headingForTest().contains("연대기") && fits && end && start, n + " / " + fits + " / " + end + " / " + r.headingForTest());
            } else {
                check("a chronicle page opens as a sheet to read: one page per name written, every page fits, pages turn", false, mc.screen);
            }
            shot(mc, "126a_read_chronicle");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.story.Readables.open(sp, new ItemStack(com.rtshop.item.ModItems.FROZEN_LOGBOOK.get())));
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof ReadScreen r) {
                boolean fits = true;
                int n = r.pagesForTest();
                for (int i = 0; i < n; i++) {
                    fits &= r.fitsForTest();
                    r.turnForTest(1);
                }
                boolean end = r.pageForTest() == n - 1;
                for (int i = 0; i < n; i++) r.turnForTest(-1);
                boolean start = r.pageForTest() == 0;
                r.turnForTest(1);
                check("the thawed logbook reads over several pages and each fits the sheet", n >= 4 && fits && end && start, n + " / " + fits + " / " + end + " / " + r.headingForTest());
            } else {
                check("the thawed logbook reads over several pages and each fits the sheet", false, mc.screen);
            }
            shot(mc, "126b_read_logbook");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.story.Readables.open(sp, new ItemStack(com.rtshop.item.ModItems.OATH_SCROLL.get())));
            waitTicks(15);
        });
        add(mc -> {
            check("the oath scroll opens to read", mc.screen instanceof ReadScreen r && r.pagesForTest() == 1 && r.fitsForTest(), mc.screen);
            shot(mc, "126c_read_oath");
            mc.setScreen(null);
            onServer(mc, sp -> {
                for (String q : READ_QUESTS) com.rtshop.quest.QuestManager.setCompletedForTest(sp, q, Boolean.TRUE.equals(READ_HAD.get(q)));
            });
            waitTicks(5);
        });
        // 도감: 마주친 몹·잡은 몹·지닌 장비가 적힌다
        add(mc -> {
            onServer(mc, sp -> {
                com.rtshop.story.Codex.resetForTest(sp);
                net.minecraft.server.level.ServerLevel lv = sp.serverLevel();
                for (net.minecraft.world.entity.EntityType<?> t : java.util.List.of(net.minecraft.world.entity.EntityType.ZOMBIE, net.minecraft.world.entity.EntityType.SKELETON,
                        net.minecraft.world.entity.EntityType.CREEPER, net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.WITHER)) {
                    if (t.create(lv) instanceof net.minecraft.world.entity.LivingEntity le) {
                        com.rtshop.story.Codex.meet(sp, le);
                        if (t != net.minecraft.world.entity.EntityType.CREEPER) com.rtshop.story.Codex.onKill(sp, le);
                    }
                }
                sp.getInventory().add(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                com.rtshop.story.Codex.open(sp);
            });
            waitTicks(20);
        });
        add(mc -> {
            if (mc.screen instanceof CodexScreen c) {
                boolean picked = c.selectForTest("minecraft:zombie");
                check("the codex opens with every mob, boss and piece of gear listed; what was met or slain is known and the rest is locked",
                        c.totalForTest(0) >= 30 && c.totalForTest(1) >= 2 && c.totalForTest(2) >= 50 && c.knownForTest(0) >= 4 && c.knownForTest(1) >= 1
                                && c.knownForTest(2) >= 1 && c.stateForTest("minecraft:creeper") == 1 && c.stateForTest("minecraft:zombie") == 2
                                && c.stateForTest("minecraft:spider") == 0 && picked,
                        c.totalForTest(0) + "/" + c.totalForTest(1) + "/" + c.totalForTest(2) + " known " + c.knownForTest(0) + "/" + c.knownForTest(1) + "/" + c.knownForTest(2));
            } else {
                check("the codex opens with every mob, boss and piece of gear listed; what was met or slain is known and the rest is locked", false, mc.screen);
            }
            waitTicks(20);
        });
        add(mc -> {
            check("picking a slain mob shows its stats and drops", CodexScreen.hasDetailForTest("minecraft:zombie"));
            shot(mc, "128a_codex_mob");
            if (mc.screen instanceof CodexScreen c) {
                c.knownOnlyForTest(true);
                int knownShown = c.shownForTest();
                c.searchForTest("zzzz-no-such-name");
                int none = c.shownForTest();
                c.searchForTest("");
                c.knownOnlyForTest(false);
                c.tabForTest(2);
                boolean sword = c.selectForTest("minecraft:iron_sword");
                check("the codex can show only what is known, search by name and list gear", knownShown >= 4 && knownShown < c.totalForTest(0) && none == 0 && sword && c.modsForTest() >= 1,
                        knownShown + " / " + none + " / " + sword);
            }
            waitTicks(10);
        });
        add(mc -> {
            shot(mc, "128b_codex_gear");
            if (mc.screen instanceof CodexScreen c) {
                c.tabForTest(1);
                c.selectForTest("minecraft:wither");
            }
            waitTicks(20);
        });
        add(mc -> {
            shot(mc, "128c_codex_boss");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.story.Codex.resetForTest(sp));
            waitTicks(5);
        });
        // 안내서(가이드북): 갈래별 쪽, 그림과 글, 낱말로 찾기
        add(mc -> {
            onServer(mc, sp -> com.rtshop.story.Guide.open(sp));
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof GuideScreen gs) {
                int all = gs.pagesForTest();
                boolean bank = gs.selectForTest("Gold 와 은행");
                gs.searchForTest("귀환권");
                int found = gs.rowsForTest();
                gs.searchForTest("zzzz-no-such-word");
                int none = gs.rowsForTest();
                gs.searchForTest("");
                gs.selectForTest("장비 강화");
                check("the guidebook opens with its pages by topic, the titles fit the list, and a word search narrows the pages",
                        all >= 20 && bank && found >= 1 && found < all && none == 0 && gs.rowsForTest() == all && gs.fitsForTest(),
                        all + " / " + found + " / " + none + " / " + gs.fitsForTest());
            } else {
                check("the guidebook opens with its pages by topic, the titles fit the list, and a word search narrows the pages", false, mc.screen);
            }
            waitTicks(10);
        });
        add(mc -> {
            shot(mc, "129a_guide_enhance");
            if (mc.screen instanceof GuideScreen gs) gs.selectForTest("조사대의 도구");
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "129b_guide_tools");
            mc.setScreen(null);
            waitTicks(5);
        });
        // 지도 표시 (3.54): 서버가 알려 준 NPC·장소가 지도 모드(Diorama)의 웨이포인트로 들어간다. 지도 모드가 없으면 아무 일도 없다.
        add(mc -> {
            onServer(mc, sp -> {
                com.rtshop.story.MapMarks.forgetAll(sp);
                com.rtshop.story.MapMarks.mark(sp, "test:mark", "RT 점검 표시", sp.level().dimension().location().toString(),
                        sp.blockPosition().getX() + 5, sp.blockPosition().getY(), sp.blockPosition().getZ(), 0xFFE8C56A);
            });
            waitTicks(70);
        });
        add(mc -> {
            String folder = com.rtshop.story.MapMarks.get().folder;
            int first = MapMarkClient.countForTest(folder, "RT 점검 표시");
            MAP_FIRST = first;
            // 같은 표시를 다시 받아도 늘지 않고, 자리가 바뀌면 있던 것을 고친다
            onServer(mc, sp -> {
                com.rtshop.story.MapMarks.sendAll(sp, false);
                com.rtshop.story.MapMarks.mark(sp, "test:mark", "RT 점검 표시", sp.level().dimension().location().toString(),
                        sp.blockPosition().getX() + 40, sp.blockPosition().getY(), sp.blockPosition().getZ(), 0xFFE8C56A);
            });
            waitTicks(50);
        });
        add(mc -> {
            String folder = com.rtshop.story.MapMarks.get().folder;
            boolean has = net.minecraftforge.fml.ModList.get().isLoaded("diorama");
            int second = MapMarkClient.countForTest(folder, "RT 점검 표시");
            if (has) {
                check("map marks land in the map mod's waypoint list (folder " + folder + "): added once, not doubled when sent again, moved in place",
                        MAP_FIRST == 1 && second == 1 && MapMarkClient.addedForTest >= 1 && MapMarkClient.movedForTest >= 1 && MapMarkClient.errorForTest.isEmpty(),
                        MAP_FIRST + " " + second + " added " + MapMarkClient.addedForTest + " moved " + MapMarkClient.movedForTest + " " + MapMarkClient.errorForTest);
                // C-8: 이 폴더의 표시는 세계 안에는 그리지 않는다 (M 지도에서만). Diorama 의 세계 안 표시가 우리 것을 건너뛴 횟수로 확인한다
                check("the town's map marks are left out of the map mod's in-world display (they show on the M map only)",
                        MapMarkClient.hiddenInWorldForTest > 0 && !MapMarkClient.hideInWorld("다른 폴더") && MapMarkClient.hideInWorld(folder),
                        MapMarkClient.hiddenInWorldForTest);
            } else {
                check("without the map mod, map marks are ignored quietly", !MapMarkClient.available() && second == -1 && MapMarkClient.addedForTest == 0);
            }
            // 점검용 표시를 지도에서 다시 뺀다
            onServer(mc, sp -> {
                com.rtshop.story.MapMarks.Config c = com.rtshop.story.MapMarks.get();
                com.rtshop.story.MapMarks.forgetAll(sp);
                try {
                    com.rtshop.net.ShopNet.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new com.rtshop.net.StoryPacket(
                            com.rtshop.net.StoryPacket.MAP_MARKS, "{\"folder\":\"" + c.folder + "\",\"marks\":[],\"gone\":[\"test:mark\"],\"notify\":false,\"force\":false}"));
                } catch (Throwable ignored) {
                }
            });
            waitTicks(40);
        });
        add(mc -> {
            if (net.minecraftforge.fml.ModList.get().isLoaded("diorama")) {
                int left = MapMarkClient.countForTest(com.rtshop.story.MapMarks.get().folder, "RT 점검 표시");
                check("a mark the server reports as gone is taken off the map again", left == 0 && MapMarkClient.removedForTest >= 1, left + " removed " + MapMarkClient.removedForTest);
            }
            waitTicks(5);
        });
        // 미니맵의 NPC 아이콘 (3.58): 말을 걸어 본 NPC 만 뜨고, 설정으로 모두 끈다
        add(mc -> {
            net.minecraft.world.entity.npc.Villager rv = net.minecraft.world.entity.EntityType.VILLAGER.create(mc.level);
            net.minecraft.world.entity.npc.Villager wild = net.minecraft.world.entity.EntityType.VILLAGER.create(mc.level);
            if (rv != null && wild != null) {
                rv.setNoAi(true);
                rv.setCustomName(net.minecraft.network.chat.Component.literal("RT radar test"));
                MapMarkClient.learnForTest("{\"radar\":false,\"marks\":[],\"gone\":[]}");
                boolean before = MapMarkClient.hideOnRadar(rv);          // 서버가 "말을 걸어 본 NPC 만" 을 켜지 않았으면 가리지 않는다
                MapMarkClient.learnForTest("{\"radar\":true,\"marks\":[],\"gone\":[]}");
                boolean unmet = MapMarkClient.hideOnRadar(rv);
                MapMarkClient.learnForTest("{\"radar\":true,\"marks\":[{\"id\":\"npc:" + rv.getUUID() + "\"}],\"gone\":[]}");
                boolean met = !MapMarkClient.hideOnRadar(rv);
                ClientConfig.get().minimapNpcIcons = false;
                boolean off = MapMarkClient.hideOnRadar(rv);
                ClientConfig.get().minimapNpcIcons = true;
                boolean others = !MapMarkClient.hideOnRadar(wild) && !MapMarkClient.hideOnRadar(mc.player);
                MapMarkClient.learnForTest("{\"radar\":true,\"marks\":[],\"gone\":[\"npc:" + rv.getUUID() + "\"]}");
                check("minimap icons: an RT Shop NPC shows only after it has been talked to, the setting hides them all, and ordinary villagers and players are never hidden",
                        !before && unmet && met && off && others && MapMarkClient.hideOnRadar(rv), before + " " + unmet + " " + met + " " + off + " " + others);
                // 지도 모드가 있으면: 말을 걸지 않은 NPC 를 곁에 세워 두고, 미니맵이 그 아이콘을 그리려 할 때 우리에게 묻는지 본다
                if (net.minecraftforge.fml.ModList.get().isLoaded("diorama") && mc.player != null) {
                    rv.setPos(mc.player.getX() + 3, mc.player.getY(), mc.player.getZ());
                    mc.level.putNonPlayerEntity(rv.getId(), rv);
                    RADAR_NPC = rv;
                    MapMarkClient.hiddenOnRadarForTest = 0;
                }
            }
            waitTicks(40);
        });
        add(mc -> {
            if (RADAR_NPC != null) {
                // 지도 모드의 설정(미니맵·몹 표시)이 꺼져 있으면 0 일 수 있어, 횟수만 적어 둔다
                check("info: the map mod's minimap asked RT Shop about an NPC icon this many times while an unmet NPC stood near", true, MapMarkClient.hiddenOnRadarForTest);
                mc.level.removeEntity(RADAR_NPC.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                RADAR_NPC = null;
            }
            MapMarkClient.learnForTest("{\"radar\":false,\"marks\":[],\"gone\":[]}");
            waitTicks(2);
        });
        // 칭호: 쌓은 기록만큼 열리고, 힘 쓰는 칭호와 내거는 칭호를 고른다
        add(mc -> {
            com.rtshop.rank.Ranks.testAnywhere = true;
            onServer(mc, sp -> {
                com.rtshop.rank.Ranks.resetForTest(sp);
                com.rtshop.rank.Ranks.add(sp, com.rtshop.rank.Ranks.KILLS, 120);
                com.rtshop.rank.Ranks.add(sp, com.rtshop.rank.Ranks.BOSSES, 3);
                com.rtshop.rank.Ranks.earned(sp, 34500);
                com.rtshop.rank.Ranks.spent(sp, 12000);
                com.rtshop.rank.Ranks.add(sp, com.rtshop.rank.Ranks.QUESTS, 12);
                com.rtshop.rank.Ranks.openTitles(sp);
            });
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof TitleScreen t) {
                check("the title keeper's screen lists every category, shows the titles earned so far as open and nothing is cut off",
                        t.catsForTest() == com.rtshop.rank.Ranks.CATS.length && t.openForTest() >= 6 && t.fitsForTest() && t.effectsForTest() == 0,
                        t.catsForTest() + " / " + t.openForTest() + " / " + t.fitsForTest());
                t.selectForTest("kills");
                t.effectForTest("kills_2");
                t.showForTest("bosses_1");
            } else {
                check("the title keeper's screen lists every category, shows the titles earned so far as open and nothing is cut off", false, mc.screen);
            }
            waitTicks(15);
        });
        add(mc -> {
            check("a title can be put to use and another one shown in front of the name from the screen",
                    mc.screen instanceof TitleScreen t && t.effectsForTest() == 1 && t.shownForTest().equals("bosses_1"), mc.screen);
            shot(mc, "127a_titles");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.rank.Ranks.openRanks(sp));
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof RankScreen r) {
                r.selectForTest("kills");
                check("the ranking clerk's screen lists every category with this week's ranking and nothing is cut off",
                        r.catsForTest() == com.rtshop.rank.Ranks.CATS.length && r.rowsForTest() >= 1 && r.fitsForTest() && r.pendingForTest() == 0,
                        r.catsForTest() + " / " + r.rowsForTest() + " / " + r.fitsForTest());
            } else {
                check("the ranking clerk's screen lists every category with this week's ranking and nothing is cut off", false, mc.screen);
            }
            shot(mc, "127b_ranks");
            onServer(mc, sp -> {
                com.rtshop.rank.Ranks.settle(sp.server);
                com.rtshop.rank.Ranks.openRanks(sp);
            });
            waitTicks(15);
        });
        add(mc -> {
            if (mc.screen instanceof RankScreen r) {
                r.selectForTest("kills");
                r.lastWeekForTest(true);
                check("after the week closes the clerk shows last week's ranking and holds a reward to collect",
                        r.rowsForTest() >= 1 && r.pendingForTest() > 0, r.rowsForTest() + " / " + r.pendingForTest());
                shot(mc, "127c_ranks_last_week");
                r.claimForTest();
            } else {
                check("after the week closes the clerk shows last week's ranking and holds a reward to collect", false, mc.screen);
            }
            waitTicks(15);
        });
        add(mc -> {
            check("the weekly reward is collected from the clerk", mc.screen instanceof RankScreen r && r.pendingForTest() == 0, mc.screen);
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.rank.Ranks.resetForTest(sp));
            com.rtshop.rank.Ranks.testAnywhere = false;
            waitTicks(5);
        });
        add(mc -> {
            com.rtshop.story.Hunts.testMode = true;
            onServer(mc, sp -> {
                com.rtshop.story.Hunts.clearForTest(sp);
                com.rtshop.story.Hunts.openBoard(sp, -1);
            });
            waitTicks(20);
        });
        add(mc -> {
            BountyScreen bs = mc.screen instanceof BountyScreen x ? x : null;
            check("the bounty board opens with bounties to pick from", bs != null && bs.rowsForTest() > 0 && bs.takenForTest() == 0, mc.screen);
            if (bs != null) bs.takeFirstForTest();
            waitTicks(20);
        });
        add(mc -> {
            BountyScreen bs = mc.screen instanceof BountyScreen x ? x : null;
            check("taking a bounty on the board marks it as taken", bs != null && bs.takenForTest() == 1, bs == null ? mc.screen : bs.takenForTest());
            shot(mc, "117_bounty_board");
            check("RT Shop windows use the chosen window font (Noto Sans by default), not the game's pixel font",
                    bs != null && UiFont.get() != mc.font && bs.getFontForTest() == UiFont.get() && UiFont.get().width("가나다") > 0,
                    UiFont.id());
            ClientConfig.get().uiFont = "serif";
            ClientConfig.get().uiFontSize = 2;
            UiFont.changed();
            waitTicks(6);
        });
        add(mc -> {
            BountyScreen bs = mc.screen instanceof BountyScreen x ? x : null;
            check("switching to the serif font in large size rebuilds the open window with it",
                    bs != null && bs.getFontForTest() == UiFont.get() && "rtshop:ui_serif_l".equals(String.valueOf(UiFont.id())), UiFont.id());
            shot(mc, "117b_bounty_serif_large");
            ClientConfig.get().uiFont = "sans";
            ClientConfig.get().uiFontSize = 1;
            UiFont.changed();
            mc.setScreen(null);
            com.rtshop.story.Hunts.testMode = false;
            onServer(mc, com.rtshop.story.Hunts::clearForTest);
            waitTicks(2);
        });
        // ------------------------------------------------------------ Nightfall 무기를 든 모습 (풀팩에서만): 폐허 대검과 비교용 다른 대검
        if (net.minecraftforge.fml.ModList.get().isLoaded("efn")) {
            String[][] held = {{"efn:ruinsgreatsword", "119_efn_ruinsgreatsword"}, {"minecraft:diamond_sword", "119b_diamond_sword"}, {"efn:yamato_dmc", "119c_efn_yamato"}, {"efn:kusabimaru", "119d_efn_kusabimaru"}, {"efn:broadblade", "119e_efn_broadblade"}, {"efn:ruinsgreatsword", "119f_efn_ruinsgreatsword_zoom"}, {"efn:meen_spear", "119g_efn_duskfire"}};
            for (String[] h : held) {
                add(mc -> {
                    mc.setScreen(null);
                    onServer(mc, sp -> {
                        var it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(h[0]));
                        if (it != null) sp.getInventory().setItem(sp.getInventory().selected, new net.minecraft.world.item.ItemStack(it));
                        sp.setYRot(180);
                        sp.setXRot(10);
                    });
                    mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                    // 마지막 사진은 가까이서 (시야각을 좁혀 확대)
                    mc.options.fov().set(h[1].endsWith("_zoom") ? 30 : 70);
                    RenderProbe.reset();
                    waitTicks(30);
                });
                add(mc -> {
                    shot(mc, h[1]);
                    com.rtshop.RtShop.LOGGER.info("[rtshop-clienttest] held {}: {}", h[0], mc.player == null ? "-" : mc.player.getMainHandItem());
                    com.rtshop.RtShop.LOGGER.info("[rtshop-clienttest] EF renderer for {}: {}", h[0], efRendererInfo(mc.player == null ? null : mc.player.getMainHandItem()));
                    String calls = RenderProbe.dump();
                    com.rtshop.RtShop.LOGGER.info("[rtshop-clienttest] EF renderer calls while holding {}: {}", h[0], calls);
                    if (h[1].equals("119_efn_ruinsgreatsword") || h[1].equals("119g_efn_duskfire")) {
                        check("Nightfall's " + h[0] + " is drawn as Epic Fight's 3D model, not Better Combat's flat item",
                                calls.contains("ef:RenderMeshItem") || calls.contains("ef:RenderAnimationItem") ? !calls.contains("vanilla-layer:PlayerItemInHandLayer:" + h[0]) : false, calls);
                    }
                    waitTicks(2);
                });
            }
            add(mc -> {
                mc.options.fov().set(70);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                onServer(mc, sp -> sp.getInventory().setItem(sp.getInventory().selected, net.minecraft.world.item.ItemStack.EMPTY));
                waitTicks(5);
            });
        }
        if (net.minecraftforge.fml.ModList.get().isLoaded("efn")) {
            add(mc -> {
                for (String id : new String[]{"efn:ruinsgreatsword", "efn:yamato_dmc", "efn:kusabimaru", "efn:hf_murasama", "efn:broadblade", "wom:moonless", "epicfight:diamond_greatsword"}) {
                    var it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id));
                    com.rtshop.RtShop.LOGGER.info("[rtshop-clienttest] EF renderer for {}: {}", id,
                            it == null || it == net.minecraft.world.item.Items.AIR ? "(no item)" : efRendererInfo(new net.minecraft.world.item.ItemStack(it)));
                }
                waitTicks(1);
            });
        }
        // ------------------------------------------------------------ 이야기로 열리는 장비 진열대
        add(mc -> {
            check("the server tells the client which gear tiers wait for which chapter (rare: middle of chapter 1, epic: chapter 1)",
                    "1장 중반".equals(StoryClient.GATE_RARE) && "1장".equals(StoryClient.GATE_EPIC), StoryClient.GATE_RARE + "/" + StoryClient.GATE_EPIC);
            net.minecraft.world.item.ItemStack sw = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            sw.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 1);
            boolean creative = mc.player.isCreative();
            check("a rare sword shows as locked until chapter 1 (plain gear is open)",
                    creative || "1장 중반".equals(StoryClient.lockedChapter(sw)) && StoryClient.lockedChapter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD)) == null,
                    creative + " " + StoryClient.lockedChapter(sw));
        });
        // ------------------------------------------------------------ 이야기 연출: 장 제목, 보스 등장(테마별), 환영
        add(mc -> {
            mc.setScreen(null);
            Cinematic.play("{\"kind\":\"title\",\"chapter\":\"제 1 장\",\"title\":\"잿불의 서약\",\"subtitle\":\"베스탈리아, 수도회의 마지막 거점\",\"color\":\"FF7A20\",\"theme\":\"ember\"}");
            Cinematic.timeForTest = 2.4F;
            waitTicks(6);
        });
        add(mc -> {
            check("a chapter title card plays", Cinematic.active() && "title".equals(Cinematic.kindForTest()));
            shot(mc, "110_story_chapter_title");
            com.rtshop.story.StoryConfig.Intro in = com.rtshop.story.StoryConfig.defaults().get("cataclysm:ignis");
            Cinematic.play(com.rtshop.story.BossIntros.cinema(in).toString());
            Cinematic.timeForTest = 2.6F;
            waitTicks(6);
        });
        add(mc -> {
            check("a boss entrance plays with the boss's name and epithet", Cinematic.active() && "boss".equals(Cinematic.kindForTest()));
            shot(mc, "111_boss_intro_ignis");
            Cinematic.play(com.rtshop.story.BossIntros.cinema(com.rtshop.story.StoryConfig.defaults().get("mowziesmobs:frostmaw")).toString());
            Cinematic.timeForTest = 2.2F;
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "111b_boss_intro_frostmaw");
            Cinematic.play(com.rtshop.story.BossIntros.cinema(com.rtshop.story.StoryConfig.defaults().get("cataclysm:the_leviathan")).toString());
            Cinematic.timeForTest = 2.2F;
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "111c_boss_intro_leviathan");
            Cinematic.play(com.rtshop.story.BossIntros.cinema(com.rtshop.story.StoryConfig.defaults().get("cataclysm:ender_guardian")).toString());
            Cinematic.timeForTest = 2.2F;
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "111d_boss_intro_ender_guardian");
            Cinematic.play("{\"kind\":\"vision\",\"speaker\":\"꿈속의 기사\",\"text\":\"또 왔구나, 견습이여.\\n이 불씨를 쥐어라. 언젠가 네가 지켜야 할 것이다.\",\"color\":\"FF7A20\"}");
            Cinematic.timeForTest = 3.2F;
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "112_story_vision");
            Cinematic.play("{\"kind\":\"names\",\"title\":\"베스탈리아 연대기 · 잠든 이름\",\"speaker\":\"기록관 이사벨\",\"text\":\"헬바르, 서약을 거절한 기사\\n하르겐 · 이졸데 · 메르윈\\n그리고 이름을 내어 준 여덟째 왕\"}");
            Cinematic.timeForTest = 4.3F;
            waitTicks(6);
        });
        add(mc -> {
            check("the name-writing scene plays (names are penned line by line)", Cinematic.active() && "names".equals(Cinematic.kindForTest()));
            shot(mc, "112b_story_names");
            Cinematic.timeForTest = -1;
            Cinematic.play("{\"kind\":\"line\",\"seconds\":0.01}");
            waitTicks(4);
        });
        // ------------------------------------------------------------ 장소 조사 · 동행
        add(mc -> {
            mc.setScreen(null);
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            mc.options.fov().set(70);
            onServer(mc, sp -> {
                com.rtshop.quest.QuestManager.NpcDef n = new com.rtshop.quest.QuestManager.NpcDef();
                n.id = "t_comp";
                n.name = "기록관 이사벨";
                com.rtshop.quest.QuestManager.loaded().npcs.put(n.id, n);
                com.rtshop.quest.QuestDef q = com.rtshop.quest.QuestDef.parse(com.google.gson.JsonParser.parseString(
                        "{\"id\":\"t_inspect\",\"giver\":\"quest_guide\",\"objectives\":[{\"type\":\"INSPECT\",\"target\":\"t_circle\",\"shape\":\"circle\","
                                + "\"text\":\"그을린 마법진\",\"lines\":[\"바닥에 원이 그을려 있다. 누군가 여기서 무언가를 불렀다.\",\"원 한가운데에 지워진 글자가 하나 있다.\"]}],"
                                + "\"companion\":{\"npc\":\"t_comp\",\"style\":\"melee\",\"lines\":{\"arrive\":[\"여기예요. 발밑을 조심해요.\"],\"spot\":[\"저기, 바닥에 뭔가 있어요.\"]}}}").getAsJsonObject());
                com.rtshop.quest.QuestManager.loaded().quests.put(q.id, q);
                sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0F, 14F);
                boolean ok = com.rtshop.quest.QuestManager.accept(sp, "t_inspect", null);
                com.rtshop.RtShop.LOGGER.info("[rtshop-clienttest] inspect quest accepted: {}", ok);
                com.rtshop.story.Spots.placeForTest(sp, "t_circle", sp.blockPosition().offset(0, 0, 3));
                com.rtshop.story.Spots.update(sp);
            });
            waitTicks(50);
        });
        add(mc -> {
            check("the trace to look at is shown in the world and can be aimed at", SpotClient.countForTest() == 1 && "t_circle".equals(SpotClient.aimedForTest()),
                    SpotClient.countForTest() + " / " + SpotClient.aimedForTest());
            boolean comp = false;
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof com.rtshop.story.CompanionEntity) comp = true;
            }
            check("the companion walks beside the player once the quest place is reached", comp);
            shot(mc, "124_spot_marker");
            onServer(mc, sp -> sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 150F, 5F));
            mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            waitTicks(25);
        });
        add(mc -> {
            shot(mc, "124b_companion");
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            onServer(mc, sp -> sp.connection.teleport(sp.getX(), sp.getY(), sp.getZ(), 0F, 14F));
            waitTicks(10);
        });
        add(mc -> {
            SpotClient.look("t_circle");
            waitTicks(20);
        });
        add(mc -> {
            check("looking closer opens what is written there", mc.screen instanceof DialogueScreen, mc.screen);
            shot(mc, "124c_spot_text");
            if (mc.player != null) mc.player.connection.sendCommand("rtshop spot done t_circle");
            mc.setScreen(null);
            waitTicks(30);
        });
        add(mc -> {
            check("after the look is finished the trace is gone", SpotClient.countForTest() == 0, SpotClient.countForTest());
            if (mc.player != null) mc.player.connection.sendCommand("rtshop notebook");
            waitTicks(15);
        });
        add(mc -> {
            check("the field notebook opens with the trace just examined", mc.screen instanceof NotebookScreen n && n.countForTest() == 1, mc.screen);
            shot(mc, "124d_notebook");
            mc.setScreen(null);
            onServer(mc, sp -> {
                com.rtshop.quest.QuestManager.abandon(sp, "t_inspect");
                com.rtshop.quest.QuestManager.loaded().quests.remove("t_inspect");
                com.rtshop.quest.QuestManager.loaded().npcs.remove("t_comp");
            });
            waitTicks(30);
        });
        // 3.61: NPC 외형(사람 모습) · 보스 등장의 3D 카메라 · 민원 창
        add(mc -> {
            LOOK_IDS.clear();
            onServer(mc, sp -> {
                net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                double fx = look.x, fz = look.z, len = Math.max(0.01, Math.sqrt(fx * fx + fz * fz));
                fx /= len;
                fz /= len;
                net.minecraft.world.phys.Vec3 front = sp.position().add(fx * 3.2, 0, fz * 3.2);
                String[] who = {"order_archivist", "bram", "order_elder"};
                for (int i361 = 0; i361 < who.length; i361++) {
                    double side = (i361 - 1) * 1.6;
                    var v361 = com.rtshop.quest.QuestManager.spawnNpc(sp.serverLevel(), front.add(-fz * side, 0, fx * side), sp.getYRot() + 180, who[i361]);
                    if (v361 != null) LOOK_IDS.add(v361.getId());
                }
            });
            waitTicks(60);
        });
        add(mc -> {
            LOOK_DRAWN = NpcLookClient.drawnForTest;
            check("3.61 npc looks: the server tells the screen which NPC wears which look, and the look files load",
                    LOOK_IDS.size() == 3 && NpcLookClient.knownForTest() >= 3 && NpcLookClient.quadsForTest("isabel") > 100 && NpcLookClient.quadsForTest("isabel") < 600 && NpcLookClient.quadsForTest("no_such_look") == -1,
                    LOOK_IDS + " known=" + NpcLookClient.knownForTest() + " isabel=" + NpcLookClient.quadsForTest("isabel"));
            waitTicks(10);
        });
        add(mc -> {
            check("3.61 npc looks: those NPCs are drawn as people instead of villagers", NpcLookClient.drawnForTest > LOOK_DRAWN, NpcLookClient.drawnForTest - LOOK_DRAWN);
            check("3.63 npc looks are cheap to draw: a figure sends at most a few hundred quads, and the faces turned away from the camera are skipped",
                    NpcLookClient.lastQuadsForTest > 20 && NpcLookClient.lastQuadsForTest < 400 && NpcLookClient.lastCulledForTest > 20,
                    NpcLookClient.lastQuadsForTest + " sent / " + NpcLookClient.lastCulledForTest + " skipped");
            check("3.64 npc name tags are drawn only up close: within the set distance yes, beyond it no (0 = as far as before, never past 64 blocks), and the NPCs standing right here have theirs",
                    NpcLookClient.nameShownAt(4.0 * 4.0, 16) && NpcLookClient.nameShownAt(16.0 * 16.0, 16) && !NpcLookClient.nameShownAt(17.0 * 17.0, 16)
                            && NpcLookClient.nameShownAt(40.0 * 40.0, 0) && !NpcLookClient.nameShownAt(70.0 * 70.0, 0) && !NpcLookClient.nameShownAt(70.0 * 70.0, 64)
                            && ClientConfig.get().npcNameDistance == 16 && NpcLookClient.namesDrawnForTest > 0,
                    ClientConfig.get().npcNameDistance + " drawn=" + NpcLookClient.namesDrawnForTest + " skipped=" + NpcLookClient.namesSkippedForTest);
            shot(mc, "131_npc_looks");
            int cam361 = LOOK_IDS.isEmpty() ? -1 : LOOK_IDS.get(LOOK_IDS.size() - 1);
            Cinematic.play("{\"kind\":\"boss\",\"name\":\"카메라 점검\",\"epithet\":\"3D 등장 연출\",\"color\":\"FF7A20\",\"shake\":0,\"seconds\":4,\"cam\":\"spiral\",\"entity\":" + cam361 + "}");
            waitTicks(30);
        });
        add(mc -> {
            check("3.61 boss camera: during a boss entrance the camera leaves the player's eyes and stands near the boss",
                    BossCam.active() && mc.getCameraEntity() != mc.player && BossCam.startsForTest > 0 && BossCam.styleForTest().equals("spiral")
                            && BossCam.STYLES.contains(com.rtshop.story.BossIntros.camOf("cataclysm:the_harbinger")), BossCam.active() + " " + BossCam.styleForTest() + " " + BossCam.lastForTest);
            shot(mc, "132_boss_cam");
            waitTicks(75);
        });
        add(mc -> {
            check("3.61 boss camera: when the entrance ends the camera is back in the player's eyes", !BossCam.active() && mc.getCameraEntity() == mc.player, mc.getCameraEntity());
            onServer(mc, sp -> {
                for (int id361 : LOOK_IDS) {
                    net.minecraft.world.entity.Entity e361 = sp.serverLevel().getEntity(id361);
                    if (e361 != null) e361.discard();
                }
                com.rtshop.petition.Petitions.forTestReset(sp.server);
                int first361 = com.rtshop.petition.Petitions.file(sp, "bug", "상점 창을 닫았다 열면 검색어가 남아 있어요.");
                com.rtshop.petition.Petitions.answer(sp.server, first361, "운영자", "확인했습니다. 다음 판에서 고치겠습니다.");
                com.rtshop.petition.Petitions.forTestNoCooldown();
                com.rtshop.petition.Petitions.file(sp, "land", "옆 토지와 경계가 한 칸 겹칩니다.");
                com.rtshop.petition.Petitions.open(sp, "", false);
            });
            waitTicks(20);
        });
        add(mc -> {
            check("3.61 reports: the clerk's window opens with the categories and a place to write", mc.screen instanceof PetitionScreen ps361 && ps361.rowsForTest() >= 5, mc.screen);
            shot(mc, "133_petition_new");
            if (mc.screen instanceof PetitionScreen pt361) pt361.tabForTest(1);
            waitTicks(6);
        });
        add(mc -> {
            shot(mc, "133b_petition_mine");
            mc.setScreen(null);
            onServer(mc, sp -> com.rtshop.petition.Petitions.forTestReset(sp.server));
            waitTicks(10);
        });
    }

    /** Epic Fight 가 이 아이템에 쓰는 렌더러 (Avalon mesh_item 이면 메쉬가 실제로 읽혔는지도). */
    static String efRendererInfo(net.minecraft.world.item.ItemStack st) {
        if (st == null || st.isEmpty()) return "(empty)";
        try {
            Class<?> ce = Class.forName("yesman.epicfight.client.ClientEngine");
            Object engine = ce.getMethod("getInstance").invoke(null);
            Object re = ce.getField("renderEngine").get(engine);
            Object r = re.getClass().getMethod("getItemRenderer", net.minecraft.world.item.ItemStack.class).invoke(re, st);
            StringBuilder b = new StringBuilder(r == null ? "null" : r.getClass().getName());
            if (r != null) {
                for (Class<?> c = r.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                        f.setAccessible(true);
                        Object v = f.get(r);
                        String vs;
                        if (v != null && v.getClass().getName().contains("Accessor")) {
                            Object got;
                            try {
                                got = v.getClass().getMethod("get").invoke(v);
                            } catch (Throwable t) {
                                got = "ERR " + (t.getCause() != null ? t.getCause() : t);
                            }
                            vs = v + " -> " + (got == null ? "null" : got.getClass().getSimpleName());
                        } else {
                            vs = String.valueOf(v);
                        }
                        if (vs.length() > 120) vs = vs.substring(0, 120);
                        b.append(" ").append(f.getName()).append("=").append(vs);
                    }
                }
            }
            return b.toString();
        } catch (Throwable t) {
            return "ERR " + t;
        }
    }
}
