/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockEntry
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.SimpleTextEntry
 *  com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 */
package com.finderfeed.fdbosses.client.boss_screen.text_block_processors;

import com.finderfeed.fdbosses.config.BossConfig;
import com.finderfeed.fdbosses.init.BossConfigs;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.TextBlockEntry;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_entries.SimpleTextEntry;
import com.finderfeed.fdlib.systems.simple_screen.fdwidgets.text_block.text_block_parser.TextBlockProcessor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class BossConfigFloatValueProcessor
extends TextBlockProcessor {
    public List<TextBlockEntry> parse(float textScale, boolean renderShadow, int textColor, HashMap<String, String> arguments) {
        float finalValue;
        String configName = arguments.get("config");
        String valueName = arguments.get("value");
        try {
            BossConfig bossConfig = BossConfigs.BOSS_CONFIG.get();
            Field field = BossConfig.class.getField(configName);
            Object config = field.get((Object)bossConfig);
            Field fieldWithValue = config.getClass().getField(valueName);
            Object obj = fieldWithValue.get(config);
            if (!(obj instanceof Number)) {
                throw new RuntimeException("A value " + valueName + " in " + configName + " is not float/int!");
            }
            finalValue = ((Number)obj).floatValue();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        SimpleTextEntry textEntry = new SimpleTextEntry((FormattedText)Component.m_237113_((String)"%.1f".formatted(Float.valueOf(finalValue))), textScale, renderShadow, textColor);
        return List.of(textEntry);
    }
}

