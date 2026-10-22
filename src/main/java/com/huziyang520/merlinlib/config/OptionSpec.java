package com.huziyang520.merlinlib.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One switch, described once.
 *
 * <h2>Why a schema instead of two hand written lists</h2>
 *
 * <p>MerlinLib offers the same switches through two optional front ends - Cloth Config and Jupiter - and
 * writes them in a third form when neither is installed ({@code client.toml} / {@code server.toml}). Each of
 * those used to need its own copy of "which keys exist, what type, what default, what range", and the three
 * copies silently drift apart: a key added to one screen and not the other is a switch a player can only
 * reach with one of the two mods installed, and a range that differs between them means the same edit is
 * accepted in one screen and rejected in the other.
 *
 * <p>So the list below is the single source. Both screens iterate it, and {@link ConfigOptions} uses the
 * same list to read and write the values, which means the three views cannot disagree by construction.
 *
 * <h2>The keys are the toml keys</h2>
 *
 * <p>{@code section.name} is exactly the flat key {@link ConfigReader} uses, so a value has one name
 * everywhere: in the toml files, in {@code settings.jcfg}, in the screens' language keys
 * ({@code merlinlib.config.<section>.<key>}) and in {@link ConfigBackend#write}.
 *
 * <h2>Hidden entries are still entries</h2>
 *
 * <p>The two {@code config_version} markers are declared here, marked {@link #hiddenFromScreens()}, and never drawn:
 * they exist so that the write path accepts them (the migrations in {@link ClientConfig#load()} and
 * {@link ServerConfig#load()} store them through the backend) and so that a screen cannot invent a value
 * for a key it does not know.
 */
public final class OptionSpec {

    /** What kind of value a switch holds. */
    public enum Kind {
        BOOLEAN, INTEGER, DOUBLE, TEXT
    }

    /**
     * A section of the layout, in the order the templates document it.
     *
     * @param id         the toml section name, also the suffix of its language key
     * @param onClient   whether the values belong to {@code client.toml} ({@code false} means {@code server.toml})
     */
    public record Section(String id, boolean onClient) {
    }

    /** The sections, in template order. */
    private static final List<Section> SECTIONS = List.of(
            new Section("general", true),
            new Section("hud", true),
            new Section("floating_text", true),
            new Section("particles", true),
            new Section("editor", true),
            new Section("macros", true),
            new Section("security", false),
            new Section("tools", false),
            new Section("content", false),
            new Section("notices", false));

    /** Every switch, in template order. */
    private static final List<OptionSpec> ALL;

    static {
        List<OptionSpec> specs = new ArrayList<>();
        // general
        // The two layout markers default to the version their own class migrates to, so a bumped layout
        // cannot leave a screen writing the previous one.
        specs.add(integer("general", "config_version", ClientConfig.CONFIG_VERSION, 0, Integer.MAX_VALUE)
                .hiddenFromScreens());
        specs.add(bool("general", "notice_enabled", true));
        // hud
        specs.add(bool("hud", "crosshair_damage", false));
        specs.add(integer("hud", "crosshair_damage_ticks", 20, 1, 400));
        specs.add(bool("hud", "collapse_enabled", true));
        specs.add(integer("hud", "collapse_threshold", 99, 1, 1_000_000));
        specs.add(bool("hud", "armour_auto_dodge", true));
        // floating_text
        specs.add(bool("floating_text", "enabled", false));
        specs.add(integer("floating_text", "duration_ticks", 20, 1, 400));
        specs.add(decimal("floating_text", "base_scale", 1.0D, 0.1D, 8.0D));
        specs.add(text("floating_text", "normal_color", "0xFFFFFFFF"));
        specs.add(text("floating_text", "critical_color", "0xFFFFAA00"));
        specs.add(integer("floating_text", "max_concurrent", 32, 1, 512));
        // particles
        specs.add(bool("particles", "limit_enabled", true));
        specs.add(integer("particles", "limit", 512, 1, 1_000_000));
        // editor
        specs.add(bool("editor", "hotkey_enabled", false));
        specs.add(integer("editor", "shift_step", 10, 1, 100_000));
        specs.add(integer("editor", "scroll_step", 1, 1, 1_000));
        specs.add(integer("editor", "scroll_step_fast", 10, 1, 100_000));
        // macros
        specs.add(bool("macros", "enabled", true));
        // security
        specs.add(bool("security", "restrict_tools_to_operators", false));
        specs.add(bool("security", "editor_requires_permission", true));
        specs.add(bool("security", "health_editor_requires_permission", true));
        // tools
        specs.add(bool("tools", "enable_testing_toolkit", true));
        specs.add(integer("tools", "max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        specs.add(integer("tools", "max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        specs.add(bool("tools", "allow_levels_above_max", true));
        specs.add(bool("tools", "allow_mismatched_enchantments", false));
        specs.add(bool("tools", "allow_editing_all_items", false));
        specs.add(bool("tools", "allow_non_test_item_damage", false));
        specs.add(bool("tools", "health_editor_requires_item", true));
        specs.add(integer("tools", "config_version", ServerConfig.CONFIG_VERSION, 0, Integer.MAX_VALUE)
                .hiddenFromScreens());
        // content
        specs.add(bool("content", "disable_generated_enchantments", false));
        // notices
        // notices.per_mod is deliberately absent: the per mod switches moved to config/MerlinLib/notices.toml
        // and are drawn by the join notice page. Keeping a spec for the old string would put a raw
        // "<modid>=true;..." text field back into both configuration screens.
        specs.add(bool("notices", "enabled", true));
        ALL = List.copyOf(specs);
    }

    private final String section;
    private final String key;
    private final Kind kind;
    private final String textDefault;
    private final int intDefault;
    private final double doubleDefault;
    private final boolean booleanDefault;
    private final double min;
    private final double max;
    private boolean hidden;

    private OptionSpec(String section, String key, Kind kind, String textDefault, int intDefault,
                       double doubleDefault, boolean booleanDefault, double min, double max) {
        this.section = section;
        this.key = section + "." + key;
        this.kind = kind;
        this.textDefault = textDefault;
        this.intDefault = intDefault;
        this.doubleDefault = doubleDefault;
        this.booleanDefault = booleanDefault;
        this.min = min;
        this.max = max;
    }

    // ---------------------------------------------------------------------------------------------
    // The factories
    // ---------------------------------------------------------------------------------------------

    private static OptionSpec bool(String section, String key, boolean value) {
        return new OptionSpec(section, key, Kind.BOOLEAN, "", 0, 0.0D, value, 0.0D, 0.0D);
    }

    private static OptionSpec integer(String section, String key, int value, int min, int max) {
        return new OptionSpec(section, key, Kind.INTEGER, "", value, 0.0D, false, min, max);
    }

    private static OptionSpec decimal(String section, String key, double value, double min, double max) {
        return new OptionSpec(section, key, Kind.DOUBLE, "", 0, value, false, min, max);
    }

    private static OptionSpec text(String section, String key, String value) {
        return new OptionSpec(section, key, Kind.TEXT, value, 0, 0.0D, false, 0.0D, 0.0D);
    }

    /**
     * Marks a switch that exists but is not shown.
     *
     * @return this spec, so it can be written inline in the list above
     */
    private OptionSpec hiddenFromScreens() {
        this.hidden = true;
        return this;
    }

    // ---------------------------------------------------------------------------------------------
    // Reading the schema
    // ---------------------------------------------------------------------------------------------

    /** @return every switch, in template order; immutable */
    public static List<OptionSpec> all() {
        return ALL;
    }

    /** @return the sections, in template order; immutable */
    public static List<Section> sections() {
        return SECTIONS;
    }

    /** @return the sections that belong to {@code client.toml} */
    public static List<Section> clientSections() {
        List<Section> client = new ArrayList<>();
        for (Section section : SECTIONS) {
            if (section.onClient()) {
                client.add(section);
            }
        }
        return Collections.unmodifiableList(client);
    }

    /**
     * @param section the section id
     * @return whether that section is stored in {@code client.toml}, {@code false} when unknown
     */
    public static boolean onClient(String section) {
        for (Section candidate : SECTIONS) {
            if (candidate.id().equals(section)) {
                return candidate.onClient();
            }
        }
        return false;
    }

    /**
     * @return every switch grouped by section, in the order the screens should draw them
     */
    public static Map<String, List<OptionSpec>> bySection() {
        Map<String, List<OptionSpec>> grouped = new LinkedHashMap<>();
        for (Section section : SECTIONS) {
            grouped.put(section.id(), new ArrayList<>());
        }
        for (OptionSpec spec : ALL) {
            grouped.computeIfAbsent(spec.section(), id -> new ArrayList<>()).add(spec);
        }
        return grouped;
    }

    /** @return the flat key, {@code section.name} */
    public String key() {
        return this.key;
    }

    /** @return the section part of {@link #key()} */
    public String section() {
        return this.section;
    }

    /** @return the name part of {@link #key()} */
    public String name() {
        int dot = this.key.indexOf('.');
        return dot < 0 ? this.key : this.key.substring(dot + 1);
    }

    /** @return the kind of value this switch holds */
    public Kind kind() {
        return this.kind;
    }

    /** @return whether the switch is kept out of the screens */
    public boolean hidden() {
        return this.hidden;
    }

    /** @return the default of a boolean switch; meaningless for the other kinds */
    public boolean booleanDefault() {
        return this.booleanDefault;
    }

    /** @return the default of an integer switch; meaningless for the other kinds */
    public int intDefault() {
        return this.intDefault;
    }

    /** @return the default of a decimal switch; meaningless for the other kinds */
    public double doubleDefault() {
        return this.doubleDefault;
    }

    /** @return the default of a text switch; meaningless for the other kinds */
    public String textDefault() {
        return this.textDefault;
    }

    /** @return the smallest accepted number */
    public double min() {
        return this.min;
    }

    /** @return the largest accepted number */
    public double max() {
        return this.max;
    }

    /**
     * @return the translation key of the switch's name, {@code merlinlib.config.<section>.<key>}
     */
    public String langKey() {
        return "merlinlib.config." + this.key;
    }

    @Override
    public String toString() {
        return this.key + " (" + this.kind + ")";
    }
}
