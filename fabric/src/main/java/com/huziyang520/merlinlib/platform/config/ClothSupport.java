package com.huziyang520.merlinlib.platform.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigBackend;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Looks for Cloth Config and uses it when it is there.
 *
 * <h2>Why every call goes through a class name</h2>
 *
 * <p>Cloth Config is optional: it is not required in {@code fabric.mod.json}, not bundled, and the mod must
 * start without it. A class whose own bytecode mentions {@code me.shedaniel...} cannot be loaded at all
 * without the mod present, no matter how carefully its callers are guarded - which is why
 * {@link ClothBackend} is only ever reached by reflection, from this class, after the check below passed.
 *
 * <p>Without Cloth the mod keeps the plain TOML files, exactly as before, and {@code /merlinlib config} says
 * that a screen needs one of the supported mods.
 */
public final class ClothSupport {

    /** The class that actually touches Cloth Config. */
    private static final String BACKEND_CLASS = "com.huziyang520.merlinlib.platform.config.ClothBackend";

    /** Mod ids Cloth Config has shipped under; either one means the screen is available. */
    private static final String[] MOD_IDS = {"cloth-config", "cloth-config2"};

    /** Whether the availability check already ran, and what it answered. */
    private static Boolean available;

    private ClothSupport() {
    }

    /**
     * @return whether Cloth Config is installed and its classes can be loaded
     */
    public static boolean isAvailable() {
        Boolean known = available;
        if (known != null) {
            return known;
        }
        boolean found = false;
        for (String id : MOD_IDS) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                found = true;
                break;
            }
        }
        if (found) {
            try {
                Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
            } catch (ClassNotFoundException | LinkageError missing) {
                found = false;
            }
        }
        available = found;
        return found;
    }

    /**
     * Installs the Cloth backed store, when Cloth Config is there.
     *
     * @return {@code true} when the store was installed
     */
    public static boolean activate() {
        if (!isAvailable()) {
            Constants.LOG.info("[MerlinLib] Cloth Config is not installed, the TOML files stay the "
                    + "configuration store and /merlinlib config will say so");
            return false;
        }
        try {
            ConfigBackend backend = (ConfigBackend) Class.forName(BACKEND_CLASS)
                    .getConstructor().newInstance();
            ConfigManager.setBackend(backend);
            return true;
        } catch (ReflectiveOperationException | LinkageError | ClassCastException failure) {
            Constants.LOG.warn("[MerlinLib] Cloth Config is installed but its screen could not be set up, "
                    + "falling back to the TOML files", failure);
            return false;
        }
    }

    /**
     * Opens the configuration screen, when there is one.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen, or {@code null} when Cloth Config is not installed or its screen failed to build
     */
    public static Screen openScreen(Screen parent) {
        ConfigBackend backend = ConfigManager.backend();
        if (backend == null || !isAvailable()) {
            return null;
        }
        try {
            Object screen = Class.forName(BACKEND_CLASS).getMethod("openScreen", Screen.class)
                    .invoke(backend, parent);
            return screen instanceof Screen built ? built : null;
        } catch (ReflectiveOperationException | LinkageError failure) {
            Constants.LOG.warn("[MerlinLib] the Cloth Config screen could not be opened", failure);
            return null;
        }
    }
}
