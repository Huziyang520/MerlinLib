// A scroll region with the vanilla scrollbar, including dragging.

package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

public final class ScrollArea {
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int rowHeight;
    private int contentHeight;
    private int scroll;
    private int dragGrab = Integer.MIN_VALUE;

    public ScrollArea(int x, int y, int width, int height, int rowHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rowHeight = Math.max(1, rowHeight);
    }

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int rowHeight() {
        return this.rowHeight;
    }

    public int rowWidth() {
        return this.width - VanillaUi.SCROLLBAR_WIDTH - 2;
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
        this.scroll = snap(this.scroll);
    }

    public int scroll() {
        return this.scroll;
    }

    public int firstRow() {
        return this.scroll / this.rowHeight;
    }

    public int maxScroll() {
        return Math.max(0, snap(this.contentHeight - this.height));
    }

    public boolean scrollable() {
        return maxScroll() > 0;
    }

    public int rowTop(int index) {
        return this.y + index * this.rowHeight - this.scroll;
    }

    public boolean rowVisible(int index) {
        int top = index * this.rowHeight - this.scroll;
        return top + this.rowHeight > 0 && top < this.height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX < this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!scrollable()) {
            return;
        }
        VanillaUi.scroller(graphics, this.x + this.width - VanillaUi.SCROLLBAR_WIDTH, this.y,
                this.height, handleTop(), handleHeight());
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        if (!scrollable() || event.button() != 0) {
            return false;
        }
        int barX = this.x + this.width - VanillaUi.SCROLLBAR_WIDTH;
        if (event.x() < barX || event.x() >= this.x + this.width
                || event.y() < this.y || event.y() >= this.y + this.height) {
            return false;
        }
        int top = handleTop();
        int handle = handleHeight();
        if (event.y() >= top && event.y() < top + handle) {
            this.dragGrab = (int) event.y() - top;
        } else {
            this.dragGrab = handle / 2;
            scrollTo((int) event.y() - this.dragGrab);
        }
        return true;
    }

    public boolean mouseDragged(MouseButtonEvent event) {
        if (this.dragGrab == Integer.MIN_VALUE) {
            return false;
        }
        scrollTo((int) event.y() - this.dragGrab);
        return true;
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        boolean dragging = this.dragGrab != Integer.MIN_VALUE;
        this.dragGrab = Integer.MIN_VALUE;
        return dragging;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!scrollable() || amount == 0.0D || !contains(mouseX, mouseY)) {
            return false;
        }
        int delta = (int) Math.signum(amount) * this.rowHeight;
        this.scroll = snap(this.scroll - delta);
        return true;
    }

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
