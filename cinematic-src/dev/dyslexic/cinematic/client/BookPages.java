/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.pipeline.TextureTarget
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexSorting
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.texture.AbstractTexture
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.util.FormattedCharSequence
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package dev.dyslexic.cinematic.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dyslexic.cinematic.Cinematic;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class BookPages {
    static final int PAPER_U = 26;
    static final int PAPER_V = 8;
    static final int W = 131;
    static final int H = 164;
    static final int SCALE = 4;
    static final int TEXT_X = 10;
    static final int TEXT_Y = 22;
    static final int TEXT_W = 114;
    static final int LINES = 14;
    private static final int POOL = 8;
    private static final ResourceLocation BOOK = new ResourceLocation("textures/gui/book.png");
    private static final List<Page> PAGES = new ArrayList<Page>();
    private static long frame;

    private BookPages() {
    }

    static boolean leftHand(int index) {
        return index < 0 || index % 2 != 0;
    }

    @Nullable
    static ResourceLocation texture(int book, int index) {
        for (Page page : PAGES) {
            if (page.book != book || page.index != index) continue;
            page.used = frame;
            return page.id;
        }
        return null;
    }

    static void prepare(int book, @Nullable List<FormattedText> text, int ... pages) {
        ++frame;
        for (int index : pages) {
            if (BookPages.texture(book, index) != null) continue;
            BookPages.paint(BookPages.take(), book, text, index);
        }
    }

    private static Page take() {
        if (PAGES.size() < 8) {
            Page page = new Page(PAGES.size());
            Minecraft.m_91087_().m_91097_().m_118495_(page.id, (AbstractTexture)page);
            PAGES.add(page);
            return page;
        }
        Page oldest = PAGES.get(0);
        for (Page page : PAGES) {
            if (page.used >= oldest.used) continue;
            oldest = page;
        }
        return oldest;
    }

    private static void paint(Page page, int book, @Nullable List<FormattedText> text, int index) {
        Minecraft mc = Minecraft.m_91087_();
        page.book = book;
        page.index = index;
        page.used = frame;
        boolean left = BookPages.leftHand(index);
        TextureTarget target = page.target;
        target.m_83931_(0.0f, 0.0f, 0.0f, 0.0f);
        target.m_83954_(Minecraft.f_91002_);
        target.m_83947_(true);
        Matrix4f projection = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix((Matrix4f)new Matrix4f().setOrtho(0.0f, 524.0f, 656.0f, 0.0f, 1000.0f, 21000.0f), (VertexSorting)VertexSorting.f_276633_);
        PoseStack view = RenderSystem.getModelViewStack();
        view.m_85836_();
        view.m_166856_();
        view.m_252880_(0.0f, 0.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
        GuiGraphics g = new GuiGraphics(mc, mc.m_91269_().m_110104_());
        g.m_280168_().m_85841_(4.0f, 4.0f, 1.0f);
        if (left) {
            g.m_280411_(BOOK, 0, 0, 131, 164, 157.0f, 8.0f, -131, 164, 256, 256);
        } else {
            g.m_280411_(BOOK, 0, 0, 131, 164, 26.0f, 8.0f, 131, 164, 256, 256);
        }
        if (text != null && index >= 0 && index < text.size()) {
            Font font = mc.f_91062_;
            List lines = font.m_92923_(text.get(index), 114);
            int x = left ? 7 : 10;
            for (int i = 0; i < Math.min(14, lines.size()); ++i) {
                g.m_280649_(font, (FormattedCharSequence)lines.get(i), x, 22 + i * 9, 0, false);
            }
            String number = String.valueOf(index + 1);
            g.m_280056_(font, number, (131 - font.m_92895_(number)) / 2, 151, 10126438, false);
        }
        g.m_280262_();
        view.m_85849_();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix((Matrix4f)projection, (VertexSorting)sorting);
        mc.m_91385_().m_83947_(true);
    }

    static void forget() {
        for (Page page : PAGES) {
            page.book = Integer.MIN_VALUE;
            page.index = Integer.MIN_VALUE;
        }
    }

    private static final class Page
    extends AbstractTexture {
        final TextureTarget target;
        final ResourceLocation id;
        int book = Integer.MIN_VALUE;
        int index = Integer.MIN_VALUE;
        long used;

        Page(int slot) {
            this.target = new TextureTarget(524, 656, false, Minecraft.f_91002_);
            this.id = Cinematic.id("book_page/" + slot);
        }

        public void m_6704_(ResourceManager manager) {
        }

        public int m_117963_() {
            return this.target.m_83975_();
        }

        public void m_117964_() {
            this.target.m_83930_();
        }
    }
}

