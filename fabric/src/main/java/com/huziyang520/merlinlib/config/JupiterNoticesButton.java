package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigMetaProvider;
import com.iafenvoy.jupiter.config.interfaces.ValueChangeCallback;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.render.screen.WidgetBuilderManager;
import com.iafenvoy.jupiter.render.widget.WidgetBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * A Jupiter row that is one button instead of one value.
 *
 * <h2>Why this needs the widget registry</h2>
 *
 * <p>Jupiter renders every entry through a {@code WidgetBuilder} chosen by the entry's {@link ConfigType},
 * and its own entry types cover booleans, numbers, strings, identifiers, enums and lists - no button. The
 * library does expose the registry behind that choice ({@code WidgetBuilderManager.register}), so a button
 * row is a custom type plus a builder for it. The type is a fresh {@code ConfigType} instance rather than a
 * shared one, because the registry is keyed by instance: taking an existing type would replace how every
 * boolean is drawn.
 *
 * <p>The row stores nothing. Jupiter still asks it for a value while it saves the mirror file, which is why
 * {@link #getCodec()} answers with a boolean that is simply written back - the mirror of
 * {@code client.toml} never carries a notice switch.
 */
final class JupiterNoticesButton {

    /** The type every instance of this row shares; also the registry key of its widget builder. */
    private static final ConfigType<Boolean> TYPE = new ConfigType<>(ConfigType.Type.DUMMY);

    /** Registration must happen once; Jupiter keeps the builder in a static map. */
    private static boolean registered;

    private JupiterNoticesButton() {
    }

    /**
     * Builds the row and, on the first call, teaches Jupiter how to draw it.
     *
     * @return the entry to add to a tab
     */
    static synchronized ConfigEntry<Boolean> create() {
        if (!registered) {
            registered = true;
            WidgetBuilderManager.register(TYPE, ButtonBuilder::new);
        }
        return new Entry();
    }

    /** The row: a name, a tooltip, and nothing to store. */
    private static final class Entry implements ConfigEntry<Boolean> {

        @Override
        public ConfigType<Boolean> getType() {
            return TYPE;
        }

        @Override
        public String getKey() {
            return "notices";
        }

        @Override
        public Component getName() {
            return Component.translatable("gui.merlinlib.notices.open");
        }

        @Override
        public Component getTooltip() {
            return Component.translatable("gui.merlinlib.notices.hint");
        }

        @Override
        public ConfigEntry<Boolean> newInstance() {
            return new Entry();
        }

        @Override
        public void registerCallback(ValueChangeCallback<Boolean> callback) {
            // Nothing changes here: the button opens a screen, it does not hold a value to react to.
        }

        @Override
        public Boolean getValue() {
            return Boolean.FALSE;
        }

        @Override
        public Boolean getDefaultValue() {
            return Boolean.FALSE;
        }

        @Override
        public void setValue(Boolean value) {
            // Deliberately empty; see the class comment.
        }

        @Override
        public Codec<Boolean> getCodec() {
            return Codec.BOOL;
        }

        @Override
        public void reset() {
            // Nothing to reset.
        }

        @Override
        public boolean canReset() {
            return false;
        }
    }

    /** Draws the row as one full width button. */
    private static final class ButtonBuilder extends WidgetBuilder<Boolean> {

        ButtonBuilder(ConfigMetaProvider provider, ConfigEntry<Boolean> config) {
            super(provider, config);
        }

        @Override
        public void addCustomElements(Context context, int x, int y, int width, int height) {
            // The parent screen is what the button returns to, so the notice page closes back into Jupiter
            // instead of back into the world.
            Button button = Button.builder(this.config.getName(),
                            pressed -> MerlinScreens.openNoticesScreen(context.parent()))
                    .bounds(x, y, width, 20)
                    .build();
            context.addWidget(button);
        }

        @Override
        public void updateCustom(boolean canSave, int width) {
            // The button is always usable: it opens a screen and never depends on a staged value.
        }

        @Override
        public void refresh() {
            // Nothing is reloaded; the page reads the switches when it opens.
        }
    }
}
