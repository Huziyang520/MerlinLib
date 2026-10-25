// Owns the two switch stores: both are read on startup and re-read on every content reload.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private static volatile ConfigBackend clientBackend;
    private static volatile ConfigBackend serverBackend;
    private static volatile ClientConfig client;
    private static volatile ServerConfig server;
    private static volatile List<ContentError> errors = List.of();

    static {
        reload();
    }

    private ConfigManager() {
    }

    public static synchronized void setBackend(ConfigBackend backend) {
        clientBackend = backend;
        Constants.LOG.info("[MerlinLib] client switches are now read from {}", backend.name());
        importLegacy(backend, ClientConfig.path(), ClientConfig.defaults(), Map.of(
                "gui.config_version", "general.config_version"));
        reload();
    }

    public static synchronized void setServerBackend(ConfigBackend backend) {
        serverBackend = backend;
        Constants.LOG.info("[MerlinLib] server switches are now read from {}", backend.name());
        importLegacy(backend, ServerConfig.path(), ServerConfig.defaults(), Map.of());
        reload();
    }

    public static ConfigBackend clientBackend() {
        return clientBackend;
    }

    public static ConfigBackend serverBackend() {
        return serverBackend;
    }

    static ConfigReader clientView() {
        ConfigBackend backend = clientBackend;
        return backend != null ? backend.view() : TomlFile.read(ClientConfig.path(), ClientConfig.defaults());
    }

    static ConfigReader serverView() {
        ConfigBackend backend = serverBackend;
        return backend != null ? backend.view() : TomlFile.read(ServerConfig.path(), ServerConfig.defaults());
    }

    public static synchronized void reload() {
        client = ClientConfig.load(clientView());
        server = ServerConfig.load(serverView());
        List<ContentError> collected = new ArrayList<>(client.errors());
        collected.addAll(server.errors());
        errors = List.copyOf(collected);

        for (ContentError error : collected) {
            Constants.LOG.warn("[MerlinLib] configuration problem: {}", error.format());
        }
    }

    public static synchronized void saveClient(Map<String, String> overrides) {
        ConfigBackend backend = clientBackend;
        if (backend != null) {
            overrides.forEach(backend::write);
            backend.save();
        } else {
            TomlFile.rewrite(ClientConfig.path(), ClientConfig.defaults(), overrides);
        }
        reload();
    }

    public static synchronized void saveServer(Map<String, String> overrides) {
        ConfigBackend backend = serverBackend;
        if (backend != null) {
            overrides.forEach(backend::write);
            backend.save();
        } else {
            TomlFile.rewrite(ServerConfig.path(), ServerConfig.defaults(), overrides);
        }
        reload();
    }

    private static void importLegacy(ConfigBackend backend, Path path, String defaults, Map<String, String> remap) {
        if (!Files.isRegularFile(path)) {
            return;
        }
        TomlFile legacy = TomlFile.read(path, defaults);
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : legacy.values().entrySet()) {
            values.put(remap.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue());
        }

        Path imported = path.resolveSibling(path.getFileName() + ".imported");
        try {
            Files.move(path, imported, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Constants.LOG.error("[MerlinLib] could not archive {}; it was not imported", path, exception);
            return;
        }

        java.util.Set<String> known = TomlFile.keysOf(defaults);
        int carried = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (known.contains(entry.getKey())) {
                backend.write(entry.getKey(), entry.getValue());
                carried++;
            }
        }
        backend.save();
        Constants.LOG.info("[MerlinLib] imported {} value(s) from {} into {}; the file was renamed to {}",
                carried, path.getFileName(), backend.name(), imported.getFileName());
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
}
