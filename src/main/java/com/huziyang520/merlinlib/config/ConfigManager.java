// Owns the two switch stores: both are read on startup and re-read on every content reload.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import net.neoforged.fml.config.ModConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private static volatile ConfigBackend clientBackend;
    private static volatile ConfigBackend serverBackend;
    private static volatile ClientConfig client = new ClientConfig();
    private static volatile ServerConfig server = new ServerConfig();
    private static volatile List<ContentError> errors = List.of();

    private ConfigManager() {
    }

    public static void setBackend(ConfigScope scope, ConfigBackend backend) {
        if (scope == ConfigScope.CLIENT) {
            clientBackend = backend;
        } else {
            serverBackend = backend;
        }
    }

    public static ConfigReader read(ConfigScope scope) {
        ConfigBackend backend = scope == ConfigScope.CLIENT ? clientBackend : serverBackend;
        return backend == null ? Defaults.INSTANCE : backend.view();
    }

    public static synchronized void reload() {
        client = ClientConfig.load();
        server = ServerConfig.load();
        List<ContentError> collected = new ArrayList<>(client.errors());
        collected.addAll(server.errors());
        errors = List.copyOf(collected);

        for (ContentError error : collected) {
            Constants.LOG.warn("[MerlinLib] configuration problem: {}", error.format());
        }
    }

    public static synchronized void onConfigLoaded(ModConfig config) {
        for (NeoSpecBackend spec : new NeoSpecBackend[]{NeoConfigs.clientSpec(), NeoConfigs.commonSpec()}) {
            if (spec.fileName().equals(config.getFileName())) {
                spec.importLegacy();
            }
        }
        reload();
    }

    public static void saveClient(Map<String, String> overrides) {
        write(ConfigScope.CLIENT, overrides);
    }

    public static void saveServer(Map<String, String> overrides) {
        write(ConfigScope.SERVER, overrides);
    }

    public static synchronized void write(ConfigScope scope, Map<String, String> overrides) {
        ConfigBackend backend = scope == ConfigScope.CLIENT ? clientBackend : serverBackend;
        if (backend == null) {
            Constants.LOG.warn("[MerlinLib] there is no {} configuration to write to", scope);
            return;
        }
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            backend.write(entry.getKey(), entry.getValue());
        }
        backend.save();
        reload();
    }

    public static ClientConfig client() {
        return client;
    }

    public static ServerConfig server() {
        return server;
    }

    public static List<ContentError> errors() {
        return errors;
    }

    private static final class Defaults implements ConfigReader {
        private static final Defaults INSTANCE = new Defaults();

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            return fallback;
        }

        @Override
        public int getInt(String key, int fallback, int min, int max) {
            return fallback;
        }

        @Override
        public double getDouble(String key, double fallback, double min, double max) {
            return fallback;
        }

        @Override
        public int getColor(String key, int fallback) {
            return fallback;
        }

        @Override
        public String getString(String key, String fallback) {
            return fallback;
        }

        @Override
        public List<ContentError> errors() {
            return List.of();
        }
    }
}
