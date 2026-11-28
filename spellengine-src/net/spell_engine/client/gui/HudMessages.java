/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.core.Holder$Reference
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 */
package net.spell_engine.client.gui;

import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.spell_engine.api.effect.EntityActionsAllowed;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.cost.Ammo;

public class HudMessages {
    public static final HudMessages INSTANCE = new HudMessages();
    public static final int DEFAULT_ERROR_MESSAGE_DURATION = 20;
    public static final int DEFAULT_ERROR_MESSAGE_FADEOUT = 10;
    private MessageState currentMessage;
    private static final String castAttemptPrefix = "hud.cast_attempt_error.";
    private boolean attemptDisplayed = false;

    public void castAttemptError(SpellCast.Attempt attempt) {
        if (this.attemptDisplayed) {
            return;
        }
        if (attempt.isSuccess() || attempt.isFail()) {
            this.attemptDisplayed = true;
        }
        if (!attempt.isFail() || !SpellEngineClient.config.showSpellCastErrors) {
            return;
        }
        String translationKey = castAttemptPrefix + attempt.result().toString().toLowerCase(Locale.ENGLISH);
        MutableComponent message = null;
        switch (attempt.result()) {
            case MISSING_ITEM: {
                Ammo.Searched item = attempt.missingItem().item();
                if (item == null) break;
                String itemName = I18n.m_118938_((String)item.getTranslationKey(), (Object[])new Object[0]);
                message = Component.m_237110_((String)translationKey, (Object[])new Object[]{itemName});
                break;
            }
            case ON_COOLDOWN: {
                message = Component.m_237115_((String)translationKey);
            }
        }
        if (message != null) {
            this.error((Component)message.m_130940_(ChatFormatting.RED));
        }
    }

    public void actionImpaired(EntityActionsAllowed.SemanticType reason) {
        this.error(I18n.m_118938_((String)("hud.action_impaired." + reason.toString().toLowerCase(Locale.ENGLISH)), (Object[])new Object[0]));
    }

    public void error(String message) {
        this.error(message, 20, 10);
    }

    public void error(String message, int duration, int fadeOut) {
        this.currentMessage = MessageState.error(message, duration, fadeOut);
    }

    public void error(Component text) {
        this.error(text, 20, 10);
    }

    public void error(Component text, int duration, int fadeOut) {
        this.currentMessage = new MessageState(text, duration, fadeOut);
    }

    public void onCooldownsChanged(List<ResourceLocation> before, List<ResourceLocation> after) {
        List<ResourceLocation> cooldownsRemoved = before.stream().filter(spellId -> !after.contains(spellId)).toList();
        ClientLevel world = Minecraft.m_91087_().f_91073_;
        if (world == null) {
            return;
        }
        if (cooldownsRemoved.isEmpty()) {
            return;
        }
        if (cooldownsRemoved.size() == 1) {
            ResourceLocation spellId2 = cooldownsRemoved.get(0);
            Holder.Reference spellEntry = SpellRegistry.from((Level)world).m_203636_(ResourceKey.m_135785_(SpellRegistry.KEY, (ResourceLocation)spellId2)).orElse(null);
            if (spellEntry == null) {
                return;
            }
            String spellName = SpellTooltip.spellTranslationKey(spellId2);
            this.info((Component)Component.m_237110_((String)"hud.cooldown_cleared.single", (Object[])new Object[]{I18n.m_118938_((String)spellName, (Object[])new Object[0])}));
        } else {
            this.info((Component)Component.m_237115_((String)"hud.cooldown_cleared.multiple"));
        }
    }

    public void info(Component message) {
        this.info(message, 20, 10);
    }

    public void infoQuick(Component message) {
        this.info(message, 10, 5);
    }

    public void info(Component message, int duration, int fadeOut) {
        this.currentMessage = new MessageState(message, duration, fadeOut);
    }

    public void tick() {
        if (this.currentMessage != null) {
            if (this.currentMessage.durationLeft <= 0) {
                this.currentMessage = null;
            } else {
                --this.currentMessage.durationLeft;
            }
        }
        Minecraft client = Minecraft.m_91087_();
        if (!client.f_91066_.f_92095_.m_90857_()) {
            this.attemptDisplayed = false;
        }
    }

    public MessageState currentError() {
        return this.currentMessage;
    }

    public static class MessageState {
        public Component message;
        public int durationLeft;
        public int fadeOut;

        public MessageState(Component message, int durationLeft, int fadeOut) {
            this.message = message;
            this.durationLeft = durationLeft;
            this.fadeOut = fadeOut;
        }

        public static MessageState error(String message, int durationLeft, int fadeOut) {
            return new MessageState((Component)Component.m_237113_((String)message).m_130940_(ChatFormatting.RED), durationLeft, fadeOut);
        }
    }
}

