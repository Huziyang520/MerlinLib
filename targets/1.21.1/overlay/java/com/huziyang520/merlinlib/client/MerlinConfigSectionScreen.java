// NeoForge's own configuration page, with the join notice section turned into the row that opens MerlinLib's notice page ...

package com.huziyang520.merlinlib.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

final class MerlinConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {
    private static final String KEY_PREFIX = "merlinlib.configuration.";
    private static final String NOTICES = "notices";

    MerlinConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    @Override
    protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        if (!NOTICES.equals(key)) {
            return super.createSection(key, subconfig, subsection);
        }
        Component name = Component.translatable(KEY_PREFIX + NOTICES);
        Component tooltip = Component.translatable(KEY_PREFIX + NOTICES + ".open.tooltip");
        Button button = Button.builder(Component.translatable(KEY_PREFIX + NOTICES + ".open"),
                        pressed -> MerlinScreens.openNoticesScreen(this))
                .tooltip(Tooltip.create(tooltip))
                .width(Button.DEFAULT_WIDTH)
                .build();
        return new Element(name, tooltip, button, false);
    }
}
