package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Owns the two switch files and their lifecycle.
 *
 * <p>Both are read when the mod is constructed and re-read whenever {@link #reload()} is called, so an
 * edited {@code server.toml} becomes visible without a restart. The 26.3 line hooked that reload onto
 * the vanilla {@code /reload} command together with the content files; on 1.20.1 the content files
 * cannot be re-registered at runtime, but the switches still can, and the command keeps calling this.
 *
 * <h2>Two storages, one set of switches</h2>
 *
 * <p>Where the values live is decided once, by {@link #backend}. With no backend they come from
 * {@code config/MerlinLib/client.toml} and {@code server.toml}, parsed by {@link TomlFile}. With a
 * Jupiter backend they come from the container that mod owns, and those two files are never written
 * again: their values were imported once and the file was renamed (see {@link JupiterBackend}).
 * Everything above this class goes through {@link #read} and {@link #write} and cannot tell the
 * difference.
 */
public final class ConfigManager {

    /**
     * The active storage, or {@code null} for the toml files.
     *
     * <p>Initialised here, and deliberately <b>before</b> the two configurations below: those are built by
     * their own {@code load()}, which reads through {@link #read}, so a backend installed afterwards would
     * arrive too late to be seen on the first launch and the values would be taken from the toml files for
     * one session. Field order is the whole of the fix.
     */
    private static volatile ConfigBackend backend = JupiterBackend.create();

    private static volatile ClientConfig client = ClientConfig.load();
    private static volatile ServerConfig server = ServerConfig.load();
    private static volatile List<ContentError> errors = List.of();

    private ConfigManager() {
    }

    /**
     * Installs a storage, or removes it.
     *
     * <p>Public because a dependent mod may want to drive the switches from somewhere else entirely; the
     * library itself only ever calls it from the field above. The next {@link #reload()} reads through the
     * new backend.
     *
     * @param installed the backend to use, or {@code null} to go back to the toml files
     */
    public static void setBackend(ConfigBackend installed) {
        backend = installed;
    }

    /** @return the active storage, or {@code null} when the toml files are used. */
    public static ConfigBackend getBackend() {
        return backend;
    }

    /**
     * Reads one of the two switch sources.
     *
     * <p>The replacement for calling {@link TomlFile#read} directly: with a backend active that call would
     * create and parse a file nothing reads any more.
     *
     * @param path     the toml file to fall back to
     * @param defaults the template written when that file is missing
     * @return the active view, never {@code null}
     */
    static ConfigReader read(Path path, String defaults) {
        ConfigBackend active = backend;
        return active == null ? TomlFile.read(path, defaults) : active.view();
    }

    /**
     * Stores selected values, wherever they live.
     *
     * <p>Same contract as {@link TomlFile#rewrite}: only the named keys are touched, keyed by
     * {@code section.name}. A backend stores them one by one and flushes once, which is what keeps a
     * migration from leaving the storage half written.
     *
     * @param path      the toml file to update when no backend is active
     * @param defaults  the template written when that file is missing
     * @param overrides the values to store
     */
    static void write(Path path, String defaults, Map<String, String> overrides) {
        if (overrides.isEmpty()) {
            return;
        }
        ConfigBackend active = backend;
        if (active == null) {
            TomlFile.rewrite(path, defaults, overrides);
            return;
        }
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            active.write(entry.getKey(), entry.getValue());
        }
        active.save();
    }

    /**
     * Restores every setting to the mod's defaults.
     *
     * <p>Done by deleting the two files and reading them again: the loader recreates a missing file from its
     * commented template, which is the same thing that happens on a fresh install, so there is no second copy of
     * the defaults to keep in step with the first. A backend that is not file based has no such template, so
     * it is left alone and only re-read.
     */
    public static synchronized void resetToDefaults() {
        if (backend != null) {
            Constants.LOG.info("[MerlinLib] the configuration is owned by {}, which has no reset of its own",
                    backend.name());
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

    public static synchronized void reload() {
        client = ClientConfig.load();
        server = ServerConfig.load();

        // A backend view is long lived, unlike a freshly parsed file, so the same complaint would be
        // printed on every reload. It is reported once.
        LinkedHashSet<ContentError> collected = new LinkedHashSet<>(client.errors());
        collected.addAll(server.errors());
        errors = List.copyOf(new ArrayList<>(collected));

        for (ContentError error : errors) {
            Constants.LOG.warn("[MerlinLib] configuration problem: {}", error.format());
        }
    }

    /**
     * Writes client switches and re-reads both sources.
     *
     * <p>The values are stored through {@link #write}, which keeps the comments of a toml file and lets a
     * backend take them instead, and the reload makes them visible immediately: every consumer reads
     * {@link #client()} at the moment it acts, so a saved switch is live without restarting the game.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveClient(java.util.Map<String, String> overrides) {
        write(ClientConfig.path(), ClientConfig.defaults(), overrides);
        reload();
    }

    /**
     * Writes authoritative server switches and re-reads both sources.
     *
     * <p>In single player the integrated server shares this process, so the new values apply at once. On
     * a dedicated server this writes the client's copy only; the server keeps its own file, which is why
     * these switches are named after the side that owns them.
     *
     * @param overrides the values to write, keyed by {@code section.key}
     */
    public static synchronized void saveServer(java.util.Map<String, String> overrides) {
        write(ServerConfig.path(), ServerConfig.defaults(), overrides);
        reload();
    }

    public static ClientConfig client() {
        return client;
    }

    public static ServerConfig server() {
        return server;
    }

    /** @return problems found while reading the switches during the last reload. */
    public static List<ContentError> errors() {
        return errors;
    }
}
