/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Key
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.Holder
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec2
 *  org.jetbrains.annotations.Nullable
 */
package net.spell_engine.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.Drawable;
import net.spell_engine.client.gui.HudElement;
import net.spell_engine.client.gui.HudKeyVisuals;
import net.spell_engine.client.gui.HudMessages;
import net.spell_engine.client.input.SpellHotbar;
import net.spell_engine.client.util.Rect;
import net.spell_engine.client.util.SpellRender;
import net.spell_engine.client.util.TextureFile;
import net.spell_engine.config.ClientConfig;
import net.spell_engine.config.HudConfig;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.cost.SpellCooldownManager;
import net.spell_engine.mixin.client.control.KeybindingAccessor;
import org.jetbrains.annotations.Nullable;

public class HudRenderHelper {
    public static void render(GuiGraphics context, float tickDelta) {
        HudRenderHelper.render(context, tickDelta, false);
    }

    public static void render(GuiGraphics context, float tickDelta, boolean config) {
        HudConfig hudConfig = (HudConfig)SpellEngineClient.hudConfig.value;
        Minecraft client = Minecraft.m_91087_();
        LocalPlayer player = client.f_91074_;
        if ((player == null || player.m_5833_()) && !config) {
            return;
        }
        ClientConfig clientConfig = SpellEngineClient.config;
        TargetWidget.ViewModel targetViewModel = TargetWidget.ViewModel.mock();
        boolean renderHotbar = true;
        SpellHotBarWidget.ViewModel hotbarViewModel = SpellHotBarWidget.ViewModel.mock();
        ErrorMessageWidget.ViewModel errorViewModel = ErrorMessageWidget.ViewModel.mock();
        CastBarWidget.ViewModel castBarViewModel = null;
        if (config) {
            castBarViewModel = CastBarWidget.ViewModel.mock();
        } else {
            targetViewModel = TargetWidget.ViewModel.from(player);
        }
        if (player != null) {
            SpellCaster.Client caster = (SpellCaster.Client)player;
            if (SpellHotbar.INSTANCE.slots.isEmpty()) {
                hotbarViewModel = SpellHotBarWidget.ViewModel.empty;
            } else {
                SpellCooldownManager cooldownManager = caster.getCooldownManager();
                List<SpellHotBarWidget.SpellViewModel> spells = SpellHotbar.INSTANCE.slots.stream().map(slot -> {
                    Holder<Spell> spellEntry = slot.spell();
                    ResourceLocation id = spellEntry != null ? ((ResourceKey)spellEntry.m_203543_().get()).m_135782_() : null;
                    ItemStack itemStack = slot.itemStack();
                    boolean useItem = itemStack != null;
                    float cooldownProgress = 0.0f;
                    if (useItem) {
                        cooldownProgress = player.m_36335_().m_41521_(itemStack.m_41720_(), tickDelta);
                    } else if (spellEntry != null) {
                        cooldownProgress = cooldownManager.getCooldownProgress(spellEntry, tickDelta);
                    }
                    return new SpellHotBarWidget.SpellViewModel(useItem ? null : SpellRender.iconTexture(id), (ItemStack)(useItem ? itemStack : null), cooldownProgress, SpellHotBarWidget.KeyBindingViewModel.from(slot.getKeyBinding(client.f_91066_)), slot.modifier() != null ? SpellHotBarWidget.KeyBindingViewModel.from(slot.modifier()) : null);
                }).collect(Collectors.toList());
                hotbarViewModel = new SpellHotBarWidget.ViewModel(spells);
            }
            renderHotbar = true;
            SpellCast.Progress spellCast = caster.getSpellCastProgress();
            if (spellCast != null) {
                castBarViewModel = new CastBarWidget.ViewModel(((Spell)spellCast.process().spell().m_203334_()).school.color, spellCast.ratio(), spellCast.process().length(), SpellRender.iconTexture(spellCast.process().id()), true, SpellParameters.isChanneled((Spell)spellCast.process().spell().m_203334_()));
            }
            if (!config) {
                HudMessages hudMessages = HudMessages.INSTANCE;
                HudMessages.MessageState error = hudMessages.currentError();
                errorViewModel = error != null && error.durationLeft > 0 ? ErrorMessageWidget.ViewModel.from(error.message, error.durationLeft, error.fadeOut, tickDelta) : null;
            }
        }
        int screenWidth = client.m_91268_().m_85445_();
        int screenHeight = client.m_91268_().m_85446_();
        Vec2 originPoint = hudConfig.castbar.base.origin.getPoint(screenWidth, screenHeight);
        Vec2 baseOffset = originPoint.m_165910_(hudConfig.castbar.base.offset);
        if (castBarViewModel != null) {
            CastBarWidget.render(context, tickDelta, hudConfig, baseOffset, castBarViewModel);
        }
        if (hudConfig.castbar.target.visible) {
            Vec2 targetOffset = baseOffset.m_165910_(hudConfig.castbar.target.offset);
            TargetWidget.render(context, tickDelta, targetOffset, targetViewModel);
        }
        if (renderHotbar || config) {
            if (config && (hotbarViewModel == null || hotbarViewModel.isEmpty())) {
                hotbarViewModel = SpellHotBarWidget.ViewModel.mock();
            }
            SpellHotBarWidget.render(context, screenWidth, screenHeight, hotbarViewModel);
        }
        if (errorViewModel != null) {
            ErrorMessageWidget.render(context, hudConfig, screenWidth, screenHeight, errorViewModel);
        }
    }

    public static class TargetWidget {
        public static void render(GuiGraphics context, float tickDelta, Vec2 starting, ViewModel viewModel) {
            Minecraft client = Minecraft.m_91087_();
            Font textRenderer = client.f_91065_.m_93082_();
            int textWidth = textRenderer.m_92895_(viewModel.text);
            int x = (int)(starting.f_82470_ - (float)textWidth / 2.0f);
            int y = (int)starting.f_82471_;
            int opacity = 255;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            context.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
            Objects.requireNonNull(textRenderer);
            context.m_280509_(x - 2, y - 2, x + textWidth + 2, y + 9 + 2, client.f_91066_.m_92143_(0));
            context.m_280488_(textRenderer, viewModel.text, x, y, 0xFFFFFF);
            RenderSystem.disableBlend();
            context.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public record ViewModel(String text) {
            public static ViewModel mock() {
                return new ViewModel("Target name");
            }

            public static ViewModel from(LocalPlayer player) {
                SpellCaster.Client caster = (SpellCaster.Client)player;
                List<Entity> targets = caster.getCurrentTargets();
                String text = targets.size() == 1 ? targets.get(0).m_7755_().getString() : "";
                return new ViewModel(text);
            }
        }
    }

    public class SpellHotBarWidget {
        public static Rect lastRendered;
        private static final TextureFile HOTBAR;
        private static final int slotHeight = 22;
        private static final int slotWidth = 20;
        private static final Map<Integer, String> customHudKeyLabels;

        private static String acronym(String phrase, int maxLength) {
            StringBuilder result = new StringBuilder();
            for (String token : phrase.split("\\s+")) {
                result.append(token.toUpperCase().charAt(0));
            }
            String resultString = result.toString();
            if (resultString.length() > maxLength) {
                resultString = resultString.substring(0, maxLength);
            }
            return result.toString();
        }

        public static void render(GuiGraphics context, int screenWidth, int screenHeight, ViewModel viewModel) {
            HudElement config = ((HudConfig)SpellEngineClient.hudConfig.value).hotbar;
            Minecraft client = Minecraft.m_91087_();
            Font textRenderer = client.f_91065_.m_93082_();
            if (viewModel.spells.isEmpty()) {
                return;
            }
            float estimatedWidth = 20 * viewModel.spells.size();
            float estimatedHeight = 22.0f;
            Vec2 origin = config.origin.getPoint(screenWidth, screenHeight).m_165910_(config.offset).m_165910_(new Vec2(estimatedWidth * -0.5f, estimatedHeight * -0.5f));
            lastRendered = new Rect(origin, origin.m_165910_(new Vec2(estimatedWidth, estimatedHeight)));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float barOpacity = 1.0f;
            context.m_280246_(1.0f, 1.0f, 1.0f, barOpacity);
            context.m_280163_(HOTBAR.id(), (int)origin.f_82470_, (int)origin.f_82471_, 0.0f, 0.0f, 10, 22, HOTBAR.width(), HOTBAR.height());
            int middleElements = viewModel.spells.size() - 1;
            for (int i = 0; i < middleElements; ++i) {
                context.m_280163_(HOTBAR.id(), (int)origin.f_82470_ + 10 + i * 20, (int)origin.f_82471_, 10.0f, 0.0f, 20, 22, HOTBAR.width(), HOTBAR.height());
            }
            context.m_280163_(HOTBAR.id(), (int)origin.f_82470_ + 10 + middleElements * 20, (int)origin.f_82471_, 170.0f, 0.0f, 12, 22, HOTBAR.width(), HOTBAR.height());
            context.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
            Vec2 iconsOffset = new Vec2(3.0f, 3.0f);
            int iconSize = 16;
            for (int i = 0; i < viewModel.spells.size(); ++i) {
                SpellViewModel spell = viewModel.spells.get(i);
                int x = (int)(origin.f_82470_ + iconsOffset.f_82470_) + 20 * i;
                int y = (int)(origin.f_82471_ + iconsOffset.f_82471_);
                RenderSystem.enableBlend();
                if (spell.iconId != null) {
                    context.m_280163_(spell.iconId, x, y, 0.0f, 0.0f, iconSize, iconSize, iconSize, iconSize);
                } else if (spell.itemStack != null) {
                    context.m_280480_(spell.itemStack, x, y);
                }
                if (spell.cooldown > 0.0f) {
                    SpellHotBarWidget.renderCooldown(context, spell.cooldown, x, y);
                }
                if (spell.keybinding() == null) continue;
                int keybindingX = x + iconSize / 2;
                int keybindingY = (int)origin.f_82471_ + 2;
                if (spell.modifier != null) {
                    keybindingX += 2;
                    int spacing = 1;
                    int modifierWidth = spell.modifier().width(textRenderer);
                    int keybindingWidth = spell.keybinding().width(textRenderer);
                    int totalWidth = modifierWidth + keybindingWidth;
                    SpellHotBarWidget.drawKeybinding(context, textRenderer, spell.modifier, keybindingX -= totalWidth / 2, keybindingY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                    SpellHotBarWidget.drawKeybinding(context, textRenderer, spell.keybinding(), keybindingX += modifierWidth + spacing, keybindingY, Drawable.Anchor.LEADING, Drawable.Anchor.TRAILING);
                    continue;
                }
                SpellHotBarWidget.drawKeybinding(context, textRenderer, spell.keybinding(), keybindingX, keybindingY, Drawable.Anchor.CENTER, Drawable.Anchor.TRAILING);
            }
            RenderSystem.disableBlend();
            context.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
        }

        private static void drawKeybinding(GuiGraphics context, Font textRenderer, KeyBindingViewModel keybinding, int x, int y, Drawable.Anchor horizontalAnchor, Drawable.Anchor verticalAnchor) {
            if (keybinding.drawable != null) {
                keybinding.drawable.draw(context, x, y, horizontalAnchor, verticalAnchor);
            } else {
                int textLength = textRenderer.m_92895_(keybinding.label);
                int xOffset = 0;
                switch (horizontalAnchor) {
                    case TRAILING: {
                        xOffset = -textLength / 2;
                        break;
                    }
                    case CENTER: {
                        xOffset = 0;
                        break;
                    }
                    case LEADING: {
                        xOffset = textLength / 2;
                    }
                }
                HudKeyVisuals.buttonLeading.draw(context, (x += xOffset) - textLength / 2, y, Drawable.Anchor.TRAILING, verticalAnchor);
                HudKeyVisuals.buttonCenter.drawFlexibleWidth(context, x - textLength / 2, y, textLength, verticalAnchor);
                HudKeyVisuals.buttonTrailing.draw(context, x + textLength / 2, y, Drawable.Anchor.LEADING, verticalAnchor);
                context.m_280137_(textRenderer, keybinding.label, x, y - 10, 0xFFFFFF);
            }
        }

        private static void renderCooldown(GuiGraphics context, float progress, int x, int y) {
            int k = y + Mth.m_14143_((float)(16.0f * (1.0f - progress)));
            int l = k + Mth.m_14167_((float)(16.0f * progress));
            context.m_285944_(RenderType.m_286086_(), x, k, x + 16, l, Integer.MAX_VALUE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        static {
            HOTBAR = new TextureFile(new ResourceLocation("textures/gui/widgets.png"), 256, 256);
            customHudKeyLabels = Map.of(342, "Al", 346, "Al", 340, "\u2191", 344, "\u2191");
        }

        public record ViewModel(List<SpellViewModel> spells) {
            public static final ViewModel empty = new ViewModel(List.of());

            public static ViewModel mock() {
                return new ViewModel(List.of(new SpellViewModel(SpellRender.iconTexture(new ResourceLocation("spell_engine", "dummy_spell")), null, 0.0f, new KeyBindingViewModel("1", null), null), new SpellViewModel(SpellRender.iconTexture(new ResourceLocation("spell_engine", "dummy_spell")), null, 0.0f, new KeyBindingViewModel("2", null), null), new SpellViewModel(SpellRender.iconTexture(new ResourceLocation("spell_engine", "dummy_spell")), null, 0.0f, new KeyBindingViewModel("3", null), null)));
            }

            public boolean isEmpty() {
                return this.spells.isEmpty();
            }
        }

        public record SpellViewModel(@Nullable ResourceLocation iconId, @Nullable ItemStack itemStack, float cooldown, KeyBindingViewModel keybinding, @Nullable KeyBindingViewModel modifier) {
        }

        public record KeyBindingViewModel(String label, @Nullable Drawable.Component drawable) {
            public static KeyBindingViewModel from(@Nullable KeyMapping keyBinding) {
                if (keyBinding == null) {
                    return new KeyBindingViewModel("", null);
                }
                InputConstants.Key boundKey = ((KeybindingAccessor)keyBinding).spellEngine_getBoundKey();
                String key = boundKey.toString();
                Drawable.Component drawable = HudKeyVisuals.custom.get(key);
                if (drawable != null) {
                    return new KeyBindingViewModel("", drawable);
                }
                String customLabel = customHudKeyLabels.get(boundKey.m_84873_());
                if (customLabel != null) {
                    return new KeyBindingViewModel(customLabel, null);
                }
                String label = keyBinding.m_90863_().getString().toUpperCase(Locale.US);
                label = SpellHotBarWidget.acronym(label, 3);
                return new KeyBindingViewModel(label, null);
            }

            public int width(Font textRenderer) {
                if (this.drawable != null) {
                    return this.drawable.draw().width();
                }
                return textRenderer.m_92895_(this.label);
            }
        }
    }

    public static class ErrorMessageWidget {
        public static Rect lastRendered;

        public static void render(GuiGraphics context, HudConfig hudConfig, int screenWidth, int screenHeight, ViewModel viewModel) {
            int alpha = (int)(viewModel.opacity * 255.0f);
            if (alpha < 10) {
                return;
            }
            Minecraft client = Minecraft.m_91087_();
            Font textRenderer = client.f_91065_.m_93082_();
            int textWidth = textRenderer.m_92852_((FormattedText)viewModel.message);
            Objects.requireNonNull(textRenderer);
            int textHeight = 9;
            HudElement config = hudConfig.error_message;
            Vec2 origin = config.origin.getPoint(screenWidth, screenHeight).m_165910_(config.offset);
            int x = (int)(origin.f_82470_ - (float)textWidth / 2.0f);
            int y = (int)origin.f_82471_;
            lastRendered = new Rect(new Vec2((float)x, (float)y), new Vec2((float)(x + textWidth), (float)(y + textHeight)));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Objects.requireNonNull(textRenderer);
            context.m_280509_(x - 2, y - 2, x + textWidth + 2, y + 9 + 2, client.f_91066_.m_92143_(0));
            context.m_280430_(textRenderer, viewModel.message(), x, y, 0xFFFFFF + (alpha << 24));
            RenderSystem.disableBlend();
        }

        public record ViewModel(Component message, float opacity) {
            public static ViewModel mock() {
                return new ViewModel((Component)Component.m_237113_((String)"Error Message!").m_130940_(ChatFormatting.RED), 1.0f);
            }

            public static ViewModel from(Component message, int durationLeft, int fadeOut, float tickDelta) {
                float tick = (float)durationLeft - tickDelta;
                float opacity = tick > (float)fadeOut ? 1.0f : tick / (float)fadeOut;
                return new ViewModel(message, opacity);
            }
        }
    }

    public static class CastBarWidget {
        public static Rect lastRendered;
        private static final float tailWidth = 5.0f;
        public static final float minWidth = 10.0f;
        private static final int textureWidth = 182;
        private static final int textureHeight = 10;
        private static final int barHeight = 5;
        private static final ResourceLocation CAST_BAR;
        private static final int spellIconSize = 16;

        public static void render(GuiGraphics context, float tickDelta, HudConfig hudConfig, Vec2 starting, ViewModel viewModel) {
            int barWidth = hudConfig.castbar.width;
            float totalWidth = (float)barWidth + 10.0f;
            int totalHeight = 5;
            int x = (int)(starting.f_82470_ - totalWidth / 2.0f);
            int y = (int)(starting.f_82471_ - (float)(totalHeight / 2));
            lastRendered = new Rect(new Vec2((float)x, (float)y), new Vec2((float)x + totalWidth, (float)(y + totalHeight)));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float red = (float)(viewModel.color >> 16 & 0xFF) / 255.0f;
            float green = (float)(viewModel.color >> 8 & 0xFF) / 255.0f;
            float blue = (float)(viewModel.color & 0xFF) / 255.0f;
            context.m_280246_(red, green, blue, 1.0f);
            CastBarWidget.renderBar(context, barWidth, true, 1.0f, x, y);
            float partialProgress = 0.0f;
            if (viewModel.allowTickDelta && viewModel.castDuration > 0.0f) {
                partialProgress = tickDelta / viewModel.castDuration;
            }
            float progress = viewModel.reverse() ? 1.0f - viewModel.progress - partialProgress : viewModel.progress + partialProgress;
            CastBarWidget.renderBar(context, barWidth, false, progress, x, y);
            context.m_280246_(1.0f, 1.0f, 1.0f, 1.0f);
            if (hudConfig.castbar.icon.visible && viewModel.iconTexture != null) {
                x = (int)(starting.f_82470_ + hudConfig.castbar.icon.offset.f_82470_);
                y = (int)(starting.f_82471_ + hudConfig.castbar.icon.offset.f_82471_);
                context.m_280163_(viewModel.iconTexture, x, y, 0.0f, 0.0f, 16, 16, 16, 16);
            }
            RenderSystem.disableBlend();
        }

        private static void renderBar(GuiGraphics context, int barWidth, boolean isBackground, float progress, int x, int y) {
            float totalWidth = (float)barWidth + 10.0f;
            float centerWidth = totalWidth - 10.0f;
            float leftRenderBegin = 0.0f;
            float centerRenderBegin = 5.0f;
            float rightRenderBegin = totalWidth - 5.0f;
            CastBarWidget.renderBarPart(context, isBackground, PART.LEFT, progress, leftRenderBegin, 5.0f, x, y, totalWidth);
            CastBarWidget.renderBarPart(context, isBackground, PART.CENTER, progress, centerRenderBegin, centerRenderBegin + centerWidth, x, y, totalWidth);
            CastBarWidget.renderBarPart(context, isBackground, PART.RIGHT, progress, rightRenderBegin, totalWidth, x, y, totalWidth);
        }

        private static void renderBarPart(GuiGraphics context, boolean isBackground, PART part, float progress, float renderBegin, float renderEnd, int x, int y, float totalWidth) {
            int u = 0;
            float partMaxWidth = renderEnd - renderBegin;
            float progressRange = (renderEnd - renderBegin) / totalWidth;
            float progressFloor = renderBegin / totalWidth;
            float adjustedProgress = Math.min(Math.max(progress - progressFloor, 0.0f), progressRange) / progressRange;
            int width = Math.round(adjustedProgress * partMaxWidth);
            switch (part.ordinal()) {
                case 0: {
                    u = 0;
                    break;
                }
                case 1: {
                    u = 5;
                    break;
                }
                case 2: {
                    u = 177;
                }
            }
            int v = isBackground ? 0 : 5;
            context.m_280163_(CAST_BAR, (int)((float)x + renderBegin), y, (float)u, (float)v, width, 5, 182, 10);
        }

        static {
            CAST_BAR = new ResourceLocation("spell_engine", "textures/hud/castbar.png");
        }

        public record ViewModel(int color, float progress, float castDuration, ResourceLocation iconTexture, boolean allowTickDelta, boolean reverse) {
            public static ViewModel mock() {
                return new ViewModel(0xFF3300, 0.5f, 1.0f, SpellRender.iconTexture(new ResourceLocation("spell_engine", "dummy_spell")), false, false);
            }
        }

        static enum PART {
            LEFT,
            CENTER,
            RIGHT;

        }
    }
}

