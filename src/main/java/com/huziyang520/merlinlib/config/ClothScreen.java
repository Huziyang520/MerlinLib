// Builds the Cloth Config screen over both switch files.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.util.MerlinColor;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClothScreen {
    private ClothScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable(ClientOptions.titleKey()));
        ConfigEntryBuilder entries = builder.entryBuilder();
        Map<String, String> pendingClient = new LinkedHashMap<>();
        Map<String, String> pendingServer = new LinkedHashMap<>();
        Map<String, String> currentClient = ClientOptions.values();
        Map<String, String> currentServer = ServerOptions.values();

        for (String section : ClientOptions.sections()) {
            ConfigCategory category = builder.getOrCreateCategory(
                    Component.translatable(ClientOptions.sectionKey(section)));
            for (ConfigOption option : ClientOptions.in(section)) {
                category.addEntry(entry(entries, option, ClientOptions.valueOf(option, currentClient),
                        pendingClient));
            }
        }

        ConfigCategory notices = null;
        for (String section : ServerOptions.sections()) {
            ConfigCategory category = builder.getOrCreateCategory(
                    Component.translatable(ClientOptions.sectionKey(section)));
            for (ConfigOption option : ServerOptions.in(section)) {
                category.addEntry(entry(entries, option, ServerOptions.valueOf(option, currentServer),
                        pendingServer));
            }
            if ("notices".equals(section)) {
                notices = category;
            }
        }

        if (notices != null) {
            notices.addEntry(new ClothNoticeButtonEntry(
                    Component.translatable(ClientOptions.noticesKey() + ".tooltip"),
                    Button.builder(Component.translatable(ClientOptions.noticesKey()),
                                    button -> MerlinScreens.openNoticesScreen(parent))
                            .bounds(0, 0, 150, VanillaUi.WIDGET_HEIGHT)
                            .build()));
        }

        builder.setSavingRunnable(() -> {
            if (!pendingClient.isEmpty()) {
                ConfigManager.saveClient(new LinkedHashMap<>(pendingClient));
                pendingClient.clear();
            }
            if (!pendingServer.isEmpty()) {
                ConfigManager.saveServer(new LinkedHashMap<>(pendingServer));
                pendingServer.clear();
            }
        });
        return builder.build();
    }

    private static AbstractConfigListEntry<?> entry(ConfigEntryBuilder entries, ConfigOption option,
                                                    String value, Map<String, String> pending) {
        Component label = Component.translatable(option.labelKey());
        Component[] tooltip = {Component.translatable(option.labelKey() + ".tooltip")};
        return switch (option.kind()) {
            case BOOLEAN -> entries.startBooleanToggle(label, Boolean.parseBoolean(value))
                    .setDefaultValue(option.defaultBoolean())
                    .setTooltip(tooltip)
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case INTEGER -> entries.startIntField(label, parseInt(value, option.defaultInt()))
                    .setDefaultValue(option.defaultInt())
                    .setMin(option.intMin())
                    .setMax(option.intMax())
                    .setTooltip(tooltip)
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case DECIMAL -> entries.startDoubleField(label, parseDouble(value, option.defaultDouble()))
                    .setDefaultValue(option.defaultDouble())
                    .setMin(option.min())
                    .setMax(option.max())
                    .setTooltip(tooltip)
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen.toString()))
                    .build();
            case COLOUR -> entries.startColorField(label, MerlinColor.parse(value).orElse(MerlinColor.WHITE))
                    .setDefaultValue(MerlinColor.parse(option.defaultValue()).orElse(MerlinColor.WHITE))
                    .setAlphaMode(true)
                    .setTooltip(tooltip)
                    .setSaveConsumer(chosen -> pending.put(option.key(), asColour(chosen)))
                    .build();
            case TEXT -> entries.startStrField(label, value)
                    .setDefaultValue(option.defaultValue())
                    .setTooltip(tooltip)
                    .setSaveConsumer(chosen -> pending.put(option.key(), chosen))
                    .build();
        };
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String asColour(int value) {
        return (value >>> 24) == 0xFF
                ? String.format("0x%06X", value & 0xFFFFFF)
                : String.format("0x%08X", value);
    }
}
