package com.huziyang520.merlinlib.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server half of the switch table: the authoritative switches of {@code server.toml}, in the
 * order that file documents them.
 *
 * <p>Declared here so the screens of the two optional front ends can show the same keys the
 * NeoForge spec declares, instead of only the client ones.
 */
public final class ServerOptions {

    /** Every option, in the order the commented file documents it. */
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

    /** @return every option, in file order */
    public static List<ConfigOption> all() {
        return ALL;
    }

    /** @return the sections in the order they first appear, which is the tab order of the screens */
    public static List<String> sections() {
        return ConfigOption.sectionsOf(ALL);
    }

    /**
     * @param section the section of {@code server.toml}
     * @return the options of that section, in file order
     */
    public static List<ConfigOption> in(String section) {
        return ConfigOption.of(ALL, section);
    }

    /**
     * The current value of every option, read from the file the server half reads.
     *
     * <p>Read fresh on every call rather than cached, for the same reason as {@link
     * ClientOptions#values()}: a hand edit takes effect the next time a screen is opened instead of the
     * next time the game starts.
     *
     * @return the values, keyed by {@code section.key}
     */
    public static Map<String, String> values() {
        return new LinkedHashMap<>(TomlFile.read(ServerConfig.path(), ServerConfig.defaults()).values());
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
