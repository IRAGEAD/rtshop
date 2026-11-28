/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.inventory.Slot
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package com.pla.spiral_abyss.client.screen;

import com.pla.spiral_abyss.client.screen.AbyssHudConfigScreen;
import com.pla.spiral_abyss.client.screen.JournalNavigation;
import com.pla.spiral_abyss.config.AbyssClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.common.ForgeConfigSpec;

public final class AbyssClientConfigScreen
extends Screen {
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_HEIGHT = 166;
    private static final int BUTTON_SIZE = 22;
    private final Screen parent;
    private EditBox xField;
    private EditBox yField;
    private Button menuButton;
    private int left;
    private int footerY;
    private boolean draggingButton;
    private boolean syncingFields;
    private boolean pendingSave;
    private double grabX;
    private double grabY;

    public AbyssClientConfigScreen(Screen parent) {
        super((Component)Component.m_237115_((String)"screen.spiral_abyss.config.title"));
        this.parent = parent;
    }

    protected void m_7856_() {
        this.saveChanges();
        this.draggingButton = false;
        this.m_7897_(false);
        this.left = this.f_96543_ / 2 - 150;
        this.footerY = this.f_96544_ - 26;
        this.xField = this.coordinateField(AbyssClientConfig.INVENTORY_BUTTON_X, "x", this.left + 12);
        this.yField = this.coordinateField(AbyssClientConfig.INVENTORY_BUTTON_Y, "y", this.left + 86);
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237115_((String)"screen.spiral_abyss.config.reset"), button -> {
            this.moveButton((Integer)AbyssClientConfig.INVENTORY_BUTTON_X.getDefault(), (Integer)AbyssClientConfig.INVENTORY_BUTTON_Y.getDefault());
            this.saveChanges();
        }).m_252987_(this.left + 148, this.footerY, 86, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)CommonComponents.f_130655_, button -> this.m_7379_()).m_252987_(this.left + 242, this.footerY, 58, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237115_((String)"screen.spiral_abyss.config.hud"), button -> {
            this.saveChanges();
            this.f_96541_.m_91152_((Screen)new AbyssHudConfigScreen(this));
        }).m_252987_(this.f_96543_ / 2 - 80, 38, 160, 20).m_253136_());
        this.menuButton = (Button)this.m_142416_((GuiEventListener)JournalNavigation.icon(0, 0, 22, 22, JournalNavigation.MENU_ICON, 256, "screen.spiral_abyss.config.drag", false, () -> {}));
        this.positionButton();
    }

    private EditBox coordinateField(ForgeConfigSpec.IntValue setting, String axis, int x) {
        EditBox field = new EditBox(this.f_96547_, x, this.footerY, 54, 20, (Component)Component.m_237115_((String)("screen.spiral_abyss.config." + axis)));
        field.m_94199_(10);
        field.m_94153_(value -> {
            if (value.isEmpty()) {
                return true;
            }
            try {
                return Integer.parseInt(value) >= 0;
            }
            catch (NumberFormatException ex) {
                return false;
            }
        });
        field.m_94144_(Integer.toString((Integer)setting.get()));
        field.m_94151_(value -> {
            if (this.syncingFields || value.isEmpty()) {
                return;
            }
            int coordinate = Integer.parseInt(value);
            if (coordinate == (Integer)setting.get()) {
                return;
            }
            setting.set((Object)coordinate);
            this.pendingSave = true;
            this.positionButton();
            this.saveChanges();
        });
        return (EditBox)this.m_142416_((GuiEventListener)field);
    }

    private void positionButton() {
        this.menuButton.m_252865_(Math.min((Integer)AbyssClientConfig.INVENTORY_BUTTON_X.get(), Math.max(0, this.f_96543_ - 22)));
        this.menuButton.m_253211_(Math.min((Integer)AbyssClientConfig.INVENTORY_BUTTON_Y.get(), Math.max(0, this.f_96544_ - 22)));
    }

    private void syncFields(boolean force) {
        this.syncingFields = true;
        if (force || !this.xField.m_93696_()) {
            this.xField.m_94144_(Integer.toString((Integer)AbyssClientConfig.INVENTORY_BUTTON_X.get()));
        }
        if (force || !this.yField.m_93696_()) {
            this.yField.m_94144_(Integer.toString((Integer)AbyssClientConfig.INVENTORY_BUTTON_Y.get()));
        }
        this.syncingFields = false;
    }

    private void moveButton(int x, int y) {
        if (x != (Integer)AbyssClientConfig.INVENTORY_BUTTON_X.get() || y != (Integer)AbyssClientConfig.INVENTORY_BUTTON_Y.get()) {
            AbyssClientConfig.INVENTORY_BUTTON_X.set((Object)x);
            AbyssClientConfig.INVENTORY_BUTTON_Y.set((Object)y);
            this.pendingSave = true;
        }
        this.positionButton();
        this.syncFields(true);
    }

    private void saveChanges() {
        if (!this.pendingSave) {
            return;
        }
        AbyssClientConfig.SPEC.save();
        this.pendingSave = false;
    }

    public boolean m_6375_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.menuButton.m_5953_(mouseX, mouseY)) {
            this.draggingButton = true;
            this.grabX = mouseX - (double)this.menuButton.m_252754_();
            this.grabY = mouseY - (double)this.menuButton.m_252907_();
            this.m_7522_((GuiEventListener)this.menuButton);
            this.m_7897_(true);
            this.menuButton.m_7435_(this.f_96541_.m_91106_());
            return true;
        }
        return super.m_6375_(mouseX, mouseY, button);
    }

    public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingButton) {
            int x = (int)Math.round(Mth.m_14008_((double)(mouseX - this.grabX), (double)0.0, (double)Math.max(0, this.f_96543_ - 22)));
            int y = (int)Math.round(Mth.m_14008_((double)(mouseY - this.grabY), (double)0.0, (double)Math.max(0, this.f_96544_ - 22)));
            this.moveButton(x, y);
            return true;
        }
        return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingButton) {
            this.draggingButton = false;
            this.m_7897_(false);
            this.saveChanges();
            return true;
        }
        return super.m_6348_(mouseX, mouseY, button);
    }

    public void m_86600_() {
        this.xField.m_94120_();
        this.yField.m_94120_();
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean overlaps;
        this.positionButton();
        this.syncFields(false);
        this.m_280273_(graphics);
        int inventoryX = (this.f_96543_ - 176) / 2;
        int inventoryY = (this.f_96544_ - 166) / 2;
        this.renderInventory(graphics, inventoryX, inventoryY, mouseX, mouseY);
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0f, 0.0f, 300.0f);
        boolean bl = overlaps = this.menuButton.m_252754_() < inventoryX + 176 && this.menuButton.m_252754_() + 22 > inventoryX && this.menuButton.m_252907_() < inventoryY + 166 && this.menuButton.m_252907_() + 22 > inventoryY;
        if (overlaps) {
            graphics.m_280637_(inventoryX - 1, inventoryY - 1, 178, 168, -21931);
        }
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 8, 0xFFFFFF);
        graphics.m_280653_(this.f_96547_, (Component)Component.m_237115_((String)(overlaps ? "screen.spiral_abyss.config.overlap" : "screen.spiral_abyss.config.description")), this.f_96543_ / 2, 22, overlaps ? 0xFFAA55 : 0xAAAAAA);
        graphics.m_280488_(this.f_96547_, "X", this.left, this.footerY + 6, 0xFFFFFF);
        graphics.m_280488_(this.f_96547_, "Y", this.left + 74, this.footerY + 6, 0xFFFFFF);
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        graphics.m_280168_().m_85849_();
    }

    private void renderInventory(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        graphics.m_280218_(INVENTORY_TEXTURE, x, y, 0, 0, 176, 166);
        graphics.m_280614_(this.f_96547_, (Component)Component.m_237115_((String)"container.crafting"), x + 97, y + 8, 0x404040, false);
        if (this.f_96541_.f_91074_ == null) {
            return;
        }
        InventoryScreen.m_274545_((GuiGraphics)graphics, (int)(x + 51), (int)(y + 75), (int)30, (float)(x + 51 - mouseX), (float)(y + 25 - mouseY), (LivingEntity)this.f_96541_.f_91074_);
        for (Slot slot : this.f_96541_.f_91074_.f_36095_.f_38839_) {
            if (!slot.m_6657_()) continue;
            graphics.m_280480_(slot.m_7993_(), x + slot.f_40220_, y + slot.f_40221_);
            graphics.m_280370_(this.f_96547_, slot.m_7993_(), x + slot.f_40220_, y + slot.f_40221_);
        }
    }

    public void m_7379_() {
        this.saveChanges();
        this.f_96541_.m_91152_(this.parent);
    }

    public void m_7861_() {
        this.saveChanges();
        this.draggingButton = false;
        this.m_7897_(false);
    }
}

