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

/**
 * Owns the two switch stores and their lifecycle.
 *
 * <p>Both are read on startup and re-read on every content reload (startup and {@code /reload}), so
 * editing a configuration and running {@code /reload} is enough to change behaviour.
 *
 * <h2>Where the values come from</h2>
 *
 * <p>By default the commented files under {@code config/MerlinLib} are the store, read through
 * {@link TomlFile}. A loader that has a configuration system of its own installs a {@link ConfigBackend}
 * with {@link #setBackend} (client switches) and {@link #setServerBackend} (server switches); from then on
 * the very same {@link ConfigReader} calls are answered by that backend instead. Nothing above this class
 * knows the difference.
 *
 * <p>The first time a backend is installed, the old TOML file of that half is imported into it and then
 * renamed to {@code *.imported}, so a player's previous settings survive the move and the import can never
 * happen twice.
 */
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

    /**
     * Installs the store the client switches are read from, and imports the old {@code client.toml} once.
     *
     * @param backend the backend, usually a loader configuration of the client half
     */
    public static synchronized void setBackend(ConfigBackend backend) {
        clientBackend = backend;
        Constants.LOG.info("[MerlinLib] client switches are now read from {}", backend.name());
        importLegacy(backend, ClientConfig.path(), ClientConfig.defaults(), Map.of(
                // Before layout 3 the marker lived in [gui], next to the switches of the drawn screens.
                "gui.config_version", "general.config_version"));
        reload();
    }

    /**
     * Installs the store the server switches are read from, and imports the old {@code server.toml} once.
     *
     * @param backend the backend, usually a loader configuration of the server half
     */
    public static synchronized void setServerBackend(ConfigBackend backend) {
        serverBackend = backend;
        Constants.LOG.info("[MerlinLib] server switches are now read from {}", backend.name());
        importLegacy(backend, ServerConfig.path(), ServerConfig.defaults(), Map.of());
        reload();
    }

    /** @return the backend the client switches come from, or {@code null} while the TOML file is used. */
    public static ConfigBackend clientBackend() {
        return clientBackend;
    }

    /** @return the backend the server switches come from, or {@code null} while the TOML file is used. */
    public static ConfigBackend serverBackend() {
        return serverBackend;
    }

    /**
     * @return the store the client switches are read from; the TOML file when no backend is installed
     */
    static ConfigReader clientView() {
        ConfigBackend backend = clientBackend;
        return backend != null ? backend.view() : TomlFile.read(ClientConfig.path(), ClientConfig.defaults());
    }

    /**
     * @return the store the server switches are read from; the TOML file when no backend is installed
     */
    static ConfigReader serverView() {
        ConfigBackend backend = serverBackend;
        return backend != null ? backend.view() : TomlFile.read(ServerConfig.path(), ServerConfig.defaults());
    }

    /**
     * Reads both stores, creating the files with their documented defaults when missing, and logs anything
     * that could not be understood.
     */
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

    /**
     * Writes client switches and re-reads both stores.
     *
     * <p>The values are written in place when the TOML file is the store, keeping its comments, and into the
     * backend otherwise; the reload makes them visible immediately, because every consumer reads
     * {@link #client()} at the moment it acts.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
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

    /**
     * Writes authoritative server switches and re-reads both stores.
     *
     * <p>In single player the integrated server shares this process, so the new values apply at once. On
     * a dedicated server this writes the client's copy only; the server keeps its own configuration.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
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

    /**
     * Moves a file written by an older build into a backend, once.
     *
     * <p>The file is renamed before anything is written through the backend, so a backend that stores its
     * values in the very same file cannot overwrite the source it is importing from. Keys the current
     * template does not declare any more - the whole {@code [gui]} section, for instance - are dropped
     * instead of being carried over: they belonged to options that no longer exist.
     *
     * @param backend  the store to import into
     * @param path     the legacy file
     * @param defaults the current template, used both to create and to filter
     * @param remap    keys that were renamed, old key to new key
     */
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

    /** @return problems found while reading the switch stores during the last reload. */
    public static List<ContentError> errors() {
        return errors;
    }
}
