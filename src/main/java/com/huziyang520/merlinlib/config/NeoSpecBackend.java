package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.Range;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The configuration of MerlinLib, declared as NeoForge specifications.
 *
 * <p>Declaring the keys in a {@link ModConfigSpec} is what gives the mod the configuration screen the game
 * already has: the mod list draws a "Config" button, the screen is built from the specification, the file is
 * written and repaired by the loader, and the comments below become the tooltips of the screen. MerlinLib
 * writes no interface of its own for its switches any more.
 *
 * <p>Two files, split by who owns the value:
 *
 * <ul>
 *   <li>{@link #client()} - {@link net.neoforged.fml.config.ModConfig.Type#CLIENT}, this computer only:
 *       display options, the particle cap, the editor steps and the macro switch;</li>
 *   <li>{@link #server()} - {@link net.neoforged.fml.config.ModConfig.Type#COMMON}, authoritative for the
 *       game: permissions, the toolkit limits and the join notice switches.</li>
 * </ul>
 *
 * <p>The server switches are registered as {@code COMMON} and not as {@code SERVER} on purpose. A
 * {@code SERVER} file lives in the save (or in {@code world/serverconfig} on a dedicated server), which
 * would move every documented key to a place the existing documentation, the {@code /reload} flow and the
 * per world defaults do not mention. {@code COMMON} keeps the file in {@code config} with the same meaning
 * it had before.
 *
 * <p>Reading and writing go through the same key strings the old {@link TomlFile} reader used
 * ({@code section.key}), so {@link ClientConfig} and {@link ServerConfig} were able to keep their shape.
 *
 * <p>Every option and every section is given an explicit translation key with
 * {@code ModConfigSpec.Builder#translation(String)}, {@code merlinlib.configuration.<file>.<section>.<key>}; see
 * {@link #LANG_PREFIX} for why the implicit key cannot be used. The matching labels, tooltips and section
 * buttons are in {@code assets/merlinlib/lang/en_us.json} and {@code zh_cn.json}, side by side and with the
 * same keys, so a missing one shows up as a bare key on screen rather than as silently English text.
 */
public final class NeoSpecBackend implements ConfigBackend {

    /** The client file, below the game's {@code config} directory. */
    public static final String CLIENT_FILE = Constants.MOD_ID + "-client.toml";

    /** The common file, below the game's {@code config} directory. */
    public static final String SERVER_FILE = Constants.MOD_ID + "-common.toml";

    /**
     * The layout this build writes into the client file.
     *
     * <p>2 is the first layout written by the specification: the whole {@code [gui]} section is gone (the
     * animated screens it configured no longer exist), and the version marker moved from {@code [gui]} to
     * {@code [general]} beside the notice switch.
     */
    public static final int CLIENT_VERSION = 2;

    /**
     * The layout this build writes into the common file.
     *
     * <p>5 is the first layout written by the specification; the version marker moved from {@code [tools]} to
     * {@code [general]}, where it is not hidden among the toolkit limits.
     */
    public static final int SERVER_VERSION = 5;

    /** Name of the pre-specification file inside {@link ConfigDirectory#root()}. */
    private static final String LEGACY_CLIENT_FILE = "client.toml";
    private static final String LEGACY_SERVER_FILE = "server.toml";

    /**
     * The prefix of every translation key the configuration screen shows.
     *
     * <p>NeoForge builds the key of an option from {@code ValueSpec#getTranslationKey()}, and falls back to
     * {@code modId + ".configuration." + <entry key>} when the spec never set one - the entry key being the
     * <em>leaf</em> only, because the screen iterates one section at a time
     * ({@code ConfigurationSectionScreen#getTranslationKey(String)} in neoforge-21.1.233 sources, line 557:
     * {@code return result != null ? result : context.modId + ".configuration." + key;}). That fallback cannot
     * name our file: {@code enabled} exists in three sections and {@code config_version} in both files, so
     * every one of them would share a single label and a single tooltip.
     *
     * <p>The spec therefore sets the key of every section and every value explicitly with
     * {@code Builder#translation(String)}, which is stored in the {@code ValueSpec} and wins over the fallback.
     * The keys read {@code merlinlib.configuration.<file>.<section>.<key>}, with {@code client} or
     * {@code common} standing for the file - the same prefix the screen uses for its own title
     * ({@code merlinlib.configuration.title}) and for the two file buttons
     * ({@code merlinlib.configuration.section.<file name>}).
     */
    private static final String LANG_PREFIX = Constants.MOD_ID + ".configuration";

    /** Suffix a file that has been read into the specification is renamed with. */
    private static final String IMPORTED_SUFFIX = ".imported";

    private final ModConfigSpec spec;
    private final String fileName;
    private final String legacyFileName;
    private final Map<String, ConfigValue<?>> values;

    /** Whether the one time import of the pre-specification file has already been attempted. */
    private boolean importAttempted;

    private NeoSpecBackend(Spec spec, String fileName, String legacyFileName) {
        this.spec = spec.build();
        this.fileName = fileName;
        this.legacyFileName = legacyFileName;
        this.values = Map.copyOf(spec.values());
    }

    /**
     * @return the backend of the client switches, with its specification built
     */
    public static NeoSpecBackend client() {
        Spec spec = new Spec(LANG_PREFIX + ".client");

        spec.section("general", "Settings that belong to the file as a whole.");
        spec.intRange("config_version", CLIENT_VERSION, 0, Integer.MAX_VALUE,
                "File layout version. Written by the mod, read only to bring a file from an older build up to "
                        + "the current layout; do not set it back by hand.");
        spec.bool("notice_enabled", true,
                "Show MerlinLib's own join notice in chat: the one that says the configuration screen was "
                        + "rebuilt and that the visual screen on the other loaders needs Cloth Config API or "
                        + "Jupiter. On by default. Business mods' notices are switched in the [notices] section "
                        + "of the common file, not here.");
        spec.end();

        spec.section("hud", "Changes to the vanilla HUD.");
        spec.bool("crosshair_damage", false,
                "Show the damage of the weapon you just used near the crosshair. Off by default. This is the "
                        + "weapon's listed damage, not the amount of health actually removed.");
        spec.intRange("crosshair_damage_ticks", 20, 1, 400,
                "How long the crosshair damage number stays on screen, in ticks (20 ticks = 1 second).");
        spec.bool("collapse_enabled", true,
                "Fold a very large health bar into one heart plus the exact numbers, instead of drawing one "
                        + "heart per two points. Up to collapse_threshold the bar stays plain vanilla.");
        spec.intRange("collapse_threshold", 99, 1, 1000000, "Health above which the bar folds.");
        spec.bool("armour_auto_dodge", true, "Move the armour bar up against the folded health row.");
        spec.end();

        spec.section("floating_text", "Damage numbers flying off a mob when it is hit.");
        spec.bool("enabled", false,
                "Show damage numbers flying off a mob when it is hit. Off by default: it is a testing aid, so "
                        + "it is something a player turns on rather than something dropped on them.");
        spec.intRange("duration_ticks", 20, 1, 400,
                "How long a damage number stays visible after a mob is hit, in ticks.");
        spec.doubleRange("base_scale", 1.0D, 0.1D, 8.0D,
                "Base scale of a damage number. Higher means larger text.");
        spec.text("normal_color", "0xFFFFFFFF",
                "ARGB colour of a normal hit, written as 0xRRGGBB (or 0xAARRGGBB), #RRGGBB or a palette name.");
        spec.text("critical_color", "0xFFFFAA00", "ARGB colour of a critical hit, in the same forms.");
        spec.intRange("max_concurrent", 32, 1, 512,
                "Upper bound on simultaneous damage numbers; the oldest one is dropped first.");
        spec.end();

        spec.section("particles", "Limits on the particles this client draws.");
        spec.bool("limit_enabled", true,
                "Cap how many particles this client draws. Some paths of the game ask for one particle per "
                        + "point of damage, so a single hit with a very large number can freeze the client "
                        + "while it builds them all - the frame never finishes because it is still making "
                        + "particles. When enabled, at most limit particles are drawn per 1/20 second; the rest "
                        + "are dropped before they exist. On by default.");
        spec.intRange("limit", 512, 1, 1000000,
                "How many particles may be drawn per 1/20 second. 512 is far above what normal play produces "
                        + "(rain, explosions, a busy farm) and still keeps a damage number in the thousands from "
                        + "freezing the client. Lower it if a very large hit still stutters.");
        spec.end();

        spec.section("editor", "The item editor.");
        spec.bool("hotkey_enabled", false,
                "Allow the editor hotkey to open the item editor. Off by default, so the library can be "
                        + "installed without claiming a key. Toggle it in game with /merlinlib editorhotkey on, "
                        + "or open the editor with /merlinlib editor.");
        spec.intRange("shift_step", 10, 1, 100000,
                "Value change when a - or + button is clicked while holding shift.");
        spec.intRange("scroll_step", 1, 1, 1000, "Value change per mouse wheel notch over a number field.");
        spec.intRange("scroll_step_fast", 10, 1, 100000,
                "Value change per mouse wheel notch while holding ctrl.");
        spec.end();

        spec.section("macros", "Command macros.");
        spec.bool("enabled", true, "Enable the macro toolkit: the macro screen and running macros from their "
                + "bound keys.");
        spec.end();

        return new NeoSpecBackend(spec, CLIENT_FILE, LEGACY_CLIENT_FILE);
    }

    /**
     * @return the backend of the server switches, with its specification built
     */
    public static NeoSpecBackend server() {
        Spec spec = new Spec(LANG_PREFIX + ".common");

        spec.section("general", "Settings that belong to the file as a whole.");
        spec.intRange("config_version", SERVER_VERSION, 0, Integer.MAX_VALUE,
                "File layout version. Written by the mod, read only to bring a file from an older build up to "
                        + "the current layout. Leave it alone; setting it back only repeats the migration.");
        spec.end();

        spec.section("security", "Who may use the testing toolkit.");
        spec.bool("restrict_tools_to_operators", false,
                "When true, only operators may use the testing toolkit (editors, health editor, macros).");
        spec.bool("editor_requires_permission", true,
                "Require command permission (operator) to open the item editor. On by default.");
        spec.bool("health_editor_requires_permission", true,
                "Require command permission (operator) to open the health editor. On by default.");
        spec.end();

        spec.section("tools", "The testing toolkit and its limits.");
        spec.bool("enable_testing_toolkit", true,
                "Master switch of the testing screens: with it off, no toolkit screen opens (the item editor, "
                        + "the health editor and the macro screen all refuse). The testing items stay in the "
                        + "creative tab.");
        spec.intRange("max_test_weapon_damage", Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                "Upper bound for the damage value of the test weapons. Defaults to the integer limit.");
        spec.intRange("max_enchantment_level", Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                "Upper bound for enchantment levels set through the item editor. Defaults to the integer "
                        + "limit.");
        spec.bool("allow_levels_above_max", true,
                "When true, an enchantment may be raised above the maximum level its own definition declares. "
                        + "On by default: the ceiling that matters is max_enchantment_level above, and the "
                        + "level codec that used to cap every saved level at 255 was widened by the mod, so the "
                        + "range is real.");
        spec.bool("allow_mismatched_enchantments", false,
                "When true, any enchantment may be added to any item, for example efficiency on a sword. Off by "
                        + "default: an enchantment that the item cannot normally carry has no effect in game.");
        spec.bool("allow_editing_all_items", false,
                "When true, the item editor opens for every item, including blocks and other things that are "
                        + "not weapons, tools or equipment. Off by default: the editor is meant for the "
                        + "equipment it can do something useful with, and opening it on a block or a bucket "
                        + "invites edits that do nothing.");
        spec.bool("allow_non_test_item_damage", false,
                "When true, the base damage of an ordinary weapon can be edited too, not only of a testing "
                        + "weapon. Off by default: ordinary weapons are what vanilla balance and other mods "
                        + "rely on.");
        spec.bool("health_editor_requires_item", true,
                "When true, the health editor only opens while holding the health editor item.");
        spec.end();

        spec.section("content", "The generated content.");
        spec.bool("disable_generated_enchantments", false,
                "Emergency switch: when true, none of the enchantments defined in config/MerlinLib/*.json are "
                        + "generated. Code api registrations are unaffected.");
        spec.end();

        spec.section("notices", "Join notices, offered to every player who joins.");
        spec.bool("enabled", true,
                "Show the join notices business mods registered through MerlinLib (MerlinApi.notices()). On by "
                        + "default; turn it off to silence every join notice at once. Takes effect on the next "
                        + "join.");
        spec.text("per_mod", "",
                "Per mod switches, written as \"<modid>=true;<modid>=false>\". A mod that is not listed uses the "
                        + "default it declared when it registered. Editing this line by hand works too.");
        spec.end();

        return new NeoSpecBackend(spec, SERVER_FILE, LEGACY_SERVER_FILE);
    }

    /**
     * @return the specification to register with the mod container
     */
    public ModConfigSpec spec() {
        return this.spec;
    }

    @Override
    public String name() {
        return this.fileName;
    }

    @Override
    public ConfigReader view() {
        return new View();
    }

    /**
     * Reads an existing pre-specification file into the specification, once, and renames it afterwards.
     *
     * <p>The file this build uses did not exist before, so a player who configured MerlinLib with an older
     * build would otherwise start from the defaults and lose every choice. Everything that can be carried
     * over is therefore copied key by key, the file is saved, and the old file is renamed to
     * {@code *.imported} - which is both the record that the import happened and what makes it idempotent:
     * the second call finds no old file and does nothing.
     *
     * <p>Called after the loader has loaded this config, because a value cannot be written before that.
     *
     * @return {@code true} when an old file was found and imported
     */
    public synchronized boolean importLegacy() {
        if (this.importAttempted) {
            return false;
        }
        if (!this.spec.isLoaded()) {
            // Not loaded yet: leave the attempt for the next call rather than marking it as done.
            return false;
        }
        this.importAttempted = true;

        Path legacy = ConfigDirectory.root().resolve(this.legacyFileName);
        if (!Files.isRegularFile(legacy)) {
            return false;
        }
        TomlFile old = TomlFile.read(legacy, "");
        int copied = 0;
        for (String key : this.values.keySet()) {
            String raw = old.getString(key, null);
            if (raw != null) {
                write(key, raw);
                copied++;
            }
        }
        applyLegacyLayout(old);
        save();
        Constants.LOG.info("[MerlinLib] imported {} value(s) from the old {} into {}", copied,
                this.legacyFileName, this.fileName);

        Path imported = legacy.resolveSibling(this.legacyFileName + IMPORTED_SUFFIX);
        try {
            Files.move(legacy, imported, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            // The values are already in the new file, so a failed rename only means the import is attempted
            // again next launch - which is harmless, the same values are copied again.
            Constants.LOG.warn("[MerlinLib] could not rename {} to {}: {}", legacy, imported,
                    exception.getMessage());
        }
        return true;
    }

    /**
     * Applies the one-off layout changes a key by key copy cannot express.
     *
     * <p>Both rules are the ones the old reader applied to a file written by an older build; only a value
     * that still holds the old default is moved, because any other value was chosen by hand.
     *
     * @param old the file that was just read
     */
    private void applyLegacyLayout(TomlFile old) {
        if (LEGACY_CLIENT_FILE.equals(this.legacyFileName)) {
            int version = old.getInt("general.config_version", 0, 0, Integer.MAX_VALUE);
            int guiVersion = old.getInt("gui.config_version", 0, 0, Integer.MAX_VALUE);
            if (Math.max(version, guiVersion) < 1 && old.getBoolean("floating_text.enabled", true)) {
                // Damage numbers used to be on by default; a file from before that change keeps showing them
                // otherwise, because a file always wins over the defaults.
                write("floating_text.enabled", "false");
            }
            return;
        }
        int version = old.getInt("general.config_version", 0, 0, Integer.MAX_VALUE);
        int toolsVersion = old.getInt("tools.config_version", 0, 0, Integer.MAX_VALUE);
        if (Math.max(version, toolsVersion) >= 4) {
            return;
        }
        if (old.getInt("tools.max_enchantment_level", 255, 1, Integer.MAX_VALUE) == 255) {
            write("tools.max_enchantment_level", Integer.toString(Integer.MAX_VALUE));
        }
        if (old.getInt("tools.max_test_weapon_damage", 100000, 1, Integer.MAX_VALUE) == 100000) {
            write("tools.max_test_weapon_damage", Integer.toString(Integer.MAX_VALUE));
        }
        if (old.getBoolean("tools.allow_editing_all_items", false)) {
            write("tools.allow_editing_all_items", "false");
        }
        if (!old.getBoolean("tools.allow_levels_above_max", false)) {
            write("tools.allow_levels_above_max", "true");
        }
    }

    @Override
    public synchronized void write(String key, String raw) {
        ConfigValue<?> value = this.values.get(key);
        if (value == null) {
            Constants.LOG.warn("[MerlinLib] {} has no key {}; the value was not written", this.fileName, key);
            return;
        }
        if (!this.spec.isLoaded()) {
            // The loader has not read this file yet (a dedicated server never loads a client file). Writing
            // would throw inside the specification, and there is no file to write it back to, so the value
            // is dropped with a note rather than lost silently.
            Constants.LOG.warn("[MerlinLib] {} is not loaded on this side, so {} was not written",
                    this.fileName, key);
            return;
        }
        Object converted = convert(value, key, raw);
        if (converted == null) {
            return;
        }
        setUnchecked(value, converted);
    }

    @Override
    public synchronized void save() {
        if (!this.spec.isLoaded()) {
            return;
        }
        this.spec.save();
    }

    /**
     * Converts the text of a file line into the type the key was declared with.
     *
     * @param value the key
     * @param key   the key's name, for the error message
     * @param raw   the text to convert
     * @return the converted value, or {@code null} when the text cannot be used
     */
    private Object convert(ConfigValue<?> value, String key, String raw) {
        Object fallback = value.getDefault();
        try {
            if (fallback instanceof Boolean) {
                if ("true".equalsIgnoreCase(raw)) {
                    return Boolean.TRUE;
                }
                if ("false".equalsIgnoreCase(raw)) {
                    return Boolean.FALSE;
                }
                Constants.LOG.warn("[MerlinLib] {}={} is not true or false; {} was not written",
                        key, raw, key);
                return null;
            }
            if (fallback instanceof Integer) {
                return (int) clamp(value, Integer.parseInt(raw.trim()));
            }
            if (fallback instanceof Long) {
                return (long) clamp(value, Long.parseLong(raw.trim()));
            }
            if (fallback instanceof Double) {
                return (double) clamp(value, Double.parseDouble(raw.trim()));
            }
            if (fallback instanceof String) {
                return raw;
            }
        } catch (NumberFormatException exception) {
            Constants.LOG.warn("[MerlinLib] {}={} is not a number; {} was not written", key, raw, key);
            return null;
        }
        Constants.LOG.warn("[MerlinLib] {} has an unsupported type, so {} was not written",
                key, key);
        return null;
    }

    /**
     * Clamps a number to the range the key was declared with.
     *
     * @param value  the key
     * @param number the number to clamp
     * @return the number, kept inside the declared range
     */
    @SuppressWarnings("unchecked")
    private static <T extends Comparable<? super T>> T clamp(ConfigValue<?> value, T number) {
        ValueSpec spec = value.getSpec();
        Range<?> range = spec == null ? null : spec.getRange();
        if (range == null) {
            return number;
        }
        Range<T> typed = (Range<T>) range;
        if (number.compareTo(typed.getMin()) < 0) {
            return typed.getMin();
        }
        if (number.compareTo(typed.getMax()) > 0) {
            return typed.getMax();
        }
        return number;
    }

    /**
     * Sets a value through the specification's own setter, which is typed to the key.
     *
     * @param value  the key
     * @param newOne the already converted value
     */
    @SuppressWarnings("unchecked")
    private static void setUnchecked(ConfigValue<?> value, Object newOne) {
        ((ConfigValue<Object>) value).set(newOne);
    }

    /**
     * Reads one key, falling back to its declared default while the loader has not read the file yet.
     *
     * @param key the key
     * @return the value, or {@code null} when this backend has no such key
     */
    private Object raw(String key) {
        ConfigValue<?> value = this.values.get(key);
        if (value == null) {
            return null;
        }
        if (!this.spec.isLoaded()) {
            return value.getDefault();
        }
        try {
            return value.get();
        } catch (RuntimeException exception) {
            // A value that cannot be read must not take the game down: the default is used instead.
            Constants.LOG.warn("[MerlinLib] could not read {} from {}: {}", key, this.fileName,
                    exception.getMessage());
            return value.getDefault();
        }
    }

    /** Builds the specification while remembering every key under its {@code section.key} name. */
    private static final class Spec {

        /** The translation key prefix of this file, {@code merlinlib.configuration.client} or {@code .common}. */
        private final String lang;
        private final ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        private final Map<String, ConfigValue<?>> values = new LinkedHashMap<>();
        private final Deque<String> path = new ArrayDeque<>();

        /**
         * @param lang the translation key prefix of this file, without a trailing dot
         */
        Spec(String lang) {
            this.lang = lang;
        }

        /**
         * Opens a section.
         *
         * <p>{@code translation()} comes before {@code push()}: the builder keeps the key aside until the
         * section is opened and then files it under the section's path, which is the entry
         * {@code ConfigurationSectionScreen} looks up for a section's title. Without it the title would be
         * {@code modid.configuration.<section>} - the leaf only, shared by both files.
         *
         * @param name    the section name, as written in the file
         * @param comment the section's note, shown as its tooltip when the lang file has no override
         */
        void section(String name, String comment) {
            this.builder.translation(this.key(name));
            this.builder.comment(comment);
            this.builder.push(name);
            this.path.addLast(name);
        }

        void end() {
            this.builder.pop();
            this.path.removeLast();
        }

        void bool(String key, boolean defaultValue, String comment) {
            this.values.put(full(key), this.builder.translation(this.key(full(key)))
                    .comment(comment).define(key, defaultValue));
        }

        void intRange(String key, int defaultValue, int min, int max, String comment) {
            this.values.put(full(key), this.builder.translation(this.key(full(key)))
                    .comment(comment).defineInRange(key, defaultValue, min, max));
        }

        void doubleRange(String key, double defaultValue, double min, double max, String comment) {
            this.values.put(full(key), this.builder.translation(this.key(full(key)))
                    .comment(comment).defineInRange(key, defaultValue, min, max));
        }

        void text(String key, String defaultValue, String comment) {
            this.values.put(full(key), this.builder.translation(this.key(full(key)))
                    .comment(comment).define(key, defaultValue));
        }

        /**
         * @param name a section or {@code section.key} name
         * @return the translation key that name is labelled with
         */
        String key(String name) {
            return this.lang + "." + name;
        }

        String full(String key) {
            StringBuilder builder = new StringBuilder();
            for (String part : this.path) {
                builder.append(part).append('.');
            }
            return builder.append(key).toString();
        }

        ModConfigSpec build() {
            return this.builder.build();
        }

        Map<String, ConfigValue<?>> values() {
            return this.values;
        }
    }

    /**
     * One read of the specification.
     *
     * <p>A fresh view is handed out for each reload and keeps the problems it found to itself, so the
     * warnings of one reload do not pile up on the next.
     */
    private final class View implements ConfigReader {

        private final List<ContentError> errors = new ArrayList<>();

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            Object raw = raw(key);
            if (raw instanceof Boolean value) {
                return value;
            }
            return invalid(key, "true or false", raw, fallback);
        }

        @Override
        public int getInt(String key, int fallback, int min, int max) {
            Object raw = raw(key);
            if (raw instanceof Number number) {
                int value = number.intValue();
                if (value < min || value > max) {
                    this.errors.add(new ContentError(NeoSpecBackend.this.fileName, key, "-",
                            "an integer in [" + min + ", " + max + "]", "found '" + value + "'"));
                    return fallback;
                }
                return value;
            }
            return invalid(key, "an integer in [" + min + ", " + max + "]", raw, fallback);
        }

        @Override
        public double getDouble(String key, double fallback, double min, double max) {
            Object raw = raw(key);
            if (raw instanceof Number number) {
                double value = number.doubleValue();
                if (value < min || value > max) {
                    this.errors.add(new ContentError(NeoSpecBackend.this.fileName, key, "-",
                            "a number in [" + min + ", " + max + "]", "found '" + value + "'"));
                    return fallback;
                }
                return value;
            }
            return invalid(key, "a number in [" + min + ", " + max + "]", raw, fallback);
        }

        @Override
        public int getColor(String key, int fallback) {
            Object raw = raw(key);
            if (raw instanceof String text) {
                return MerlinColor.parse(text).orElseGet(() -> {
                    this.errors.add(new ContentError(NeoSpecBackend.this.fileName, key, "-",
                            "a colour such as 0xRRGGBB, #RRGGBB or a palette name", "found '" + text + "'"));
                    return fallback;
                });
            }
            return invalid(key, "a colour such as 0xRRGGBB, #RRGGBB or a palette name", raw, fallback);
        }

        @Override
        public String getString(String key, String fallback) {
            Object raw = raw(key);
            if (raw instanceof String text) {
                return text;
            }
            return invalid(key, "a quoted string", raw, fallback);
        }

        @Override
        public List<ContentError> errors() {
            return List.copyOf(this.errors);
        }

        /**
         * Records a value that is missing or of the wrong type and answers with the fallback.
         *
         * @param key      the key
         * @param expected what the key should hold
         * @param raw      what was read
         * @param fallback the value to answer with
         * @return the fallback
         */
        private <T> T invalid(String key, String expected, Object raw, T fallback) {
            this.errors.add(new ContentError(NeoSpecBackend.this.fileName, key, "-", expected,
                    raw == null ? "the key is missing" : "found '" + raw + "'"));
            return fallback;
        }
    }
}
