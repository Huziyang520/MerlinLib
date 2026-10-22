package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the mod list's Config button.
 *
 * <h2>Why the extension point and not a screen of our own</h2>
 *
 * <p>Forge's mod list asks one question per mod: "did this mod register a
 * {@code ConfigScreenHandler.ConfigScreenFactory}?" - it goes through
 * {@code ConfigScreenHandler#getScreenFactoryFor}, which reads
 * {@code ModContainer#getCustomExtension}. Registering the container with Jupiter (which MerlinLib already
 * did) is invisible to that question, which is why the button was missing even with Jupiter installed. The
 * registration below is the whole of the fix; the screen it returns is whichever optional front end is
 * installed (see {@link MerlinConfigFrontend}).
 *
 * <h2>Registered only when there is something to show</h2>
 *
 * <p>The extension point is registered with FML's supplier overload, but the decision is taken here and not
 * inside the factory: with neither front end installed nothing is registered, so the mod list shows no button
 * instead of a button that opens nothing. That is also why {@link MerlinConfigFrontend#available()} is asked
 * first - it only looks at the class path and never throws.
 *
 * <h2>Client only</h2>
 *
 * <p>{@link ConfigScreenHandler} is a client class. This class is therefore only referenced from a
 * {@code Dist.CLIENT} branch, and its method is wrapped so that a failure costs the button rather than the
 * game.
 */
public final class MerlinConfigEntry {

    private MerlinConfigEntry() {
    }

    /**
     * Registers the Config button, if any front end can draw a screen.
     *
     * <p>Called once, from the mod constructor. Forge collects extension points during construction, so a
     * registration made later would be ignored.
     */
    public static void register() {
        if (!MerlinConfigFrontend.available()) {
            Constants.LOG.info("[MerlinLib] neither Cloth Config nor Jupiter is installed; the switches live in "
                    + "config/MerlinLib/client.toml and server.toml");
            return;
        }
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
                        Screen screen = MerlinConfigFrontend.open(parent);
                        // Never return null to Forge: the mod list would then show a dead button.
                        return screen == null ? parent : screen;
                    }));
            Constants.LOG.info("[MerlinLib] the mod list's Config button is registered ({})",
                    MerlinConfigFrontend.cloth() ? "Cloth Config" : "Jupiter");
        } catch (Throwable failure) {
            Constants.LOG.warn("[MerlinLib] the mod list's Config button could not be registered; "
                    + "the switches stay editable in the toml files", failure);
        }
    }
}
