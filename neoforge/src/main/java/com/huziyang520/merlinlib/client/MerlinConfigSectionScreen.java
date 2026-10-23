package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * One config page of NeoForge's own configuration screen, plus the row that opens the join notice
 * page.
 */
final class MerlinConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    MerlinConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    @Override
    protected void addOptions() {
        super.addOptions();
        this.list.addBig(Button.builder(Component.translatable("merlinlib.configuration.notices.open"),
                        button -> MerlinScreens.openNoticesScreen(this))
                .width(Button.DEFAULT_WIDTH)
                .build());
    }
}
