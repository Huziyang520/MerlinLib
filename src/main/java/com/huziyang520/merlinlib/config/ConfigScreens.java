package com.huziyang520.merlinlib.config;

import net.minecraft.client.gui.screens.Screen;

/**
 * Opens a front end's own configuration screen, for the mod list's Config button.
 *
 * <h2>Client only</h2>
 *
 * <p>The only thing here is a screen, so this class is only ever touched from the client side. It is also the
 * seam that keeps the Jupiter check honest: {@link JupiterSettings} names Jupiter types in its signatures and
 * its constant pool, and is therefore loaded only after {@code Class.forName} has proved the library is
 * there (see {@link JupiterBackend}). A JVM resolves a class reference lazily at the {@code invokestatic}, so
 * a process without Jupiter never loads it - provided nothing here is called.
 */
public final class ConfigScreens {

    private ConfigScreens() {
    }

    /**
     * @param parent the mod list screen, restored when the player closes this one
     * @return Jupiter's screen for MerlinLib's container, or {@code null} when no container was built
     */
    public static Screen jupiter(Screen parent) {
        return JupiterSettings.screen(parent);
    }
}
