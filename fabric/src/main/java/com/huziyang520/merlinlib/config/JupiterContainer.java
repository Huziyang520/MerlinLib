// The Jupiter half of the optional configuration screen: a container registered with Jupiter that mirrors the two switch ...

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

public final class JupiterContainer extends FileConfigContainer {
    private static final String FILE = "config/MerlinLib/settings.jcfg";
    private final Map<String, String> pendingClient = new LinkedHashMap<>();
    private final Map<String, String> pendingServer = new LinkedHashMap<>();

    public JupiterContainer() {
        super(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "settings"),
                Component.translatable(ClientOptions.titleKey()), FILE);
    }

    @Override
    public void init() {
        Map<String, String> client = ClientOptions.values();
        for (String section : ClientOptions.sections()) {
            ConfigGroup group = createTab(section,
                    Component.translatable(ClientOptions.sectionKey(section)));
            for (ConfigOption option : ClientOptions.in(section)) {
                group.addEntry(entry(option, ClientOptions.valueOf(option, client), this.pendingClient));
            }
        }

        Map<String, String> server = ServerOptions.values();
        ConfigGroup notices = null;
        for (String section : ServerOptions.sections()) {
            ConfigGroup group = createTab(section,
                    Component.translatable(ClientOptions.sectionKey(section)));
            for (ConfigOption option : ServerOptions.in(section)) {
                group.addEntry(entry(option, ServerOptions.valueOf(option, server), this.pendingServer));
            }
            if ("notices".equals(section)) {
                notices = group;
            }
        }

        if (notices != null) {
            notices.addEntry(JupiterNoticesButton.create());
        }
    }

    @Override
    public void load() {
        this.configTabs.clear();
        init();
    }

    @Override
    public void save() {
        try {
            Files.createDirectories(Path.of(FILE).getParent());
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not create the directory of {}", FILE, exception);
        }
        super.save();
        if (!this.pendingClient.isEmpty()) {
            Map<String, String> values = new LinkedHashMap<>(this.pendingClient);
            this.pendingClient.clear();
            ConfigManager.saveClient(values);
        }
        if (!this.pendingServer.isEmpty()) {
            Map<String, String> values = new LinkedHashMap<>(this.pendingServer);
            this.pendingServer.clear();
            ConfigManager.saveServer(values);
        }
    }

    private ConfigEntry<?> entry(ConfigOption option, String value, Map<String, String> pending) {
        Component label = Component.translatable(option.labelKey());
        Component tooltip = Component.translatable(option.labelKey() + ".tooltip");
        String key = option.key();
        return switch (option.kind()) {
            case BOOLEAN -> BooleanEntry.builder(label, Boolean.parseBoolean(value))
                    .key(key)
                    .tooltip(tooltip)
                    .callback((chosen, initial, reset) -> pending.put(key, chosen.toString()))
                    .build();

            case INTEGER -> JupiterNumberEntry.create(label, tooltip, key,
                    parseInt(value, option.defaultInt()), option.intMin(), option.intMax(),
                    chosen -> pending.put(key, chosen.toString()));
            case DECIMAL -> DoubleEntry.builder(label, parseDouble(value, option.defaultDouble()))
                    .key(key)
                    .tooltip(tooltip)
                    .min(option.min())
                    .max(option.max())
                    .callback((chosen, initial, reset) -> pending.put(key, chosen.toString()))
                    .build();

            case COLOUR, TEXT -> StringEntry.builder(label, value)
                    .key(key)
                    .tooltip(tooltip)
                    .callback((chosen, initial, reset) -> pending.put(key, chosen))
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
}
