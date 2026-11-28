/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Lighting
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.joml.Matrix4f
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.block.AbyssGateActivationBlock;
import com.pla.spiral_abyss.client.screen.ActivationRecipePreview;
import com.pla.spiral_abyss.init.BlockInit;
import com.pla.spiral_abyss.world.AbyssPortalFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class AbyssHelpPanel {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("spiral_abyss", "textures/gui/journal/help_background.png");
    private static final int WIDTH = 720;
    private static final int HEIGHT = 405;
    private static final Quaternionf ROTATION = new Quaternionf().rotateX((float)Math.toRadians(12.0)).rotateY((float)Math.toRadians(-18.0));
    private final List<Part> parts = new ArrayList<Part>();

    public AbyssHelpPanel() {
        AbyssPortalFrame frame = new AbyssPortalFrame(BlockPos.f_121853_, Direction.SOUTH);
        for (int y = 0; y <= 5; ++y) {
            for (int x = -2; x <= 2; ++x) {
                BlockState state = frame.expectedFrameState(x, y);
                if (state == null) continue;
                this.parts.add(new Part(state, x, y, 0, AbyssHelpPanel.kind(state)));
            }
        }
        BlockPos controller = frame.anchor();
        this.parts.add(new Part((BlockState)((AbyssGateActivationBlock)((Object)BlockInit.ABYSS_GATE_ACTIVATE_BLOCK.get())).m_49966_().m_61124_((Property)AbyssGateActivationBlock.f_54117_, (Comparable)Direction.SOUTH), controller.m_123341_(), controller.m_123342_(), controller.m_123343_(), Kind.ACTIVATION));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(GuiGraphics graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Minecraft client = Minecraft.m_91087_();
        graphics.m_280411_(BACKGROUND, x, y, width, height, 0.0f, 0.0f, 1280, 720, 1280, 720);
        float scale = Math.min((float)width / 720.0f, (float)height / 405.0f);
        float left = (float)x + ((float)width - 720.0f * scale) / 2.0f;
        float top = (float)y + ((float)height - 405.0f * scale) / 2.0f;
        float mx = ((float)mouseX - left) / scale;
        float my = ((float)mouseY - top) / scale;
        graphics.m_280168_().m_85836_();
        try {
            graphics.m_280168_().m_252880_(left, top, 0.0f);
            graphics.m_280168_().m_85841_(scale, scale, scale);
            Kind hovered = null;
            for (Part part : this.parts) {
                part.prepare(client);
                if (!part.contains(mx, my)) continue;
                hovered = part.kind;
            }
            for (Kind kind : Kind.values()) {
                if (!(mx >= (float)(kind.labelX - 80)) || !(mx <= (float)(kind.labelX + 80)) || !(my >= (float)(kind.labelY - 5)) || !(my <= (float)(kind.labelY + 19))) continue;
                hovered = kind;
            }
            Kind selected = hovered;
            graphics.m_286007_(() -> {
                for (Part part : this.parts) {
                    part.outline(graphics, selected == part.kind);
                }
            });
            Lighting.m_84931_();
            graphics.m_280168_().m_85836_();
            try {
                AbyssHelpPanel.gateTransform(graphics.m_280168_());
                for (Part part : this.parts) {
                    graphics.m_280168_().m_85836_();
                    graphics.m_280168_().m_252880_((float)part.x, (float)part.y, (float)part.z);
                    client.m_91289_().m_110912_(part.state, graphics.m_280168_(), (MultiBufferSource)graphics.m_280091_(), 0xF000F0, OverlayTexture.f_118083_);
                    graphics.m_280168_().m_85849_();
                }
                graphics.m_280262_();
            }
            finally {
                graphics.m_280168_().m_85849_();
                Lighting.m_84931_();
            }
            graphics.m_280168_().m_252880_(0.0f, 0.0f, 250.0f);
            for (Kind kind : Kind.values()) {
                if (hovered == Kind.ACTIVATION && kind == Kind.VERTICAL) continue;
                AbyssHelpPanel.label(graphics, kind, hovered == kind);
            }
            if (hovered == Kind.ACTIVATION) {
                ActivationRecipePreview.render(graphics);
            }
            AbyssHelpPanel.details(graphics, hovered);
        }
        finally {
            graphics.m_280168_().m_85849_();
        }
    }

    private static void gateTransform(PoseStack pose) {
        pose.m_252880_(360.0f, 325.0f, 100.0f);
        pose.m_85841_(34.0f, -34.0f, 34.0f);
        pose.m_252781_(ROTATION);
        pose.m_85837_(-0.5, 0.0, -0.5);
    }

    private static Kind kind(BlockState state) {
        if (state.m_60713_((Block)BlockInit.ABYSS_GATE_HORIZONTAL.get())) {
            return Kind.HORIZONTAL;
        }
        if (state.m_60713_((Block)BlockInit.ABYSS_GATE_CURVE.get())) {
            return Kind.CURVE;
        }
        return Kind.VERTICAL;
    }

    private static void label(GuiGraphics graphics, Kind kind, boolean hovered) {
        Font font = Minecraft.m_91087_().f_91062_;
        int color = hovered ? kind.color : -1251846;
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)kind.labelX, (float)kind.labelY, 0.0f);
        graphics.m_280168_().m_85841_(1.35f, 1.35f, 1.0f);
        graphics.m_280653_(font, (Component)Component.m_237115_((String)("ui.spiral_abyss.help." + kind.key)), 0, 0, color);
        graphics.m_280168_().m_85849_();
        int elbow = kind.labelY + 34;
        graphics.m_286007_(() -> {
            AbyssHelpPanel.dashed(graphics, kind.labelX, kind.labelY + 19, kind.labelX, elbow, color);
            AbyssHelpPanel.dashed(graphics, kind.labelX, elbow, kind.targetX, elbow, color);
            AbyssHelpPanel.dashed(graphics, kind.targetX, elbow, kind.targetX, kind.targetY, color);
            graphics.m_280509_(kind.labelX - 2, kind.labelY + 17, kind.labelX + 3, kind.labelY + 22, color);
            graphics.m_280509_(kind.targetX - 2, kind.targetY - 2, kind.targetX + 3, kind.targetY + 3, kind.color);
        });
    }

    private static void dashed(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int length = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (length == 0) {
            return;
        }
        for (int i = 0; i < length; i += 7) {
            int end = Math.min(i + 4, length);
            int ax = x1 + (x2 - x1) * i / length;
            int ay = y1 + (y2 - y1) * i / length;
            int bx = x1 + (x2 - x1) * end / length;
            int by = y1 + (y2 - y1) * end / length;
            graphics.m_280509_(Math.min(ax, bx), Math.min(ay, by), Math.max(ax, bx) + 2, Math.max(ay, by) + 2, color);
        }
    }

    private static void details(GuiGraphics graphics, Kind hovered) {
        Font font = Minecraft.m_91087_().f_91062_;
        graphics.m_280509_(24, 342, 696, 397, -1172893657);
        graphics.m_280509_(24, 342, 27, 397, hovered == null ? -4217387 : hovered.color);
        String key = hovered == null ? "overview" : hovered.key;
        MutableComponent title = Component.m_237115_((String)("ui.spiral_abyss.help." + key));
        MutableComponent description = Component.m_237115_((String)("ui.spiral_abyss.help." + key + ".description"));
        float titleScale = Math.min(1.25f, 644.0f / (float)Math.max(1, font.m_92852_((FormattedText)title)));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(38.0f, 350.0f, 0.0f);
        graphics.m_280168_().m_85841_(titleScale, titleScale, 1.0f);
        graphics.m_280614_(font, (Component)title, 0, 0, -793403, false);
        graphics.m_280168_().m_85849_();
        float low = 0.0f;
        float high = 1.25f;
        Objects.requireNonNull(font);
        int spacing = 9 + 2;
        for (int i = 0; i < 16; ++i) {
            float candidate = (low + high) / 2.0f;
            int lines = font.m_92923_((FormattedText)description, Math.max(1, (int)(644.0f / candidate))).size();
            if ((float)(lines * spacing) * candidate <= 27.0f) {
                low = candidate;
                continue;
            }
            high = candidate;
        }
        float bodyScale = Math.max(1.0E-5f, low);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(38.0f, 366.0f, 0.0f);
        graphics.m_280168_().m_85841_(bodyScale, bodyScale, 1.0f);
        int lineY = 0;
        for (FormattedCharSequence line : font.m_92923_((FormattedText)description, Math.max(1, (int)(644.0f / bodyScale)))) {
            graphics.m_280649_(font, line, 0, lineY, -1976083, false);
            lineY += spacing;
        }
        graphics.m_280168_().m_85849_();
    }

    private static final class Part {
        final BlockState state;
        final int x;
        final int y;
        final int z;
        final Kind kind;
        BakedModel cachedModel;
        int minX;
        int minY;
        int width;
        int height;
        boolean[] mask;
        final List<int[]> outline = new ArrayList<int[]>();

        Part(BlockState state, int x, int y, int z, Kind kind) {
            this.state = state;
            this.x = x;
            this.y = y;
            this.z = z;
            this.kind = kind;
        }

        void prepare(Minecraft client) {
            int py;
            BakedModel model = client.m_91289_().m_110910_(this.state);
            if (this.cachedModel == model) {
                return;
            }
            this.cachedModel = model;
            this.outline.clear();
            PoseStack pose = new PoseStack();
            AbyssHelpPanel.gateTransform(pose);
            pose.m_252880_((float)this.x, (float)this.y, (float)this.z);
            Matrix4f matrix = pose.m_85850_().m_252922_();
            ArrayList<float[]> polygons = new ArrayList<float[]>();
            float lowX = 720.0f;
            float lowY = 405.0f;
            float highX = 0.0f;
            float highY = 0.0f;
            for (int side = 0; side < 7; ++side) {
                Direction direction = side == 6 ? null : Direction.values()[side];
                for (BakedQuad quad : model.m_213637_(this.state, direction, RandomSource.m_216335_((long)42L))) {
                    int[] data = quad.m_111303_();
                    int stride = data.length / 4;
                    float[] polygon = new float[8];
                    for (int v = 0; v < 4; ++v) {
                        Vector3f point = new Vector3f(Float.intBitsToFloat(data[v * stride]), Float.intBitsToFloat(data[v * stride + 1]), Float.intBitsToFloat(data[v * stride + 2]));
                        matrix.transformPosition(point);
                        polygon[v * 2] = point.x;
                        polygon[v * 2 + 1] = point.y;
                        lowX = Math.min(lowX, point.x);
                        lowY = Math.min(lowY, point.y);
                        highX = Math.max(highX, point.x);
                        highY = Math.max(highY, point.y);
                    }
                    polygons.add(polygon);
                }
            }
            if (polygons.isEmpty()) {
                this.mask = new boolean[0];
                this.height = 0;
                this.width = 0;
                return;
            }
            this.minX = (int)Math.floor(lowX) - 3;
            this.minY = (int)Math.floor(lowY) - 3;
            this.width = (int)Math.ceil(highX) - this.minX + 4;
            this.height = (int)Math.ceil(highY) - this.minY + 4;
            this.mask = new boolean[this.width * this.height];
            for (py = 0; py < this.height; ++py) {
                block4: for (int px = 0; px < this.width; ++px) {
                    for (float[] polygon : polygons) {
                        if (!Part.inside(polygon, (float)(this.minX + px) + 0.5f, (float)(this.minY + py) + 0.5f)) continue;
                        this.mask[py * this.width + px] = true;
                        continue block4;
                    }
                }
            }
            for (py = 0; py < this.height; ++py) {
                int start = -1;
                for (int px = 0; px <= this.width; ++px) {
                    boolean edge;
                    boolean bl = edge = px < this.width && !this.pixel(px, py) && this.nearby(px, py);
                    if (edge && start < 0) {
                        start = px;
                    }
                    if (edge || start < 0) continue;
                    this.outline.add(new int[]{this.minX + start, this.minY + py, this.minX + px});
                    start = -1;
                }
            }
        }

        boolean contains(float mx, float my) {
            return this.pixel((int)mx - this.minX, (int)my - this.minY);
        }

        boolean pixel(int px, int py) {
            return px >= 0 && py >= 0 && px < this.width && py < this.height && this.mask[py * this.width + px];
        }

        boolean nearby(int px, int py) {
            for (int dy = -2; dy <= 2; ++dy) {
                for (int dx = -2; dx <= 2; ++dx) {
                    if (!this.pixel(px + dx, py + dy)) continue;
                    return true;
                }
            }
            return false;
        }

        void outline(GuiGraphics graphics, boolean selected) {
            int color = selected ? this.kind.color : this.kind.color & 0xFFFFFF | 0xA0000000;
            for (int[] run : this.outline) {
                graphics.m_280509_(run[0], run[1], run[2], run[1] + 1, color);
            }
        }

        static boolean inside(float[] polygon, float px, float py) {
            boolean result = false;
            int i = 0;
            int j = 3;
            while (i < 4) {
                float jy;
                float ix = polygon[i * 2];
                float iy = polygon[i * 2 + 1];
                float jx = polygon[j * 2];
                if (iy > py != (jy = polygon[j * 2 + 1]) > py && px < (jx - ix) * (py - iy) / (jy - iy) + ix) {
                    result = !result;
                }
                j = i++;
            }
            return result;
        }
    }

    private static enum Kind {
        HORIZONTAL("horizontal", -7218839, 168, 72, 315, 144),
        ACTIVATION("activation", -7478048, 528, 45, 367, 117),
        VERTICAL("vertical", -6583041, 559, 191, 438, 234),
        CURVE("curve", -1015402, 155, 237, 286, 286);

        final String key;
        final int color;
        final int labelX;
        final int labelY;
        final int targetX;
        final int targetY;

        private Kind(String key, int color, int labelX, int labelY, int targetX, int targetY) {
            this.key = key;
            this.color = color;
            this.labelX = labelX;
            this.labelY = labelY;
            this.targetX = targetX;
            this.targetY = targetY;
        }
    }
}

