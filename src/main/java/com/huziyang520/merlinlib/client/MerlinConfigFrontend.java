package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.client.gui.screens.Screen;

/**
 * The mod list's Config button: which optional front end draws the screen, if any.
 *
 * <h2>The problem this solves</h2>
 *
 * <p>Forge draws that button only for a mod that registered a
 * {@code net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory} extension point. Jupiter was
 * installed and its container was registered, but Jupiter only offers its screens through its own mod's
 * button, so MerlinLib appeared in the mod list with no way to open its switches - which is exactly the
 * report this class answers.
 *
 * <h2>Either front end, or neither</h2>
 *
 * <p>Cloth Config is preferred when it is present: its screen is built from {@link
 * com.huziyang520.merlinlib.config.OptionSpec}, the same schema Jupiter's container is built from, so the two
 * show the same switches. When only Jupiter is there, its own container screen is opened instead. When
 * neither is there, nothing is registered at all - the button is then absent rather than dead, and the
 * switches stay in the toml files. That is why {@link #available()} is consulted before the extension point is
 * registered, and not from inside the screen factory.
 *
 * <h2>Nothing here names a third party type</h2>
 *
 * <p>Every reference to Cloth or Jupiter lives in a class that is loaded only after its own
 * {@code Class.forName} probe has passed, so a missing library is a skipped branch rather than a
 * {@code LinkageError} during start up. The probes are also the single place that spells the two class names,
 * which is why {@link #cloth()} and {@link #jupiter()} are public.
 */
public final class MerlinConfigFrontend {

    /** Cloth Config's builder: present in every 11.x version, and the entry point of the screen. */
    private static final String CLOTH_PROBE = "me.shedaniel.clothconfig2.api.ConfigBuilder";

    /** Jupiter's container registry, the same class {@code JupiterBackend} probes for. */
    private static final String JUPITER_PROBE = "com.iafenvoy.jupiter.ConfigManager";

    private MerlinConfigFrontend() {
    }

    /** @return whether Cloth Config is on the class path */
    public static boolean cloth() {
        return present(CLOTH_PROBE);
    }

    /** @return whether Jupiter is on the class path */
    public static boolean jupiter() {
        return present(JUPITER_PROBE);
    }

    /** @return whether any optional front end can draw a configuration screen */
    public static boolean available() {
        return cloth() || jupiter();
    }

    /**
     * Builds the configuration screen.
     *
     * <p>Cloth first, then Jupiter. A front end that is installed but fails to build - a half installed jar, a
     * version whose api moved - is logged and skipped rather than thrown: the player still gets the game, and
     * if the other front end works they get a screen too.
     *
     * @param parent the mod list screen, restored when the player closes the configuration
     * @return the screen, or {@code null} when no front end could produce one
     */
    public static Screen open(Screen parent) {
        if (cloth()) {
            try {
                return com.huziyang520.merlinlib.client.config.ClothConfigScreen.build(parent);
            } catch (Throwable failure) {
                Constants.LOG.warn("[MerlinLib] Cloth Config is installed but its screen could not be built", failure);
            }
        }
        if (jupiter()) {
            try {
                Screen screen = com.huziyang520.merlinlib.config.ConfigScreens.jupiter(parent);
                if (screen != null) {
                    return screen;
                }
            } catch (Throwable failure) {
                Constants.LOG.warn("[MerlinLib] Jupiter is installed but its screen could not be opened", failure);
            }
        }
        return null;
    }

    /**
     * @param name a fully qualified class name
     * @return whether that class can be loaded, without initialising it
     */
    private static boolean present(String name) {
        try {
            Class.forName(name, false, MerlinConfigFrontend.class.getClassLoader());
            return true;
        } catch (Throwable missing) {
            return false;
        }
    }
}
