package com.huziyang520.merlinlib.client;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * NeoForge's own configuration page, with the join notice section turned into the row that opens
 * MerlinLib's notice page, where one switch per registered mod is listed.
 */
final class MerlinConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {

    /** Prefix of the translation keys of the configuration screens. */
    private static final String KEY_PREFIX = "merlinlib.configuration.";

    /** The section whose content is MerlinLib's own page: its rows depend on what other mods registered. */
    private static final String NOTICES = "notices";

    MerlinConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig config, Component title) {
        super(parent, type, config, title);
    }

    /**
     * Keeps every section NeoForge builds, except the join notice one.
     *
     * <p>The override is here rather than on a section page because the loader builds the page of a section
     * itself, from the spec alone: a subclass of the section screen only ever sees the sections, and the one
     * page whose rows cannot be read from a spec is exactly this one. The global switch that used to be the
     * only row of that page is the first row of the page this opens, so nothing becomes unreachable.
     *
     * @param key        the section name, as the spec spells it
     * @param subconfig  the values directly inside the section
     * @param subsection the section itself
     * @return the row to show for it
     */
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
