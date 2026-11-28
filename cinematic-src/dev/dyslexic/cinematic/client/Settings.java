/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 *  net.minecraftforge.fml.event.config.ModConfigEvent$Reloading
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.station.StationKind;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class Settings {
    public static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue HEIGHT_LOGIC;
    static final Map<StationKind, ForgeConfigSpec.BooleanValue> SCENES;
    static final ForgeConfigSpec.BooleanValue CONTROL_HINTS;
    static final ForgeConfigSpec.BooleanValue CAMPFIRE_INVITE;
    static final ForgeConfigSpec.IntValue APPROACH_SPEED;
    static final ForgeConfigSpec.IntValue LEAVE_SPEED;
    static final ForgeConfigSpec.IntValue LEAN_STRENGTH;
    static final ForgeConfigSpec.IntValue SCENE_ZOOM;
    static final ForgeConfigSpec.BooleanValue QUICK_RETURN;
    static final ForgeConfigSpec.IntValue QUICK_MODE;
    static final ForgeConfigSpec.IntValue QUICK_WINDOW;
    static final ForgeConfigSpec.IntValue ARM_STYLE;
    static final ForgeConfigSpec.BooleanValue LOOK_TOGGLES;
    static final ForgeConfigSpec.BooleanValue LETTERBOX;
    static final ForgeConfigSpec.BooleanValue LIVING_CAMERA;
    static final ForgeConfigSpec.BooleanValue CAMERA_SHAKE;
    static final ForgeConfigSpec.BooleanValue SCREEN_FLASH;
    static final ForgeConfigSpec.BooleanValue REDUCE_MOTION;
    static final ForgeConfigSpec.BooleanValue CLIENT_ONLY_MODE;
    static final ForgeConfigSpec.BooleanValue SERVER_HINT_SEEN;
    static final ForgeConfigSpec.BooleanValue DEVELOPER_MODE;
    private static final Map<ForgeConfigSpec.ConfigValue<?>, Object> RECENT;
    private static long recentUntil;
    private static volatile boolean reloaded;

    private Settings() {
    }

    static String key(StationKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static boolean get(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? (Boolean)value.get() : (Boolean)value.getDefault();
    }

    private static double percent(ForgeConfigSpec.IntValue value) {
        return (double)(SPEC.isLoaded() ? (Integer)value.get() : (Integer)value.getDefault()).intValue() / 100.0;
    }

    public static double approachSpeed() {
        return Settings.quickMode() == 2 ? 1.0 : Settings.percent(APPROACH_SPEED);
    }

    public static double leaveSpeed() {
        return Settings.percent(LEAVE_SPEED);
    }

    public static double leanStrength() {
        return Settings.percent(LEAN_STRENGTH);
    }

    public static double sceneZoom() {
        return Settings.percent(SCENE_ZOOM);
    }

    public static boolean heightLogic() {
        return Settings.get(HEIGHT_LOGIC);
    }

    public static boolean scene(StationKind kind) {
        return Settings.get(SCENES.get((Object)kind));
    }

    public static boolean controlHints() {
        return Settings.get(CONTROL_HINTS);
    }

    public static boolean clientOnlyMode() {
        return Settings.get(CLIENT_ONLY_MODE);
    }

    public static boolean developerMode() {
        return Settings.get(DEVELOPER_MODE);
    }

    public static boolean campfireInvite() {
        return Settings.get(CAMPFIRE_INVITE);
    }

    public static boolean quickReturn() {
        return Settings.quickMode() != 0;
    }

    public static int quickMode() {
        int mode = SPEC.isLoaded() ? (Integer)QUICK_MODE.get() : (Integer)QUICK_MODE.getDefault();
        return mode < 0 ? (Settings.get(QUICK_RETURN) ? 1 : 0) : mode;
    }

    public static int armStyle() {
        return SPEC.isLoaded() ? (Integer)ARM_STYLE.get() : (Integer)ARM_STYLE.getDefault();
    }

    public static int quickWindow() {
        return SPEC.isLoaded() ? (Integer)QUICK_WINDOW.get() : (Integer)QUICK_WINDOW.getDefault();
    }

    public static boolean lookToggles() {
        return Settings.get(LOOK_TOGGLES);
    }

    public static boolean letterbox() {
        return Settings.get(LETTERBOX) && !Settings.get(REDUCE_MOTION);
    }

    public static boolean livingCamera() {
        return Settings.get(LIVING_CAMERA) && !Settings.get(REDUCE_MOTION);
    }

    public static boolean cameraShake() {
        return Settings.get(CAMERA_SHAKE) && !Settings.get(REDUCE_MOTION);
    }

    public static boolean screenFlash() {
        return Settings.get(SCREEN_FLASH) && !Settings.get(REDUCE_MOTION);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static <T> void set(ForgeConfigSpec.ConfigValue<T> value, T v) {
        Map<ForgeConfigSpec.ConfigValue<?>, Object> map = RECENT;
        synchronized (map) {
            RECENT.put(value, v);
            recentUntil = System.currentTimeMillis() + 3000L;
        }
        value.set(v);
    }

    static void onReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            reloaded = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void tick() {
        if (!reloaded) {
            return;
        }
        reloaded = false;
        Map<ForgeConfigSpec.ConfigValue<?>, Object> map = RECENT;
        synchronized (map) {
            if (System.currentTimeMillis() > recentUntil) {
                RECENT.clear();
                return;
            }
            RECENT.forEach((value, v) -> {
                if (!v.equals(value.get())) {
                    value.set(v);
                }
            });
        }
    }

    static {
        SCENES = new EnumMap<StationKind, ForgeConfigSpec.BooleanValue>(StationKind.class);
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.translation("cinematic.configuration.scenes").push("scenes");
        HEIGHT_LOGIC = b.comment(new String[]{"When a block's work cannot be reached from where the player stands (too high, too far, covered),", "true opens the block's own window instead of the scene; false only says why, over the crosshair."}).translation("cinematic.configuration.heightLogic").define("heightLogic", false);
        b.translation("cinematic.configuration.blocks").push("blocks");
        for (StationKind kind : StationKind.values()) {
            String key = Settings.key(kind);
            SCENES.put(kind, b.comment("Whether " + key + " plays its scene (false: the block keeps its usual window).").translation("cinematic.configuration." + key).define(key, true));
        }
        b.pop();
        b.pop();
        b.translation("cinematic.configuration.interface").push("interface");
        CONTROL_HINTS = b.comment("The line of controls at the bottom of the screen during a scene.").translation("cinematic.configuration.controlHints").define("controlHints", true);
        CAMPFIRE_INVITE = b.comment("The \"[H] Sit by the fire\" invitation under the crosshair when looking at a campfire (the key works either way).").translation("cinematic.configuration.campfireInvite").define("campfireInvite", true);
        b.pop();
        b.translation("cinematic.configuration.camera").push("camera");
        APPROACH_SPEED = b.comment("How fast the camera leans in to the block once the player has stepped up to it, in percent (100: 1.2 s).").translation("cinematic.configuration.approachSpeed").defineInRange("approachSpeed", 100, 50, 200);
        LEAVE_SPEED = b.comment("How fast the player steps away and the camera comes back to the eyes, in percent (100: about a second).").translation("cinematic.configuration.leaveSpeed").defineInRange("leaveSpeed", 100, 50, 200);
        LEAN_STRENGTH = b.comment("How far the camera leans in from the eyes towards the block, in percent (0: it stays at the eyes, looking at the work).").translation("cinematic.configuration.leanStrength").defineInRange("leanStrength", 100, 0, 100);
        SCENE_ZOOM = b.comment("How much the view narrows on the work in a scene, in percent (0: the field of view stays your own).").translation("cinematic.configuration.sceneZoom").defineInRange("sceneZoom", 100, 0, 100);
        QUICK_RETURN = b.comment("Coming back to the same station soon after stepping away, the way in is short: a quick lean, no bars.").translation("cinematic.configuration.quickReturn").define("quickReturn", true);
        QUICK_MODE = b.comment("0: cinematic entry; 1: quick on a recent return; 2: quick on every entry. -1 keeps the old quickReturn setting.").translation("cinematic.configuration.quickMode").defineInRange("quickMode", -1, -1, 2);
        QUICK_WINDOW = b.comment("For how many seconds after stepping away coming back to the same station is quick.").translation("cinematic.configuration.quickWindow").defineInRange("quickWindow", 12, 5, 30);
        ARM_STYLE = b.comment("0: original stretching arms; 1: natural reach; 2: floating forearms.").translation("cinematic.configuration.armStyle").defineInRange("armStyle", 0, 0, 2);
        LOOK_TOGGLES = b.comment("Looking round from behind while at work (Left Alt, rebindable): false while the key is held, true pressed on and off.").translation("cinematic.configuration.lookToggles").define("lookToggles", false);
        LETTERBOX = b.comment("Black bars at the top and bottom while the camera moves in and out.").translation("cinematic.configuration.letterbox").define("letterbox", true);
        LIVING_CAMERA = b.comment("The camera breathes slightly and drifts a little towards the cursor while working.").translation("cinematic.configuration.livingCamera").define("livingCamera", true);
        CAMERA_SHAKE = b.comment("The camera trembles at big moments (a hammer blow, a spell, the beacon lighting up).").translation("cinematic.configuration.cameraShake").define("cameraShake", true);
        SCREEN_FLASH = b.comment("The screen flashes at big moments (the light of a spell, white-hot metal).").translation("cinematic.configuration.screenFlash").define("screenFlash", true);
        REDUCE_MOTION = b.comment("Turns the letterbox bars, the breathing camera, the shake and the flashes off at once, whatever they are set to.").translation("cinematic.configuration.reduceMotion").define("reduceMotion", false);
        b.pop();
        b.translation("cinematic.configuration.multiplayer").push("multiplayer");
        CLIENT_ONLY_MODE = b.comment("On a server without Cinematic Interactions, show local scenes over supported vanilla menus. Set this from the main menu; it takes effect on the next connection. Other stations keep their vanilla behavior.").translation("cinematic.configuration.clientOnlyMode").define("clientOnlyMode", false);
        SERVER_HINT_SEEN = b.comment("Whether the one-time server-list note has already been answered.").define("serverHintSeen", false);
        b.pop();
        b.translation("cinematic.configuration.developer").push("developer");
        DEVELOPER_MODE = b.comment("Show the local station layout editors and enable their G shortcut. Saved layouts apply even when this is false.").translation("cinematic.configuration.developerMode").define("developerMode", false);
        b.pop();
        SPEC = b.build();
        RECENT = new LinkedHashMap();
    }
}

