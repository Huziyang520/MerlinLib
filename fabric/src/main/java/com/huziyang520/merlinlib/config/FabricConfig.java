package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;

/**
 * Wiring of the optional configuration screens on Fabric.
 *
 * <h2>Optional means optional</h2>
 *
 * <p>MerlinLib draws no configuration interface of its own: it keeps its switches in {@code client.toml}
 * and lets whichever screen builder the pack already has show them. Two are supported, in this order:
 *
 * <ol>
 *   <li><b>Cloth Config</b> - the screen this line has always shipped with, editing {@code client.toml}
 *       through its own widget set;</li>
 *   <li><b>Jupiter</b> - used when Cloth is absent, which also lists MerlinLib in Jupiter's own
 *       configuration list, so the switches are reachable from there as well as from
 *       {@code /merlinlib config} and the Mod Menu button.</li>
 * </ol>
 *
 * <p>Order matters: a pack that has both keeps the Cloth screen it already knew, and nothing is installed
 * twice. With neither installed the TOML file is read directly, no configuration class is ever resolved,
 * and the command says which mods would give the switches a screen.
 */
public final class FabricConfig {

    /** The class that proves Cloth Config is installed, asked for by name so nothing is linked. */
    private static final String CLOTH_API = "me.shedaniel.clothconfig2.api.ConfigBuilder";

    private FabricConfig() {
    }

    /**
     * Installs the first available screen builder, and nothing when there is none.
     *
     * <p>Cloth Config and Jupiter both write through a {@link TomlBackend}, so the switches keep living in
     * the one documented file no matter which screen is in use, and removing either mod loses nothing.
     */
    public static void install() {
        if (clothPresent()) {
            ConfigManager.setBackend(new TomlBackend("cloth"));
            ConfigScreens.install(ClothScreen::create);
            Constants.LOG.info("[MerlinLib] Cloth Config API found: the configuration screen edits client.toml");
            return;
        }
        if (JupiterUi.present()) {
            JupiterUi.install();
            return;
        }
        Constants.LOG.info("[MerlinLib] neither Cloth Config nor Jupiter is installed: client.toml is read "
                + "directly and /merlinlib config cannot open a screen");
    }

    /** @return whether Cloth Config is on the classpath */
    private static boolean clothPresent() {
        try {
            Class.forName(CLOTH_API);
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
