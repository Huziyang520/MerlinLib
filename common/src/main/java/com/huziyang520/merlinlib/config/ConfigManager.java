package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the two switch files and their lifecycle.
 *
 * <p>Both are read on startup and re-read on every content reload (startup and {@code /reload}), so
 * editing {@code server.toml} and running {@code /reload} is enough to change server behaviour.
 */
public final class ConfigManager {

    private static volatile ClientConfig client = ClientConfig.load();
    private static volatile ServerConfig server = ServerConfig.load();
    private static volatile List<ContentError> errors = List.of();

    private ConfigManager() {
    }

    /**
     * Reads both files, creating them with their documented defaults when missing, and logs anything
     * that could not be understood.
     */
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
