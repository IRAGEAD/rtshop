/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockEntry
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.interactions.TextBlockEntryInteraction
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.SimpleTextEntry
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.image_entry.ImageInText
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.image_entry.ImageTextEntry
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor
 *  com.mojang.blaze3d.vertex.PoseStack
 *  net.minecraft.Util
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.texture.TextureAtlas
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.MobEffectTextureManager
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.effect.MobEffect
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.finderfeed.fdbosses.client.boss_screen.text_block_processors;

import com.finderfeed.fdbosses.client.BossRenderUtil;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockEntry;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.interactions.TextBlockEntryInteraction;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.SimpleTextEntry;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.image_entry.ImageInText;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.image_entry.ImageTextEntry;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

public class MobEffectTextProcessor
extends TextBlockProcessor {
    public List<TextBlockEntry> parse(float textScale, boolean renderShadow, int textColor, HashMap<String, String> arguments) {
        int level;
        String s;
        if (!arguments.containsKey("id")) {
            throw new RuntimeException("Couldn't find 'id' on mob effect text processor");
        }
        String id = arguments.get("id");
        ResourceLocation effectId = ResourceLocation.m_135820_((String)id);
        MobEffect effect = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            throw new RuntimeException("Unknown effect provided in mob effect text processor: " + id);
        }
        int color = effect.m_19484_();
        MobEffectTextureManager mobeffecttexturemanager = Minecraft.m_91087_().m_91306_();
        TextureAtlasSprite sprite = mobeffecttexturemanager.m_118732_(effect);
        TextureAtlas atlas = mobeffecttexturemanager.f_118884_;
        ImageInText imageInText = new ImageInText(sprite.m_247685_(), sprite.m_118409_(), sprite.m_118411_(), sprite.m_118410_(), sprite.m_118412_(), (float)atlas.f_276067_, (float)atlas.f_276070_);
        ResourceLocation location = ForgeRegistries.MOB_EFFECTS.getKey((Object)effect);
        String key = Util.m_137492_((String)"effect_description", (ResourceLocation)location);
        TextBlockEntryInteraction interaction = TextBlockEntryInteraction.hoverOver((textBlock, graphics, mx, my) -> {
            PoseStack matrices = graphics.m_280168_();
            matrices.m_85836_();
            matrices.m_252880_(0.0f, 0.0f, 100.0f);
            BossRenderUtil.renderBossScreenTooltip(graphics, (Component)Component.m_237115_((String)key), mx, my, 200.0f, textColor, 1.0f);
            matrices.m_85849_();
        });
        ImageTextEntry imageTextEntry = new ImageTextEntry(imageInText, textScale, interaction);
        MutableComponent displayName = effect.m_19482_().m_6881_();
        if (arguments.containsKey("level") && !(s = this.levelToString(level = Integer.parseInt(arguments.get("level")))).isEmpty()) {
            displayName = displayName.m_130946_(" " + s);
        }
        SimpleTextEntry text = new SimpleTextEntry((FormattedText)displayName.m_130948_(Style.f_131099_.m_178520_(color).m_131162_(Boolean.valueOf(true))), textScale, renderShadow, textColor, interaction);
        return List.of(imageTextEntry, text);
    }

    private String levelToString(int level) {
        switch (level) {
            case 1: {
                return "II";
            }
            case 2: {
                return "III";
            }
            case 3: {
                return "IV";
            }
            case 4: {
                return "V";
            }
            case 5: {
                return "VI";
            }
            case 6: {
                return "VII";
            }
            case 7: {
                return "VIII";
            }
        }
        return "";
    }
}

