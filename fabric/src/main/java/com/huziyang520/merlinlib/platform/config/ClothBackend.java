package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigReader;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ServerConfig;
import com.huziyang520.merlinlib.config.SplitReader;
import com.huziyang520.merlinlib.config.TomlFile;
import com.huziyang520.merlinlib.util.MerlinColor;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fabric configuration store: the TOML files, shown through Cloth Config.
 *
 * <h2>What is stored where</h2>
 *
 * <p>The values stay in {@code config/MerlinLib/client.toml} and {@code server.toml}. Cloth Config is a screen
 * library, not a store, so the file remains the thing to edit by hand, to back up, and to read with any other
 * tool - and a player without Cloth loses nothing but the screen.
 *
 * <h2>Why this class is never mentioned directly</h2>
 *
 * <p>Cloth Config is an optional dependency: it is not bundled, not required, and the mod has to start without
 * it. Every reference to it therefore lives in this one class, which is only ever loaded through
 * {@link ClothSupport} after that check has passed. A class that mentions an optional mod's types in its own
 * bytecode cannot be loaded without it, however carefully the call site is guarded.
 */
public final class ClothBackend implements ConfigBackend {

    /** Sections that belong to the server file; everything else is the client file. */
    private static final Set<String> SERVER_SECTIONS = Set.of("security", "tools", "content", "notices");

    /** Values written by the screen but not saved yet, keyed by {@code section.key}. */
    private final Map<String, String> pending = new LinkedHashMap<>();

    @Override
    public ConfigReader view() {
        // Read fresh on every call: view() is asked for once per configuration load, not once per getter, and a
        // file edited by hand is then picked up by the next /reload without any cache to invalidate.
        return new SplitReader(TomlFile.read(ClientConfig.path(), ClientConfig.defaults()),
                TomlFile.read(ServerConfig.path(), ServerConfig.defaults()), SERVER_SECTIONS);
    }

    @Override
    public void write(String key, String value) {
        this.pending.put(key, value);
    }

    @Override
    public void save() {
        Map<String, String> clientValues = new LinkedHashMap<>();
        Map<String, String> serverValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.pending.entrySet()) {
            if (SERVER_SECTIONS.contains(sectionOf(entry.getKey()))) {
                serverValues.put(entry.getKey(), entry.getValue());
            } else {
                clientValues.put(entry.getKey(), entry.getValue());
            }
        }
        this.pending.clear();
        TomlFile.rewrite(ClientConfig.path(), ClientConfig.defaults(), clientValues);
        TomlFile.rewrite(ServerConfig.path(), ServerConfig.defaults(), serverValues);
    }

    @Override
    public String name() {
        return "Cloth Config over the TOML files";
    }

    /**
     * Opens the configuration screen.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen, ready to be shown
     */
    public Screen openScreen(Screen parent) {
        ConfigReader reader = view();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(TITLE_KEY));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, ConfigCategory> categories = new LinkedHashMap<>();
        for (Option option : OPTIONS) {
            String section = sectionOf(option.key());
            ConfigCategory category = categories.computeIfAbsent(section,
                    name -> builder.getOrCreateCategory(Component.translatable(sectionKey(name))));
            category.addEntry(entry(entries, option, reader));
        }
        builder.setSavingRunnable(this::commit);
        return builder.build();
    }

    /** Writes what the screen collected and re-reads both files, so a saved switch is live at once. */
    private void commit() {
        save();
        ConfigManager.reload();
        Constants.LOG.info("[MerlinLib] configuration written to the TOML files from the Cloth Config screen");
    }

    /**
     * Builds one entry.
     *
     * <p>Each option carries its own default as well: Cloth's reset button restores that value, and the default
     * is also what a key deleted from the file falls back to.
     */
    private AbstractConfigListEntry entry(ConfigEntryBuilder entries, Option option, ConfigReader reader) {
        Component label = Component.translatable("merlinlib.configuration." + option.key());
        String key = option.key();
        switch (option.kind()) {
            case BOOL: {
                boolean current = reader.getBoolean(key, option.defaultValue() != 0.0D);
                return entries.startBooleanToggle(label, current)
                        .setDefaultValue(option.defaultValue() != 0.0D)
                        .setSaveConsumer(value -> this.pending.put(key, Boolean.toString(value)))
                        .build();
            }
            case INT: {
                int current = reader.getInt(key, (int) option.defaultValue(), (int) option.min(), (int) option.max());
                return entries.startIntField(label, current)
                        .setMin((int) option.min())
                        .setMax((int) option.max())
                        .setDefaultValue((int) option.defaultValue())
                        .setSaveConsumer(value -> this.pending.put(key, Integer.toString(value)))
                        .build();
            }
            case DOUBLE: {
                double current = reader.getDouble(key, option.defaultValue(), option.min(), option.max());
                return entries.startDoubleField(label, current)
                        .setMin(option.min())
                        .setMax(option.max())
                        .setDefaultValue(option.defaultValue())
                        .setSaveConsumer(value -> this.pending.put(key, Double.toString(value)))
                        .build();
            }
            case COLOR: {
                int current = reader.getColor(key, (int) option.defaultValue());
                return entries.startAlphaColorField(label, current)
                        .setDefaultValue((int) option.defaultValue())
                        .setSaveConsumer(value -> this.pending.put(key, String.format(Locale.ROOT, "0x%08X", value)))
                        .build();
            }
            case TEXT:
            default: {
                String current = reader.getString(key, option.text());
                return entries.startStrField(label, current)
                        .setDefaultValue(option.text())
                        .setSaveConsumer(value -> this.pending.put(key, value))
                        .build();
            }
        }
    }

    private static String sectionOf(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? "" : key.substring(0, dot);
    }

    private static String sectionKey(String section) {
        return "merlinlib.configuration." + section;
    }

    /** Translation key of the screen title. */
    private static final String TITLE_KEY = "merlinlib.configuration.title";

    /** The kinds of entry the screen knows how to build. */
    private enum Kind {
        BOOL, INT, DOUBLE, COLOR, TEXT
    }

    /**
     * One option of the screen.
     *
     * @param section      the section, which also decides the file and the category
     * @param name         the key inside that section
     * @param kind         how the value is edited
     * @param defaultValue the default, also used by the reset button
     * @param min          the lowest accepted number, unused for the other kinds
     * @param max          the highest accepted number, unused for the other kinds
     * @param text         the default text, unused for the other kinds
     */
    private record Option(String section, String name, Kind kind, double defaultValue, double min, double max,
                          String text) {

        String key() {
            return this.section + "." + this.name;
        }
    }

    private static Option flag(String section, String name, boolean value) {
        return new Option(section, name, Kind.BOOL, value ? 1.0D : 0.0D, 0.0D, 1.0D, "");
    }

    private static Option number(String section, String name, int value, int min, int max) {
        return new Option(section, name, Kind.INT, value, min, max, "");
    }

    private static Option decimal(String section, String name, double value, double min, double max) {
        return new Option(section, name, Kind.DOUBLE, value, min, max, "");
    }

    private static Option colour(String section, String name, int value) {
        return new Option(section, name, Kind.COLOR, value, 0.0D, 0.0D, "");
    }

    private static Option text(String section, String name, String value) {
        return new Option(section, name, Kind.TEXT, 0.0D, 0.0D, 0.0D, value);
    }

    /** Every option, in the order the screen shows them. */
    private static final List<Option> OPTIONS = new ArrayList<>(List.of(
            flag("general", "notice_enabled", true),

            flag("hud", "crosshair_damage", false),
            number("hud", "crosshair_damage_ticks", 20, 1, 400),
            flag("hud", "collapse_enabled", true),
            number("hud", "collapse_threshold", 99, 1, 1000000),
            flag("hud", "armour_auto_dodge", true),

            flag("floating_text", "enabled", false),
            number("floating_text", "duration_ticks", 20, 1, 400),
            decimal("floating_text", "base_scale", 1.0D, 0.1D, 8.0D),
            colour("floating_text", "normal_color", MerlinColor.WHITE),
            colour("floating_text", "critical_color", MerlinColor.ORANGE),
            number("floating_text", "max_concurrent", 32, 1, 512),

            flag("particles", "limit_enabled", true),
            number("particles", "limit", 512, 1, 1000000),

            flag("editor", "hotkey_enabled", false),
            number("editor", "shift_step", 10, 1, 100000),
            number("editor", "scroll_step", 1, 1, 1000),
            number("editor", "scroll_step_fast", 10, 1, 100000),

            flag("macros", "enabled", true),

            flag("security", "restrict_tools_to_operators", false),
            flag("security", "editor_requires_permission", true),
            flag("security", "health_editor_requires_permission", true),

            flag("tools", "enable_testing_toolkit", true),
            number("tools", "max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE),
            number("tools", "max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE),
            flag("tools", "allow_levels_above_max", true),
            flag("tools", "allow_mismatched_enchantments", false),
            flag("tools", "allow_editing_all_items", false),
            flag("tools", "allow_non_test_item_damage", false),
            flag("tools", "health_editor_requires_item", true),

            flag("content", "disable_generated_enchantments", false),

            flag("notices", "enabled", true),
            text("notices", "per_mod", "")));
}
