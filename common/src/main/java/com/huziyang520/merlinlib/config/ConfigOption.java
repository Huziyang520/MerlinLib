// One switch of either configuration file, declared once for every configuration screen: Cloth Config, Jupiter and ...

package com.huziyang520.merlinlib.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ConfigOption(String section, String key, Kind kind, String defaultValue, double min, double max) {
    public static final String KEY_PREFIX = "merlinlib.configuration.";

    public enum Kind {
        BOOLEAN,

        INTEGER,

        DECIMAL,

        COLOUR,

        TEXT
    }

    public String labelKey() {
        return KEY_PREFIX + this.key;
    }

    public String sectionKey() {
        return KEY_PREFIX + this.section;
    }

    public int intMin() {
        return (int) this.min;
    }

    public int intMax() {
        return (int) this.max;
    }

    public int defaultInt() {
        return Integer.parseInt(this.defaultValue.trim());
    }

    public double defaultDouble() {
        return Double.parseDouble(this.defaultValue.trim());
    }

    public boolean defaultBoolean() {
        return Boolean.parseBoolean(this.defaultValue);
    }

    public static ConfigOption bool(String section, String key, boolean value) {
        return new ConfigOption(section, section + "." + key, Kind.BOOLEAN, Boolean.toString(value), 0, 0);
    }

    public static ConfigOption number(String section, String key, int value, int min, int max) {
        return new ConfigOption(section, section + "." + key, Kind.INTEGER, Integer.toString(value), min, max);
    }

    public static ConfigOption decimal(String section, String key, double value, double min, double max) {
        return new ConfigOption(section, section + "." + key, Kind.DECIMAL, Double.toString(value), min, max);
    }

    public static ConfigOption text(String section, String key, String value) {
        return new ConfigOption(section, section + "." + key, Kind.TEXT, value, 0, 0);
    }

    public static ConfigOption colour(String section, String key, String value) {
        return new ConfigOption(section, section + "." + key, Kind.COLOUR, value, 0, 0);
    }

    public static List<String> sectionsOf(List<ConfigOption> all) {
        List<String> sections = new ArrayList<>();
        for (ConfigOption option : all) {
            if (!sections.contains(option.section)) {
                sections.add(option.section);
            }
        }
        return sections;
    }

    public static List<ConfigOption> of(List<ConfigOption> all, String section) {
        List<ConfigOption> options = new ArrayList<>();
        for (ConfigOption option : all) {
            if (option.section.equals(section)) {
                options.add(option);
            }
        }
        return options;
    }

    public static String valueOf(ConfigOption option, Map<String, String> values) {
        return values.getOrDefault(option.key, option.defaultValue);
    }
}
