/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.world.phys.BlockHitResult
 */
package com.pla.spiral_abyss.client.platform;

import com.pla.spiral_abyss.platform.event.AbyssEvents;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.BlockHitResult;

public final class AbyssClientEvents {

    @FunctionalInterface
    public static interface Overlay {
        public void render(Object var1, GuiGraphics var2, float var3, int var4, int var5);
    }

    public static class ScreenInit {
        private final Screen screen;
        private final Consumer<AbstractWidget> add;

        public ScreenInit(Screen screen, Consumer<AbstractWidget> add) {
            this.screen = screen;
            this.add = add;
        }

        public Screen getScreen() {
            return this.screen;
        }

        public void addListener(AbstractWidget widget) {
            this.add.accept(widget);
        }
    }

    public static class ScreenEvent {

        public static class KeyPressed {

            public static class Pre
            extends AbyssEvents.Event {
                public Screen screen;
                public int keyCode;
                public int scanCode;

                public Pre(Screen screen, int keyCode, int scanCode) {
                    this.screen = screen;
                    this.keyCode = keyCode;
                    this.scanCode = scanCode;
                }

                public Screen getScreen() {
                    return this.screen;
                }

                public int getKeyCode() {
                    return this.keyCode;
                }

                public int getScanCode() {
                    return this.scanCode;
                }
            }
        }

        public static class Render {

            public static class Pre
            extends AbyssEvents.Event {
                public Screen screen;

                public Pre(Screen screen) {
                    this.screen = screen;
                }

                public Screen getScreen() {
                    return this.screen;
                }
            }
        }
    }

    public static class RenderHighlightEvent {

        public static class Block
        extends AbyssEvents.Event {
            public BlockHitResult target;

            public Block(BlockHitResult target) {
                this.target = target;
            }

            public BlockHitResult getTarget() {
                return this.target;
            }
        }
    }

    public static class RenderHandEvent
    extends AbyssEvents.Event {
    }

    public static class RenderGuiEvent {

        public static class Pre
        extends AbyssEvents.Event {
        }
    }

    public static class ClientPlayerNetworkEvent {

        public static class LoggingOut
        extends AbyssEvents.Event {
        }
    }

    public static class TickEvent {

        public static class ClientTickEvent
        extends AbyssEvents.Event {
            public final Phase phase;

            public ClientTickEvent(Phase phase) {
                this.phase = phase;
            }
        }

        public static enum Phase {
            START,
            END;

        }
    }
}

