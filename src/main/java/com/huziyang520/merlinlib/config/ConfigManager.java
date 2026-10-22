package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.ContentError;
import net.neoforged.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Owns the two configuration files and their lifecycle.
 *
 * <p>The values themselves are declared by {@link NeoSpecBackend} and stored by the loader, which also builds
 * the configuration screen from them. This class is the one place the rest of the mod asks for the current
 * values: it keeps one parsed {@link ClientConfig} and one parsed {@link ServerConfig}, and replaces them
 * whenever the files change.
 *
 * <p>Both are re-read on startup and on every content reload (startup and {@code /reload}), so editing the
 * common file by hand and running {@code /reload} is enough to change server behaviour.
 */
public final class ConfigManager {

    private static volatile ConfigBackend clientBackend;
    private static volatile ConfigBackend serverBackend;
    private static volatile ClientConfig client = new ClientConfig();
    private static volatile ServerConfig server = new ServerConfig();
    private static volatile List<ContentError> errors = List.of();

    private ConfigManager() {
    }

    /**
     * Installs the storage behind one of the two files.
     *
     * <p>Called once, from the loader entrypoint, right after the specifications are registered. Nothing is
     * read here: the loader may not have loaded the file yet, so {@link #reload()} is what turns a backend
     * into the values the mod reads.
     *
     * @param scope   which file the backend stores
     * @param backend the backend, or {@code null} to answer every question with the declared default
     */
    public static void setBackend(ConfigScope scope, ConfigBackend backend) {
        if (scope == ConfigScope.CLIENT) {
            clientBackend = backend;
        } else {
            serverBackend = backend;
        }
    }

    /**
     * @param scope which file to read
     * @return a reader over the current values of that file
     */
    public static ConfigReader read(ConfigScope scope) {
        ConfigBackend backend = scope == ConfigScope.CLIENT ? clientBackend : serverBackend;
        return backend == null ? Defaults.INSTANCE : backend.view();
    }

    /**
     * Reads both files and logs anything that could not be understood.
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
     * Called by the loader once one of the two files has been read.
     *
     * <p>Two things happen here, in this order:
     *
     * <ol>
     *   <li>a file left behind by a build from before the loader stored the switches is imported, once, and
     *       renamed to {@code *.imported};</li>
     *   <li>the values are re-read, so this launch already sees them. Without that the mod would keep the
     *       declared defaults until the next reload, because the files are read after the mod is built.</li>
     * </ol>
     *
     * @param config the file the loader has just read
     */
    public static synchronized void onConfigLoaded(ModConfig config) {
        for (ConfigBackend backend : new ConfigBackend[]{clientBackend, serverBackend}) {
            if (backend instanceof NeoSpecBackend spec && spec.name().equals(config.getFileName())) {
                spec.importLegacy();
            }
        }
        reload();
    }

    /**
     * Writes switches and re-reads both files.
     *
     * <p>Every consumer reads {@link #client()} or {@link #server()} at the moment it acts, so a written
     * switch is live without restarting the game.
     *
     * @param scope     which file to write
     * @param overrides the values to write, keyed by {@code section.key}
     */
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

    /** @return problems found while reading the configuration files during the last reload. */
    public static List<ContentError> errors() {
        return errors;
    }

    /**
     * The reader used before the loader has installed a backend, and on a side that never loads one.
     *
     * <p>It answers every question with the fallback the caller passed, which is the declared default, so a
     * value is never invented: the mod simply behaves the way a fresh install does.
     */
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
