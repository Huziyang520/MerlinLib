package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.util.MerlinColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The one list of configuration options, shared by every configuration screen.
 *
 * <h2>Why the list lives here and not in a screen</h2>
 *
 * <p>Fabric can show the settings through more than one optional mod - Cloth Config, Jupiter - and NeoForge
 * shows them through its own {@code ConfigurationScreen}. What a screen does differs; which switches exist
 * does not. Keeping the keys, kinds, defaults and bounds in one place is what stops two screens from slowly
 * drifting apart, which is exactly what happened when each backend carried its own copy: a switch added to
 * one list was missing from the other until someone noticed.
 *
 * <p>A screen maps {@link Option} to its own entry types and reads the values through
 * {@link ConfigReader}; nothing here knows about a loader, a library or a file.
 *
 * <h2>Colours</h2>
 *
 * <p>A colour is an ARGB integer here, because that is what the code wants, and its text form is
 * {@code 0xAARRGGBB}, because that is what a configuration file holds. {@link #colourText(int)} and the
 * {@code TomlFile} colour parser are the two ends of that conversion.
 */
public final class ConfigOptions {

    /**
     * Sections that belong to the server file; every other section is the client file.
     *
     * <p>These are the switches that decide what a server allows, so they are written to
     * {@code server.toml} and read as authoritative. The screen shows both halves in one list, which is why
     * the split has to be stated once, here, rather than guessed per screen.
     */
    public static final Set<String> SERVER_SECTIONS = Set.of("security", "tools", "content", "notices");

    /** The kinds of entry a screen has to be able to build. */
    public enum Kind {
        BOOL, INT, DOUBLE, COLOR, TEXT
    }

    /**
     * One option of the configuration.
     *
     * @param section      the section, which also decides the file and the screen's category
     * @param name         the key inside that section
     * @param kind         how the value is edited
     * @param defaultValue the default, also used by a screen's reset button
     * @param min          the lowest accepted number, unused for the other kinds
     * @param max          the highest accepted number, unused for the other kinds
     * @param text         the default text, unused for the other kinds
     */
    public record Option(String section, String name, Kind kind, double defaultValue, double min, double max,
                         String text) {

        /** @return the key as the files and {@link ConfigReader} address it, {@code section.key} */
        public String key() {
            return this.section + "." + this.name;
        }

        /** @return the translation key of the label a screen shows */
        public String labelKey() {
            return "merlinlib.configuration." + key();
        }

        /** @return the default of a {@link Kind#BOOL} option */
        public boolean defaultFlag() {
            return this.defaultValue != 0.0D;
        }

        /** @return the default of an {@link Kind#INT} option */
        public int defaultInt() {
            return (int) this.defaultValue;
        }

        /** @return the text form of the default, which for a colour is {@code 0xAARRGGBB} */
        public String defaultText() {
            return this.kind == Kind.COLOR ? colourText((int) this.defaultValue) : this.text;
        }
    }

    private static final List<Option> ALL = List.of(
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
            text("notices", "per_mod", ""));

    private ConfigOptions() {
    }

    /** @return every option, in the order a screen shows them */
    public static List<Option> all() {
        return ALL;
    }

    /**
     * Groups the options by section, keeping the order of {@link #all()}.
     *
     * <p>A screen that shows one tab or category per section needs the options under their own heading; doing
     * the grouping here means two screens cannot disagree about which section an option belongs to.
     *
     * @return section name to its options, in declaration order
     */
    public static Map<String, List<Option>> bySection() {
        Map<String, List<Option>> grouped = new LinkedHashMap<>();
        for (Option option : ALL) {
            grouped.computeIfAbsent(option.section(), section -> new java.util.ArrayList<>()).add(option);
        }
        return grouped;
    }

    /**
     * @param key the {@code section.key} to look up
     * @return the option, or {@code null} when the key is not one of ours
     */
    public static Option find(String key) {
        for (Option option : ALL) {
            if (option.key().equals(key)) {
                return option;
            }
        }
        return null;
    }

    /**
     * @param key a {@code section.key} key
     * @return the part before the first dot, or an empty string when there is none
     */
    public static String sectionOf(String key) {
        int dot = key.indexOf('.');
        return dot < 0 ? "" : key.substring(0, dot);
    }

    /**
     * @param section a section name
     * @return the translation key of that section's heading
     */
    public static String sectionKey(String section) {
        return "merlinlib.configuration." + section;
    }

    /**
     * Formats a colour the way both a configuration file and a text field hold it.
     *
     * @param argb the packed colour
     * @return {@code 0xAARRGGBB}, upper case
     */
    public static String colourText(int argb) {
        return String.format(Locale.ROOT, "0x%08X", argb);
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
}
