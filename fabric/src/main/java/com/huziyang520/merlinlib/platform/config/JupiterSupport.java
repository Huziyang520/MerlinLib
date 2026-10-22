package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Looks for Jupiter and uses it when it is there.
 *
 * <h2>What "using it" means</h2>
 *
 * <p>Jupiter is a configuration list, not a configuration screen of its own: every mod that wants one registers
 * a container with it, and Jupiter draws whatever it was given. So there are two separate things to do, and
 * both are optional:
 *
 * <ul>
 *     <li><b>declare the settings</b> ({@code register} on {@link JupiterBackend}): this is what makes MerlinLib
 *     appear in Jupiter's own list of configurations, with its tabs and entries. It happens whenever Jupiter is
 *     installed, even if Cloth Config is installed too, so a player who prefers Jupiter's list finds the mod
 *     there either way;</li>
 *     <li><b>install the store</b>: only when nothing else owns it, because the TOML files must have exactly one
 *     backend. Cloth Config gets there first when it is present, which is the order the mod documents.</li>
 * </ul>
 *
 * <h2>Why every call goes through a class name</h2>
 *
 * <p>Jupiter is optional: not in {@code depends}, not bundled, and the mod has to start without it. A class whose
 * own bytecode mentions {@code com.iafenvoy...} cannot be loaded at all without the mod present, no matter how
 * carefully its callers are guarded - which is why {@link JupiterBackend} is only ever reached by reflection,
 * from this class, after the check below passed.
 */
public final class JupiterSupport {

    /** The class that actually touches Jupiter. */
    private static final String BACKEND_CLASS = "com.huziyang520.merlinlib.platform.config.JupiterBackend";

    /** Jupiter's mod id. */
    private static final String MOD_ID = "jupiter";

    /** Whether the availability check already ran, and what it answered. */
    private static Boolean available;

    /** The backend instance, once it was created. */
    private static ConfigBackend backend;

    private JupiterSupport() {
    }

    /**
     * @return whether Jupiter is installed and its classes can be loaded
     */
    public static boolean isAvailable() {
        Boolean known = available;
        if (known != null) {
            return known;
        }
        boolean found = FabricLoader.getInstance().isModLoaded(MOD_ID);
        if (found) {
            try {
                Class.forName("com.iafenvoy.jupiter.ConfigManager");
            } catch (ClassNotFoundException | LinkageError missing) {
                found = false;
            }
        }
        available = found;
        return found;
    }

    /**
     * Declares MerlinLib to Jupiter and, when no other store is installed, makes this one the store.
     *
     * @return {@code true} when Jupiter is there and its container was registered
     */
    public static boolean activate() {
        if (!isAvailable()) {
            Constants.LOG.info("[MerlinLib] Jupiter is not installed, so it cannot show the configuration; "
                    + "the TOML files stay editable by hand");
            return false;
        }
        Object created = backend();
        if (created == null) {
            return false;
        }
        try {
            created.getClass().getMethod("register").invoke(created);
        } catch (ReflectiveOperationException | LinkageError failure) {
            Constants.LOG.warn("[MerlinLib] Jupiter is installed but the configuration could not be declared to "
                    + "it", failure);
            return false;
        }
        if (ConfigManager.backend() == null) {
            ConfigManager.setBackend((ConfigBackend) created);
        } else {
            // Cloth Config already owns the store. Jupiter still gets the container above, so both screens show
            // the same values; only one of them may be the thing ConfigManager reads and writes through.
            Constants.LOG.info("[MerlinLib] Jupiter shows the configuration list, the store stays: {}",
                    ConfigManager.backend().name());
        }
        return true;
    }

    /**
     * Opens Jupiter's screen for the MerlinLib settings.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen, or {@code null} when Jupiter is missing or its screen failed to build
     */
    public static Screen openScreen(Screen parent) {
        if (!isAvailable()) {
            return null;
        }
        Object created = backend();
        if (created == null) {
            return null;
        }
        try {
            Object screen = created.getClass().getMethod("openScreen", Screen.class).invoke(created, parent);
            return screen instanceof Screen built ? built : null;
        } catch (ReflectiveOperationException | LinkageError failure) {
            Constants.LOG.warn("[MerlinLib] the Jupiter configuration screen could not be opened", failure);
            return null;
        }
    }

    /**
     * @return the backend instance, created on first use, or {@code null} when it cannot be created
     */
    private static Object backend() {
        ConfigBackend known = backend;
        if (known != null) {
            return known;
        }
        try {
            ConfigBackend created = (ConfigBackend) Class.forName(BACKEND_CLASS)
                    .getConstructor().newInstance();
            backend = created;
            return created;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException failure) {
            Constants.LOG.warn("[MerlinLib] Jupiter is installed but its configuration backend could not be "
                    + "created; the TOML files stay editable by hand", failure);
            available = Boolean.FALSE;
            return null;
        }
    }
}
