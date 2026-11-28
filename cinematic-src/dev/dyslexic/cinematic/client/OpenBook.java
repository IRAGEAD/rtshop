/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.screens.inventory.BookViewScreen$BookAccess
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.Compat;
import dev.dyslexic.cinematic.client.Anim;
import dev.dyslexic.cinematic.client.BookPages;
import dev.dyslexic.cinematic.client.Fx;
import dev.dyslexic.cinematic.client.Picking;
import dev.dyslexic.cinematic.client.Scene;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

final class OpenBook {
    static final double PAGE_W = 0.4;
    static final double PAGE_H = 0.500763358778626;
    static final double GUTTER = 0.007;
    static final double MARGIN = 0.014;
    static final double COVER = 0.012;
    static final double STACK = 0.032;
    static final double LEAF = 0.0012;
    static final double TURN = 0.62;
    static final double RIFFLE = 0.34;
    static final int COLS = 16;
    static final int ROWS = 6;
    static final double DEEP = 0.42100000000000004;
    static final double TALL = 0.528763358778626;
    static final double THICK = 0.056;
    private static final ResourceLocation BOOK_GUI = new ResourceLocation("textures/gui/book.png");
    private static final float PU0 = 0.2734375f;
    private static final float PU1 = 0.296875f;
    private static final float PV0 = 0.234375f;
    private static final float PV1 = 0.3515625f;
    static final Look LEATHER = new Look("entity/enchanting_table_book", 0.0f, 0.0f, 0.09375f, 0.3125f, 0.0f, 0.0f, 0.09375f, 0.3125f);
    Vec3 origin = Vec3.f_82478_;
    Vec3 a = new Vec3(1.0, 0.0, 0.0);
    Vec3 s = new Vec3(0.0, 1.0, 0.0);
    Vec3 n = new Vec3(0.0, 0.0, 1.0);
    Look look = LEATHER;
    boolean closing;
    double fold;
    int spread = -1;
    private final List<Turn> turns = new ArrayList<Turn>();
    private double peekLeft;
    private double peekRight;
    private double leftShare = 0.5;
    int bookId;
    private List<FormattedText> text = List.of();

    OpenBook() {
    }

    void place(Vec3 origin, Vec3 a, Vec3 s, Vec3 n) {
        this.origin = origin;
        this.a = a;
        this.s = s;
        this.n = n;
    }

    boolean read(ItemStack book) {
        if (book.m_41619_()) {
            return false;
        }
        int id = Compat.hashItem(book);
        if (id == this.bookId) {
            return false;
        }
        this.bookId = id;
        BookViewScreen.BookAccess access = BookViewScreen.BookAccess.m_98308_((ItemStack)book);
        this.text = new ArrayList<FormattedText>();
        for (int i = 0; access != null && i < access.m_5732_(); ++i) {
            this.text.add(access.m_98310_(i));
        }
        this.spread = this.spread < 0 ? -1 : Math.min(this.spread, this.lastSpread());
        return true;
    }

    int lastSpread() {
        return this.text.size() / 2;
    }

    int pageCount() {
        return this.text.size();
    }

    void openAt(int spread) {
        this.spread = Mth.m_14045_((int)spread, (int)0, (int)this.lastSpread());
        this.leftShare = this.share();
    }

    private double share() {
        return this.lastSpread() == 0 ? 0.5 : (double)this.spread / (double)this.lastSpread();
    }

    Turned turn(int dir) {
        Turn last;
        int to = this.spread + dir;
        if (to < 0 || to > this.lastSpread()) {
            return Turned.NO_PAGE;
        }
        if (!this.turns.isEmpty() && ((last = this.turns.get(this.turns.size() - 1)).dir() != dir || last.phase() < 0.28)) {
            return Turned.WAIT;
        }
        this.turns.add(new Turn(Scene.clock(), 0.62, this.spread, dir));
        this.spread = to;
        return Turned.YES;
    }

    void follow(int to) {
        to = Math.min(this.lastSpread(), to);
        if (to != this.spread && this.spread >= 0) {
            this.turns.add(new Turn(Scene.clock(), 0.62, this.spread, to > this.spread ? 1 : -1));
            this.spread = to;
        }
    }

    void riffle(double start) {
        for (int j = 0; j < 3; ++j) {
            this.turns.add(new Turn(start + (double)j * 0.12, 0.34, this.spread - 3 + j, 1));
        }
    }

    boolean turning() {
        return !this.turns.isEmpty();
    }

    void settle(Side hover, boolean idle, double dt) {
        while (!this.turns.isEmpty() && this.turns.get(0).phase() >= 1.0) {
            this.turns.remove(0);
        }
        this.leftShare = Anim.approach(this.leftShare, this.share(), 3.0, dt);
        this.peekRight = Anim.approach(this.peekRight, (idle &= this.turns.isEmpty() && !this.closing) && hover == Side.RIGHT && this.spread < this.lastSpread() ? 1.0 : 0.0, 9.0, dt);
        this.peekLeft = Anim.approach(this.peekLeft, idle && hover == Side.LEFT && this.spread > 0 ? 1.0 : 0.0, 9.0, dt);
    }

    void preparePages() {
        if (this.spread < 0 || this.fold >= 0.999) {
            return;
        }
        LinkedHashSet<Integer> pages = new LinkedHashSet<Integer>();
        for (Leaf leaf : this.leaves()) {
            pages.add(leaf.right());
            pages.add(leaf.left());
        }
        int[] still = this.still();
        pages.add(still[0]);
        pages.add(still[1]);
        if (this.turns.isEmpty()) {
            for (int k = -2; k <= 1; ++k) {
                pages.add(2 * this.spread + k + (k < 0 ? -1 : 1));
            }
        }
        BookPages.prepare(this.bookId, this.text, pages.stream().mapToInt(Integer::intValue).toArray());
    }

    private List<Leaf> leaves() {
        ArrayList<Leaf> leaves = new ArrayList<Leaf>();
        int count = this.turns.size();
        for (int j = 0; j < count; ++j) {
            Turn turn = this.turns.get(j);
            double phase = turn.phase();
            double eased = Anim.inOutCubic(Anim.window(phase, 0.16, 0.94));
            double curl = 0.95 * Anim.hump(Anim.window(phase, 0.0, 0.55)) - 0.55 * Anim.hump(Anim.window(phase, 0.5, 1.0));
            double p = turn.dir() > 0 ? eased : 1.0 - eased;
            double lift = Mth.m_14139_((double)p, (double)(0.0012 * (double)(count - j + 1)), (double)(0.0012 * (double)(j + 2)));
            if (turn.dir() > 0) {
                leaves.add(new Leaf(p, curl, 2 * turn.from(), 2 * turn.from() + 1, lift));
                continue;
            }
            leaves.add(new Leaf(p, -curl, 2 * turn.from() - 2, 2 * turn.from() - 1, lift));
        }
        if (count == 0 && !this.closing) {
            if (this.peekRight > 0.01) {
                leaves.add(new Leaf(0.0, 0.55 * Anim.smoother(this.peekRight), 2 * this.spread, 2 * this.spread + 1, 0.0024));
            }
            if (this.peekLeft > 0.01) {
                leaves.add(new Leaf(1.0, -0.55 * Anim.smoother(this.peekLeft), 2 * this.spread - 2, 2 * this.spread - 1, 0.0024));
            }
        }
        return leaves;
    }

    private int[] still() {
        if (!this.turns.isEmpty()) {
            int[] nArray;
            int from = this.turns.get(0).from();
            if (this.turns.get(0).dir() > 0) {
                int[] nArray2 = new int[2];
                nArray2[0] = 2 * from - 1;
                nArray = nArray2;
                nArray2[1] = 2 * this.spread;
            } else {
                int[] nArray3 = new int[2];
                nArray3[0] = 2 * this.spread - 1;
                nArray = nArray3;
                nArray3[1] = 2 * from;
            }
            return nArray;
        }
        int left = this.peekLeft > 0.01 ? 2 * this.spread - 3 : 2 * this.spread - 1;
        int right = this.peekRight > 0.01 ? 2 * this.spread + 2 : 2 * this.spread;
        return new int[]{left, right};
    }

    Side under() {
        Vec3 unit = this.n.m_82541_();
        Vec3 hit = Picking.cursorOnPlane(this.origin.m_82549_(this.n.m_82490_(0.02)), unit);
        if (hit == null) {
            return Side.NONE;
        }
        Vec3 rel = hit.m_82546_(this.origin);
        double u = rel.m_82526_(this.a) / this.a.m_82556_();
        double v = rel.m_82526_(this.s) / this.s.m_82556_();
        if (Math.abs(v) > 0.280381679389313 || Math.abs(u) > 0.447) {
            return Side.NONE;
        }
        return u < 0.0 ? Side.LEFT : Side.RIGHT;
    }

    Vec3 corner(boolean left) {
        double x = 0.41700000000000004;
        return this.lying(left, x, -0.255381679389313, OpenBook.surface(x, this.stack(left)) + 0.004);
    }

    Vec3 rest(boolean right) {
        return this.lying(!right, 0.36700000000000005, -0.268381679389313, 0.035);
    }

    Vec3 pageCenter(boolean left) {
        double x = 0.20700000000000002;
        return this.lying(left, x, 0.0, OpenBook.surface(x, this.stack(left)));
    }

    Vec3 at(double x, double v, double h) {
        return this.origin.m_82549_(this.a.m_82490_(x)).m_82549_(this.s.m_82490_(v)).m_82549_(this.n.m_82490_(h));
    }

    @Nullable
    Vec3[] turningHands() {
        Vec3[] vec3Array;
        if (this.turns.isEmpty()) {
            return null;
        }
        Turn turn = this.turns.get(this.turns.size() - 1);
        if (turn.length() < 0.62) {
            return null;
        }
        double phase = turn.phase();
        boolean forward = turn.dir() > 0;
        Leaf leaf = this.leaves().get(this.turns.size() - 1);
        Vec3 held = this.leafPoints(leaf)[0][16].m_82549_(this.n.m_82541_().m_82490_(0.025));
        Vec3 home = this.rest(forward);
        double reach = Anim.smoother(Anim.window(phase, 0.0, 0.14));
        double letGo = Anim.smoother(Anim.window(phase, 0.72, 1.0));
        Vec3 working = Anim.lerp(Anim.lerp(this.corner(!forward), held, reach), home, letGo);
        if (forward) {
            Vec3[] vec3Array2 = new Vec3[2];
            vec3Array2[0] = working;
            vec3Array = vec3Array2;
            vec3Array2[1] = this.rest(false);
        } else {
            Vec3[] vec3Array3 = new Vec3[2];
            vec3Array3[0] = this.rest(true);
            vec3Array = vec3Array3;
            vec3Array3[1] = working;
        }
        return vec3Array;
    }

    Vec3 foldCorner() {
        return this.leafPoints(this.foldLeaf())[0][16].m_82549_(this.n.m_82541_().m_82490_(0.025));
    }

    private double stack(boolean left) {
        double share = left ? this.leftShare : 1.0 - this.leftShare;
        return 0.032 * (0.12 + 0.76 * share);
    }

    private static double surface(double x, double stack) {
        double k = Math.min(1.0, x / 0.096);
        return stack * (0.42 + 0.58 * Math.sin(k * Math.PI / 2.0)) + 0.004 * Math.sin(Math.PI * Math.min(1.0, x / 0.40700000000000003));
    }

    private Vec3 lying(boolean left, double x, double v, double h) {
        return this.origin.m_82549_(this.a.m_82490_(left ? -x : x)).m_82549_(this.s.m_82490_(v)).m_82549_(this.n.m_82490_(h));
    }

    private static double angle(Leaf leaf, double x, double v) {
        double lead = 0.75 - 0.5 * (v / 0.500763358778626);
        double out = Math.pow(x / 0.40700000000000003, 1.5);
        return Math.max(0.0, Math.min(Math.PI, Math.PI * leaf.p() + leaf.curl() * lead * out));
    }

    private Vec3[][] leafPoints(Leaf leaf) {
        Vec3[][] points = new Vec3[7][17];
        double stackRight = this.stack(false);
        double stackLeft = this.stack(true);
        for (int r = 0; r <= 6; ++r) {
            double v = -0.250381679389313 + 0.500763358778626 * (double)r / 6.0;
            double across = 0.0;
            double up = OpenBook.surface(0.0, stackRight) * (1.0 - leaf.p()) + OpenBook.surface(0.0, stackLeft) * leaf.p() + leaf.lift();
            double before = 0.0;
            for (int c = 0; c <= 16; ++c) {
                double x = 0.007 + 0.4 * (double)c / 16.0;
                double th = OpenBook.angle(leaf, (before + x) / 2.0, v);
                before = x;
                double flat = Math.cos(OpenBook.angle(leaf, x, v));
                double bump = flat >= 0.0 ? OpenBook.surface(x, stackRight) - OpenBook.surface(0.0, stackRight) : OpenBook.surface(x, stackLeft) - OpenBook.surface(0.0, stackLeft);
                points[r][c] = this.origin.m_82549_(this.a.m_82490_(across += Math.cos(th) * (x - before))).m_82549_(this.s.m_82490_(v)).m_82549_(this.n.m_82490_((up += Math.sin(th) * (x - before)) + bump * flat * flat));
            }
        }
        return points;
    }

    private Leaf foldLeaf() {
        return new Leaf(1.0 - this.fold, 0.25 * Anim.hump(this.fold), -1000, 2 * this.spread - 1, 0.0012);
    }

    void render(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        if (this.fold >= 0.999) {
            this.drawShut(pose, buffers, cam, light);
            return;
        }
        this.drawCover(pose, buffers, cam, light);
        if (this.closing) {
            this.drawStack(pose, buffers, cam, false, 2 * this.spread, light);
            Leaf half = this.foldLeaf();
            Vec3[][] points = this.leafPoints(half);
            this.drawLeafFace(pose, buffers, cam, points, true, half.left(), light);
            this.drawCoverFace(pose, buffers, cam, points, light);
            return;
        }
        int[] still = this.still();
        this.drawStack(pose, buffers, cam, true, still[0], light);
        this.drawStack(pose, buffers, cam, false, still[1], light);
        for (Leaf leaf : this.leaves()) {
            Vec3[][] points = this.leafPoints(leaf);
            this.drawLeafFace(pose, buffers, cam, points, false, leaf.right(), light);
            this.drawLeafFace(pose, buffers, cam, points, true, leaf.left(), light);
        }
    }

    private float[] boardUv(TextureAtlasSprite sprite, boolean mirrored) {
        float[] fArray;
        float u0 = Fx.u(sprite, this.look.u0());
        float u1 = Fx.u(sprite, this.look.u1());
        float v0 = Fx.v(sprite, this.look.v0());
        float v1 = Fx.v(sprite, this.look.v1());
        if (mirrored) {
            float[] fArray2 = new float[8];
            fArray2[0] = u1;
            fArray2[1] = v1;
            fArray2[2] = u0;
            fArray2[3] = v1;
            fArray2[4] = u0;
            fArray2[5] = v0;
            fArray2[6] = u1;
            fArray = fArray2;
            fArray2[7] = v0;
        } else {
            float[] fArray3 = new float[8];
            fArray3[0] = u0;
            fArray3[1] = v1;
            fArray3[2] = u1;
            fArray3[3] = v1;
            fArray3[4] = u1;
            fArray3[5] = v0;
            fArray3[6] = u0;
            fArray = fArray3;
            fArray3[7] = v0;
        }
        return fArray;
    }

    private void drawCover(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        TextureAtlasSprite sprite = Fx.sprite(this.look.texture());
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)TextureAtlas.f_118259_));
        double halfW = 0.42100000000000004;
        double halfH = 0.264381679389313;
        float[] plain = this.boardUv(sprite, false);
        for (boolean left : new boolean[]{true, false}) {
            if (left && this.fold > 0.0) continue;
            Vec3 inner = this.origin.m_82549_(this.n.m_82490_(-5.0E-4));
            Vec3 outer = inner.m_82549_(this.a.m_82490_(left ? -halfW : halfW));
            Vec3 lo = this.s.m_82490_(-halfH);
            Vec3 hi = this.s.m_82490_(halfH);
            Vec3[] top = new Vec3[]{inner.m_82549_(lo), outer.m_82549_(lo), outer.m_82549_(hi), inner.m_82549_(hi)};
            Vec3 down = this.n.m_82490_(-0.012);
            if (left) {
                OpenBook.quad(pose, vc, cam, new Vec3[]{top[1], top[0], top[3], top[2]}, this.boardUv(sprite, true), this.n, light, 0.9f);
            } else {
                OpenBook.quad(pose, vc, cam, top, plain, this.n, light, 0.9f);
            }
            Vec3 out = this.a.m_82490_(left ? -1.0 : 1.0);
            Vec3[] side = new Vec3[]{outer.m_82549_(lo).m_82549_(down), outer.m_82549_(hi).m_82549_(down), outer.m_82549_(hi), outer.m_82549_(lo)};
            OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(side) : side, plain, out, light, 0.7f);
            Vec3[] near = new Vec3[]{inner.m_82549_(lo).m_82549_(down), outer.m_82549_(lo).m_82549_(down), outer.m_82549_(lo), inner.m_82549_(lo)};
            OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(near) : near, plain, this.s.m_82490_(-1.0), light, 0.75f);
            Vec3[] far = new Vec3[]{inner.m_82549_(hi), outer.m_82549_(hi), outer.m_82549_(hi).m_82549_(down), inner.m_82549_(hi).m_82549_(down)};
            OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(far) : far, plain, this.s, light, 0.75f);
        }
    }

    private void drawStack(PoseStack pose, MultiBufferSource buffers, Vec3 cam, boolean left, int page, int light) {
        double stack = this.stack(left);
        Vec3[][] points = new Vec3[2][17];
        for (int c = 0; c <= 16; ++c) {
            double x = 0.007 + 0.4 * (double)c / 16.0;
            double h = OpenBook.surface(x, stack);
            points[0][c] = this.lying(left, x, -0.250381679389313, h);
            points[1][c] = this.lying(left, x, 0.250381679389313, h);
        }
        this.drawGrid(pose, buffers, cam, points, 1, left, page, light, 0.0);
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)BOOK_GUI));
        float[] uv = new float[]{0.2734375f, 0.3515625f, 0.296875f, 0.3515625f, 0.296875f, 0.234375f, 0.2734375f, 0.234375f};
        for (int c = 0; c < 16; ++c) {
            Vec3 b0 = this.drop(points[0][c]);
            Vec3 b1 = this.drop(points[0][c + 1]);
            Vec3[] edge = new Vec3[]{b0, b1, points[0][c + 1], points[0][c]};
            OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(edge) : edge, uv, this.s.m_82490_(-1.0), light, 0.82f);
            Vec3 t0 = this.drop(points[1][c]);
            Vec3 t1 = this.drop(points[1][c + 1]);
            Vec3[] back = new Vec3[]{points[1][c], points[1][c + 1], t1, t0};
            OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(back) : back, uv, this.s, light, 0.82f);
        }
        Vec3 o0 = points[0][16];
        Vec3 o1 = points[1][16];
        Vec3[] outer = new Vec3[]{this.drop(o0), this.drop(o1), o1, o0};
        OpenBook.quad(pose, vc, cam, left ? OpenBook.reverse(outer) : outer, uv, this.a.m_82490_(left ? -1.0 : 1.0), light, 0.78f);
    }

    private Vec3 drop(Vec3 point) {
        return point.m_82546_(this.n.m_82490_(point.m_82546_(this.origin).m_82526_(this.n) / this.n.m_82556_()));
    }

    private void drawLeafFace(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3[][] points, boolean leftFace, int page, int light) {
        this.drawGrid(pose, buffers, cam, points, 6, leftFace, page, light, leftFace ? -4.0E-4 : 4.0E-4);
    }

    private void drawCoverFace(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3[][] points, int light) {
        TextureAtlasSprite sprite = Fx.sprite(this.look.texture());
        VertexConsumer vc = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)TextureAtlas.f_118259_));
        Vec3 c00 = points[0][0];
        Vec3 c01 = points[0][16];
        Vec3 c11 = points[6][16];
        Vec3 c10 = points[6][0];
        Vec3 normal = c01.m_82546_(c00).m_82537_(c10.m_82546_(c00)).m_82541_();
        Vec3 off = normal.m_82490_((this.stack(true) + 0.006) * this.n.m_82553_());
        Vec3[] face = new Vec3[]{c00.m_82549_(off), c01.m_82549_(off), c11.m_82549_(off), c10.m_82549_(off)};
        OpenBook.quad(pose, vc, cam, face, this.boardUv(sprite, false), normal, light, 0.95f);
    }

    private void drawShut(PoseStack pose, MultiBufferSource buffers, Vec3 cam, int light) {
        TextureAtlasSprite sprite = Fx.sprite(this.look.texture());
        VertexConsumer board = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)TextureAtlas.f_118259_));
        float[] plain = this.boardUv(sprite, false);
        double v = 0.264381679389313;
        this.slab(pose, board, cam, 0.0, 0.42100000000000004, -v, v, -0.012, 0.0, plain, light);
        this.slab(pose, board, cam, 0.0, 0.42100000000000004, -v, v, 0.032, 0.044, plain, light);
        float[] spine = new float[]{Fx.u(sprite, this.look.su0()), Fx.v(sprite, this.look.sv1()), Fx.u(sprite, this.look.su1()), Fx.v(sprite, this.look.sv1()), Fx.u(sprite, this.look.su1()), Fx.v(sprite, this.look.sv0()), Fx.u(sprite, this.look.su0()), Fx.v(sprite, this.look.sv0())};
        double x = -6.0E-4;
        OpenBook.face(pose, board, cam, this.at(x, -v, -0.012), this.at(x, -v, 0.044), this.at(x, v, 0.044), this.at(x, v, -0.012), spine, this.a.m_82490_(-1.0), light, 1.0f);
        VertexConsumer paper = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)BOOK_GUI));
        float[] uv = new float[]{0.2734375f, 0.3515625f, 0.296875f, 0.3515625f, 0.296875f, 0.234375f, 0.2734375f, 0.234375f};
        double x0 = 0.007;
        double x1 = 0.40700000000000003;
        double p = 0.250381679389313;
        OpenBook.face(pose, paper, cam, this.at(x1, -p, 0.0), this.at(x1, p, 0.0), this.at(x1, p, 0.032), this.at(x1, -p, 0.032), uv, this.a, light, 0.86f);
        OpenBook.face(pose, paper, cam, this.at(x0, p, 0.0), this.at(x1, p, 0.0), this.at(x1, p, 0.032), this.at(x0, p, 0.032), uv, this.s, light, 0.86f);
        OpenBook.face(pose, paper, cam, this.at(x0, -p, 0.0), this.at(x1, -p, 0.0), this.at(x1, -p, 0.032), this.at(x0, -p, 0.032), uv, this.s.m_82490_(-1.0), light, 0.8f);
    }

    private void slab(PoseStack pose, VertexConsumer vc, Vec3 cam, double x0, double x1, double v0, double v1, double h0, double h1, float[] uv, int light) {
        OpenBook.face(pose, vc, cam, this.at(x0, v0, h1), this.at(x1, v0, h1), this.at(x1, v1, h1), this.at(x0, v1, h1), uv, this.n, light, 0.95f);
        OpenBook.face(pose, vc, cam, this.at(x0, v0, h0), this.at(x1, v0, h0), this.at(x1, v1, h0), this.at(x0, v1, h0), uv, this.n.m_82490_(-1.0), light, 0.95f);
        OpenBook.face(pose, vc, cam, this.at(x1, v0, h0), this.at(x1, v1, h0), this.at(x1, v1, h1), this.at(x1, v0, h1), uv, this.a, light, 0.7f);
        OpenBook.face(pose, vc, cam, this.at(x0, v1, h0), this.at(x1, v1, h0), this.at(x1, v1, h1), this.at(x0, v1, h1), uv, this.s, light, 0.75f);
        OpenBook.face(pose, vc, cam, this.at(x0, v0, h0), this.at(x1, v0, h0), this.at(x1, v0, h1), this.at(x0, v0, h1), uv, this.s.m_82490_(-1.0), light, 0.75f);
    }

    private void drawGrid(PoseStack pose, MultiBufferSource buffers, Vec3 cam, Vec3[][] points, int rows, boolean leftFace, int page, int light, double offset) {
        float vMax;
        float vMin;
        float uMax;
        float uMin;
        VertexConsumer vc;
        ResourceLocation texture = BookPages.texture(this.bookId, page);
        if (texture != null) {
            vc = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)texture));
            uMin = 0.0f;
            uMax = 1.0f;
            vMin = 1.0f;
            vMax = 0.0f;
        } else {
            vc = buffers.m_6299_(RenderType.m_110446_((ResourceLocation)BOOK_GUI));
            boolean mirrored = BookPages.leftHand(page);
            uMin = (float)(26 + (mirrored ? 131 : 0)) / 256.0f;
            uMax = (float)(26 + (mirrored ? 0 : 131)) / 256.0f;
            vMin = 0.03125f;
            vMax = 0.671875f;
        }
        double scale = this.n.m_82553_();
        int cols = points[0].length - 1;
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                Vec3 p00 = points[r][c];
                Vec3 p01 = points[r][c + 1];
                Vec3 p11 = points[r + 1][c + 1];
                Vec3 p10 = points[r + 1][c];
                Vec3 normal = p01.m_82546_(p00).m_82537_(p10.m_82546_(p00));
                if (normal.m_82556_() < 1.0E-12) continue;
                normal = normal.m_82541_();
                if (leftFace) {
                    normal = normal.m_82490_(-1.0);
                }
                Vec3 lift = normal.m_82490_(Math.abs(offset) * scale);
                float x0 = (float)c / (float)cols;
                float x1 = (float)(c + 1) / (float)cols;
                float y0 = 1.0f - (float)r / (float)rows;
                float y1 = 1.0f - (float)(r + 1) / (float)rows;
                float tu0 = leftFace ? 1.0f - x0 : x0;
                float tu1 = leftFace ? 1.0f - x1 : x1;
                tu0 = uMin + (uMax - uMin) * tu0;
                tu1 = uMin + (uMax - uMin) * tu1;
                float tv0 = vMin + (vMax - vMin) * y0;
                float tv1 = vMin + (vMax - vMin) * y1;
                Vec3[] corners = new Vec3[]{p00.m_82549_(lift), p01.m_82549_(lift), p11.m_82549_(lift), p10.m_82549_(lift)};
                float[] uv = new float[]{tu0, tv0, tu1, tv0, tu1, tv1, tu0, tv1};
                if (leftFace) {
                    corners = OpenBook.reverse(corners);
                    uv = new float[]{tu0, tv1, tu1, tv1, tu1, tv0, tu0, tv0};
                }
                OpenBook.quad(pose, vc, cam, corners, uv, normal, light, 1.0f);
            }
        }
    }

    private static Vec3[] reverse(Vec3[] q) {
        return new Vec3[]{q[3], q[2], q[1], q[0]};
    }

    private static void face(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3 c0, Vec3 c1, Vec3 c2, Vec3 c3, float[] uv, Vec3 out, int light, float shade) {
        Vec3[] corners = new Vec3[]{c0, c1, c2, c3};
        if (c1.m_82546_(c0).m_82537_(c3.m_82546_(c0)).m_82526_(out) < 0.0) {
            corners = OpenBook.reverse(corners);
            uv = new float[]{uv[6], uv[7], uv[4], uv[5], uv[2], uv[3], uv[0], uv[1]};
        }
        OpenBook.quad(pose, vc, cam, corners, uv, out, light, shade);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, Vec3 cam, Vec3[] corners, float[] uv, Vec3 normal, int light, float shade) {
        int c = Math.round(255.0f * shade);
        Vec3 unit = normal.m_82541_();
        for (int i = 0; i < 4; ++i) {
            Vec3 p = corners[i].m_82546_(cam);
            vc.m_252986_(pose.m_85850_().m_252922_(), (float)p.f_82479_, (float)p.f_82480_, (float)p.f_82481_).m_6122_(c, c, c, 255).m_7421_(uv[i * 2], uv[i * 2 + 1]).m_86008_(OverlayTexture.f_118083_).m_85969_(light).m_252939_(pose.m_85850_().m_252943_(), (float)unit.f_82479_, (float)unit.f_82480_, (float)unit.f_82481_).m_5752_();
        }
    }

    record Look(String texture, float u0, float v0, float u1, float v1, float su0, float sv0, float su1, float sv1) {
    }

    static enum Turned {
        YES,
        WAIT,
        NO_PAGE;

    }

    private record Turn(double at, double length, int from, int dir) {
        double phase() {
            return Anim.clamp01((Scene.clock() - this.at) / this.length);
        }
    }

    static enum Side {
        NONE,
        LEFT,
        RIGHT;

    }

    private record Leaf(double p, double curl, int right, int left, double lift) {
    }
}

