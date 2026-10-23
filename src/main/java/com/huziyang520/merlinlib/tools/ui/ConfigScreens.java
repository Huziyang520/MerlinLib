package com.huziyang520.merlinlib.tools.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Holds the platform configuration screen, when the platform has one.
 */
public final class ConfigScreens {

    /**
     * Builds the configuration screen of one platform.
 */
    @FunctionalInterface
    public interface Opener {

        /**
         * @param parent the screen to return to, or {@code null} to resume the game
         * @return the screen to show, or {@code null} when this platform cannot build one
 */
        Screen create(Screen parent);
    }

    private static volatile Opener opener;

    private ConfigScreens() {
    }

    /**
     * Installs the platform factory. Calling it again replaces the previous one.
     *
     * @param factory the factory to use
 */
    public static void install(Opener factory) {
        opener = factory;
    }

    /** @return whether this platform has a configuration screen to open */
    public static boolean available() {
        return opener != null;
    }

    /**
     * Builds the platform configuration screen without showing it.
     *
     * @param parent the screen it returns to
     * @return the screen, or {@code null} when this platform cannot build one
 */
    public static Screen create(Screen parent) {
        Opener factory = opener;
        return factory == null ? null : factory.create(parent);
    }

    /**
     * Opens the platform configuration screen.
     *
     * @param parent the screen to return to, or {@code null} to resume the game
     * @return {@code true} when a screen was opened
 */
    public static boolean open(Screen parent) {
        Screen screen = create(parent);
        if (screen == null) {
            return false;
        }
        Minecraft.getInstance().setScreen(screen);
        return true;
    }
}
