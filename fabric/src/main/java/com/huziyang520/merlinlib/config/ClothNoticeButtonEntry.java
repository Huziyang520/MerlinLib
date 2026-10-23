package com.huziyang520.merlinlib.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * A Cloth Config row that is one button instead of one value, with the label on the left and the
 * button the caller built on the right. It holds no value and nothing to reset.
 */
final class ClothNoticeButtonEntry extends TooltipListEntry<Boolean> {

    /** Width the button prefers; a narrow panel shrinks it instead of overflowing. */
    private static final int BUTTON_WIDTH = 150;

    private final Button button;
    private final List<AbstractWidget> widgets;

    /**
     * @param fieldName the label drawn on the left of the row
     * @param button    the button placed on the right of the row
     */
    ClothNoticeButtonEntry(Component fieldName, Button button) {
        super(fieldName, () -> Optional.<Component[]>empty());
        this.button = button;
        this.widgets = List.of(button);
    }

    /** @return {@code false}; this row holds no value, and Cloth only asks while saving other rows */
    @Override
    public Boolean getValue() {
        return Boolean.FALSE;
    }

    /** @return {@code empty}; nothing to reset, which is also what hides the reset button */
    @Override
    public Optional<Boolean> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public int getItemHeight() {
        return 24;
    }

    /**
     * Places and draws the button, then draws the label beside it.
     *
     * <p>The geometry comes in on every frame rather than being cached by the widget, which is how this
     * Cloth version lays its list out: a row that keeps its own coordinates ends up in the wrong place as
     * soon as the window is resized or the list is scrolled.
     *
     * @param graphics     the render state extractor
     * @param index        the row index in the list
     * @param y            the top of the row
     * @param x            the left of the entry
     * @param entryWidth   the width available to the entry
     * @param entryHeight  the height of the row
     * @param mouseX       the cursor x
     * @param mouseY       the cursor y
     * @param hovered      whether the row is under the cursor
     * @param delta        the partial tick
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth,
                                   int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        super.extractRenderState(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        int width = Math.min(BUTTON_WIDTH, Math.max(80, entryWidth / 2));
        int height = 20;
        this.button.setX(x + entryWidth - width);
        this.button.setY(y + (entryHeight - height) / 2);
        this.button.setWidth(width);
        this.button.setHeight(height);
        this.button.extractRenderState(graphics, mouseX, mouseY, delta);

        Font font = Minecraft.getInstance().font;
        graphics.text(font, getFieldName(), x, this.button.getY() + 6, 0xFFFFFFFF, true);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.widgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.widgets;
    }
}
