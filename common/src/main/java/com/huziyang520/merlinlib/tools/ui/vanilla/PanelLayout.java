package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A screen's panel rectangle plus the row cursor used to lay its content out.
 *
 * <p>A panel is defined by the size of its <em>content</em>: the frame, the padding and the centring
 * are derived from it. That is the whole point, because it lets a screen size itself from measured
 * text (see {@link VanillaUi#measureWidth}) rather than from a constant that only fits one language.
 *
 * <p>Rows are handed out by {@link #row(int)} in the order they are requested, so a screen never
 * computes a y coordinate itself and rows can never overlap.
 */
public final class PanelLayout {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private int cursor;

    private PanelLayout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cursor = y + VanillaUi.PADDING;
    }

    /**
     * Builds a centred panel around a content box of the given size.
     *
     * <p>The panel is clamped to the window before it is centred: a screen that asks for more than the
     * window has - which is normal at a high GUI scale - would otherwise be placed at a negative coordinate
     * and simply run off the screen. Screens that can shrink their content use
     * {@link VanillaScreen#contentHeightLimit()} to ask for a height that fits in the first place; this is
     * the safety net for the ones that cannot.
     *
     * @param screen         the screen, for the window size
     * @param contentWidth   width available to the content
     * @param contentHeight  height available to the content
     * @return the layout, with the row cursor at the top of the content
     */
    public static PanelLayout centered(Screen screen, int contentWidth, int contentHeight) {
        int margin = 4;
        int width = Math.min(contentWidth + VanillaUi.PADDING * 2, Math.max(60, screen.width - margin * 2));
        int height = Math.min(contentHeight + VanillaUi.PADDING * 2, Math.max(60, screen.height - margin * 2));
        int x = Math.max(margin, (screen.width - width) / 2);
        int y = Math.max(margin, (screen.height - height) / 2);
        return new PanelLayout(x, y, width, height);
    }

    /**
     * The content width a panel needs for a labelled column of controls.
     *
     * @param font         the font
     * @param labels       the labels sharing the left column
     * @param controlWidth the widest control in the right column
     * @param minContent   a lower bound
     * @return the content width in pixels
     */
    public static int labelColumn(Font font, List<Component> labels, int controlWidth, int minContent) {
        return VanillaUi.measureWidth(font, labels, controlWidth, minContent);
    }

    /** @return the panel's left edge */
    public int x() {
        return this.x;
    }

    /** @return the panel's top edge */
    public int y() {
        return this.y;
    }

    /** @return the panel width, including the frame */
    public int width() {
        return this.width;
    }

    /** @return the panel height, including the frame */
    public int height() {
        return this.height;
    }

    /** @return the content's left edge */
    public int left() {
        return this.x + VanillaUi.PADDING;
    }

    /** @return the content's right edge */
    public int right() {
        return this.x + this.width - VanillaUi.PADDING;
    }

    /** @return the content width */
    public int contentWidth() {
        return this.width - VanillaUi.PADDING * 2;
    }

    /** @return the content's top edge */
    public int top() {
        return this.y + VanillaUi.PADDING;
    }

    /** @return the content's bottom edge */
    public int contentBottom() {
        return this.y + this.height - VanillaUi.PADDING;
    }

    /** @return the y of the last full widget row that still fits in the content */
    public int lastRowY() {
        return contentBottom() - VanillaUi.WIDGET_HEIGHT;
    }

    /** @return the current row cursor */
    public int cursor() {
        return this.cursor;
    }

    /**
     * Moves the row cursor.
     *
     * @param y the new cursor position
     */
    public void cursorTo(int y) {
        this.cursor = y;
    }

    /**
     * Reserves a row and returns its top edge.
     *
     * @param height the height to reserve, usually {@link VanillaUi#WIDGET_HEIGHT}
     * @return the top edge of the reserved row
     */
    public int row(int height) {
        int top = this.cursor;
        this.cursor += height;
        return top;
    }

    /**
     * Reserves the standard widget row.
     *
     * @return the top edge of the reserved row
     */
    public int row() {
        return row(VanillaUi.WIDGET_HEIGHT);
    }

    /**
     * Reserves a gap without drawing anything.
     *
     * @param pixels the gap height
     */
    public void gap(int pixels) {
        this.cursor += pixels;
    }

    /**
     * The width of one button in an evenly split row.
     *
     * @param count how many buttons share the content width
     * @return the width of each button
     */
    public int sliceWidth(int count) {
        return (contentWidth() - VanillaUi.GAP * (count - 1)) / count;
    }

    /**
     * The x of one button in an evenly split row.
     *
     * @param index the button index, starting at 0
     * @param count how many buttons share the content width
     * @return the left edge of that button
     */
    public int sliceX(int index, int count) {
        return left() + index * (sliceWidth(count) + VanillaUi.GAP);
    }

    /**
     * Draws the panel itself. Called by {@link VanillaScreen} from the background pass.
     *
     * @param graphics the render state extractor
     */
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        VanillaUi.panel(graphics, this.x, this.y, this.width, this.height);
    }
}
