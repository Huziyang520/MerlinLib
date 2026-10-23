package com.huziyang520.merlinlib.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
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
    /** The child the container considers focused; this row holds one button and never keeps one. */
    private GuiEventListener focused;

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
     * Places and draws the button, then draws the label beside it. The geometry arrives on every frame
     * rather than being cached, which is how this Cloth version lays its list out.
 */
    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        int width = Math.min(BUTTON_WIDTH, Math.max(80, entryWidth / 2));
        int height = 20;
        this.button.setX(x + entryWidth - width);
        this.button.setY(y + (entryHeight - height) / 2);
        this.button.setWidth(width);
        this.button.setHeight(height);
        this.button.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(Minecraft.getInstance().font, getFieldName(), x, this.button.getY() + 6,
                0xFFFFFFFF, true);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.widgets;
    }

    @Override
    public void setFocused(GuiEventListener focused) {
        this.focused = focused;
    }

    @Override
    public GuiEventListener getFocused() {
        return this.focused;
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean dragging) {
        // The row never drags anything itself; the button inside it handles its own clicks.
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.widgets;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        // Nothing of its own to narrate: the button inside the row carries the label.
    }
}
