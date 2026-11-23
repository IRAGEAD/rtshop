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
    private ClientSelfTest2() {
    }

    interface Step {
        void run(Minecraft mc);
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static volatile int npc = -1;
    static volatile int qnpc = -1;
    private static volatile int landId = -1;
    private static volatile int farmA = -1, farmB = -1;
    private static volatile int dishIdx = -1;
    private static volatile String lifeJob = "";

    static int count() {
        return STEPS.size();
    }

    static void run(Minecraft mc, int k) {
        STEPS.get(k).run(mc);
    }

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
            boolean ok = mc.screen instanceof QuestScreen qs && qs.hasBackForTest();
            check("the quest window opened from a merchant has a [Back] button", ok, mc.screen);
            shot(mc, "95_quest_back_button");
            if (mc.screen instanceof QuestScreen qs && qs.hasBackForTest()) qs.goBack();
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
            check("the border is a low outline along the ground (no wall into the sky)", BorderRenderer.maxRiseForTest() < 12, BorderRenderer.maxRiseForTest());
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
                com.rtshop.raid.AbyssWeekly.cleared(sp, 1);
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
        add(mc -> {
            shot(mc, "118_font_calib");
            mc.setScreen(null);
            // 퀘스트 NPC(기록관 이사벨)도 우클릭하면 대화 상자가 먼저 열린다
            qnpc = -1;
            DialogueScreen.autoMainForTest = false;
            onServer(mc, sp -> {
                net.minecraft.world.phys.Vec3 look = sp.getLookAngle();
                var v = com.rtshop.quest.QuestManager.spawnNpc(sp.serverLevel(), sp.position().add(look.x * 2, 0, look.z * 2), sp.getYRot() + 180,
                        "quest_order_archivist");
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
            }
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
            Cinematic.play("{\"kind\":\"title\",\"chapter\":\"제 1 장\",\"title\":\"잿불의 서약\",\"subtitle\":\"골디아, 수도회의 마지막 거점\",\"color\":\"FF7A20\",\"theme\":\"ember\"}");
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
            Cinematic.timeForTest = -1;
            Cinematic.play("{\"kind\":\"line\",\"seconds\":0.01}");
            waitTicks(4);
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
