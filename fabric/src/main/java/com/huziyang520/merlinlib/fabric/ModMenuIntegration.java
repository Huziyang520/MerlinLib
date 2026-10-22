package com.huziyang520.merlinlib.fabric;

import com.huziyang520.merlinlib.platform.config.ClothSupport;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu integration: gives MerlinLib the config button on the Fabric mods list screen.
 *
 * <p>The button opens the Cloth Config screen, and only exists while Cloth Config is installed: the factory
 * answers {@code null} otherwise, which is Mod Menu's way of saying a mod has no screen. Nothing here mentions a
 * Cloth Config type - the screen is built through {@link ClothSupport}, which loads it by name.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        ConfigScreenFactory<Screen> factory = ClothSupport::openScreen;
        return factory;
    }
}
