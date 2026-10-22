package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;

/**
 * Wiring of the optional Cloth Config dependency on Fabric.
 *
 * <h2>Optional means optional</h2>
 *
 * <p>Cloth Config is a screen builder and nothing else in MerlinLib needs it, so it is declared as a
 * recommendation rather than a dependency and the whole feature is behind one classpath check. Without it
 * the TOML file is read directly, the {@code /merlinlib config} command says which mod is missing, and no
 * Cloth class is ever resolved - the screen builder lives in {@link ClothScreen}, which is only reached
 * through the factory installed at the end of {@link #install()}.
 *
 * <p>With it, the same {@code client.toml} is still the store: see {@link ClothBackend} for why.
 */
public final class FabricConfig {

    /** The class that proves Cloth Config is installed, asked for by name so nothing is linked. */
    private static final String CLOTH_API = "me.shedaniel.clothconfig2.api.ConfigBuilder";

    private FabricConfig() {
    }

    /**
     * Installs the Cloth backed store and screen when Cloth Config is present, and does nothing otherwise.
     */
    public static void install() {
        if (!clothPresent()) {
            Constants.LOG.info("[MerlinLib] Cloth Config API is not installed: client.toml is read directly "
                    + "and /merlinlib config cannot open a screen");
            return;
        }
        ConfigManager.setBackend(new ClothBackend());
        ConfigScreens.install(ClothScreen::create);
        Constants.LOG.info("[MerlinLib] Cloth Config API found: the configuration screen edits client.toml");
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
