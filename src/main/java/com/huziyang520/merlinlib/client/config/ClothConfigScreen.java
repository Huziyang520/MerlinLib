package com.huziyang520.merlinlib.client.config;

import com.huziyang520.merlinlib.config.ConfigOptions;
import com.huziyang520.merlinlib.config.OptionSpec;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The configuration screen drawn by Cloth Config.
 *
 * <h2>Loaded only when Cloth Config is there</h2>
 *
 * <p>The package name says {@code config} and the class name says Cloth: every import above is Cloth's or
 * Minecraft's client's, so this class must not be class-loaded on an installation without Cloth Config. It is
 * reached only from {@code MerlinConfigFrontend#open}, after that class has proved
 * {@code me.shedaniel.clothconfig2.api.ConfigBuilder} loads.
 *
 * <h2>One category per section, one entry per switch</h2>
 *
 * <p>Both come from {@link OptionSpec}: the categories are its sections and the entries are its switches, in
 * its order. Nothing in this file knows a key, a default or a range, which is what makes it impossible for
 * this screen and Jupiter's to drift apart.
 *
 * <h2>When the values are written</h2>
 *
 * <p>Cloth calls the save consumers as the player changes a widget, and the saving runnable once when the
 * screen is closed with Done. The consumers therefore only collect the new values; the runnable hands them to
 * {@link ConfigOptions#write}, which splits them by side and reloads - the same path a command takes, so a
 * change is live immediately and the toml files (or Jupiter's file) are updated only once.
 */
public final class ClothConfigScreen {

    /** The section whose category carries the pointer to the join notice page. */
    private static final String NOTICES = "notices";

    private ClothConfigScreen() {
    }

    /**
     * Builds the screen.
     *
     * @param parent the mod list screen, restored when the player closes this one
     * @return the screen, ready to be shown
     */
    public static Screen build(Screen parent) {
        Map<String, String> pending = new LinkedHashMap<>();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("merlinlib.config.title"))
                .setSavingRunnable(() -> ConfigOptions.write(pending));

        ConfigEntryBuilder entries = builder.entryBuilder();
        for (Map.Entry<String, List<OptionSpec>> section : OptionSpec.bySection().entrySet()) {
            ConfigCategory category = builder
                    .getOrCreateCategory(Component.translatable("merlinlib.config.tab." + section.getKey()));
            for (OptionSpec spec : section.getValue()) {
                if (spec.hidden()) {
                    // The layout version markers: read by the migrations, not something to edit by hand.
                    continue;
                }
                add(category, entries, spec, pending);
            }
            if (NOTICES.equals(section.getKey())) {
                // The per mod switches are not options: the list of mods only exists at runtime, and Cloth
                // 11.1.136 has no action row or sub-screen builder to hand them to (no startSubScreenBuilder,
                // no button entry). The line names the page that does draw them, which is the same page
                // Jupiter's category points at, so all three front ends agree.
                category.addEntry(entries.startTextDescription(
                        Component.translatable("merlinlib.config.notices.open_hint")).build());
            }
        }
        return builder.build();
    }

    /**
     * Adds one switch as the widget its kind calls for.
     *
     * @param category the section it belongs to
     * @param entries  Cloth's entry factory
     * @param spec     the switch
     * @param pending  where the new value is collected until the screen is saved
     */
    private static void add(ConfigCategory category, ConfigEntryBuilder entries, OptionSpec spec,
                            Map<String, String> pending) {
        Component label = Component.translatable(spec.langKey());
        String current = ConfigOptions.read(spec);
        switch (spec.kind()) {
            case BOOLEAN:
                category.addEntry(entries.startBooleanToggle(label, Boolean.parseBoolean(current))
                        .setDefaultValue(spec.booleanDefault())
                        .setSaveConsumer(value -> pending.put(spec.key(), Boolean.toString(value)))
                        .build());
                break;
            case INTEGER:
                category.addEntry(entries.startIntField(label, (int) number(current, spec.intDefault()))
                        .setDefaultValue(spec.intDefault())
                        .setMin((int) spec.min())
                        .setMax((int) spec.max())
                        .setSaveConsumer(value -> pending.put(spec.key(), Integer.toString(value)))
                        .build());
                break;
            case DOUBLE:
                category.addEntry(entries.startDoubleField(label, number(current, spec.doubleDefault()))
                        .setDefaultValue(spec.doubleDefault())
                        .setMin(spec.min())
                        .setMax(spec.max())
                        .setSaveConsumer(value -> pending.put(spec.key(), Double.toString(value)))
                        .build());
                break;
            default:
                category.addEntry(entries.startStrField(label, current)
                        .setDefaultValue(spec.textDefault())
                        .setSaveConsumer(value -> pending.put(spec.key(), value))
                        .build());
                break;
        }
    }

    /**
     * Parses a stored number, falling back rather than throwing.
     *
     * <p>{@link ConfigOptions#read} already returns a value that parsed once, so this is belt and braces: a
     * broken value must still open the screen, and the fallback keeps the widget usable.
     *
     * @param text     the stored value
     * @param fallback the default to use when it is not a number
     * @return the number
     */
    private static double number(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (RuntimeException malformed) {
            return fallback;
        }
    }
}
