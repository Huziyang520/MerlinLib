package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.util.MerlinColor;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the Cloth Config screen over the client switches.
 *
 * <p>This class is the only one that mentions Cloth Config types, and it is only ever loaded after
 * {@link FabricConfig} has found the mod on the classpath, so a game without Cloth Config never has to
 * resolve a single Cloth class.
 *
 * <p>The keys, the ranges and the labels are not repeated here: they come from {@link ClientOptions}, the
 * same list the Jupiter screen walks, so a switch added there appears in both interfaces and a range can
 * only be declared once. Nothing is written until the player presses save: the entries collect their values
 * into a local map and that map is handed to {@link ConfigManager#saveClient} in one go, which is also what
 * makes the change visible immediately.
 */
public final class ClothScreen {

    private ClothScreen() {
    }

    /**
     * Builds the screen.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen to show
     */
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.merlinlib.title"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, String> pending = new LinkedHashMap<>();
        Map<String, String> current = ClientOptions.values();

        for (String section : ClientOptions.sections()) {
            ConfigCategory category = builder.getOrCreateCategory(
                    Component.translatable("config.merlinlib." + section));
            for (ClientOptions.Option option : ClientOptions.in(section)) {
                category.addEntry(entry(entries, option, ClientOptions.valueOf(option, current), pending));
            }
        }

        builder.setSavingRunnable(() -> {
            if (pending.isEmpty()) {
                return;
            }
            Map<String, String> values = new LinkedHashMap<>(pending);
            pending.clear();
            ConfigManager.saveClient(values);
        });
        return builder.build();
    }

    /**
     * Builds the control one option needs, bound to the staged map.
     *
     * @param entries the entry builder of the screen
     * @param option  the option being shown
     * @param value   its value in the file, as the file spells it
     * @param pending the map values are staged in until the player saves
     * @return the finished entry
     */
    private static AbstractConfigListEntry<?> entry(ConfigEntryBuilder entries, ClientOptions.Option option,
                                                    String value, Map<String, String> pending) {
        Component label = Component.translatable(option.labelKey());
        return switch (option.kind()) {
            case BOOLEAN -> entries.startBooleanToggle(label, Boolean.parseBoolean(value))
                    .setDefaultValue(option.defaultBoolean())
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case INTEGER -> entries.startIntField(label, parseInt(value, option.defaultInt()))
                    .setDefaultValue(option.defaultInt())
                    .setMin(option.intMin())
                    .setMax(option.intMax())
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case DECIMAL -> entries.startDoubleField(label, parseDouble(value, option.defaultDouble()))
                    .setDefaultValue(option.defaultDouble())
                    .setMin(option.min())
                    .setMax(option.max())
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case COLOUR -> entries.startColorField(label, MerlinColor.parse(value).orElse(MerlinColor.WHITE))
                    .setDefaultValue(MerlinColor.parse(option.defaultValue()).orElse(MerlinColor.WHITE))
                    .setAlphaMode(true)
                    .setSaveConsumer(chosen -> pending.put(option.key(), asColour(chosen)))
                    .build();
            case TEXT -> entries.startStrField(label, value)
                    .setDefaultValue(option.defaultValue())
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen))
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

    /**
     * Writes a colour the way {@code client.toml} documents it.
     *
     * @param value the ARGB value from the colour picker
     * @return {@code 0xRRGGBB} for opaque colours, {@code 0xAARRGGBB} otherwise
     */
    private static String asColour(int value) {
        return (value >>> 24) == 0xFF
                ? String.format("0x%06X", value & 0xFFFFFF)
                : String.format("0x%08X", value);
    }
}
