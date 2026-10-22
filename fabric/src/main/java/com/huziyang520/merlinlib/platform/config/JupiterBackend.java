package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigDirectory;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigOptions;
import com.huziyang520.merlinlib.config.ConfigReader;
import com.iafenvoy.jupiter.config.ConfigGroup;
import com.iafenvoy.jupiter.config.container.FileConfigContainer;
import com.iafenvoy.jupiter.config.entry.BaseEntry;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.entry.StringEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.render.screen.JupiterScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Fabric configuration screen number two: Jupiter over the TOML files.
 *
 * <h2>Why there are two screens</h2>
 *
 * <p>A player who installs either Cloth Config or Jupiter gets a settings screen, and this mod has to work with
 * neither installed. So the values and the file behind them are shared ({@link TomlStore},
 * {@link ConfigOptions}) and only the screen library differs: {@link ClothBackend} for Cloth, this class for
 * Jupiter.
 *
 * <h2>How Jupiter is given the settings</h2>
 *
 * <p>Jupiter does not draw a screen somebody else built: its configuration list is fed by
 * {@code AbstractConfigContainer} instances registered with {@code com.iafenvoy.jupiter.ConfigManager}, and it
 * draws whatever those declare. {@link JupiterContainer} is that declaration - one tab per section, one entry
 * per option, built from {@link ConfigOptions} so the two Fabric screens cannot disagree about which switches
 * exist.
 *
 * <p>Jupiter also keeps a file of its own next to the TOML pair ({@code config/MerlinLib/settings.jcfg})
 * because a container is expected to be able to write one. That file is a mirror: the TOML files stay the
 * store, which is why {@link JupiterContainer#load()} refreshes from the TOML values instead of reading the
 * mirror. A value edited by hand in either file therefore takes effect.
 *
 * <h2>Why every Jupiter type stays inside this file</h2>
 *
 * <p>Jupiter is optional - not bundled, not required, not a dependency. A class that mentions its types cannot
 * be loaded without it, so this class and its nested container are only ever reached through
 * {@link JupiterSupport}, after the "is it installed" check. Nothing in common or in the rest of the mod may
 * refer to them.
 */
public final class JupiterBackend implements ConfigBackend {

    /** Namespace and path of the container, which is also the id Jupiter lists it under. */
    private static final Identifier CONTAINER_ID = Identifier.fromNamespaceAndPath("merlinlib", "settings");

    /** The TOML store, shared with the Cloth screen. */
    private final TomlStore store = new TomlStore("Jupiter over the TOML files");

    private JupiterContainer container;

    @Override
    public ConfigReader view() {
        return this.store.view();
    }

    @Override
    public void write(String key, String value) {
        this.store.write(key, value);
    }

    @Override
    public void save() {
        this.store.save();
    }

    @Override
    public String name() {
        return this.store.name();
    }

    /**
     * Declares MerlinLib to Jupiter, so its configuration list shows this mod.
     *
     * <p>Registering calls {@code init()} and then {@code load()} on the container, which fills the tabs with the
     * values currently in effect; the container is kept so {@link #openScreen(Screen)} can hand it back.
     *
     * @return {@code true} when the container was accepted
     */
    public boolean register() {
        if (this.container != null) {
            return true;
        }
        try {
            JupiterContainer declared = new JupiterContainer(this.store);
            com.iafenvoy.jupiter.ConfigManager.getInstance().registerConfigHandler(declared);
            this.container = declared;
            return true;
        } catch (RuntimeException | LinkageError failure) {
            Constants.LOG.warn("[MerlinLib] Jupiter is installed but the configuration could not be declared to "
                    + "it; the TOML files stay editable by hand", failure);
            return false;
        }
    }

    /**
     * Builds Jupiter's screen for these settings.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen, or {@code null} when the container could not be declared
     */
    public Screen openScreen(Screen parent) {
        if (!register()) {
            return null;
        }
        // The third argument is Jupiter's "client config" flag: true means the values belong to this
        // installation and are saved locally, which is exactly what a TOML file next to the game is.
        return JupiterScreen.getConfigScreen(parent, this.container, true);
    }

    /** @return the file Jupiter's mirror is written to, with its directory created if it was missing */
    private static Path settingsFile() {
        Path root = ConfigDirectory.root();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not create {}; Jupiter's mirror file will be skipped",
                    root, exception);
        }
        return root.resolve("settings.jcfg");
    }

    /**
     * The settings as Jupiter sees them: one tab per section, one entry per option.
     *
     * <p>The entries are built from {@link ConfigOptions#all()} and read through the same {@link ConfigReader}
     * the rest of the mod uses, so a value shown here is the value in effect.
     */
    static final class JupiterContainer extends FileConfigContainer {

        /** The store the entries write into and read from. */
        private final TomlStore store;

        JupiterContainer(TomlStore store) {
            super(CONTAINER_ID, Component.translatable("merlinlib.configuration.title"),
                    settingsFile().toString());
            this.store = store;
        }

        @Override
        public void init() {
            // Declaring the tabs from scratch on every call: Jupiter calls init() when the container is
            // registered and could call it again after a resource reload, and appending a second set of tabs
            // would show every option twice.
            getConfigTabs().clear();
            ConfigReader reader = this.store.view();
            for (Map.Entry<String, List<ConfigOptions.Option>> section : ConfigOptions.bySection().entrySet()) {
                ConfigGroup tab = createTab(section.getKey(), ConfigOptions.sectionKey(section.getKey()));
                for (ConfigOptions.Option option : section.getValue()) {
                    tab.addEntry(entry(option, reader));
                }
            }
        }

        /**
         * Refreshes the shown values from the TOML files.
         *
         * <p>Deliberately not Jupiter's own file reader: the TOML pair is the store and the mirror is written
         * from it. Reading the mirror here would let the two disagree after a hand edit, and the screen would then
         * show a value the game is not using.
         */
        @Override
        public void load() {
            ConfigReader reader = this.store.view();
            for (ConfigGroup group : getConfigTabs()) {
                for (ConfigEntry<?> declared : group.getConfigs()) {
                    if (!(declared instanceof BaseEntry<?> entry)) {
                        continue;
                    }
                    ConfigOptions.Option option = ConfigOptions.find(group.getId() + "." + entry.getKey());
                    if (option != null) {
                        set(entry, option, reader);
                    }
                }
            }
        }

        /**
         * Writes what the screen holds into the TOML files, then into the mirror.
         *
         * <p>Jupiter calls this when its screen closes, so the write is followed by a reload of both
         * configurations: a saved switch is in effect by the time the player is back in the game.
         */
        @Override
        public void save() {
            for (ConfigGroup group : getConfigTabs()) {
                for (ConfigEntry<?> declared : group.getConfigs()) {
                    if (!(declared instanceof BaseEntry<?> entry)) {
                        continue;
                    }
                    ConfigOptions.Option option = ConfigOptions.find(group.getId() + "." + entry.getKey());
                    if (option != null) {
                        this.store.write(option.key(), text(option, entry));
                    }
                }
            }
            this.store.save();
            ConfigManager.reload();
            super.save();
            Constants.LOG.info("[MerlinLib] configuration written to the TOML files from the Jupiter screen");
        }

        /** Builds one entry, at the value currently in effect. */
        private static ConfigEntry<?> entry(ConfigOptions.Option option, ConfigReader reader) {
            Component label = Component.translatable(option.labelKey());
            String key = option.key();
            switch (option.kind()) {
                case BOOL:
                    return BooleanEntry.builder(label, reader.getBoolean(key, option.defaultFlag()))
                            .key(option.name())
                            .build();
                case INT:
                    return IntegerEntry.builder(label,
                                    reader.getInt(key, option.defaultInt(), (int) option.min(), (int) option.max()))
                            .range((int) option.min(), (int) option.max())
                            .key(option.name())
                            .build();
                case DOUBLE:
                    return DoubleEntry.builder(label,
                                    reader.getDouble(key, option.defaultValue(), option.min(), option.max()))
                            .range(option.min(), option.max())
                            .key(option.name())
                            .build();
                case COLOR:
                    return StringEntry.builder(label,
                                    ConfigOptions.colourText(reader.getColor(key, (int) option.defaultValue())))
                            .key(option.name())
                            .build();
                case TEXT:
                default:
                    return StringEntry.builder(label, reader.getString(key, option.text()))
                            .key(option.name())
                            .build();
            }
        }

        /** Puts the value currently in effect into one entry. */
        @SuppressWarnings("unchecked")
        private static void set(BaseEntry<?> entry, ConfigOptions.Option option, ConfigReader reader) {
            String key = option.key();
            Object value = switch (option.kind()) {
                case BOOL -> reader.getBoolean(key, option.defaultFlag());
                case INT -> reader.getInt(key, option.defaultInt(), (int) option.min(), (int) option.max());
                case DOUBLE -> reader.getDouble(key, option.defaultValue(), option.min(), option.max());
                case COLOR -> ConfigOptions.colourText(reader.getColor(key, (int) option.defaultValue()));
                case TEXT -> reader.getString(key, option.text());
            };
            ((BaseEntry<Object>) entry).setValue(value);
        }

        /** Turns the value of one entry into the text the TOML files hold. */
        private static String text(ConfigOptions.Option option, BaseEntry<?> entry) {
            Object value = entry.getValue();
            if (value == null) {
                return option.defaultText();
            }
            return switch (option.kind()) {
                case BOOL -> Boolean.toString((Boolean) value);
                case INT -> Integer.toString(((Number) value).intValue());
                case DOUBLE -> Double.toString(((Number) value).doubleValue());
                case COLOR, TEXT -> String.valueOf(value);
            };
        }
    }
}
