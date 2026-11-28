/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractSliderButton
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.ContainerObjectSelectionList
 *  net.minecraft.client.gui.components.ContainerObjectSelectionList$Entry
 *  net.minecraft.client.gui.components.CycleButton
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.narration.NarratableEntry
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.CommonComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.util.FormattedCharSequence
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$ConfigValue
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 */
package dev.dyslexic.cinematic.client;

import dev.dyslexic.cinematic.client.LayoutProfiles;
import dev.dyslexic.cinematic.client.Settings;
import dev.dyslexic.cinematic.station.StationKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeConfigSpec;

public final class SettingsScreen
extends Screen {
    private static final int ROW_WIDTH = 310;
    private static final int CONTROL = 120;
    private static final int NOTE = 11051670;
    private static final int HEADER = 14729344;
    private static final Object[][] GROUPS = new Object[][]{{"crafting", StationKind.TABLE, StationKind.STONECUTTER, StationKind.LOOM, StationKind.CARTOGRAPHY}, {"cooking", StationKind.FURNACE, StationKind.SMOKER, StationKind.BLAST_FURNACE, StationKind.CAMPFIRE}, {"smithing", StationKind.ANVIL, StationKind.GRINDSTONE, StationKind.SMITHING}, {"magic", StationKind.ENCHANTING, StationKind.BREWING, StationKind.BEACON}, {"other", StationKind.LECTERN, StationKind.JUKEBOX, StationKind.CHISELED_BOOKSHELF}};
    private final Screen parent;
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> draft = new HashMap();
    private final Map<StationKind, Integer> profiles = new EnumMap<StationKind, Integer>(StationKind.class);
    private final boolean inWorld;
    private Tab tab = Tab.GENERAL;
    private String searchText = "";
    private String query = "";
    private EditBox search;
    private Rows rows;
    private final List<Button> tabButtons = new ArrayList<Button>();
    private boolean dirty;
    @Nullable
    private Component described;

    public SettingsScreen(Screen parent) {
        super((Component)Component.m_237115_((String)"cinematic.settings.title"));
        this.parent = parent;
        this.inWorld = Minecraft.m_91087_().f_91073_ != null;
    }

    private boolean bool(ForgeConfigSpec.BooleanValue value) {
        return (Boolean)this.draft.getOrDefault(value, value.get());
    }

    private int number(ForgeConfigSpec.IntValue value) {
        return (Integer)this.draft.getOrDefault(value, value.get());
    }

    private void put(ForgeConfigSpec.ConfigValue<?> value, Object to) {
        this.draft.put(value, to);
    }

    private int profile(StationKind kind) {
        return this.profiles.getOrDefault((Object)kind, LayoutProfiles.selected(kind));
    }

    private void apply() {
        for (Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object> change : this.draft.entrySet()) {
            ForgeConfigSpec.ConfigValue<?> value = change.getKey();
            if (value.get().equals(change.getValue())) continue;
            Settings.set(value, change.getValue());
        }
        this.profiles.forEach(LayoutProfiles::select);
    }

    private Preset preset() {
        for (Preset preset : Preset.values()) {
            if (preset == Preset.CUSTOM || this.number(Settings.APPROACH_SPEED) != preset.approach || this.number(Settings.LEAVE_SPEED) != preset.leave || this.number(Settings.LEAN_STRENGTH) != preset.lean || this.number(Settings.SCENE_ZOOM) != preset.zoom || this.bool(Settings.LETTERBOX) != preset.letterbox || this.bool(Settings.LIVING_CAMERA) != preset.living || this.bool(Settings.CAMERA_SHAKE) != preset.shake || this.bool(Settings.SCREEN_FLASH) != preset.flash) continue;
            return preset;
        }
        return Preset.CUSTOM;
    }

    private void choose(Preset preset) {
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.APPROACH_SPEED, preset.approach);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.LEAVE_SPEED, preset.leave);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.LEAN_STRENGTH, preset.lean);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.SCENE_ZOOM, preset.zoom);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.LETTERBOX, preset.letterbox);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.LIVING_CAMERA, preset.living);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.CAMERA_SHAKE, preset.shake);
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.SCREEN_FLASH, preset.flash);
    }

    private Option toggle(Tab on, String key, ForgeConfigSpec.BooleanValue value, Function<Boolean, Component> text) {
        return this.toggle(on, (Component)Component.m_237115_((String)("cinematic.configuration." + key)), (Component)Component.m_237115_((String)("cinematic.configuration." + key + ".tooltip")), value, text);
    }

    private Option toggle(Tab on, Component label, Component description, final ForgeConfigSpec.BooleanValue value, final Function<Boolean, Component> text) {
        return new Option(on, label, description){

            @Override
            AbstractWidget control() {
                return CycleButton.m_168894_(text::apply).m_168961_((Object[])new Boolean[]{true, false}).m_168948_((Object)SettingsScreen.this.bool(value)).m_168929_().m_168936_(0, 0, 120, 20, this.label, (button, to) -> {
                    SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, to);
                    SettingsScreen.this.dirty = value == Settings.DEVELOPER_MODE;
                });
            }

            @Override
            boolean changed() {
                return SettingsScreen.this.bool(value) != ((Boolean)value.getDefault()).booleanValue();
            }

            @Override
            void reset() {
                SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, value.getDefault());
            }
        };
    }

    private Option slider(Tab on, String key, final ForgeConfigSpec.IntValue value, final int min, final int max, final Function<Integer, Component> text) {
        return new Option(on, (Component)Component.m_237115_((String)("cinematic.configuration." + key)), (Component)Component.m_237115_((String)("cinematic.configuration." + key + ".tooltip"))){

            @Override
            AbstractWidget control() {
                return new IntSlider(value, min, max, text);
            }

            @Override
            boolean changed() {
                return SettingsScreen.this.number(value) != ((Integer)value.getDefault()).intValue();
            }

            @Override
            void reset() {
                SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, value.getDefault());
            }
        };
    }

    private int quickMode() {
        int mode = this.number(Settings.QUICK_MODE);
        return mode < 0 ? (this.bool(Settings.QUICK_RETURN) ? 1 : 0) : mode;
    }

    private Option choice(Tab on, final String key, final ForgeConfigSpec.IntValue value, final int count, final IntSupplier current, final int made) {
        return new Option(on, (Component)Component.m_237115_((String)("cinematic.configuration." + key)), (Component)Component.m_237115_((String)("cinematic.configuration." + key + ".tooltip"))){

            @Override
            AbstractWidget control() {
                Object[] values = new Integer[count];
                for (int i = 0; i < count; ++i) {
                    values[i] = i;
                }
                return CycleButton.m_168894_(v -> Component.m_237115_((String)("cinematic.settings." + key + "." + v)).m_130940_(ChatFormatting.WHITE)).m_168961_(values).m_168948_((Object)current.getAsInt()).m_168929_().m_168936_(0, 0, 120, 20, this.label, (button, to) -> {
                    SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, to);
                    SettingsScreen.this.dirty = true;
                });
            }

            @Override
            boolean changed() {
                return current.getAsInt() != made;
            }

            @Override
            void reset() {
                SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, made);
            }
        };
    }

    private Option presetOption() {
        return new Option(Tab.CAMERA, (Component)Component.m_237115_((String)"cinematic.settings.preset"), (Component)Component.m_237115_((String)"cinematic.settings.preset.tooltip")){

            @Override
            AbstractWidget control() {
                return new Button(0, 0, 120, 20, (Component)Component.m_237119_(), button -> {
                    Preset now = SettingsScreen.this.preset();
                    SettingsScreen.this.choose(Preset.values()[now == Preset.CUSTOM ? Preset.BALANCED.ordinal() : (now.ordinal() + 1) % Preset.CUSTOM.ordinal()]);
                    SettingsScreen.this.dirty = true;
                }, narration -> (MutableComponent)narration.get()){

                    public Component m_6035_() {
                        Preset now = SettingsScreen.this.preset();
                        return now.title().m_6881_().m_130940_(now == Preset.CUSTOM ? ChatFormatting.GOLD : ChatFormatting.WHITE);
                    }
                };
            }

            @Override
            boolean changed() {
                return SettingsScreen.this.preset() != Preset.BALANCED;
            }

            @Override
            void reset() {
                SettingsScreen.this.choose(Preset.BALANCED);
            }
        };
    }

    private Option profileOption(final StationKind kind) {
        Option option = new Option(Tab.ADVANCED, LayoutProfiles.name(kind), (Component)Component.m_237115_((String)"cinematic.settings.profiles.tooltip")){

            @Override
            AbstractWidget control() {
                return CycleButton.m_168894_(LayoutProfiles::value).m_168961_((Object[])new Integer[]{0, 1, 2}).m_168948_((Object)SettingsScreen.this.profile(kind)).m_168929_().m_168936_(0, 0, 120, 20, this.label, (button, to) -> SettingsScreen.this.profiles.put(kind, (Integer)to));
            }

            @Override
            boolean changed() {
                return SettingsScreen.this.profile(kind) != 0;
            }

            @Override
            void reset() {
                SettingsScreen.this.profiles.put(kind, 0);
            }
        };
        option.developer = true;
        return option;
    }

    private List<Object> options() {
        Function<Boolean, Component> onOff = on -> SettingsScreen.value(on, "cinematic.settings.on", "cinematic.settings.off");
        Function<Boolean, Component> showHide = on -> SettingsScreen.value(on, "cinematic.settings.show", "cinematic.settings.hide");
        ArrayList<Object> all = new ArrayList<Object>();
        all.add(this.toggle(Tab.GENERAL, "heightLogic", Settings.HEIGHT_LOGIC, on -> on != false ? Component.m_237115_((String)"cinematic.settings.reach.window").m_130940_(ChatFormatting.WHITE) : Component.m_237115_((String)"cinematic.settings.reach.notice").m_130940_(ChatFormatting.WHITE)));
        all.add(this.toggle(Tab.GENERAL, "controlHints", Settings.CONTROL_HINTS, showHide));
        Option invite = this.toggle(Tab.GENERAL, "campfireInvite", Settings.CAMPFIRE_INVITE, showHide);
        invite.enabled = () -> this.bool(Settings.SCENES.get((Object)StationKind.CAMPFIRE));
        all.add(invite);
        all.add(this.toggle(Tab.GENERAL, "lookToggles", Settings.LOOK_TOGGLES, on -> Component.m_237115_((String)(on != false ? "cinematic.settings.look.toggle" : "cinematic.settings.look.hold")).m_130940_(ChatFormatting.WHITE)));
        all.add(this.choice(Tab.GENERAL, "armStyle", Settings.ARM_STYLE, 3, () -> this.number(Settings.ARM_STYLE), 0));
        for (Object[] group : GROUPS) {
            all.add(new Header(Tab.STATIONS, (Component)Component.m_237115_((String)("cinematic.settings.group." + String.valueOf(group[0])))));
            for (int i = 1; i < group.length; ++i) {
                StationKind kind = (StationKind)((Object)group[i]);
                all.add(new StationOption(kind));
            }
        }
        all.add(this.presetOption());
        all.add(this.toggle(Tab.CAMERA, "reduceMotion", Settings.REDUCE_MOTION, onOff));
        all.add(new Header(Tab.CAMERA, (Component)Component.m_237115_((String)"cinematic.settings.pace")));
        all.add(this.choice(Tab.CAMERA, "quickMode", Settings.QUICK_MODE, 3, this::quickMode, 1));
        Option approach = this.slider(Tab.CAMERA, "approachSpeed", Settings.APPROACH_SPEED, 50, 200, SettingsScreen::percent);
        approach.enabled = () -> this.quickMode() != 2;
        all.add(approach);
        all.add(this.slider(Tab.CAMERA, "leaveSpeed", Settings.LEAVE_SPEED, 50, 200, SettingsScreen::percent));
        Option window = this.slider(Tab.CAMERA, "quickWindow", Settings.QUICK_WINDOW, 5, 30, v -> Component.m_237110_((String)"cinematic.settings.seconds", (Object[])new Object[]{v}));
        window.enabled = () -> this.quickMode() == 1;
        all.add(window);
        all.add(new Header(Tab.CAMERA, (Component)Component.m_237115_((String)"cinematic.settings.view")));
        all.add(this.slider(Tab.CAMERA, "leanStrength", Settings.LEAN_STRENGTH, 0, 100, SettingsScreen::percent));
        all.add(this.slider(Tab.CAMERA, "sceneZoom", Settings.SCENE_ZOOM, 0, 100, SettingsScreen::percent));
        all.add(new Header(Tab.CAMERA, (Component)Component.m_237115_((String)"cinematic.settings.effects")));
        String[] stringArray = new String[]{"letterbox", "livingCamera", "cameraShake", "screenFlash"};
        int n = stringArray.length;
        for (int i = 0; i < n; ++i) {
            String key;
            ForgeConfigSpec.BooleanValue value = switch (key = stringArray[i]) {
                case "letterbox" -> Settings.LETTERBOX;
                case "livingCamera" -> Settings.LIVING_CAMERA;
                case "cameraShake" -> Settings.CAMERA_SHAKE;
                default -> Settings.SCREEN_FLASH;
            };
            Option effect = this.toggle(Tab.CAMERA, key, value, onOff);
            effect.enabled = () -> !this.bool(Settings.REDUCE_MOTION);
            all.add(effect);
        }
        Option clientOnly = this.toggle(Tab.ADVANCED, (Component)Component.m_237115_((String)"cinematic.configuration.clientOnlyMode"), (Component)(this.inWorld ? Component.m_237115_((String)"cinematic.settings.clientOnlyMode.locked") : Component.m_237115_((String)"cinematic.configuration.clientOnlyMode.tooltip")), Settings.CLIENT_ONLY_MODE, onOff);
        clientOnly.enabled = () -> !this.inWorld;
        all.add(clientOnly);
        all.add(this.toggle(Tab.ADVANCED, "developerMode", Settings.DEVELOPER_MODE, onOff));
        all.add(new Header(Tab.ADVANCED, (Component)Component.m_237115_((String)"cinematic.settings.profiles"), true));
        for (StationKind kind : StationKind.values()) {
            if (!LayoutProfiles.supports(kind)) continue;
            all.add(this.profileOption(kind));
        }
        return all;
    }

    protected void m_7856_() {
        this.tabButtons.clear();
        this.search = new EditBox(this.f_96547_, this.f_96543_ / 2 - 100, 20, 200, 18, (Component)Component.m_237115_((String)"cinematic.settings.search"));
        this.search.m_257771_((Component)Component.m_237115_((String)"cinematic.settings.search").m_130940_(ChatFormatting.DARK_GRAY));
        this.search.m_94144_(this.searchText);
        this.search.m_94151_(text -> {
            this.searchText = text;
            this.query = text.trim().toLowerCase(Locale.ROOT);
            this.fill(true);
        });
        this.m_142416_((GuiEventListener)this.search);
        int tabWidth = Math.min(90, (this.f_96543_ - 20) / Tab.values().length);
        int tabsLeft = this.f_96543_ / 2 - tabWidth * Tab.values().length / 2;
        for (Tab t : Tab.values()) {
            Button button = Button.m_253074_((Component)Component.m_237115_((String)("cinematic.settings.tab." + t.name().toLowerCase(Locale.ROOT))), b -> {
                this.tab = t;
                this.search.m_94144_("");
                this.fill(true);
            }).m_252987_(tabsLeft + t.ordinal() * tabWidth, 42, tabWidth - 2, 20).m_253136_();
            this.tabButtons.add(button);
            this.m_142416_((GuiEventListener)button);
        }
        this.rows = new Rows(this.f_96541_, this.f_96543_, Math.max(40, this.f_96544_ - 66 - 94), 66);
        this.m_142416_((GuiEventListener)this.rows);
        int footer = this.f_96544_ - 26;
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)Component.m_237115_((String)"cinematic.settings.reset_all"), b -> {
            for (Object o : this.options()) {
                if (!(o instanceof Option)) continue;
                Option option = (Option)o;
                if (!option.enabled.getAsBoolean() || option.developer && !this.bool(Settings.DEVELOPER_MODE)) continue;
                option.reset();
            }
            this.dirty = true;
        }).m_252987_(this.f_96543_ / 2 - 155, footer, 100, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)CommonComponents.f_130656_, b -> {
            this.draft.clear();
            this.profiles.clear();
            this.f_96541_.m_91152_(this.parent);
        }).m_252987_(this.f_96543_ / 2 + 155 - 204, footer, 100, 20).m_253136_());
        this.m_142416_((GuiEventListener)Button.m_253074_((Component)CommonComponents.f_130655_, b -> this.m_7379_()).m_252987_(this.f_96543_ / 2 + 155 - 100, footer, 100, 20).m_253136_());
        this.fill(false);
    }

    private void fill(boolean keepScroll) {
        double scroll = keepScroll ? this.rows.m_93517_() : 0.0;
        boolean searching = !this.query.isEmpty();
        for (Button button : this.tabButtons) {
            button.f_93623_ = searching || this.tabButtons.indexOf(button) != this.tab.ordinal();
        }
        ArrayList<Row> list = new ArrayList<Row>();
        Tab heading = null;
        ArrayList<AbstractWidget> pair = new ArrayList<AbstractWidget>();
        boolean developer = this.bool(Settings.DEVELOPER_MODE);
        int found = 0;
        if (!searching && this.tab == Tab.STATIONS) {
            list.add(new Row(Row.Style.BAR, (Component)Component.m_237115_((String)"cinematic.settings.stations.note"), null, (AbstractWidget)Button.m_253074_((Component)Component.m_237115_((String)"cinematic.settings.all_on"), b -> this.stations(true)).m_253046_(78, 20).m_253136_(), (AbstractWidget)Button.m_253074_((Component)Component.m_237115_((String)"cinematic.settings.all_off"), b -> this.stations(false)).m_253046_(78, 20).m_253136_()));
        }
        for (Object o : this.options()) {
            AbstractWidget control;
            if (o instanceof Header) {
                Header header = (Header)o;
                if (searching || header.on() != this.tab || !developer && header.developer()) continue;
                this.flush(pair, list);
                list.add(new Row(Row.Style.HEADER, header.title(), null, null, null));
                continue;
            }
            Option option = (Option)o;
            if (searching ? !option.matches(this.query) : option.on != this.tab || option.developer && !developer) continue;
            ++found;
            if (searching && heading != option.on) {
                this.flush(pair, list);
                heading = option.on;
                list.add(new Row(Row.Style.HEADER, SettingsScreen.tabTitle(option.on), null, null, null));
            }
            if ((control = option.control()) instanceof StationButton) {
                pair.add(control);
                if (pair.size() != 2) continue;
                this.flush(pair, list);
                continue;
            }
            this.flush(pair, list);
            Button reset = Button.m_253074_((Component)Component.m_237113_((String)"\u21ba"), b -> {
                option.reset();
                this.dirty = true;
            }).m_253046_(20, 20).m_253136_();
            reset.m_257544_(Tooltip.m_257550_((Component)Component.m_237115_((String)"cinematic.settings.reset")));
            list.add(new Row(Row.Style.OPTION, option.label, option, control, (AbstractWidget)reset));
        }
        this.flush(pair, list);
        if (!searching && this.tab == Tab.ADVANCED && !developer) {
            list.add(new Row(Row.Style.NOTE, (Component)Component.m_237115_((String)"cinematic.settings.profiles.hidden"), null, null, null));
        }
        if (searching) {
            list.add(0, new Row(Row.Style.NOTE, (Component)(found == 0 ? Component.m_237110_((String)"cinematic.settings.no_results", (Object[])new Object[]{this.searchText.trim()}) : Component.m_237110_((String)"cinematic.settings.results", (Object[])new Object[]{found})), null, null, null));
        }
        this.rows.set(list);
        this.rows.m_93410_(scroll);
    }

    private void flush(List<AbstractWidget> pair, List<Row> list) {
        if (!pair.isEmpty()) {
            list.add(new Row(Row.Style.PAIR, null, null, pair.get(0), pair.size() > 1 ? pair.get(1) : null));
            pair.clear();
        }
    }

    private void stations(boolean on) {
        for (StationKind kind : Settings.SCENES.keySet()) {
            if (kind == StationKind.ADDON) continue;
            this.put((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)kind), on);
        }
        this.dirty = true;
    }

    private static Component tabTitle(Tab t) {
        return Component.m_237115_((String)("cinematic.settings.tab." + t.name().toLowerCase(Locale.ROOT)));
    }

    void showDeveloper() {
        this.put((ForgeConfigSpec.ConfigValue<?>)Settings.DEVELOPER_MODE, true);
        this.fill(true);
    }

    public void m_86600_() {
        if (this.dirty) {
            this.dirty = false;
            this.fill(true);
        }
    }

    public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.described = null;
        this.m_280273_(graphics);
        super.m_88315_(graphics, mouseX, mouseY, partialTick);
        graphics.m_280653_(this.f_96547_, this.f_96539_, this.f_96543_ / 2, 7, 0xFFFFFF);
        int top = this.f_96544_ - 88;
        int box = 56;
        int left = this.f_96543_ / 2 - 155;
        graphics.m_280509_(left, top, left + 310, top + box, -1879048192);
        graphics.m_280637_(left, top, 310, box, 0x40FFFFFF);
        Component text = this.described != null ? this.described : Component.m_237115_((String)"cinematic.settings.describe");
        int y = top + 5;
        for (FormattedCharSequence line : this.f_96547_.m_92923_((FormattedText)text, 298)) {
            if (y > top + box - 10) break;
            graphics.m_280649_(this.f_96547_, line, left + 6, y, this.described != null ? 0xE0E0E0 : 11051670, false);
            y += 10;
        }
    }

    public void m_7379_() {
        this.apply();
        this.f_96541_.m_91152_(this.parent);
    }

    private static Component value(boolean on, String onKey, String offKey) {
        return Component.m_237115_((String)(on ? onKey : offKey)).m_130940_(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    private static Component percent(int v) {
        return Component.m_237113_((String)(v + "%"));
    }

    private static ItemLike icon(StationKind kind) {
        return switch (kind) {
            default -> throw new IncompatibleClassChangeError();
            case StationKind.TABLE -> Items.f_41960_;
            case StationKind.FURNACE -> Items.f_41962_;
            case StationKind.SMOKER -> Items.f_42769_;
            case StationKind.BLAST_FURNACE -> Items.f_42770_;
            case StationKind.ENCHANTING -> Items.f_42100_;
            case StationKind.ANVIL -> Items.f_42146_;
            case StationKind.GRINDSTONE -> Items.f_42773_;
            case StationKind.SMITHING -> Items.f_42775_;
            case StationKind.STONECUTTER -> Items.f_42776_;
            case StationKind.LOOM -> Items.f_42719_;
            case StationKind.CARTOGRAPHY -> Items.f_42771_;
            case StationKind.BREWING -> Items.f_42543_;
            case StationKind.CAMPFIRE -> Items.f_42781_;
            case StationKind.LECTERN -> Items.f_42774_;
            case StationKind.BEACON -> Items.f_42065_;
            case StationKind.JUKEBOX -> Items.f_41984_;
            case StationKind.CHISELED_BOOKSHELF -> Items.f_244640_;
            case StationKind.ADDON -> Items.f_42127_;
        };
    }

    private static enum Tab {
        GENERAL,
        STATIONS,
        CAMERA,
        ADVANCED;

    }

    private static enum Preset {
        CINEMATIC(80, 80, 100, 100, true, true, true, true),
        BALANCED(100, 100, 100, 100, true, true, true, true),
        FAST(160, 160, 70, 60, false, true, true, true),
        MINIMAL(200, 200, 0, 0, false, false, false, false),
        CUSTOM(0, 0, 0, 0, false, false, false, false);

        final int approach;
        final int leave;
        final int lean;
        final int zoom;
        final boolean letterbox;
        final boolean living;
        final boolean shake;
        final boolean flash;

        private Preset(int approach, int leave, int lean, int zoom, boolean letterbox, boolean living, boolean shake, boolean flash) {
            this.approach = approach;
            this.leave = leave;
            this.lean = lean;
            this.zoom = zoom;
            this.letterbox = letterbox;
            this.living = living;
            this.shake = shake;
            this.flash = flash;
        }

        Component title() {
            return Component.m_237115_((String)("cinematic.settings.preset." + this.name().toLowerCase(Locale.ROOT)));
        }
    }

    private abstract class Option {
        final Tab on;
        final Component label;
        final Component description;
        BooleanSupplier enabled = () -> true;
        boolean developer;

        Option(Tab on, Component label, Component description) {
            this.on = on;
            this.label = label;
            this.description = description;
        }

        abstract AbstractWidget control();

        abstract boolean changed();

        abstract void reset();

        boolean matches(String q) {
            return (this.label.getString() + " " + this.description.getString()).toLowerCase(Locale.ROOT).contains(q);
        }
    }

    private record Header(Tab on, Component title, boolean developer) {
        Header(Tab on, Component title) {
            this(on, title, false);
        }
    }

    private final class StationOption
    extends Option {
        final StationKind kind;

        StationOption(StationKind kind) {
            super(Tab.STATIONS, LayoutProfiles.name(kind), (Component)Component.m_237115_((String)("cinematic.configuration." + Settings.key(kind) + ".tooltip")));
            this.kind = kind;
        }

        @Override
        AbstractWidget control() {
            return new StationButton(this);
        }

        @Override
        boolean changed() {
            return !SettingsScreen.this.bool(Settings.SCENES.get((Object)this.kind));
        }

        @Override
        void reset() {
            SettingsScreen.this.put((ForgeConfigSpec.ConfigValue)Settings.SCENES.get((Object)this.kind), true);
        }
    }

    private static final class Rows
    extends ContainerObjectSelectionList<Row> {
        Rows(Minecraft minecraft, int width, int height, int y) {
            super(minecraft, width, height, y, y + height, 24);
            this.m_93488_(false);
            this.m_93496_(false);
        }

        void set(List<Row> list) {
            this.m_5988_(list);
        }

        public int m_5759_() {
            return 310;
        }

        protected int m_5756_() {
            return this.f_93388_ / 2 + 155 + 6;
        }
    }

    private final class Row
    extends ContainerObjectSelectionList.Entry<Row> {
        private final Style style;
        @Nullable
        private final Component label;
        @Nullable
        private final Option option;
        @Nullable
        private final AbstractWidget first;
        @Nullable
        private final AbstractWidget second;
        private final List<AbstractWidget> widgets = new ArrayList<AbstractWidget>();

        Row(@Nullable Style style, @Nullable Component label, @Nullable Option option, @Nullable AbstractWidget first, AbstractWidget second) {
            this.style = style;
            this.label = label;
            this.option = option;
            this.first = first;
            this.second = second;
            if (first != null) {
                this.widgets.add(first);
            }
            if (second != null) {
                this.widgets.add(second);
            }
        }

        public void m_6311_(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            switch (this.style) {
                case HEADER: {
                    graphics.m_280614_(SettingsScreen.this.f_96547_, this.label, left + 4, top + 10, 14729344, false);
                    break;
                }
                case NOTE: {
                    graphics.m_280056_(SettingsScreen.this.f_96547_, SettingsScreen.this.f_96547_.m_92834_(this.label.getString(), width - 8), left + 4, top + 8, 11051670, false);
                    break;
                }
                case OPTION: {
                    boolean enabled = this.option.enabled.getAsBoolean();
                    graphics.m_280056_(SettingsScreen.this.f_96547_, SettingsScreen.this.f_96547_.m_92834_(this.label.getString(), width - 120 - 32), left + 4, top + 6, enabled ? 0xFFFFFF : 0x808080, false);
                    this.first.f_93623_ = enabled;
                    this.first.m_264152_(left + width - 120 - 24, top);
                    this.first.m_88315_(graphics, mouseX, mouseY, partialTick);
                    this.second.m_264152_(left + width - 20, top);
                    this.second.f_93624_ = enabled && this.option.changed();
                    this.second.m_88315_(graphics, mouseX, mouseY, partialTick);
                    if (!hovering) break;
                    SettingsScreen.this.described = this.option.description;
                    break;
                }
                case BAR: {
                    graphics.m_280056_(SettingsScreen.this.f_96547_, SettingsScreen.this.f_96547_.m_92834_(this.label.getString(), width - 170), left + 4, top + 6, 11051670, false);
                    this.first.m_264152_(left + width - 160, top);
                    this.second.m_264152_(left + width - 78, top);
                    this.first.m_88315_(graphics, mouseX, mouseY, partialTick);
                    this.second.m_88315_(graphics, mouseX, mouseY, partialTick);
                    break;
                }
                case PAIR: {
                    this.first.m_264152_(left, top);
                    this.first.m_88315_(graphics, mouseX, mouseY, partialTick);
                    if (this.first.m_274382_()) {
                        SettingsScreen.this.described = ((StationButton)this.first).option.description;
                    }
                    if (this.second == null) break;
                    this.second.m_264152_(left + width - this.second.m_5711_(), top);
                    this.second.m_88315_(graphics, mouseX, mouseY, partialTick);
                    if (!this.second.m_274382_()) break;
                    SettingsScreen.this.described = ((StationButton)this.second).option.description;
                }
            }
        }

        public List<? extends GuiEventListener> m_6702_() {
            return this.widgets;
        }

        public List<? extends NarratableEntry> m_142437_() {
            return this.widgets;
        }

        static enum Style {
            HEADER,
            NOTE,
            OPTION,
            PAIR,
            BAR;

        }
    }

    private final class StationButton
    extends Button {
        final StationOption option;
        private final ItemStack stack;

        StationButton(StationOption option) {
            super(0, 0, 150, 20, (Component)Component.m_237119_(), button -> {
                ForgeConfigSpec.BooleanValue value;
                SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)value, !SettingsScreen.this.bool(value = Settings.SCENES.get((Object)option.kind)));
            }, narration -> (MutableComponent)narration.get());
            this.option = option;
            this.stack = new ItemStack(SettingsScreen.icon(option.kind));
        }

        public Component m_6035_() {
            boolean on = SettingsScreen.this.bool(Settings.SCENES.get((Object)this.option.kind));
            return Component.m_237119_().m_7220_(this.option.label).m_130946_(" \u00b7 ").m_7220_(SettingsScreen.value(on, "cinematic.settings.on", "cinematic.settings.off"));
        }

        public void m_280139_(GuiGraphics graphics, Font font, int color) {
            graphics.m_280480_(this.stack, this.m_252754_() + 3, this.m_252907_() + 2);
            StationButton.m_280138_((GuiGraphics)graphics, (Font)font, (Component)this.m_6035_(), (int)(this.m_252754_() + 22), (int)this.m_252907_(), (int)(this.m_252754_() + this.m_5711_() - 3), (int)(this.m_252907_() + this.m_93694_()), (int)color);
        }
    }

    private final class IntSlider
    extends AbstractSliderButton {
        private final ForgeConfigSpec.IntValue setting;
        private final int min;
        private final int max;
        private final Function<Integer, Component> text;

        IntSlider(ForgeConfigSpec.IntValue setting, int min, int max, Function<Integer, Component> text) {
            super(0, 0, 120, 20, (Component)Component.m_237119_(), (double)(Mth.m_14045_((int)SettingsScreen.this.number(setting), (int)min, (int)max) - min) / (double)(max - min));
            this.setting = setting;
            this.min = min;
            this.max = max;
            this.text = text;
            this.m_5695_();
        }

        private int current() {
            return this.min + (int)Math.round(this.f_93577_ * (double)(this.max - this.min));
        }

        protected void m_5695_() {
            int v = this.current();
            this.m_93666_((Component)this.text.apply(v).m_6881_().m_130940_(v == (Integer)this.setting.getDefault() ? ChatFormatting.WHITE : ChatFormatting.GOLD));
        }

        protected void m_5697_() {
            SettingsScreen.this.put((ForgeConfigSpec.ConfigValue<?>)this.setting, this.current());
        }
    }
}

