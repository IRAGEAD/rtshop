/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package com.pla.spiral_abyss.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pla.spiral_abyss.network.AbyssNetwork;
import com.pla.spiral_abyss.platform.AbyssRegistries;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

final class RewardPreviewPanel {
    private static final int TILE_SIZE = 40;
    private static final int GAP = 6;
    private static final int PER_PAGE = 32;
    private final List<AbyssNetwork.RewardPreview> rewards;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int tileSize;
    private final int cellSize;
    private int scroll;
    private int hovered = -1;
    private int page;
    private int popupX;
    private int popupY;
    private int popupWidth;
    private int popupHeight;
    private boolean inspecting;

    RewardPreviewPanel(List<AbyssNetwork.RewardPreview> rewards, int x, int y, int width, int height) {
        this.rewards = rewards;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.tileSize = Math.max(20, Math.min(40, Math.min(width - 6, height)));
        this.cellSize = this.tileSize + 6;
    }

    private int columns() {
        return Math.max(1, (this.width - 6 + 6) / this.cellSize);
    }

    private int maxScroll() {
        int rows = (this.rewards.size() + this.columns() - 1) / this.columns();
        return Math.max(0, rows * this.cellSize - 6 - this.height);
    }

    boolean popupContains(double mouseX, double mouseY) {
        return this.inspecting && Screen.m_96638_() && this.hovered >= 0 && this.rewards.get(this.hovered).table() && RewardPreviewPanel.within(mouseX, mouseY, this.popupX, this.popupY, this.popupWidth, this.popupHeight);
    }

    private int iconAt(double mouseX, double mouseY) {
        if (!RewardPreviewPanel.within(mouseX, mouseY, this.x, this.y, this.columns() * this.cellSize - 6, this.height)) {
            return -1;
        }
        int localX = (int)mouseX - this.x;
        int localY = (int)mouseY - this.y + this.scroll;
        if (localX % this.cellSize >= this.tileSize || localY % this.cellSize >= this.tileSize) {
            return -1;
        }
        int index = localY / this.cellSize * this.columns() + localX / this.cellSize;
        return index < this.rewards.size() ? index : -1;
    }

    private static boolean within(double px, double py, int x, int y, int w, int h) {
        return px >= (double)x && px < (double)(x + w) && py >= (double)y && py < (double)(y + h);
    }

    private static ItemStack stack(ResourceLocation id) {
        Item item = AbyssRegistries.ITEMS.getValue(id);
        return item == null ? ItemStack.f_41583_ : new ItemStack((ItemLike)item);
    }

    private static ItemStack iconStack(AbyssNetwork.RewardPreview reward) {
        return reward.icon().m_41619_() ? RewardPreviewPanel.stack(reward.id()) : reward.icon();
    }

    private static Component tableName(ResourceLocation id) {
        String path = id.m_135815_().substring(id.m_135815_().lastIndexOf(47) + 1).replace('_', ' ');
        StringBuilder name = new StringBuilder();
        for (String word : path.split(" ")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return Component.m_237110_((String)"screen.spiral_abyss.chest_loot", (Object[])new Object[]{name.toString()});
    }

    void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        Font font = Minecraft.m_91087_().f_91062_;
        graphics.m_280614_(font, (Component)Component.m_237115_((String)"screen.spiral_abyss.rewards"), this.x, this.y - 13, -1, false);
        if (!Screen.m_96638_()) {
            this.inspecting = false;
        }
        int hit = this.iconAt(mouseX, mouseY);
        if (!this.inspecting) {
            if (hit != this.hovered) {
                this.page = 0;
            }
            this.hovered = hit;
            boolean bl = this.inspecting = hit >= 0 && this.rewards.get(hit).table() && Screen.m_96638_();
        }
        if (this.hovered >= 0) {
            this.popupWidth = Math.min(232, screenWidth - 12);
            int n = font.m_92923_((FormattedText)Component.m_237115_((String)"screen.spiral_abyss.loot_chance_basis"), this.popupWidth - 16).size();
            Objects.requireNonNull(font);
            int noteHeight = n * 9 + 4;
            this.popupHeight = Math.min(146 + noteHeight, screenHeight - 12);
            int anchorX = this.x + this.hovered % this.columns() * this.cellSize;
            int anchorY = this.y + this.hovered / this.columns() * this.cellSize - this.scroll;
            this.popupX = Mth.m_14045_((int)(anchorX + this.cellSize), (int)6, (int)(screenWidth - this.popupWidth - 6));
            this.popupY = Mth.m_14045_((int)(anchorY - 8), (int)6, (int)(screenHeight - this.popupHeight - 6));
        }
        graphics.m_280588_(this.x, this.y, this.x + this.width, this.y + this.height);
        for (int i = 0; i < this.rewards.size(); ++i) {
            int sx = this.x + i % this.columns() * this.cellSize;
            int sy = this.y + i / this.columns() * this.cellSize - this.scroll;
            if (sy + this.tileSize <= this.y || sy >= this.y + this.height) continue;
            AbyssNetwork.RewardPreview reward = this.rewards.get(i);
            int color = this.inspecting && i == this.hovered ? 12417279 : (reward.rare() ? 16768329 : 3336180);
            boolean showChance = reward.table() || reward.rare();
            this.icon(graphics, reward.table() ? new ItemStack((ItemLike)Items.f_42009_) : RewardPreviewPanel.iconStack(reward), sx, sy, color);
            if (!showChance) continue;
            this.percentage(graphics, reward.displayChance(), sx, sy);
        }
        graphics.m_280618_();
        if (this.rewards.isEmpty()) {
            graphics.m_280614_(font, (Component)Component.m_237115_((String)"screen.spiral_abyss.no_rewards"), this.x + 2, this.y + 6, 12109786, false);
        }
        if (this.maxScroll() > 0) {
            int thumb = Math.max(8, this.height * this.height / (this.height + this.maxScroll()));
            int top = this.y + (this.height - thumb) * this.scroll / this.maxScroll();
            graphics.m_280509_(this.x + this.width - 3, this.y, this.x + this.width, this.y + this.height, -13286053);
            graphics.m_280509_(this.x + this.width - 3, top, this.x + this.width, top + thumb, -6563866);
        }
        if (this.hovered < 0) {
            return;
        }
        AbyssNetwork.RewardPreview reward = this.rewards.get(this.hovered);
        if (!reward.table()) {
            graphics.m_280153_(font, RewardPreviewPanel.iconStack(reward), mouseX, mouseY);
            return;
        }
        this.renderContents(graphics, reward, mouseX, mouseY);
    }

    private void renderContents(GuiGraphics graphics, AbyssNetwork.RewardPreview reward, int mouseX, int mouseY) {
        Font font = Minecraft.m_91087_().f_91062_;
        int pages = Math.max(1, (reward.contents().size() + 32 - 1) / 32);
        this.page = Mth.m_14045_((int)this.page, (int)0, (int)(pages - 1));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 450.0f);
        graphics.m_280509_(this.popupX, this.popupY, this.popupX + this.popupWidth, this.popupY + this.popupHeight, -183495648);
        graphics.m_280637_(this.popupX, this.popupY, this.popupWidth, this.popupHeight, -12067861);
        Component title = RewardPreviewPanel.tableName(reward.id());
        float scale = Math.min(1.0f, ((float)this.popupWidth - 16.0f) / (float)Math.max(1, font.m_92852_((FormattedText)title)));
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)(this.popupX + 8), (float)(this.popupY + 7), 0.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280614_(font, title, 0, 0, -1, false);
        graphics.m_280168_().m_85849_();
        graphics.m_280614_(font, (Component)Component.m_237115_((String)(reward.incomplete() ? "screen.spiral_abyss.known_loot" : "screen.spiral_abyss.possible_loot")), this.popupX + 8, this.popupY + 21, 11452114, false);
        List chanceNote = font.m_92923_((FormattedText)Component.m_237115_((String)"screen.spiral_abyss.loot_chance_basis"), this.popupWidth - 16);
        for (int line = 0; line < chanceNote.size(); ++line) {
            FormattedCharSequence formattedCharSequence = (FormattedCharSequence)chanceNote.get(line);
            Objects.requireNonNull(font);
            graphics.m_280649_(font, formattedCharSequence, this.popupX + 8, this.popupY + 33 + line * 9, 11452114, false);
        }
        int n = chanceNote.size();
        Objects.requireNonNull(font);
        int noteHeight = n * 9 + 4;
        ItemStack underMouse = ItemStack.f_41583_;
        int end = Math.min(reward.contents().size(), (this.page + 1) * 32);
        for (int i = this.page * 32; i < end; ++i) {
            int index = i % 32;
            int sx = this.popupX + 8 + index % 8 * 26;
            int sy = this.popupY + 36 + noteHeight + index / 8 * 22;
            ItemStack item = RewardPreviewPanel.stack(reward.contents().get(i));
            graphics.m_280480_(item, sx, sy);
            if (!this.inspecting || !RewardPreviewPanel.within(mouseX, mouseY, sx - 1, sy - 1, 18, 18)) continue;
            underMouse = item;
        }
        if (reward.contents().isEmpty()) {
            graphics.m_280614_(font, (Component)Component.m_237115_((String)"screen.spiral_abyss.loot_contents_unavailable"), this.popupX + 8, this.popupY + 40 + noteHeight, 12109786, false);
        }
        MutableComponent footer = pages > 1 ? Component.m_237110_((String)"screen.spiral_abyss.loot_page", (Object[])new Object[]{this.page + 1, pages}) : Component.m_237115_((String)(reward.incomplete() ? "screen.spiral_abyss.loot_partial" : "screen.spiral_abyss.loot_varies"));
        graphics.m_280614_(font, (Component)footer, this.popupX + 8, this.popupY + this.popupHeight - 15, 11452114, false);
        graphics.m_280168_().m_85849_();
        if (!underMouse.m_41619_()) {
            graphics.m_280168_().m_85836_();
            graphics.m_280168_().m_252880_(0.0f, 0.0f, 600.0f);
            graphics.m_280153_(font, underMouse, mouseX, mouseY);
            graphics.m_280168_().m_85849_();
        }
    }

    private void percentage(GuiGraphics graphics, double chance, int x, int y) {
        String label = chance > 0.0 && chance < 0.001 ? "<0.1%" : BigDecimal.valueOf(chance * 100.0).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
        Font font = Minecraft.m_91087_().f_91062_;
        float scale = Math.min(1.0f, ((float)this.tileSize - 6.0f) / (float)Math.max(1, font.m_92895_(label)));
        graphics.m_280168_().m_85836_();
        PoseStack poseStack = graphics.m_280168_();
        float f = (float)(x + this.tileSize - 3) - (float)font.m_92895_(label) * scale;
        float f2 = y + this.tileSize - 3;
        Objects.requireNonNull(font);
        poseStack.m_252880_(f, f2 - 9.0f * scale, 250.0f);
        graphics.m_280168_().m_85841_(scale, scale, 1.0f);
        graphics.m_280056_(font, label, 0, 0, -1, true);
        graphics.m_280168_().m_85849_();
    }

    private void icon(GuiGraphics graphics, ItemStack stack, int x, int y, int rgb) {
        graphics.m_280509_(x, y, x + this.tileSize, y + this.tileSize, -1725878980);
        int iconSize = Math.min(24, this.tileSize - 8);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_((float)x + (float)(this.tileSize - iconSize) / 2.0f, (float)y + (float)(this.tileSize - iconSize) / 2.0f, 0.0f);
        graphics.m_280168_().m_85841_((float)iconSize / 16.0f, (float)iconSize / 16.0f, 1.0f);
        graphics.m_280480_(stack, 0, 0);
        graphics.m_280168_().m_85849_();
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 200.0f);
        graphics.m_286007_(() -> {
            graphics.m_280024_(x, y + 1, x + 1, y + this.tileSize - 1, 0x22000000 | rgb, 0xFF000000 | rgb);
            graphics.m_280024_(x + this.tileSize - 1, y + 1, x + this.tileSize, y + this.tileSize - 1, 0x22000000 | rgb, 0xFF000000 | rgb);
            graphics.m_280509_(x + 1, y + this.tileSize - 1, x + this.tileSize - 1, y + this.tileSize, 0xFF000000 | rgb);
            graphics.m_280024_(x + 1, y + this.tileSize - 6, x + this.tileSize - 1, y + this.tileSize - 1, rgb, 0x55000000 | rgb);
        });
        graphics.m_280168_().m_85849_();
    }

    boolean scroll(double mouseX, double mouseY, double amount) {
        int pages;
        if (!Screen.m_96638_()) {
            this.inspecting = false;
        }
        if (this.hovered >= 0 && this.rewards.get(this.hovered).table() && (this.popupContains(mouseX, mouseY) || this.iconAt(mouseX, mouseY) == this.hovered) && (pages = Math.max(1, (this.rewards.get(this.hovered).contents().size() + 32 - 1) / 32)) > 1) {
            this.page = Math.floorMod(this.page + (amount < 0.0 ? 1 : -1), pages);
            return true;
        }
        if (!RewardPreviewPanel.within(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
            return false;
        }
        if (this.inspecting) {
            return true;
        }
        this.scroll = Mth.m_14045_((int)(this.scroll - (int)(amount * (double)this.cellSize)), (int)0, (int)this.maxScroll());
        this.hovered = -1;
        return true;
    }
}

