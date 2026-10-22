package com.huziyang520.merlinlib.fabric;

import com.huziyang520.merlinlib.tools.ui.ConfigScreens;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Mod Menu integration: gives MerlinLib the config button on the Fabric mods list screen.
 *
 * <p>The button opens whatever screen the platform installed - a Cloth Config screen when that mod is
 * present, otherwise a Jupiter screen, otherwise nothing - and answers {@code null} when there is none,
 * which is how Mod Menu hides the button rather than opening an empty one. MerlinLib itself draws no
 * configuration interface.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            if (!ConfigScreens.available()) {
                return null;
            }
            return ConfigScreens.create(parent);
        };
    }
}
