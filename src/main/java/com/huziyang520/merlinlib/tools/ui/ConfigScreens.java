// Holds the platform configuration screen, when the platform has one.

package com.huziyang520.merlinlib.tools.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ConfigScreens {
    @FunctionalInterface
    public interface Opener {
        Screen create(Screen parent);
    }

    private static volatile Opener opener;

    private ConfigScreens() {
    }

    public static void install(Opener factory) {
        opener = factory;
    }

    public static boolean available() {
        return opener != null;
    }

    public static Screen create(Screen parent) {
        Opener factory = opener;
        return factory == null ? null : factory.create(parent);
    }

    public static boolean open(Screen parent) {
        Screen screen = create(parent);
        if (screen == null) {
            return false;
        }
        Minecraft.getInstance().setScreen(screen);
        return true;
    }
}
