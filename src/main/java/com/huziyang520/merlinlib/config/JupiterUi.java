// Wiring of the optional Jupiter dependency on Fabric.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;

public final class JupiterUi {
    private static final String API = "com.iafenvoy.jupiter.ConfigManager";
    private static JupiterContainer container;

    private JupiterUi() {
    }

    public static boolean present() {
        try {
            Class.forName(API);
            return true;
        } catch (ClassNotFoundException | LinkageError missing) {
            return false;
        }
    }

    public static void install() {
        container = new JupiterContainer();
        com.iafenvoy.jupiter.ConfigManager.getInstance().registerConfigHandler(container);
        ConfigManager.setBackend(new TomlBackend("jupiter"));
        ConfigScreens.install(JupiterUi::create);
        Constants.LOG.info("[MerlinLib] Jupiter found: the switches are listed in Jupiter's configuration "
                + "list and edited there, client.toml stays the store");
    }

    public static Screen create(Screen parent) {
        return com.iafenvoy.jupiter.render.screen.JupiterScreen.getConfigScreen(parent, container, false);
    }
}
