package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Owns the two configuration files and their lifecycle.
 *
 * <p>Both are read on startup, on every content reload (startup and {@code /reload}), and whenever a switch
 * is saved, so a change is visible without restarting the game.
 *
 * <h2>Where the values come from</h2>
 *
 * <p>By default the store is the commented TOML file pair next to the game. A loader may replace it: the
 * NeoForge entrypoint installs a store backed by {@code ModConfigSpec} - which also gives the mod the native
 * configuration screen - and the Fabric client installs one backed by Cloth Config when that mod is present.
 * Everything in this mod reads through {@link #read(Path, String)}, so neither {@link ClientConfig} nor
 * {@link ServerConfig} knows which store is in use.
 *
 * <p>The store holds both halves at once: see {@link SplitReader} for how a key is routed to the client or
 * the server half.
 */
public final class ConfigManager {

    private static volatile ClientConfig client;
    private static volatile ServerConfig server;
    private static volatile List<ContentError> errors = List.of();
    /** The injected store, or {@code null} while the TOML files are the store. */
    private static volatile ConfigBackend backend;

    private ConfigManager() {
    }

    /**
     * Installs the loader's configuration store and reads it.
     *
     * <p>Called once by the loader entrypoint, before the rest of the mod starts. Until this happens the TOML
     * files are the store, which is why it has to run before anything reads a setting.
     *
     * @param store the store to use, never {@code null}
     */
    public static synchronized void setBackend(ConfigBackend store) {
        backend = store;
        Constants.LOG.info("[MerlinLib] configuration store: {}", store.name());
        reload();
    }

    /** @return the injected store, or {@code null} when the TOML files are the store */
    public static ConfigBackend backend() {
        return backend;
    }

    /**
     * Opens a configuration store for one file.
     *
     * <p>The single place that decides where a setting comes from: the injected backend when there is one,
     * the TOML file otherwise.
     *
     * @param path     the file the setting would live in
     * @param defaults the template written when that file does not exist yet
     * @return the view to read the settings from
     */
    static ConfigReader read(Path path, String defaults) {
        ConfigBackend store = backend;
        return store != null ? store.view() : TomlFile.read(path, defaults);
    }

    /**
     * Writes a set of settings into the store that is in use.
     *
     * @param path      the file the settings would live in
     * @param defaults  the template written when that file does not exist yet
     * @param overrides the values to write, keyed by {@code section.key}
     */
    static void rewrite(Path path, String defaults, Map<String, String> overrides) {
        if (overrides.isEmpty()) {
            return;
        }
        ConfigBackend store = backend;
        if (store != null) {
            overrides.forEach(store::write);
            store.save();
            return;
        }
        TomlFile.rewrite(path, defaults, overrides);
    }

    /**
     * Restores every setting to the mod's defaults.
     *
     * <p>With the TOML files as the store this is done by deleting them and reading them again: a missing file
     * is recreated from its commented template, which is the same thing that happens on a fresh install, so
     * there is no second copy of the defaults to keep in step with the first. When a loader store is
     * installed, that store owns the file and its own screen offers the reset, so nothing is deleted here.
     */
    public static synchronized void resetToDefaults() {
        if (backend != null) {
            Constants.LOG.info("[MerlinLib] settings are restored from the {} screen", backend.name());
            reload();
            return;
        }
        for (String name : new String[]{"client.toml", "server.toml"}) {
            try {
                java.nio.file.Files.deleteIfExists(ConfigDirectory.root().resolve(name));
            } catch (java.io.IOException exception) {
                Constants.LOG.error("[MerlinLib] could not remove {} while restoring defaults", name, exception);
            }
        }
        Constants.LOG.info("[MerlinLib] configuration restored to defaults");
        reload();
    }

    /** Reads both halves of the store and logs anything that could not be understood. */
    public static synchronized void reload() {
        client = ClientConfig.load();
        server = ServerConfig.load();

        List<ContentError> collected = new ArrayList<>(client.errors());
        collected.addAll(server.errors());
        errors = List.copyOf(collected);

        for (ContentError error : collected) {
            Constants.LOG.warn("[MerlinLib] configuration problem: {}", error.format());
        }

        // The notice's own switch lives in the client configuration, so it is refreshed here: a reload is the
        // only moment the value is known to be current, and re-registering the map entry costs nothing.
        com.huziyang520.merlinlib.notice.ConfigUiNotice.register();
    }

    /**
     * Writes client switches and reads both halves again.
     *
     * <p>The reload makes the change visible immediately: every consumer reads {@link #client()} at the
     * moment it acts, so a saved switch is live without restarting the game.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveClient(Map<String, String> overrides) {
        rewrite(ClientConfig.path(), ClientConfig.defaults(), overrides);
        reload();
    }

    /**
     * Writes authoritative server switches and reads both halves again.
     *
     * <p>In single player the integrated server shares this process, so the new values apply at once. On a
     * dedicated server this writes the client's copy only; the server keeps its own store, which is why these
     * switches are labelled server owned.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveServer(Map<String, String> overrides) {
        rewrite(ServerConfig.path(), ServerConfig.defaults(), overrides);
        reload();
    }

    /** @return the client switches, read on first use so a loader store is already installed */
    public static ClientConfig client() {
        ClientConfig current = client;
        if (current == null) {
            reload();
            current = client;
        }
        return current;
    }

    /** @return the server switches, read on first use so a loader store is already installed */
    public static ServerConfig server() {
        ServerConfig current = server;
        if (current == null) {
            reload();
            current = server;
        }
        return current;
    }

    /** @return problems found while reading the configuration during the last reload. */
    public static List<ContentError> errors() {
        return errors;
    }
}
