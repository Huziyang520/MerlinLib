// The server half of the switch table: the authoritative switches of server.toml, in the order that file documents them.

package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ServerOptions {
    private static final List<ConfigOption> ALL = List.of(
            ConfigOption.bool("security", "restrict_tools_to_operators", false),
            ConfigOption.bool("security", "editor_requires_permission", true),
            ConfigOption.bool("security", "health_editor_requires_permission", true),
            ConfigOption.bool("tools", "enable_testing_toolkit", true),
            ConfigOption.number("tools", "max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE),
            ConfigOption.number("tools", "max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE),
            ConfigOption.bool("tools", "allow_levels_above_max", true),
            ConfigOption.bool("tools", "allow_mismatched_enchantments", false),
            ConfigOption.bool("tools", "allow_editing_all_items", false),
            ConfigOption.bool("tools", "allow_non_test_item_damage", false),
            ConfigOption.bool("tools", "health_editor_requires_item", true),
            ConfigOption.bool("content", "disable_generated_enchantments", false),
            ConfigOption.bool("notices", "enabled", true));

    private ServerOptions() {
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

    public static Map<String, String> values() {
        return new LinkedHashMap<>(TomlFile.read(ServerConfig.path(), ServerConfig.defaults()).values());
    }

    public static String valueOf(ConfigOption option, Map<String, String> values) {
        return ConfigOption.valueOf(option, values);
    }
}
