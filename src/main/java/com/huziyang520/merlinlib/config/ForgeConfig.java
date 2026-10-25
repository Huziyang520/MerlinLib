// Wiring of the optional configuration screens on Forge: Cloth Config first, Jupiter otherwise.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;

public final class ForgeConfig {
    private static final String CLOTH_API = "me.shedaniel.clothconfig2.api.ConfigBuilder";

    private ForgeConfig() {
    }

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
                + "directly and the mod list has no configuration screen");
    }

    public static boolean available() {
        return ConfigScreens.available();
    }

    private static boolean clothPresent() {
        try {
            Class.forName(CLOTH_API);
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }
}
