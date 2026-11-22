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
    private static volatile int landId = -1;
    private static volatile int farmA = -1, farmB = -1;

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
            check("the border is a low outline along the ground (no wall into the sky)", BorderRenderer.maxRiseForTest() < 3.5, BorderRenderer.maxRiseForTest());
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
                com.rtshop.land.CastlePlots.Def d = com.rtshop.land.CastlePlots.def();
                int bx = d.mapX0 + (int) (d.mapW * 0.70), bz = d.mapZ0 + (int) (d.mapH * 0.52);
                // 밀밭 A: 기울어진 타원, 밀밭 B: 콩 모양
                farmA = plot(sp, "밀밭 A", 133520, bx, bz, 70, 50, (x, z) -> {
                    double u = (x - 35) / 34.0, v = (z - 25 - (x - 35) * 0.25) / 17.0;
                    return u * u + v * v <= 1;
                }).id;
                farmB = plot(sp, "밀밭 B", 98000, bx + 10, bz + 60, 60, 44, (x, z) -> {
                    double u = (x - 30) / 29.0, v = (z - 22) / 21.0;
                    double u2 = (x - 30) / 14.0, v2 = (z - 40) / 12.0;
                    return u * u + v * v <= 1 && u2 * u2 + v2 * v2 > 1;
                }).id;
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
    }
}
