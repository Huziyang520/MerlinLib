package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;

/**
 * Wiring of the optional Jupiter dependency on Fabric.
 */
public final class JupiterUi {

    /** The class that proves Jupiter is installed, asked for by name so nothing is linked. */
    private static final String API = "com.iafenvoy.jupiter.ConfigManager";

    /** The container registered with Jupiter, kept so the opener can show it again. */
    private static JupiterContainer container;

    private JupiterUi() {
    }

    /** @return whether Jupiter is on the classpath */
    public static boolean present() {
        try {
            Class.forName(API);
            return true;
        } catch (ClassNotFoundException | LinkageError missing) {
            return false;
        }
    }

    /**
     * Registers MerlinLib with Jupiter and installs the opener.
     *
     * <p>{@code registerConfigHandler} is what puts MerlinLib in Jupiter's own configuration list: it stores
     * the container, then calls {@code init()} and {@code load()} on it, so the tabs exist by the time the
     * list is opened. {@code client.toml} remains the store - see {@link JupiterContainer} for why.
     */
    public static void install() {
        container = new JupiterContainer();
        com.iafenvoy.jupiter.ConfigManager.getInstance().registerConfigHandler(container);
        ConfigManager.setBackend(new TomlBackend("jupiter"));
        ConfigScreens.install(JupiterUi::create);
        Constants.LOG.info("[MerlinLib] Jupiter found: the switches are listed in Jupiter's configuration "
                + "list and edited there, client.toml stays the store");
    }

    /**
     * Builds Jupiter's screen over the MerlinLib container.
     *
     * <p>{@code getConfigScreen} picks the single tab or the tabbed layout itself, and its third argument is
     * the "this is a server synced configuration" flag - {@code false} here, because these switches are
     * read from this client's own file and nothing is sent anywhere.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return the screen to show
     */
    public static Screen create(Screen parent) {
        return com.iafenvoy.jupiter.render.screen.JupiterScreen.getConfigScreen(parent, container, false);
    }
}
