package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.util.MerlinColor;
import com.iafenvoy.jupiter.ConfigManager;
import com.iafenvoy.jupiter.config.ConfigGroup;
import com.iafenvoy.jupiter.config.container.AbstractConfigContainer;
import com.iafenvoy.jupiter.config.container.FileConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.entry.StringEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.render.screen.ConfigContainerScreen;
import net.minecraft.client.gui.screens.Screen;
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
        // Kept statically as well, so the mod list's Config button can open this very container's screen
        // without rebuilding it (which would re-register it with Jupiter).
        OUR_CONTAINER = settings.container;
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

    private void add(ConfigGroup tab, ConfigEntry<?> entry, OptionSpec.Kind kind) {
        tab.addEntry(entry);
        this.entries.put(entry.getKey(), entry);
        this.kinds.put(entry.getKey(), kindOf(kind));
    }

    private static Kind kindOf(OptionSpec.Kind kind) {
        return switch (kind) {
            case BOOLEAN -> Kind.BOOLEAN;
            case INTEGER -> Kind.INTEGER;
            case DOUBLE -> Kind.DOUBLE;
            case TEXT -> Kind.TEXT;
        };
    }

    /**
     * Declares every option, in the order {@link OptionSpec} lists them.
     *
     * <p>The list itself lives in {@link OptionSpec}, shared with the Cloth screen and with the read/write
     * path. This method only turns a spec into the Jupiter entry type its kind calls for; it holds no key,
     * default or range of its own, which is what keeps the two screens covering exactly the same switches.
     */
    private void buildTabs() {
        Map<String, ConfigGroup> tabs = new LinkedHashMap<>();
        for (OptionSpec.Section section : OptionSpec.sections()) {
            tabs.put(section.id(), tab(section.id()));
        }
        for (OptionSpec spec : OptionSpec.all()) {
            ConfigGroup tab = tabs.get(spec.section());
            if (tab == null) {
                continue;
            }
            add(tab, entryFor(spec), spec.kind());
        }
    }

    /**
     * @param spec the switch
     * @return the Jupiter entry holding it
     */
    private static ConfigEntry<?> entryFor(OptionSpec spec) {
        Component label = Component.translatable(spec.langKey());
        switch (spec.kind()) {
            case BOOLEAN:
                return BooleanEntry.builder(label, spec.booleanDefault())
                        .key(spec.key()).visible(!spec.hidden()).build();
            case INTEGER: {
                IntegerEntry.Builder builder = IntegerEntry.builder(label, spec.intDefault())
                        .range((int) spec.min(), (int) spec.max())
                        .key(spec.key());
                if (spec.hidden()) {
                    builder.visible(false);
                }
                return builder.build();
            }
            case DOUBLE:
                return DoubleEntry.builder(label, spec.doubleDefault())
                        .range(spec.min(), spec.max())
                        .key(spec.key()).build();
            default:
                return StringEntry.builder(label, spec.textDefault())
                        .key(spec.key()).build();
        }
    }

    /**
     * Opens Jupiter's own screen for this container.
     *
     * <p>Used when Cloth Config is absent: the mod list's Config button then shows the same switches in
     * Jupiter's {@code ConfigContainerScreen}. Only reachable through the {@code Class.forName} guard in
     * {@link JupiterBackend}, which is why it may name Jupiter types at all.
     *
     * @param parent the mod list screen, restored when the player closes this one
     * @return the screen
     */
    static Screen screen(Screen parent) {
        // A container only exists once Jupiter accepted the registration. Without one there is no screen, and
        // handing Jupiter's screen a null container would fail later, inside its own render pass.
        if (OUR_CONTAINER == null) {
            return null;
        }
        return new ConfigContainerScreen(parent, OUR_CONTAINER, false);
    }

    /** The single container, kept so a screen can be opened without rebuilding it. */
    private static AbstractConfigContainer OUR_CONTAINER;

}
