package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.config.ConfigGroup;
import com.iafenvoy.jupiter.config.container.FileConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.entry.StringEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MerlinLib's switches, stored in a Jupiter container.
 *
 * <h2>Loaded only when Jupiter is there</h2>
 *
 * <p>Every type imported above comes from Jupiter, so this class is only ever touched through
 * {@link JupiterBackend}, which checks that the library exists first. Nothing else in the mod may refer to
 * it: a static reference from a class that always loads would take the server down at start up on an
 * installation without Jupiter.
 *
 * <h2>One container, both switches</h2>
 *
 * <p>The client and server options share one container because the two toml files they replace were
 * already read by the same process and their keys never collide ({@code hud.*} versus {@code tools.*}).
 * The {@code config_version} markers stay in as hidden entries: they are written by the migrations and
 * read back on the next launch, but nothing useful happens if a player edits them.
 *
 * <h2>Values are read through {@link ConfigReader}</h2>
 *
 * <p>The two configuration classes ask the same questions they used to ask a parsed file, and this class
 * answers them from the live entries - so a value changed in Jupiter's screen is visible to the next
 * action, exactly like an edit to the toml file used to be.
 */
final class JupiterSettings implements ConfigBackend, ConfigReader {

    /**
     * Where Jupiter writes its JSON.
     *
     * <p>Deliberately <b>not</b> a {@code .json} file inside {@code config/MerlinLib}: that directory is
     * scanned for content definitions ({@link ConfigDirectory#jsonFiles()}), and a settings file there
     * would be parsed as an enchantment definition and reported as a broken one.
     */
    private static final String FILE = "config/MerlinLib/settings.jcfg";

    /** The layout version this build writes; see the migration in {@link ClientConfig#load()}. */
    private static final int CLIENT_LAYOUT = 4;

    /** The layout version {@link ServerConfig} writes. */
    private static final int SERVER_LAYOUT = 5;

    /** What kind of value an entry holds, for the string based {@link #write} path. */
    private enum Kind {
        BOOLEAN, INTEGER, DOUBLE, TEXT
    }

    private final Map<String, ConfigEntry<?>> entries = new LinkedHashMap<>();
    private final Map<String, Kind> kinds = new LinkedHashMap<>();
    private final List<ContentError> errors = new ArrayList<>();
    private final Container container = new Container();

    private JupiterSettings() {
    }

    /**
     * Builds the container, hands it to Jupiter and returns the backend.
     *
     * <p>Registering is what calls {@code init()} (which builds the entries) and then {@code load()} (which
     * reads Jupiter's file, or writes it with the defaults when it is missing), so the values are live by
     * the time this returns.
     *
     * @return the backend
     */
    static ConfigBackend open() {
        JupiterSettings settings = new JupiterSettings();
        ConfigManager.getInstance().registerConfigHandler(settings.container);
        return settings;
    }

    // ---------------------------------------------------------------------------------------------
    // ConfigBackend
    // ---------------------------------------------------------------------------------------------

    @Override
    public ConfigReader view() {
        return this;
    }

    @Override
    public void write(String key, String rawValue) {
        ConfigEntry<?> entry = this.entries.get(key);
        Kind kind = this.kinds.get(key);
        if (entry == null || kind == null) {
            // A key from an older layout, or from a caller that keeps its own set of overrides. Ignoring it
            // is the documented behaviour: the set of switches is fixed by code.
            Constants.LOG.debug("[MerlinLib] {} is not a MerlinLib option, ignoring it", key);
            return;
        }
        try {
            switch (kind) {
                case BOOLEAN -> set(entry, Boolean.parseBoolean(rawValue.trim()));
                case INTEGER -> set(entry, Integer.decode(rawValue.trim()));
                case DOUBLE -> set(entry, Double.parseDouble(rawValue.trim()));
                case TEXT -> set(entry, unquote(rawValue.trim()));
            }
        } catch (NumberFormatException malformed) {
            this.invalid(key, "a number", rawValue);
        }
    }

    @Override
    public void save() {
        this.container.save();
    }

    @Override
    public String name() {
        return "Jupiter (" + FILE + ")";
    }

    // ---------------------------------------------------------------------------------------------
    // ConfigReader
    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean getBoolean(String key, boolean fallback) {
        Object value = value(key);
        return value instanceof Boolean stored ? stored : fallback;
    }

    @Override
    public int getInt(String key, int fallback, int min, int max) {
        Object value = value(key);
        if (!(value instanceof Number stored)) {
            return fallback;
        }
        int parsed = stored.intValue();
        if (parsed < min || parsed > max) {
            this.invalid(key, "an integer in [" + min + ", " + max + "]", Integer.toString(parsed));
            return fallback;
        }
        return parsed;
    }

    @Override
    public double getDouble(String key, double fallback, double min, double max) {
        Object value = value(key);
        if (!(value instanceof Number stored)) {
            return fallback;
        }
        double parsed = stored.doubleValue();
        if (parsed < min || parsed > max) {
            this.invalid(key, "a number in [" + min + ", " + max + "]", Double.toString(parsed));
            return fallback;
        }
        return parsed;
    }

    @Override
    public int getColor(String key, int fallback) {
        Object value = value(key);
        if (!(value instanceof String stored)) {
            return fallback;
        }
        Optional<Integer> parsed = MerlinColor.parse(stored);
        if (parsed.isEmpty()) {
            this.invalid(key, "a colour such as 0xRRGGBB, #RRGGBB or a palette name", stored);
            return fallback;
        }
        return parsed.get();
    }

    @Override
    public String getString(String key, String fallback) {
        Object value = value(key);
        return value == null ? fallback : String.valueOf(value);
    }

    @Override
    public List<ContentError> errors() {
        return List.copyOf(this.errors);
    }

    /**
     * @param key the flat key
     * @return the stored value, or {@code null} when there is no such option
     */
    private Object value(String key) {
        ConfigEntry<?> entry = this.entries.get(key);
        return entry == null ? null : entry.getValue();
    }

    private void invalid(String key, String expected, String found) {
        this.errors.add(new ContentError("settings.jcfg", key, "-", expected, "found '" + found + "'"));
    }

    @SuppressWarnings("unchecked")
    private static void set(ConfigEntry<?> entry, Object value) {
        ((ConfigEntry<Object>) entry).setValue(value);
    }

    /**
     * Removes the quotes a toml file puts around a string, so an imported value and one typed into
     * Jupiter's text field end up identical.
     *
     * @param value the raw value
     * @return the value without its surrounding quotes
     */
    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    // ---------------------------------------------------------------------------------------------
    // The container and its options
    // ---------------------------------------------------------------------------------------------

    /**
     * Jupiter's view of this mod's options: one tab per section of the toml layout.
     *
     * <p>Not static, so {@code init()} can register the entries with the outer object; Jupiter keeps the
     * instance for the life of the process, which is the same lifetime this backend has.
     */
    private final class Container extends FileConfigContainer {

        private Container() {
            super(new ResourceLocation(Constants.MOD_ID, "settings"),
                    Component.translatable("merlinlib.config.title"), FILE);
        }

        @Override
        public void init() {
            if (!getConfigTabs().isEmpty()) {
                return;
            }
            buildTabs();
        }
    }

    private ConfigGroup tab(String id) {
        return this.container.createTab(id, Component.translatable("merlinlib.config.tab." + id));
    }

    private static Component label(String key) {
        return Component.translatable("merlinlib.config." + key);
    }

    private void add(ConfigGroup tab, ConfigEntry<?> entry) {
        tab.addEntry(entry);
        this.entries.put(entry.getKey(), entry);
        this.kinds.put(entry.getKey(), kindOf(entry));
    }

    private static Kind kindOf(ConfigEntry<?> entry) {
        if (entry instanceof BooleanEntry) {
            return Kind.BOOLEAN;
        }
        if (entry instanceof IntegerEntry) {
            return Kind.INTEGER;
        }
        if (entry instanceof DoubleEntry) {
            return Kind.DOUBLE;
        }
        return Kind.TEXT;
    }

    /**
     * Declares every option, in the order the toml templates document them.
     *
     * <p>Kept beside the templates in {@link ClientConfig} and {@link ServerConfig} by hand: those two are
     * the documented reference a player reads, this is what a Jupiter screen shows, and the reader used
     * depends on which one is installed.
     */
    private void buildTabs() {
        ConfigGroup general = tab("general");
        add(general, IntegerEntry.builder(label("general.config_version"), CLIENT_LAYOUT)
                .key("general.config_version").visible(false).build());
        add(general, BooleanEntry.builder(label("general.notice_enabled"), true)
                .key("general.notice_enabled").build());

        ConfigGroup hud = tab("hud");
        add(hud, BooleanEntry.builder(label("hud.crosshair_damage"), false)
                .key("hud.crosshair_damage").build());
        add(hud, IntegerEntry.builder(label("hud.crosshair_damage_ticks"), 20).range(1, 400)
                .key("hud.crosshair_damage_ticks").build());
        add(hud, BooleanEntry.builder(label("hud.collapse_enabled"), true)
                .key("hud.collapse_enabled").build());
        add(hud, IntegerEntry.builder(label("hud.collapse_threshold"), 99).range(1, 1000000)
                .key("hud.collapse_threshold").build());
        add(hud, BooleanEntry.builder(label("hud.armour_auto_dodge"), true)
                .key("hud.armour_auto_dodge").build());

        ConfigGroup floating = tab("floating_text");
        add(floating, BooleanEntry.builder(label("floating_text.enabled"), false)
                .key("floating_text.enabled").build());
        add(floating, IntegerEntry.builder(label("floating_text.duration_ticks"), 20).range(1, 400)
                .key("floating_text.duration_ticks").build());
        add(floating, DoubleEntry.builder(label("floating_text.base_scale"), 1.0D).range(0.1D, 8.0D)
                .key("floating_text.base_scale").build());
        add(floating, StringEntry.builder(label("floating_text.normal_color"), "0xFFFFFFFF")
                .key("floating_text.normal_color").build());
        add(floating, StringEntry.builder(label("floating_text.critical_color"), "0xFFFFAA00")
                .key("floating_text.critical_color").build());
        add(floating, IntegerEntry.builder(label("floating_text.max_concurrent"), 32).range(1, 512)
                .key("floating_text.max_concurrent").build());

        ConfigGroup particles = tab("particles");
        add(particles, BooleanEntry.builder(label("particles.limit_enabled"), true)
                .key("particles.limit_enabled").build());
        add(particles, IntegerEntry.builder(label("particles.limit"), 512).range(1, 1000000)
                .key("particles.limit").build());

        ConfigGroup editor = tab("editor");
        add(editor, BooleanEntry.builder(label("editor.hotkey_enabled"), false)
                .key("editor.hotkey_enabled").build());
        add(editor, IntegerEntry.builder(label("editor.shift_step"), 10).range(1, 100000)
                .key("editor.shift_step").build());
        add(editor, IntegerEntry.builder(label("editor.scroll_step"), 1).range(1, 1000)
                .key("editor.scroll_step").build());
        add(editor, IntegerEntry.builder(label("editor.scroll_step_fast"), 10).range(1, 100000)
                .key("editor.scroll_step_fast").build());

        ConfigGroup macros = tab("macros");
        add(macros, BooleanEntry.builder(label("macros.enabled"), true)
                .key("macros.enabled").build());

        ConfigGroup security = tab("security");
        add(security, BooleanEntry.builder(label("security.restrict_tools_to_operators"), false)
                .key("security.restrict_tools_to_operators").build());
        add(security, BooleanEntry.builder(label("security.editor_requires_permission"), true)
                .key("security.editor_requires_permission").build());
        add(security, BooleanEntry.builder(label("security.health_editor_requires_permission"), true)
                .key("security.health_editor_requires_permission").build());

        ConfigGroup tools = tab("tools");
        add(tools, BooleanEntry.builder(label("tools.enable_testing_toolkit"), true)
                .key("tools.enable_testing_toolkit").build());
        add(tools, IntegerEntry.builder(label("tools.max_test_weapon_damage"), Integer.MAX_VALUE).range(1, Integer.MAX_VALUE)
                .key("tools.max_test_weapon_damage").build());
        add(tools, IntegerEntry.builder(label("tools.max_enchantment_level"), Integer.MAX_VALUE).range(1, Integer.MAX_VALUE)
                .key("tools.max_enchantment_level").build());
        add(tools, BooleanEntry.builder(label("tools.allow_levels_above_max"), true)
                .key("tools.allow_levels_above_max").build());
        add(tools, BooleanEntry.builder(label("tools.allow_mismatched_enchantments"), false)
                .key("tools.allow_mismatched_enchantments").build());
        add(tools, BooleanEntry.builder(label("tools.allow_editing_all_items"), false)
                .key("tools.allow_editing_all_items").build());
        add(tools, BooleanEntry.builder(label("tools.allow_non_test_item_damage"), false)
                .key("tools.allow_non_test_item_damage").build());
        add(tools, BooleanEntry.builder(label("tools.health_editor_requires_item"), true)
                .key("tools.health_editor_requires_item").build());
        add(tools, IntegerEntry.builder(label("tools.config_version"), SERVER_LAYOUT)
                .key("tools.config_version").visible(false).build());

        ConfigGroup content = tab("content");
        add(content, BooleanEntry.builder(label("content.disable_generated_enchantments"), false)
                .key("content.disable_generated_enchantments").build());

        ConfigGroup notices = tab("notices");
        add(notices, BooleanEntry.builder(label("notices.enabled"), true)
                .key("notices.enabled").build());
        add(notices, StringEntry.builder(label("notices.per_mod"), "")
                .key("notices.per_mod").build());
    }
}
