// A Jupiter row for a whole number: a field with a - and a + beside it, so a range that reaches the integer limit can ...

package com.huziyang520.merlinlib.config;

import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigMetaProvider;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.render.screen.WidgetBuilderManager;
import com.iafenvoy.jupiter.render.widget.WidgetBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

final class JupiterNumberEntry {
    private static final ConfigType<Integer> TYPE = new ConfigType<>(ConfigType.Type.SINGLE);
    private static final int STEP = 16;
    private static boolean registered;

    private JupiterNumberEntry() {
    }

    static IntegerEntry create(Component label, Component tooltip, String key, int value, int min, int max,
                               Consumer<Integer> sink) {
        if (!registered) {
            registered = true;
            WidgetBuilderManager.register(TYPE, StepBuilder::new);
        }
        IntegerEntry.Builder builder = IntegerEntry.builder(label, value)
                .key(key)
                .min(min)
                .max(max)
                .tooltip(tooltip)
                .callback((chosen, initial, reset) -> sink.accept(chosen));
        return new Entry(builder);
    }

    private static final class Entry extends IntegerEntry {
        Entry(IntegerEntry.Builder builder) {
            super(builder);
        }

        @Override
        public ConfigType<Integer> getType() {
            return TYPE;
        }

        @Override
        public ConfigEntry<Integer> newInstance() {
            return new Entry(new IntegerEntry.Builder(this));
        }
    }

    private static final class StepBuilder extends WidgetBuilder<Integer> {
        private EditBox field;
        private Button minus;
        private Button plus;

        StepBuilder(ConfigMetaProvider provider, ConfigEntry<Integer> config) {
            super(provider, config);
        }

        @Override
        public void addCustomElements(Context context, int x, int y, int width, int height) {
            int fieldWidth = Math.max(40, width - 2 * STEP - 4);
            this.field = new EditBox(this.minecraft.font, x, y, fieldWidth, height,
                    this.config.getName() == null ? Component.empty() : this.config.getName());
            this.field.setMaxLength(11);
            this.field.setValue(String.valueOf(this.config.getValue()));
            this.field.setResponder(this::onTyped);
            context.addWidget(this.field);
            this.minus = button(context, Component.literal("-"), x + fieldWidth + 2, y, height, -1);
            this.plus = button(context, Component.literal("+"), x + fieldWidth + 2 + STEP + 2, y, height, 1);
        }

        private Button button(Context context, Component text, int x, int y, int height, int direction) {
            Button widget = Button.builder(text, pressed -> this.step(direction)).bounds(x, y, STEP, height).build();
            context.addWidget(widget);
            return widget;
        }

        private void step(int direction) {
            int current = this.config.getValue() == null ? 0 : this.config.getValue();
            int step = Minecraft.getInstance().hasShiftDown() ? 10 : 1;
            this.config.setValue(Math.max(low(), Math.min(high(), current + direction * step)));
            this.refresh();
        }

        private void onTyped(String text) {
            try {
                int typed = Integer.parseInt(text.trim());
                this.canSave = typed >= low() && typed <= high();
                this.field.setTextColor(this.canSave ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
                if (this.canSave) {
                    this.config.setValue(typed);
                }
            } catch (NumberFormatException notANumber) {
                this.canSave = text.isBlank();
                this.field.setTextColor(0xFFFF5555);
            }
        }

        @Override
        public void updateCustom(boolean visible, int y) {
            if (this.field != null) {
                this.field.visible = visible;
                this.field.active = visible;
                this.field.setY(y);
            }
            for (Button widget : new Button[] {this.minus, this.plus}) {
                if (widget != null) {
                    widget.visible = visible;
                    widget.setY(y);
                }
            }
        }

        @Override
        public void refresh() {
            if (this.field != null) {
                this.field.setValue(String.valueOf(this.config.getValue()));
                this.field.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                this.canSave = true;
            }
        }

        private int low() {
            return this.config instanceof IntegerEntry entry && entry.getMinValue() != null
                    ? entry.getMinValue()
                    : Integer.MIN_VALUE;
        }

        private int high() {
            return this.config instanceof IntegerEntry entry && entry.getMaxValue() != null
                    ? entry.getMaxValue()
                    : Integer.MAX_VALUE;
        }
    }
}
