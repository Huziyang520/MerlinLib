// The client half of the switch table: display options and interaction tuning, in the order client.toml documents them.

package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientOptions {
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

    public static List<ConfigOption> all() {
        return ALL;
    }

    public static List<String> sections() {
        return ConfigOption.sectionsOf(ALL);
    }

    public static List<ConfigOption> in(String section) {
        return ConfigOption.of(ALL, section);
    }

    public static String sectionKey(String section) {
        return ConfigOption.KEY_PREFIX + section;
    }

    public static String titleKey() {
        return ConfigOption.KEY_PREFIX + "title";
    }

    public static String noticesKey() {
        return ConfigOption.KEY_PREFIX + "notices.open";
    }

    public static Map<String, String> values() {
        return new LinkedHashMap<>(TomlFile.read(ClientConfig.path(), ClientConfig.defaults()).values());
    }

    public static String valueOf(ConfigOption option, Map<String, String> values) {
        return ConfigOption.valueOf(option, values);
    }
}
