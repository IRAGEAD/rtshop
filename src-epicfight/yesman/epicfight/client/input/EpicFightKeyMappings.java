/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterKeyMappingsEvent
 *  net.minecraftforge.client.settings.IKeyConflictContext
 *  net.minecraftforge.client.settings.KeyConflictContext
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package yesman.epicfight.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.client.input.CombatKeyMapping;

@Mod.EventBusSubscriber(modid="epicfight", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
public class EpicFightKeyMappings {
    public static final KeyMapping WEAPON_INNATE_SKILL_TOOLTIP = new KeyMapping("key.epicfight.show_tooltip", (IKeyConflictContext)KeyConflictContext.GUI, InputConstants.Type.KEYSYM, 340, "key.epicfight.gui");
    public static final KeyMapping SKILL_EDIT = new KeyMapping("key.epicfight.skill_gui", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 75, "key.epicfight.gui");
    public static final KeyMapping OPEN_CONFIG_SCREEN = new KeyMapping("key.epicfight.config", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, -1, "key.epicfight.gui");
    public static final KeyMapping DODGE = new CombatKeyMapping("key.epicfight.dodge", 342, "key.epicfight.combat");
    public static final KeyMapping GUARD = new CombatKeyMapping("key.epicfight.guard", InputConstants.Type.MOUSE, 1, "key.epicfight.combat");
    public static final KeyMapping ATTACK = new CombatKeyMapping("key.epicfight.attack", InputConstants.Type.MOUSE, 0, "key.epicfight.combat");
    public static final KeyMapping WEAPON_INNATE_SKILL = new CombatKeyMapping("key.epicfight.weapon_innate_skill", InputConstants.Type.MOUSE, 0, "key.epicfight.combat");
    public static final KeyMapping MOVER_SKILL = new CombatKeyMapping("key.epicfight.mover_skill", 32, "key.epicfight.combat");
    public static final KeyMapping SWITCH_MODE = new KeyMapping("key.epicfight.switch_mode", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 82, "key.epicfight.combat");
    public static final KeyMapping LOCK_ON = new KeyMapping("key.epicfight.lock_on", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 71, "key.epicfight.camera");
    public static final KeyMapping LOCK_ON_SHIFT_LEFT = new KeyMapping("key.epicfight.lock_on_shift_left", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 263, "key.epicfight.camera");
    public static final KeyMapping LOCK_ON_SHIFT_RIGHT = new KeyMapping("key.epicfight.lock_on_shift_right", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 262, "key.epicfight.camera");
    public static final KeyMapping LOCK_ON_SHIFT_FREELY = new KeyMapping("key.epicfight.lock_on_shift_freely", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, 2, "key.epicfight.camera");
    public static final KeyMapping SWITCH_VANILLA_MODEL_DEBUGGING = new KeyMapping("key.epicfight.switch_vanilla_model_debug", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, -1, "key.epicfight.system");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(WEAPON_INNATE_SKILL_TOOLTIP);
        event.register(SWITCH_MODE);
        event.register(DODGE);
        event.register(GUARD);
        event.register(ATTACK);
        event.register(WEAPON_INNATE_SKILL);
        event.register(MOVER_SKILL);
        event.register(SKILL_EDIT);
        event.register(LOCK_ON);
        event.register(LOCK_ON_SHIFT_LEFT);
        event.register(LOCK_ON_SHIFT_RIGHT);
        event.register(LOCK_ON_SHIFT_FREELY);
        event.register(OPEN_CONFIG_SCREEN);
        event.register(SWITCH_VANILLA_MODEL_DEBUGGING);
    }
}

