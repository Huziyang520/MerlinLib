// A Jupiter row that is one button instead of one value: the way into the join notice page.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.tools.ui.MerlinScreens;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigMetaProvider;
import com.iafenvoy.jupiter.config.interfaces.ValueChangeCallback;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.render.screen.WidgetBuilderManager;
import com.iafenvoy.jupiter.render.widget.builder.AbstractButtonWidgetBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class JupiterNoticesButton {
    private static final ConfigType<Boolean> TYPE = new ConfigType<>(ConfigType.Type.DUMMY);
    private static boolean registered;

    private JupiterNoticesButton() {
    }

    static synchronized ConfigEntry<Boolean> create() {
        if (!registered) {
            registered = true;
            WidgetBuilderManager.register(TYPE, ButtonBuilder::new);
        }
        return new Entry();
    }

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
            return Component.translatable(ClientOptions.noticesKey());
        }

        @Override
        public Component getTooltip() {
            return Component.translatable(ClientOptions.noticesKey() + ".tooltip");
        }

        @Override
        public ConfigEntry<Boolean> newInstance() {
            return new Entry();
        }

        @Override
        public void registerCallback(ValueChangeCallback<Boolean> callback) {
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
        }

        @Override
        public Codec<Boolean> getCodec() {
            return Codec.BOOL;
        }

        @Override
        public void reset() {
        }

        @Override
        public boolean canReset() {
            return false;
        }
    }

    private static final class ButtonBuilder extends AbstractButtonWidgetBuilder<Boolean> {
        ButtonBuilder(ConfigMetaProvider provider, ConfigEntry<Boolean> config) {
            super(provider, config, config::getName);
        }

        @Override
        protected Button createButton(Context context, int x, int y, int width, int height) {
            Button.Builder builder = Button.builder(this.config.getName(),
                            pressed -> MerlinScreens.openNoticesScreen(context.parent()))
                    .bounds(x, y, width, height);
            Component tooltip = this.config.getTooltip();
            if (tooltip != null) {
                builder = builder.tooltip(Tooltip.create(tooltip));
            }
            return builder.build();
        }
    }
}
