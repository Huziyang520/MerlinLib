package com.huziyang520.merlinlib.config;

import com.iafenvoy.jupiter.config.entry.IntegerEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigEntry;
import com.iafenvoy.jupiter.config.interfaces.ConfigMetaProvider;
import com.iafenvoy.jupiter.config.type.ConfigType;
import com.iafenvoy.jupiter.render.screen.WidgetBuilderManager;
import com.iafenvoy.jupiter.render.widget.WidgetBuilder;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * A Jupiter row for a whole number: a field with a - and a + beside it, so a range that reaches the
 * integer limit can still be stepped.
 */
final class JupiterNumberEntry {

    /** The type every instance of this row shares; also the registry key of its widget builder. */
    private static final ConfigType<Integer> TYPE = new ConfigType<>(ConfigType.Type.SINGLE);

    /** Width of one step button. */
    private static final int STEP = 16;

    /** Registration must happen once; Jupiter keeps the builder in a static map. */
    private static boolean registered;

    private JupiterNumberEntry() {
    }

    /**
     * Builds the row and, on the first call, teaches Jupiter how to draw it.
     *
     * @param label   the translation of the option name
     * @param tooltip the translation of its description
     * @param key     the key the mirror file stores, spelled {@code section.option}
     * @param value   the current value
     * @param min     the lowest accepted value
     * @param max     the highest accepted value
     * @param sink    receives every value the player picks
     * @return the entry to add to a tab
     */
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

    /** The row itself: a bounded integer, with our own type so our own widget is picked. */
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

    /** Draws the field and the two step buttons, and keeps them on the row Jupiter moves them to. */
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

        /**
         * @param text      the label of the button
         * @param x         its left edge
         * @param y         its top edge
         * @param height    its height, the same as the field
         * @param direction -1 to step down, 1 to step up
         * @return the button, already added to the row
         */
        private Button button(Context context, Component text, int x, int y, int height, int direction) {
            Button widget = Button.builder(text, pressed -> this.step(direction)).bounds(x, y, STEP, height).build();
            context.addWidget(widget);
            return widget;
        }

        /**
         * Moves the value by one step, or by ten while shift is held - the same step the item editor uses, so
         * a large range is reachable without typing the number again.
         *
         * @param direction -1 to step down, 1 to step up
         */
        private void step(int direction) {
            int current = this.config.getValue() == null ? 0 : this.config.getValue();
            int step = Screen.hasShiftDown() ? 10 : 1;
            this.config.setValue(Math.max(low(), Math.min(high(), current + direction * step)));
            this.refresh();
        }

        /**
         * Takes the value the player typed, and marks the row as unsaveable while it is not a number in
         * range, so a half typed number cannot be written to the file.
         *
         * @param text the text in the field
         */
        private void onTyped(String text) {
            try {
                int typed = Integer.parseInt(text.trim());
                boolean inRange = typed >= low() && typed <= high();
                this.canSave = inRange;
                this.field.setTextColor(inRange ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
                if (inRange) {
                    this.config.setValue(typed);
                }
            } catch (NumberFormatException notANumber) {
                // An empty or partial field is not an error yet: the player is still typing.
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

        /** @return the lowest accepted value, from the entry itself */
        private int low() {
            Integer min = this.config instanceof IntegerEntry entry ? entry.getMinValue() : null;
            return min == null ? Integer.MIN_VALUE : min;
        }

        /** @return the highest accepted value, from the entry itself */
        private int high() {
            Integer max = this.config instanceof IntegerEntry entry ? entry.getMaxValue() : null;
            return max == null ? Integer.MAX_VALUE : max;
        }
    }
}
