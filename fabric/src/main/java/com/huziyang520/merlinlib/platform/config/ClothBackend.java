package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigOptions;
import com.huziyang520.merlinlib.config.ConfigReader;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric configuration screen: Cloth Config over the TOML files.
 *
 * <h2>What is stored where</h2>
 *
 * <p>The values stay in {@code config/MerlinLib/client.toml} and {@code server.toml}. Cloth Config is a screen
 * library, not a store, so the file remains the thing to edit by hand, to back up, and to read with any other
 * tool - and a player without Cloth loses nothing but the screen. The store is {@link TomlStore}, shared with
 * the Jupiter screen, so both screens read and write the same file the same way.
 *
 * <h2>Why this class is never mentioned directly</h2>
 *
 * <p>Cloth Config is an optional dependency: it is not bundled, not required, and the mod has to start without
 * it. Every reference to it therefore lives in this one class, which is only ever loaded through
 * {@link ClothSupport} after that check has passed. A class that mentions an optional mod's types in its own
 * bytecode cannot be loaded without it, however carefully the call site is guarded.
 */
public final class ClothBackend implements ConfigBackend {

    /** The file store both Fabric screens write through. */
    private final TomlStore store = new TomlStore("Cloth Config over the TOML files");

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
     * Opens the configuration screen.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen, ready to be shown
     */
    public Screen openScreen(Screen parent) {
        ConfigReader reader = view();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(TITLE_KEY));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, ConfigCategory> categories = new LinkedHashMap<>();
        for (ConfigOptions.Option option : ConfigOptions.all()) {
            String section = option.section();
            ConfigCategory category = categories.computeIfAbsent(section,
                    name -> builder.getOrCreateCategory(Component.translatable(ConfigOptions.sectionKey(name))));
            category.addEntry(entry(entries, option, reader));
        }
        builder.setSavingRunnable(this::commit);
        // The join notice switches are one row per installed mod, which a fixed list of entries cannot express and
        // a text field must not (that was the retired notices.per_mod string). They have a screen of their own, so
        // the notices category says where it is: a list entry cannot open another screen in Cloth Config, and a row
        // that silently did nothing would be worse than one that names the command.
        ConfigCategory notices = categories.get("notices");
        if (notices != null) {
            notices.addEntry(entries.startTextDescription(
                    Component.translatable("gui.merlinlib.notices.command_hint")).build());
        }
        return builder.build();
    }

    /** Writes what the screen collected and re-reads both files, so a saved switch is live at once. */
    private void commit() {
        save();
        ConfigManager.reload();
        Constants.LOG.info("[MerlinLib] configuration written to the TOML files from the Cloth Config screen");
    }

    /**
     * Builds one entry.
     *
     * <p>Each option carries its own default as well: Cloth's reset button restores that value, and the default
     * is also what a key deleted from the file falls back to.
     */
    private AbstractConfigListEntry entry(ConfigEntryBuilder entries, ConfigOptions.Option option,
                                          ConfigReader reader) {
        Component label = Component.translatable(option.labelKey());
        // The description is a translation key as well: the configuration file documents the same switch in
        // comments, but a comment cannot follow the language the game is running in, and this screen can.
        Component tooltip = Component.translatable(option.tooltipKey());
        String key = option.key();
        switch (option.kind()) {
            case BOOL: {
                boolean current = reader.getBoolean(key, option.defaultFlag());
                return entries.startBooleanToggle(label, current)
                        .setTooltip(tooltip)
                        .setDefaultValue(option.defaultFlag())
                        .setSaveConsumer(value -> this.store.write(key, Boolean.toString(value)))
                        .build();
            }
            case INT: {
                int current = reader.getInt(key, option.defaultInt(), (int) option.min(), (int) option.max());
                return entries.startIntField(label, current)
                        .setTooltip(tooltip)
                        .setMin((int) option.min())
                        .setMax((int) option.max())
                        .setDefaultValue(option.defaultInt())
                        .setSaveConsumer(value -> this.store.write(key, Integer.toString(value)))
                        .build();
            }
            case DOUBLE: {
                double current = reader.getDouble(key, option.defaultValue(), option.min(), option.max());
                return entries.startDoubleField(label, current)
                        .setTooltip(tooltip)
                        .setMin(option.min())
                        .setMax(option.max())
                        .setDefaultValue(option.defaultValue())
                        .setSaveConsumer(value -> this.store.write(key, Double.toString(value)))
                        .build();
            }
            case COLOR: {
                int current = reader.getColor(key, (int) option.defaultValue());
                return entries.startAlphaColorField(label, current)
                        .setTooltip(tooltip)
                        .setDefaultValue((int) option.defaultValue())
                        .setSaveConsumer(value -> this.store.write(key, ConfigOptions.colourText(value)))
                        .build();
            }
            case TEXT:
            default: {
                String current = reader.getString(key, option.text());
                return entries.startStrField(label, current)
                        .setTooltip(tooltip)
                        .setDefaultValue(option.text())
                        .setSaveConsumer(value -> this.store.write(key, value))
                        .build();
            }
        }
    }

    /** Translation key of the screen title. */
    private static final String TITLE_KEY = "merlinlib.configuration.title";
}
