package com.rtshop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** 중세풍 나무 단추 (무쇠 띠·리벳). Button.builder 대신 MButton.of 로 만든다. */
public class MButton extends Button {
    protected MButton(int x, int y, int w, int h, Component msg, OnPress onPress) {
        super(x, y, w, h, msg, onPress, DEFAULT_NARRATION);
    }

    public static B of(Component msg, OnPress onPress) {
        return new B(msg, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        Ui.button(g, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(), getMessage().getString(), isHoveredOrFocused(), this.active);
    }

    public static final class B {
        private final Component msg;
        private final OnPress onPress;
        private int x, y, w = 150, h = 20;
        private Tooltip tip;

        B(Component msg, OnPress onPress) {
            this.msg = msg;
            this.onPress = onPress;
        }

        public B bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        public B pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public B size(int w, int h) {
            this.w = w;
            this.h = h;
            return this;
        }

        public B width(int w) {
            this.w = w;
            return this;
        }

        public B tooltip(Tooltip t) {
            this.tip = t;
            return this;
        }

        public Button build() {
            MButton b = new MButton(x, y, w, h, msg, onPress);
            if (tip != null) b.setTooltip(tip);
            return b;
        }
    }
}
