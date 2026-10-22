package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.iafenvoy.jupiter.config.ConfigGroup;
import com.iafenvoy.jupiter.config.container.FileConfigContainer;
import com.iafenvoy.jupiter.config.entry.BooleanEntry;
import com.iafenvoy.jupiter.config.entry.DoubleEntry;
import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.entry.StringEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Jupiter half of the optional configuration screen.
 *
 * <p>Jupiter's own list already holds a container per mod, so appearing in it is a matter of registering
 * one with {@code ConfigManager#registerConfigHandler}: the list screen reads
 * {@code ConfigManager.getInstance().getConfigs()}, and every container it finds becomes a row. This class
 * is that container.
 *
 * <h2>What the file is, and what it is not</h2>
 *
 * <p>The container is a {@code FileConfigContainer} writing {@code config/MerlinLib/settings.jcfg}, which is
 * what makes it a first class Jupiter configuration - Jupiter serialises it and shows it under the
 * "Jupiter" source. It is a <em>mirror</em>, not the store: {@code client.toml} stays authoritative, so
 * {@link #load()} rebuilds the tabs from {@link ClientOptions#values()} instead of reading the {@code jcfg}
 * back, and {@link #save()} writes the {@code jcfg} and then folds the same values into {@code client.toml}
 * through {@link ConfigManager#saveClient}. A player who hand edits {@code client.toml} therefore never
 * sees stale values here, and one who uses this screen never ends up with two files that disagree.
 *
 * <p>The keys, labels, defaults and ranges all come from {@link ClientOptions}, the same list the Cloth
 * Config screen walks: there is one key list, and adding an option to it adds a control to both.
 */
public final class JupiterContainer extends FileConfigContainer {

    /** Where Jupiter writes its mirror of the switches. */
    private static final String FILE = "config/MerlinLib/settings.jcfg";

    /** Values the screen has collected, applied to {@code client.toml} when the player saves. */
    private final Map<String, String> pending = new LinkedHashMap<>();

    public JupiterContainer() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "settings"),
                Component.translatable("config.merlinlib.title"), FILE);
    }

    /**
     * Builds the tabs, one per section of {@code client.toml}, from the values the file holds right now.
     */
    @Override
    public void init() {
        Map<String, String> values = ClientOptions.values();
        for (String section : ClientOptions.sections()) {
            ConfigGroup group = createTab(section, Component.translatable("config.merlinlib." + section));
            for (ClientOptions.Option option : ClientOptions.in(section)) {
                group.addEntry(entry(option, ClientOptions.valueOf(option, values)));
            }
        }
        // One tab holding the way into the join notice page: the per mod switches are not values Jupiter can
        // list, because the set of mods is only known at runtime.
        ConfigGroup notices = createTab("notices", Component.translatable("config.merlinlib.notices"));
        notices.addEntry(JupiterNoticesButton.create());
    }

    /**
     * Rebuilds the tabs from the authoritative file.
     *
     * <p>Overridden because the {@code jcfg} is only a mirror: reading it back would let a stale mirror win
     * over a hand edited {@code client.toml}. Rebuilding rather than reassigning every entry also means the
     * mirror picks up the direction of a value that changed on disk.
     */
    @Override
    public void load() {
        this.configTabs.clear();
        init();
    }

    /**
     * Writes the Jupiter mirror, then applies the same values to {@code client.toml}.
     *
     * <p>Called by Jupiter when the player saves or leaves the screen. {@code super.save()} is what makes the
     * file exist at all; the createDirectories call before it covers the very first launch, where
     * {@code config/MerlinLib} may not have been made yet by anything else.
     */
    @Override
    public void save() {
        try {
            Files.createDirectories(Path.of(FILE).getParent());
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not create the directory of {}", FILE, exception);
        }
        super.save();
        if (this.pending.isEmpty()) {
            return;
        }
        Map<String, String> values = new LinkedHashMap<>(this.pending);
        this.pending.clear();
        ConfigManager.saveClient(values);
    }

    /**
     * Builds the control for one option.
     *
     * <p>The label is a translation key turned into a component by Jupiter itself, and the key it stores is
     * set explicitly so the {@code jcfg} uses the same {@code section.key} spelling as {@code client.toml}.
     * No tooltip is set: the label is all there is to say about a switch, and a tooltip repeating it would
     * only add a second line of the same text.
     *
     * @param option the option
     * @param value  its current value, as the file spells it
     * @return the entry
     */
    private ConfigEntry<?> entry(ClientOptions.Option option, String value) {
        Component label = Component.translatable(option.labelKey());
        String key = option.key();
        return switch (option.kind()) {
            case BOOLEAN -> BooleanEntry.builder(label, Boolean.parseBoolean(value))
                    .key(key)
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            case INTEGER -> IntegerEntry.builder(label, parseInt(value, option.defaultInt()))
                    .key(key)
                    .min(option.intMin())
                    .max(option.intMax())
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            case DECIMAL -> DoubleEntry.builder(label, parseDouble(value, option.defaultDouble()))
                    .key(key)
                    .min(option.min())
                    .max(option.max())
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            // Jupiter has no colour picker, so a colour is edited as the text the file documents - the same
            // string client.toml accepts, palette name included.
            case COLOUR, TEXT -> StringEntry.builder(label, value)
                    .key(key)
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen))
                    .build();
        };
    }

    /**
     * @param text     the text from the file
     * @param fallback the value to use when it is not a number
     * @return the number
     */
    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /**
     * @param text     the text from the file
     * @param fallback the value to use when it is not a number
     * @return the number
     */
    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
