package com.huziyang520.merlinlib.fabric;

import com.huziyang520.merlinlib.platform.config.ClothSupport;
import com.huziyang520.merlinlib.platform.config.JupiterSupport;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu integration: gives MerlinLib the config button on the Fabric mods list screen.
 *
 * <p>Cloth Config answers first, Jupiter second: either is enough for a button, since both draw the same
 * switches from the same store. With neither installed the factory answers {@code null}, which is Mod Menu's way
 * of saying a mod has no screen. Nothing here mentions a Cloth Config or a Jupiter type - both screens are built
 * through their support class, which loads them by name.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        ConfigScreenFactory<Screen> factory = parent -> {
            Screen cloth = ClothSupport.openScreen(parent);
            return cloth != null ? cloth : JupiterSupport.openScreen(parent);
        };
        return factory;
    }
}
