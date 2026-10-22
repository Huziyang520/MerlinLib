package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * The optional Jupiter backed storage.
 *
 * <h2>What this class is</h2>
 *
 * <p>Jupiter is a configuration front end: it owns the screen the mod list offers and the file behind it.
 * When it is installed, MerlinLib stores its switches in a container of Jupiter's instead of writing its
 * own {@code client.toml} / {@code server.toml}, so the same switches are edited in Jupiter's screen with
 * no second screen to maintain.
 *
 * <h2>It must load when Jupiter is absent</h2>
 *
 * <p>Jupiter is a <b>soft</b> dependency: no {@code dependencies} entry, no {@code compileOnly} class in
 * the shipped jar's code path, and no annotation of any kind. This class therefore mentions no Jupiter
 * type at all - it only checks that the library is on the class path, and then hands the work to
 * {@link JupiterSettings}, which is compiled against Jupiter's api and is <em>not</em> loaded until that
 * check has passed. A JVM resolves a class's constants lazily, so an uninstalled Jupiter means the
 * reference inside {@link #create()} is never followed.
 *
 * <p>Why the api is called directly in {@link JupiterSettings} rather than through
 * {@code java.lang.reflect}: the container has to be a <em>subclass</em> of Jupiter's
 * {@code FileConfigContainer}, and a builder chain of {@code BooleanEntry.Builder} /
 * {@code IntegerEntry.Builder} calls reproduced reflectively would be unchecked by the compiler - a
 * renamed method would compile and fail only in the log of a player. The class loading guard above gives
 * the same safety as reflection, and the compiler checks the rest.
 *
 * <h2>Client only</h2>
 *
 * <p>The backend is only installed on a client. A dedicated server has no configuration screen to offer,
 * and Jupiter's container classes pull Minecraft's client facing types during their own initialisation;
 * a server keeps the toml files, which is exactly what it had before.
 *
 * <h2>The one-off import</h2>
 *
 * <p>A player who already has {@code client.toml} must not lose their switches when Jupiter is installed,
 * so the first launch reads that file and writes every value into the container, then renames it to
 * {@code client.toml.imported}. The rename is the marker: once the file is gone there is nothing to
 * import, so the step cannot run twice.
 */
public final class JupiterBackend {

    /** Jupiter's registry of configuration containers. Referenced by name only, never as a type. */
    private static final String PROBE = "com.iafenvoy.jupiter.ConfigManager";

    private JupiterBackend() {
    }

    /**
     * Builds the backend when it can be built.
     *
     * <p>Called once, from the field that {@link ConfigManager} reads first. Every failure is a warning and
     * a {@code null}: a configuration front end that could not be reached must never cost the player their
     * settings, and the toml files are always there to fall back on.
     *
     * @return the backend, or {@code null} to keep using the toml files
     */
    public static ConfigBackend create() {
        if (!onClient()) {
            return null;
        }
        if (!present()) {
            Constants.LOG.info("[MerlinLib] Jupiter is not installed; the configuration is kept in "
                    + "config/MerlinLib/client.toml and server.toml");
            return null;
        }
        try {
            ConfigBackend backend = JupiterSettings.open();
            importLegacy(backend, ClientConfig.path(), ClientConfig.defaults());
            importLegacy(backend, ServerConfig.path(), ServerConfig.defaults());
            Constants.LOG.info("[MerlinLib] the configuration is now owned by {}", backend.name());
            return backend;
        } catch (Throwable failure) {
            // Catchable, not Exception: a missing method or a half installed jar raises LinkageError, and
            // either of those must still leave the toml files in charge.
            Constants.LOG.warn("[MerlinLib] Jupiter is installed but its configuration could not be used; "
                    + "the configuration stays in config/MerlinLib/*.toml", failure);
            return null;
        }
    }

    /**
     * @return whether this process is a client, which is the only side that has a configuration screen
     */
    private static boolean onClient() {
        try {
            return FMLLoader.getDist().isClient();
        } catch (Throwable unavailable) {
            return false;
        }
    }

    /**
     * @return whether Jupiter's container registry is on the class path
     */
    private static boolean present() {
        try {
            Class.forName(PROBE, false, JupiterBackend.class.getClassLoader());
            return true;
        } catch (Throwable missing) {
            return false;
        }
    }

    /**
     * Moves the values of one legacy toml file into the backend, then retires the file.
     *
     * <p>Every key the file holds is handed over, including ones the backend does not know: a key that no
     * longer exists (the removed {@code [gui]} animation switches, for example) is ignored there, which is
     * the same outcome as leaving it in a file nothing reads.
     *
     * @param backend  the storage to fill
     * @param file     the toml file to import
     * @param defaults the template that file would have been created from, unused when it exists
     */
    private static void importLegacy(ConfigBackend backend, Path file, String defaults) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        Map<String, String> legacy = TomlFile.read(file, defaults).values();
        for (Map.Entry<String, String> entry : legacy.entrySet()) {
            backend.write(entry.getKey(), entry.getValue());
        }
        backend.save();

        Path imported = file.resolveSibling(file.getFileName() + ".imported");
        try {
            Files.move(file, imported, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            // Not fatal: the values are in the backend already. Without the rename the import simply runs
            // again next launch, which writes the same values to the same keys.
            Constants.LOG.warn("[MerlinLib] could not rename {} after importing it", file, failure);
            return;
        }
        Constants.LOG.info("[MerlinLib] imported {} key(s) from {} into {}; the file is now {}",
                legacy.size(), file.getFileName(), backend.name(), imported.getFileName());
    }
}
