package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ForgeConfig;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Registers the mod list's Config button: Forge draws it only for a mod that registered a
 * {@code ConfigScreenHandler.ConfigScreenFactory}, so that extension point is what this class adds.
 */
public final class MerlinConfigEntry {

    private MerlinConfigEntry() {
    }

    /**
     * Registers the Config button, if a front end was installed that can draw a screen.
     *
     * <p>Nothing is registered without one, so the mod list shows no button rather than a dead one. Called
     * from the mod constructor, because Forge collects extension points while the mod is being built.
 */
    public static void register() {
        if (!ForgeConfig.available()) {
            Constants.LOG.info("[MerlinLib] no configuration screen is installed; the switches stay editable "
                    + "in config/MerlinLib/client.toml");
            return;
        }
        try {
            ModLoadingContext.get().registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> {
                        Screen screen = ConfigScreens.create(parent);
                        // Never return null to Forge: the mod list would then show a dead button.
                        return screen == null ? parent : screen;
                    }));
            Constants.LOG.info("[MerlinLib] the mod list's Config button is registered");
        } catch (Throwable failure) {
            Constants.LOG.warn("[MerlinLib] the mod list's Config button could not be registered", failure);
        }
    }
}
