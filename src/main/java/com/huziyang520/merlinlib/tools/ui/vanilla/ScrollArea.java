package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A scroll region with the vanilla scrollbar, including dragging.
 *
 * <p>Content is a stack of equal height rows, so the region never scrolls to a half row: the offset is
 * always a multiple of the row height, exactly like a vanilla list. Rows are drawn by the screen at
 * {@link #rowTop(int)}, while their widgets stay real widgets: the screen repositions them whenever the
 * scroll changes and hides the ones that scrolled out. That keeps clicking, hovering and Tab focus
 * working while the list scrolls, which a purely painted list would break.
 *
 * <p>The scrollbar itself is the same colours vanilla draws a list scrollbar with, so it looks and drags
 * like a vanilla list: click the track to jump, drag the handle with the left button, or use the wheel over
 * the region.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>This is where the input rewrite lands, and it is a real signature change for callers. 26.3 delivered
 * a whole mouse event as one object ({@code MouseButtonEvent}, with {@code x()}, {@code y()} and
 * {@code button()} accessors) and a {@code GuiGraphicsExtractor} for drawing. This version passes the three
 * pieces of a mouse event as separate arguments, so all four mouse methods below had to be rewritten:
 *
 * <table border="1">
 *   <caption>mouse method mapping</caption>
 *   <tr><th>26.3</th><th>1.20.1</th></tr>
 *   <tr><td>{@code mouseClicked(MouseButtonEvent)}</td>
 *       <td>{@code mouseClicked(double, double, int)}</td></tr>
 *   <tr><td>{@code mouseDragged(MouseButtonEvent)}</td>
 *       <td>{@code mouseDragged(double, double, int, double, double)}</td></tr>
 *   <tr><td>{@code mouseReleased(MouseButtonEvent)}</td>
 *       <td>{@code mouseReleased(double, double, int)}</td></tr>
 *   <tr><td>{@code render(GuiGraphicsExtractor)}</td>
 *       <td>{@code render(GuiGraphics)}</td></tr>
 * </table>
 *
 * <p>{@link #mouseDragged} deliberately takes the full five argument vanilla shape - including the two
 * drag deltas it does not read - so that a screen can forward its own
 * {@code GuiEventListener#mouseDragged(double, double, int, double, double)} override straight into it
 * without unpacking anything. The two unused parameters are the price of that convenience.
 *
 * <p>{@link #mouseScrolled} needed <b>no</b> change: it already took three arguments on 26.3, and
 * {@code GuiEventListener#mouseScrolled(double, double, double)} is three arguments here. The "four
 * argument scroll" this project warns about is the 1.20.2+ shape and never applied to the 26.3 line.
 */
public final class ScrollArea {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rowHeight;

    private int contentHeight;
    private int scroll;
    private int dragGrab = Integer.MIN_VALUE;

    /**
     * @param x         region left
     * @param y         region top
     * @param width     region width, the scrollbar is carved out of its right edge
     * @param height    viewport height, normally a whole number of rows
     * @param rowHeight height of one row, the scroll step
     */
    public ScrollArea(int x, int y, int width, int height, int rowHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rowHeight = Math.max(1, rowHeight);
    }

    /** @return the region's left edge */
    public int x() {
        return this.x;
    }

    /** @return the region's top edge */
    public int y() {
        return this.y;
    }

    /** @return the region width */
    public int width() {
        return this.width;
    }

    /** @return the viewport height */
    public int height() {
        return this.height;
    }

    /** @return the height of one row */
    public int rowHeight() {
        return this.rowHeight;
    }

    /** @return the width a row may occupy, leaving the scrollbar clear */
    public int rowWidth() {
        return this.width - VanillaUi.SCROLLBAR_WIDTH - 2;
    }

    /**
     * Sets how tall the content is, which decides whether a scrollbar is needed.
     *
     * @param contentHeight the total height of all rows
     */
    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        this.scroll = snap(this.scroll);
    }

    /** @return the current scroll offset in pixels, always a whole number of rows */
    public int scroll() {
        return this.scroll;
    }

    /** @return the index of the first visible row */
    public int firstRow() {
        return this.scroll / this.rowHeight;
    }

    /** @return the largest usable scroll offset */
    public int maxScroll() {
        return Math.max(0, snap(this.contentHeight - this.height));
    }

    /** @return true when the content does not fit and a scrollbar is drawn */
    public boolean scrollable() {
        return maxScroll() > 0;
    }

    /**
     * The y a row starts at once the scroll offset is applied.
     *
     * @param index the row index
     * @return the absolute y of the row
     */
    public int rowTop(int index) {
        return this.y + index * this.rowHeight - this.scroll;
    }

    /**
     * Whether a row overlaps the visible region.
     *
     * @param index the row index
     * @return true when at least part of the row is visible
     */
    public boolean rowVisible(int index) {
        int top = index * this.rowHeight - this.scroll;
        return top + this.rowHeight > 0 && top < this.height;
    }

    /** @return true when the given point is inside the viewport */
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    /**
     * Draws the scrollbar, if one is needed. Call after the rows so the bar sits on top.
     *
     * @param graphics the graphics object
     */
    public void render(GuiGraphics graphics) {
        if (!scrollable()) {
            return;
        }
        VanillaUi.scroller(graphics, this.x + this.width - VanillaUi.SCROLLBAR_WIDTH, this.y,
                this.height, handleTop(), handleHeight());
    }

    /**
     * Handles a click on the track or the handle.
     *
     * <p><b>What changed / 1.20.1 note:</b> takes the cursor and the button separately rather than one
     * {@code MouseButtonEvent}. The button test is unchanged: 0 is the left button on both versions.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button, 0 for the left one
     * @return true when the scrollbar consumed the click
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!scrollable() || button != 0) {
            return false;
        }
        int barX = this.x + this.width - VanillaUi.SCROLLBAR_WIDTH;
        if (mouseX < barX || mouseX >= this.x + this.width
                || mouseY < this.y || mouseY >= this.y + this.height) {
            return false;
        }
        int top = handleTop();
        int handle = handleHeight();
        if (mouseY >= top && mouseY < top + handle) {
            this.dragGrab = (int) mouseY - top;
        } else {
            this.dragGrab = handle / 2;
            scrollTo((int) mouseY - this.dragGrab);
        }
        return true;
    }

    /**
     * Continues a drag started by {@link #mouseClicked}.
     *
     * <p><b>What changed / 1.20.1 note:</b> the two trailing parameters are the vanilla drag deltas. This
     * region only needs the cursor position, but taking the whole vanilla signature lets a screen forward
     * its own override unchanged.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button, unused
     * @param dragX  the x delta since the last mouse event, unused
     * @param dragY  the y delta since the last mouse event, unused
     * @return true when a drag is in progress
     */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragGrab == Integer.MIN_VALUE) {
            return false;
        }
        scrollTo((int) mouseY - this.dragGrab);
        return true;
    }

    /**
     * Ends a drag.
     *
     * <p><b>What changed / 1.20.1 note:</b> takes the cursor and the button separately rather than one
     * {@code MouseButtonEvent}; neither is read, since any release ends the drag.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param button the mouse button
     * @return true when a drag was in progress
     */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean dragging = this.dragGrab != Integer.MIN_VALUE;
        this.dragGrab = Integer.MIN_VALUE;
        return dragging;
    }

    /**
     * Scrolls with the wheel while the cursor is inside the region.
     *
     * <p><b>What changed / 1.20.1 note:</b> nothing. Three arguments on 26.3, three arguments here.
     *
     * @param mouseX the cursor x
     * @param mouseY the cursor y
     * @param amount the wheel delta, positive when scrolling up
     * @return true when the region consumed the scroll
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!scrollable() || amount == 0.0D || !contains(mouseX, mouseY)) {
            return false;
        }
        int delta = (int) Math.signum(amount) * this.rowHeight;
        this.scroll = snap(this.scroll - delta);
        return true;
    }

    /**
     * Scrolls a row into view, used when a row is focused by the keyboard.
     *
     * @param index the row index
     */
    public void scrollToRow(int index) {
        int top = index * this.rowHeight;
        int bottom = top + this.rowHeight;
        if (top < this.scroll) {
            this.scroll = snap(top);
        } else if (bottom > this.scroll + this.height) {
            this.scroll = snap(bottom - this.height);
        }
    }

    private void scrollTo(int handleTop) {
        int travel = this.height - handleHeight();
        if (travel <= 0) {
            this.scroll = 0;
            return;
        }
        int relative = VanillaUi.clamp(handleTop - this.y, 0, travel);
        this.scroll = snap(relative * maxScroll() / travel);
    }

    private int snap(int pixels) {
        int snapped = pixels / this.rowHeight * this.rowHeight;
        return VanillaUi.clamp(snapped, 0, Math.max(0, this.contentHeight - this.height));
    }

    private int handleHeight() {
        if (!scrollable()) {
            return this.height;
        }
        return Math.max(VanillaUi.MIN_HANDLE_HEIGHT, this.height * this.height / this.contentHeight);
    }

    private int handleTop() {
        if (!scrollable()) {
            return this.y;
        }
        int travel = this.height - handleHeight();
        int max = maxScroll();
        return this.y + (max == 0 ? 0 : travel * this.scroll / max);
    }
}
