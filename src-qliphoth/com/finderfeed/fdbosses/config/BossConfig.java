/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.finderfeed.fdlib.systems.config.Comment
 *  com.finderfeed.fdlib.systems.config.ConfigValue
 *  com.finderfeed.fdlib.systems.config.ManualSerializeable
 *  com.finderfeed.fdlib.systems.config.ReflectiveJsonConfig
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package com.finderfeed.fdbosses.config;

import com.finderfeed.fdbosses.FDBosses;
import com.finderfeed.fdbosses.config.ChesedConfig;
import com.finderfeed.fdbosses.config.EffectConfig;
import com.finderfeed.fdbosses.config.GeburahConfig;
import com.finderfeed.fdbosses.config.ItemConfig;
import com.finderfeed.fdbosses.config.MalkuthConfig;
import com.finderfeed.fdlib.systems.config.Comment;
import com.finderfeed.fdlib.systems.config.ConfigValue;
import com.finderfeed.fdlib.systems.config.ManualSerializeable;
import com.finderfeed.fdlib.systems.config.ReflectiveJsonConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BossConfig
extends ReflectiveJsonConfig
implements ManualSerializeable {
    @ConfigValue
    @Comment(value="Should bosses despawn if no target is found")
    public boolean bossesDespawn = false;
    @ConfigValue
    public float peacefulDifficultyBossDamageMuliplier = 0.5f;
    @ConfigValue
    public float easyDifficultyBossDamageMultiplier = 0.75f;
    @ConfigValue
    public float normalDifficultyBossDamageMultiplier = 1.0f;
    @ConfigValue
    public float hardDifficultyBossDamageMultiplier = 1.25f;
    public List<String> blocksAllowedToBreakInArena = new ArrayList<String>(List.of("(grave)|(tomb)", "^tombstone:grave_simple$"));
    public List<Pattern> blocksAllowedToBreakInArenaPatterns = new ArrayList<Pattern>();
    @ConfigValue
    public ChesedConfig chesedConfig = new ChesedConfig();
    @ConfigValue
    public EffectConfig effectConfig = new EffectConfig();
    @ConfigValue
    public ItemConfig itemConfig = new ItemConfig();
    @ConfigValue
    public MalkuthConfig malkuthConfig = new MalkuthConfig();
    @ConfigValue
    public GeburahConfig geburahConfig = new GeburahConfig();

    public BossConfig() {
        super(FDBosses.location("bosses"));
    }

    public boolean isClientside() {
        return false;
    }

    public boolean process(JsonObject jsonObject) {
        boolean res = this.processBlocksAllowedInArenaSave(jsonObject);
        return res;
    }

    private boolean processBlocksAllowedInArenaSave(JsonObject jsonObject) {
        if (jsonObject.has("blocksAllowedToBreakInArena")) {
            try {
                JsonArray array = jsonObject.getAsJsonArray("blocksAllowedToBreakInArena");
                this.blocksAllowedToBreakInArena = new ArrayList<String>();
                this.blocksAllowedToBreakInArenaPatterns.clear();
                for (JsonElement s : array) {
                    String regex = s.getAsString();
                    try {
                        Pattern reg = Pattern.compile(regex);
                        this.blocksAllowedToBreakInArena.add(regex);
                        this.blocksAllowedToBreakInArenaPatterns.add(reg);
                    }
                    catch (PatternSyntaxException patternSyntaxException) {
                        FDBosses.LOGGER.error("Error parsing regex pattern: " + regex + ", skipping...");
                        patternSyntaxException.printStackTrace();
                    }
                }
                return false;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        jsonObject.remove("_comment_blocksAllowedToBreakInArena");
        jsonObject.remove("blocksAllowedToBreakInArena");
        jsonObject.addProperty("_comment_blocksAllowedToBreakInArena", "Block id regex. If you wish to add a single block id, do it like that: ^modid:block_id$");
        JsonArray array = new JsonArray();
        this.blocksAllowedToBreakInArenaPatterns.clear();
        for (String s : this.blocksAllowedToBreakInArena) {
            array.add(s);
            this.blocksAllowedToBreakInArenaPatterns.add(Pattern.compile(s));
        }
        jsonObject.add("blocksAllowedToBreakInArena", (JsonElement)array);
        return true;
    }
}

