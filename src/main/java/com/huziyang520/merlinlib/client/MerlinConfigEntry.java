// Registers the mod list's Config button: Forge draws it only for a mod that registered a ...

package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ForgeConfig;
import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class MerlinConfigEntry {
    private MerlinConfigEntry() {
    }

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
                        return screen == null ? parent : screen;
                    }));
            Constants.LOG.info("[MerlinLib] the mod list's Config button is registered");
        } catch (Throwable failure) {
            Constants.LOG.warn("[MerlinLib] the mod list's Config button could not be registered", failure);
        }
    }
}
