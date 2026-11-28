/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.tiny_config.ConfigManager
 */
package net.spell_engine.api.spell.container;

import java.util.List;
import net.spell_engine.api.spell.container.SpellContainer;
import net.tiny_config.ConfigManager;

public class SpellContainerTemplates {
    public static ConfigManager<Config> config = new ConfigManager("spell_container_templates_v2", (Object)SpellContainerTemplates.defaults()).builder().setDirectory("spell_engine").sanitize(true).build();

    public static Config defaults() {
        return new Config();
    }

    public static class Config {
        public SpellContainer spell_book = new SpellContainer(SpellContainer.ContentType.NONE, "", null, "", 0, List.of());
    }
}

