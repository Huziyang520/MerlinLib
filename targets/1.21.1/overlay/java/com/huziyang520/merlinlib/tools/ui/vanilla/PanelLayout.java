// A screen's panel rectangle plus the row cursor used to lay its content out.

package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

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

    public static PanelLayout centered(Screen screen, int contentWidth, int contentHeight) {
        int margin = 4;
        int width = Math.min(contentWidth + VanillaUi.PADDING * 2, Math.max(60, screen.width - margin * 2));
        int height = Math.min(contentHeight + VanillaUi.PADDING * 2, Math.max(60, screen.height - margin * 2));
        int x = Math.max(margin, (screen.width - width) / 2);
        int y = Math.max(margin, (screen.height - height) / 2);
        return new PanelLayout(x, y, width, height);
    }

    public static int labelColumn(Font font, List<Component> labels, int controlWidth, int minContent) {
        return VanillaUi.measureWidth(font, labels, controlWidth, minContent);
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

    public int left() {
        return this.x + VanillaUi.PADDING;
    }

    public int right() {
        return this.x + this.width - VanillaUi.PADDING;
    }

    public int contentWidth() {
        return this.width - VanillaUi.PADDING * 2;
    }

    public int top() {
        return this.y + VanillaUi.PADDING;
    }

    public int contentBottom() {
        return this.y + this.height - VanillaUi.PADDING;
    }

    public int lastRowY() {
        return contentBottom() - VanillaUi.WIDGET_HEIGHT;
    }

    public int cursor() {
        return this.cursor;
    }

    public void cursorTo(int y) {
        this.cursor = y;
    }

    public int row(int height) {
        int top = this.cursor;
        this.cursor += height;
        return top;
    }

    public int row() {
        return row(VanillaUi.WIDGET_HEIGHT);
    }

    public void gap(int pixels) {
        this.cursor += pixels;
    }

    public int sliceWidth(int count) {
        return (contentWidth() - VanillaUi.GAP * (count - 1)) / count;
    }

    public int sliceX(int index, int count) {
        return left() + index * (sliceWidth(count) + VanillaUi.GAP);
    }

    public void draw(GuiGraphics graphics) {
        VanillaUi.panel(graphics, this.x, this.y, this.width, this.height);
    }
}
