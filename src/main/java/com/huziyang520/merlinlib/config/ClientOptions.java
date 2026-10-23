package com.huziyang520.merlinlib.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The client switches, declared once for every configuration screen: Cloth Config, Jupiter and
 * NeoForge all walk this list, so one key list and one set of ranges serve three screens.
 */
public final class ClientOptions {

    /** Prefix of every translation key of the configuration screens. */
    public static final String KEY_PREFIX = "merlinlib.configuration.";

    /** The control an option needs: a toggle, a ranged number, a colour or text. */
    public enum Kind {
        /** A box that is ticked or not. */
        BOOLEAN,
        /** A whole number, held inside {@code min} and {@code max}. */
        INTEGER,
        /** A decimal number, held inside {@code min} and {@code max}. */
        DECIMAL,
        /** An ARGB colour, written {@code 0xRRGGBB} or as a palette name. */
        COLOUR,
        /** Text. */
        TEXT
    }

    /**
 * One switch.
 */
    public record Option(String section, String key, Kind kind, String defaultValue, double min, double max) {

        /** @return the translation key of the label shown in a configuration screen */
        public String labelKey() {
            return KEY_PREFIX + this.key;
        }

        /** @return the translation key of the section tab */
        public String sectionKey() {
            return ClientOptions.sectionKey(this.section);
        }

        /** @return the lowest accepted value of a whole number option */
        public int intMin() {
            return (int) this.min;
        }

        /** @return the highest accepted value of a whole number option */
        public int intMax() {
            return (int) this.max;
        }

        /** @return the default of a whole number option */
        public int defaultInt() {
            return Integer.parseInt(this.defaultValue.trim());
        }

        /** @return the default of a decimal option */
        public double defaultDouble() {
            return Double.parseDouble(this.defaultValue.trim());
        }

        /** @return whether the default is ticked, for a toggle */
        public boolean defaultBoolean() {
            return Boolean.parseBoolean(this.defaultValue);
        }
    }

    /** Every option, in the order the commented file documents them. */
    private static final List<Option> ALL = List.of(
            bool("general", "notice_enabled", true),
            bool("hud", "crosshair_damage", false),
            number("hud", "crosshair_damage_ticks", 20, 1, 400),
            bool("hud", "collapse_enabled", true),
            number("hud", "collapse_threshold", 99, 1, 1000000),
            bool("hud", "armour_auto_dodge", true),
            bool("floating_text", "enabled", false),
            number("floating_text", "duration_ticks", 20, 1, 400),
            decimal("floating_text", "base_scale", 1.0D, 0.1D, 8.0D),
            colour("floating_text", "normal_color", "0xFFFFFFFF"),
            colour("floating_text", "critical_color", "0xFFFFAA00"),
            number("floating_text", "max_concurrent", 32, 1, 512),
            bool("particles", "limit_enabled", true),
            number("particles", "limit", 512, 1, 1000000),
            bool("editor", "hotkey_enabled", false),
            number("editor", "shift_step", 10, 1, 100000),
            number("editor", "scroll_step", 1, 1, 1000),
            number("editor", "scroll_step_fast", 10, 1, 100000),
            bool("macros", "enabled", true));

    private ClientOptions() {
    }

    private static Option bool(String section, String key, boolean value) {
        return new Option(section, section + "." + key, Kind.BOOLEAN, Boolean.toString(value), 0, 0);
    }

    private static Option number(String section, String key, int value, int min, int max) {
        return new Option(section, section + "." + key, Kind.INTEGER, Integer.toString(value), min, max);
    }

    private static Option decimal(String section, String key, double value, double min, double max) {
        return new Option(section, section + "." + key, Kind.DECIMAL, Double.toString(value), min, max);
    }

    private static Option text(String section, String key, String value) {
        return new Option(section, section + "." + key, Kind.TEXT, value, 0, 0);
    }

    private static Option colour(String section, String key, String value) {
        return new Option(section, section + "." + key, Kind.COLOUR, value, 0, 0);
    }

    /** @return every option, in file order */
    public static List<Option> all() {
        return ALL;
    }

    /**
     * @param section the section of {@code client.toml}
     * @return the translation key of that section's tab
 */
    public static String sectionKey(String section) {
        return KEY_PREFIX + section;
    }

    /** @return the translation key of the screen title */
    public static String titleKey() {
        return KEY_PREFIX + "title";
    }

    /** @return the translation key of the entry that opens the join notice page */
    public static String noticesKey() {
        return KEY_PREFIX + "notices.open";
    }

    /**
     * @return the sections in the order they first appear, which is the tab order of both screens
 */
    public static List<String> sections() {
        List<String> sections = new ArrayList<>();
        for (Option option : ALL) {
            if (!sections.contains(option.section())) {
                sections.add(option.section());
            }
        }
        return sections;
    }

    /**
     * @param section the section to filter by
     * @return the options of that section, in file order
 */
    public static List<Option> in(String section) {
        List<Option> options = new ArrayList<>();
        for (Option option : ALL) {
            if (option.section().equals(section)) {
                options.add(option);
            }
        }
        return options;
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
    public static String valueOf(Option option, Map<String, String> values) {
        return values.getOrDefault(option.key(), option.defaultValue());
    }
}
