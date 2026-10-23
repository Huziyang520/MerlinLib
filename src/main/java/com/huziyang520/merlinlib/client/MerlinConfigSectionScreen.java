package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.List;

/**
 * One config page of NeoForge's own configuration screen, plus the row that opens the join notice page.
 *
 * <p>On 1.21.1 {@code ConfigurationScreen} is {@code final} but its inner
 * {@code ConfigurationScreen$ConfigurationSectionScreen} is not, and it declares the public constructor
 * {@code (Screen, ModConfig.Type, ModConfig, Component)} that this class forwards to. {@code addOptions()} is
 * {@code protected} and is called from {@code OptionsSubScreen.addContents()} after the list field is filled,
 * so a row can be appended there.
 *
 * <p>26.x has {@code OptionsList#addBig(AbstractWidget)} for that row. 1.21.1's {@code addBig} only accepts an
 * {@code OptionInstance}, so the equivalent here is {@code addSmall(List)} with a single widget, which the
 * vanilla list lays out as one full width row of its own.
 */
final class MerlinConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    MerlinConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    @Override
    protected void addOptions() {
        super.addOptions();
        this.list.addSmall(List.of(Button.builder(
                        Component.translatable("merlinlib.configuration.notices.open"),
                        button -> MerlinScreens.openNoticesScreen(this))
                .width(Button.DEFAULT_WIDTH)
                .build()));
    }
}
