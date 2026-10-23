package com.huziyang520.merlinlib.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One switch of either configuration file, declared once for every configuration screen: Cloth
 * Config, Jupiter and NeoForge all walk lists of these, so one key set and one range serve them all.
 *
 * @param section      the table of the file, also the tab of a screen
 * @param key          the full key, spelled {@code section.option}
 * @param kind         the control the option needs
 * @param defaultValue the value the file ships with, as the file spells it
 * @param min          the lowest accepted value of a number, unused by the other kinds
 * @param max          the highest accepted value of a number, unused by the other kinds
 */
public record ConfigOption(String section, String key, Kind kind, String defaultValue, double min, double max) {

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

    /** @return the translation key of the label shown in a configuration screen */
    public String labelKey() {
        return KEY_PREFIX + this.key;
    }

    /** @return the translation key of the section tab */
    public String sectionKey() {
        return KEY_PREFIX + this.section;
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

    /**
     * @param section the section of the file
     * @param key     the option name inside that section
     * @param value   the default, as the file spells it
     * @return a toggle
     */
    public static ConfigOption bool(String section, String key, boolean value) {
        return new ConfigOption(section, section + "." + key, Kind.BOOLEAN, Boolean.toString(value), 0, 0);
    }

    /**
     * @param section the section of the file
     * @param key     the option name inside that section
     * @param value   the default number
     * @param min     the lowest accepted value
     * @param max     the highest accepted value
     * @return a whole number option
     */
    public static ConfigOption number(String section, String key, int value, int min, int max) {
        return new ConfigOption(section, section + "." + key, Kind.INTEGER, Integer.toString(value), min, max);
    }

    /**
     * @param section the section of the file
     * @param key     the option name inside that section
     * @param value   the default number
     * @param min     the lowest accepted value
     * @param max     the highest accepted value
     * @return a decimal option
     */
    public static ConfigOption decimal(String section, String key, double value, double min, double max) {
        return new ConfigOption(section, section + "." + key, Kind.DECIMAL, Double.toString(value), min, max);
    }

    /**
     * @param section the section of the file
     * @param key     the option name inside that section
     * @param value   the default text
     * @return a text option
     */
    public static ConfigOption text(String section, String key, String value) {
        return new ConfigOption(section, section + "." + key, Kind.TEXT, value, 0, 0);
    }

    /**
     * @param section the section of the file
     * @param key     the option name inside that section
     * @param value   the default colour, as the file spells it
     * @return a colour option
     */
    public static ConfigOption colour(String section, String key, String value) {
        return new ConfigOption(section, section + "." + key, Kind.COLOUR, value, 0, 0);
    }

    /**
     * @param all the declared options, in file order
     * @return the sections in the order they first appear, which is the tab order of the screens
     */
    public static List<String> sectionsOf(List<ConfigOption> all) {
        List<String> sections = new ArrayList<>();
        for (ConfigOption option : all) {
            if (!sections.contains(option.section)) {
                sections.add(option.section);
            }
        }
        return sections;
    }

    /**
     * @param all     the declared options
     * @param section the section to filter by
     * @return the options of that section, in file order
     */
    public static List<ConfigOption> of(List<ConfigOption> all, String section) {
        List<ConfigOption> options = new ArrayList<>();
        for (ConfigOption option : all) {
            if (option.section.equals(section)) {
                options.add(option);
            }
        }
        return options;
    }

    /**
     * @param option the option to read
     * @param values the values of the file it belongs to
     * @return the value the screen should start with
     */
    public static String valueOf(ConfigOption option, Map<String, String> values) {
        return values.getOrDefault(option.key, option.defaultValue);
    }
}
