package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The client half of the switch table: display options and interaction tuning, in the order
 * {@code client.toml} documents them.
 */
public final class ClientOptions {

    /** Every option, in the order the commented file documents it. */
    private static final List<ConfigOption> ALL = List.of(
            ConfigOption.bool("hud", "crosshair_damage", false),
            ConfigOption.number("hud", "crosshair_damage_ticks", 20, 1, 400),
            ConfigOption.bool("hud", "collapse_enabled", true),
            ConfigOption.number("hud", "collapse_threshold", 99, 1, 1000000),
            ConfigOption.bool("hud", "armour_auto_dodge", true),
            ConfigOption.bool("floating_text", "enabled", false),
            ConfigOption.number("floating_text", "duration_ticks", 20, 1, 400),
            ConfigOption.decimal("floating_text", "base_scale", 1.0D, 0.1D, 8.0D),
            ConfigOption.colour("floating_text", "normal_color", "0xFFFFFFFF"),
            ConfigOption.colour("floating_text", "critical_color", "0xFFFFAA00"),
            ConfigOption.number("floating_text", "max_concurrent", 32, 1, 512),
            ConfigOption.bool("particles", "limit_enabled", true),
            ConfigOption.number("particles", "limit", 512, 1, 1000000),
            ConfigOption.bool("editor", "hotkey_enabled", false),
            ConfigOption.number("editor", "shift_step", 10, 1, 100000),
            ConfigOption.number("editor", "scroll_step", 1, 1, 1000),
            ConfigOption.number("editor", "scroll_step_fast", 10, 1, 100000),
            ConfigOption.bool("macros", "enabled", true));

    private ClientOptions() {
    }

    /** @return every option, in file order */
    public static List<ConfigOption> all() {
        return ALL;
    }

    /** @return the sections in the order they first appear, which is the tab order of both screens */
    public static List<String> sections() {
        return ConfigOption.sectionsOf(ALL);
    }

    /**
     * @param section the section of {@code client.toml}
     * @return the options of that section, in file order
     */
    public static List<ConfigOption> in(String section) {
        return ConfigOption.of(ALL, section);
    }

    /**
     * @param section the section of {@code client.toml}
     * @return the translation key of that section's tab
     */
    public static String sectionKey(String section) {
        return ConfigOption.KEY_PREFIX + section;
    }

    /** @return the translation key of the screen title */
    public static String titleKey() {
        return ConfigOption.KEY_PREFIX + "title";
    }

    /** @return the translation key of the entry that opens the join notice page */
    public static String noticesKey() {
        return ConfigOption.KEY_PREFIX + "notices.open";
    }

    /**
     * The current value of every option, read from the file the mod itself reads.
     *
     * <p>Read fresh on every call rather than cached: a player may have edited the file between two
     * openings of the screen, and a screen that shows yesterday's values would then write them back.
     *
     * @return the values, keyed by {@code section.key}
     */
    public static Map<String, String> values() {
        return new LinkedHashMap<>(TomlFile.read(ClientConfig.path(), ClientConfig.defaults()).values());
    }

    /**
     * @param option the option to read
     * @param values the map from {@link #values()}
     * @return the value the screen should start with
     */
    public static String valueOf(ConfigOption option, Map<String, String> values) {
        return ConfigOption.valueOf(option, values);
    }
}
