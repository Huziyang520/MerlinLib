package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the two switch files and their lifecycle.
 *
 * <p>Both are read when the mod is constructed and re-read whenever {@link #reload()} is called, so an
 * edited {@code server.toml} becomes visible without a restart. The 26.3 line hooked that reload onto
 * the vanilla {@code /reload} command together with the content files; on 1.20.1 the content files
 * cannot be re-registered at runtime, but the switches still can, and the command keeps calling this.
 */
public final class ConfigManager {

    private static volatile ClientConfig client = ClientConfig.load();
    private static volatile ServerConfig server = ServerConfig.load();
    private static volatile List<ContentError> errors = List.of();

    private ConfigManager() {
    }

    /**
     * Restores every setting to the mod's defaults.
     *
     * <p>Done by deleting the two files and reading them again: the loader recreates a missing file from its
     * commented template, which is the same thing that happens on a fresh install, so there is no second copy of
     * the defaults to keep in step with the first.
     */
    public static synchronized void resetToDefaults() {
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

    /**
     * Writes client switches and re-reads both files.
     *
     * <p>The values are written into {@code client.toml} in place, keeping its comments, and the reload
     * makes them visible immediately: every consumer reads {@link #client()} at the moment it acts, so a
     * saved switch is live without restarting the game.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveClient(java.util.Map<String, String> overrides) {
        TomlFile.rewrite(ClientConfig.path(), ClientConfig.defaults(), overrides);
        reload();
    }

    /**
     * Writes authoritative server switches and re-reads both files.
     *
     * <p>In single player the integrated server shares this process, so the new values apply at once. On
     * a dedicated server this writes the client's copy only; the server keeps its own file, which is why
     * the configuration screen labels these switches as server owned.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveServer(java.util.Map<String, String> overrides) {
        TomlFile.rewrite(ServerConfig.path(), ServerConfig.defaults(), overrides);
        reload();
    }

    public static ClientConfig client() {
        return client;
    }

    public static ServerConfig server() {
        return server;
    }

    /** @return problems found while reading the switch files during the last reload. */
    public static List<ContentError> errors() {
        return errors;
    }
}
