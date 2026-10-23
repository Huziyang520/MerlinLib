package com.huziyang520.merlinlib.client;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.List;

/**
 * One config page of NeoForge's own configuration screen, plus the row that opens the join notice
 * page.
 *
 * <p>{@code ConfigurationSectionScreen} is inheritable on this branch exactly as on 26.3 -
 * {@code javap} on 26.1.2.94 shows it extends {@code OptionsSubScreen}, keeps the protected
 * {@code addOptions()} hook and the protected {@code list} field. Only the way a raw widget is put
 * into that list differs: 26.1.2's {@code OptionsList.addBig} takes an {@code OptionInstance} and
 * nothing else, so the button goes in through {@code addSmall(List)}, which is the overload that
 * accepts widgets. The row then spans the list width, the same place {@code addBig} puts it on 26.3.
 */
final class MerlinConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    MerlinConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    @Override
    protected void addOptions() {
        super.addOptions();
        AbstractWidget notices = Button.builder(Component.translatable("merlinlib.configuration.notices.open"),
                        button -> MerlinScreens.openNoticesScreen(this))
                .width(Button.DEFAULT_WIDTH)
                .build();
        this.list.addSmall(List.of(notices));
    }
}
