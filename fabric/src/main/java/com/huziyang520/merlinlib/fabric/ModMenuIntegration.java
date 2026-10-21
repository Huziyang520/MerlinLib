package com.huziyang520.merlinlib.fabric;

import com.huziyang520.merlinlib.tools.ui.MerlinConfigScreen;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Mod Menu integration: gives MerlinLib the config button on the Fabric mods list screen.
 */
@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public com.terraformersmc.modmenu.api.ConfigScreenFactory<MerlinConfigScreen> getModConfigScreenFactory() {
        return parent -> new MerlinConfigScreen(parent);
    }
}
