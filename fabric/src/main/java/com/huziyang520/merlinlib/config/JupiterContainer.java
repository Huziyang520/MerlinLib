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
 * The Jupiter half of the optional configuration screen: a container registered with Jupiter that
 * mirrors {@link ClientOptions} into {@code client.toml}, which stays the store.
 */
public final class JupiterContainer extends FileConfigContainer {

    /** Where Jupiter writes its mirror of the switches. */
    private static final String FILE = "config/MerlinLib/settings.jcfg";

    /** Values the screen has collected, applied to {@code client.toml} when the player saves. */
    private final Map<String, String> pending = new LinkedHashMap<>();

    public JupiterContainer() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "settings"),
                Component.translatable(ClientOptions.titleKey()), FILE);
    }

    /**
     * Builds the tabs, one per section of {@code client.toml}, from the values the file holds right now.
     */
    @Override
    public void init() {
        Map<String, String> values = ClientOptions.values();
        for (String section : ClientOptions.sections()) {
            ConfigGroup group = createTab(section,
                    Component.translatable(ClientOptions.sectionKey(section)));
            for (ClientOptions.Option option : ClientOptions.in(section)) {
                group.addEntry(entry(option, ClientOptions.valueOf(option, values)));
            }
        }
        // The join notice page itself is a screen of MerlinLib's own, because the set of mods that
        // registered one is only known at runtime; this tab holds the way into it.
        ConfigGroup notices = createTab("notices", Component.translatable(ClientOptions.sectionKey("notices")));
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
        Component tooltip = Component.translatable(option.labelKey() + ".tooltip");
        String key = option.key();
        return switch (option.kind()) {
            case BOOLEAN -> BooleanEntry.builder(label, Boolean.parseBoolean(value))
                    .key(key)
                    .tooltip(tooltip)
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            case INTEGER -> IntegerEntry.builder(label, parseInt(value, option.defaultInt()))
                    .key(key)
                    .tooltip(tooltip)
                    .min(option.intMin())
                    .max(option.intMax())
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            case DECIMAL -> DoubleEntry.builder(label, parseDouble(value, option.defaultDouble()))
                    .key(key)
                    .tooltip(tooltip)
                    .min(option.min())
                    .max(option.max())
                    .callback((chosen, initial, reset) -> this.pending.put(key, chosen.toString()))
                    .build();
            // Jupiter has no colour picker, so a colour is edited as the text the file documents - the same
            // string client.toml accepts, palette name included.
            case COLOUR, TEXT -> StringEntry.builder(label, value)
                    .key(key)
                    .tooltip(tooltip)
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
