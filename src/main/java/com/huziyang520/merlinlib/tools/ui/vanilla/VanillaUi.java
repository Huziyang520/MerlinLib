// The vanilla look and feel of every MerlinLib screen, expressed as drawing primitives plus the few measurements screens ...

package com.huziyang520.merlinlib.tools.ui.vanilla;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import java.util.ArrayList;
import java.util.List;

public final class VanillaUi {
    public static final int WIDGET_HEIGHT = 20;
    public static final int ROW_STEP = 24;
    public static final int PADDING = 8;
    public static final int GAP = 6;
    public static final int STEP_BUTTON = 16;
    public static final int SCROLLBAR_WIDTH = 6;
    public static final int LIST_ROW_HEIGHT = 20;
    public static final int MIN_HANDLE_HEIGHT = 20;
    public static final int CORNER_RADIUS = 2;
    public static final int MAX_PANEL_WIDTH = 480;
    public static final int PANEL_OUTLINE = 0xFF000000;
    public static final int PANEL_BODY = 0xFFC6C6C6;
    public static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    public static final int PANEL_SHADOW = 0xFF555555;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_HINT = 0xFFFFFFFF;
    public static final int TEXT_DISABLED = 0xFFA0A0A0;
    public static final int TEXT_ERROR = 0xFFFF5555;
    public static final int TEXT_HIGHLIGHT = 0xFFFFFF55;
    public static final int SEPARATOR = 0xFF8B8B8B;
    public static final int LIST_BACKGROUND = 0xFF000000;
    public static final int LIST_OUTLINE = 0xFF808080;
    public static final int LIST_OUTLINE_FOCUSED = 0xFFFFFFFF;
    public static final int SLOT_EDGE_DARK = 0xFF373737;
    public static final int SLOT_EDGE_LIGHT = 0xFFFFFFFF;
    public static final int SLOT_BODY = 0xFF8B8B8B;
    public static final int SCROLLER_TRACK = 0xFF000000;
    public static final int SCROLLER_HANDLE = 0xFF808080;

    private VanillaUi() {
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        roundedRect(graphics, x, y, width, height, PANEL_OUTLINE);
        roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, PANEL_BODY);
        graphics.fill(x + 2, y + 1, x + width - 2, y + 2, PANEL_HIGHLIGHT);
        graphics.fill(x + 1, y + 2, x + 2, y + height - 2, PANEL_HIGHLIGHT);
        graphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, PANEL_SHADOW);
        graphics.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, PANEL_SHADOW);
    }

    public static void roundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (width < 2 * CORNER_RADIUS || height < 2 * CORNER_RADIUS) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        graphics.fill(x + CORNER_RADIUS, y, x + width - CORNER_RADIUS, y + height, color);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, color);
        graphics.fill(x, y + CORNER_RADIUS, x + width, y + height - CORNER_RADIUS, color);
    }

    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_EDGE_DARK);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BODY);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, SLOT_EDGE_LIGHT);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, SLOT_EDGE_LIGHT);
        graphics.fill(x, y + 17, x + 1, y + 18, SLOT_BODY);
        graphics.fill(x + 17, y, x + 18, y + 1, SLOT_BODY);
    }

    public static void scroller(GuiGraphics graphics, int x, int y, int height,
                                int handleTop, int handleHeight) {
        roundedRect(graphics, x, y, SCROLLBAR_WIDTH, height, SCROLLER_TRACK);
        roundedRect(graphics, x + 1, handleTop, SCROLLBAR_WIDTH - 2, handleHeight, SCROLLER_HANDLE);
    }

    public static void separator(GuiGraphics graphics, int x1, int x2, int y) {
        graphics.fill(x1, y, x2, y + 1, SEPARATOR);
    }

    public static void listArea(GuiGraphics graphics, int x, int y, int width, int height) {
        roundedRect(graphics, x, y, width, height, LIST_OUTLINE);
        roundedRect(graphics, x + 1, y + 1, width - 2, height - 2, LIST_BACKGROUND);
    }

    public static void rowOutline(GuiGraphics graphics, int x, int y, int width, int height,
                                  boolean focused) {
        int colour = focused ? LIST_OUTLINE_FOCUSED : LIST_OUTLINE;
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y,
                            int color, int maxWidth) {
        graphics.drawString(font, Component.literal(clip(font, text.getString(), maxWidth)), x, y,
                color, true);
    }

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    public static int textRight(GuiGraphics graphics, Font font, Component text, int right, int y,
                                int color, int maxWidth) {
        String clipped = clip(font, text.getString(), maxWidth);
        int width = font.width(clipped);
        graphics.drawString(font, Component.literal(clipped), right - width, y, color, true);
        return right - width;
    }

    public static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        List<FormattedText> parts = font.getSplitter()
                .splitLines(Component.literal(text), Math.max(24, maxWidth), Style.EMPTY);
        for (FormattedText part : parts) {
            lines.add(part.getString());
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    public static String clip(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int limit = maxWidth - font.width(ellipsis);
        if (limit <= 0) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, limit) + ellipsis;
    }

    public static int labelWidth(Font font, List<Component> labels) {
        int widest = 0;
        for (Component label : labels) {
            widest = Math.max(widest, font.width(label));
        }
        return widest;
    }

    public static int measureWidth(Font font, List<Component> labels, int controlWidth, int minWidth) {
        return Math.max(minWidth, labelWidth(font, labels) + GAP + controlWidth + PADDING);
    }

    public static Button button(Component label, Button.OnPress onPress, int x, int y, int width) {
        return Button.builder(label, onPress).bounds(x, y, width, WIDGET_HEIGHT).build();
    }

    public static Button rowButton(Component label, Button.OnPress onPress, int x, int y, int width, int height) {
        return Button.builder(label, onPress).bounds(x, y, width, height).build();
    }

    public static Button compact(Component label, Button.OnPress onPress, int x, int y) {
        return Button.builder(label, onPress).bounds(x, y, STEP_BUTTON, STEP_BUTTON).build();
    }

    public static EditBox field(Font font, int x, int y, int width, Component label, String value,
                                int maxLength) {
        EditBox box = new EditBox(font, x, y, width, WIDGET_HEIGHT, label);
        box.setMaxLength(maxLength);
        box.setValue(value);
        return box;
    }

    public static String roman(int value) {
        if (value <= 0) {
            return "0";
        }
        if (value > 10) {
            return Integer.toString(value);
        }
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            default -> "X";
        };
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static int parseClamped(String text, int fallback, int min, int max) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return clamp(fallback, min, max);
        }
        try {
            return clamp(Integer.parseInt(trimmed), min, max);
        } catch (NumberFormatException exception) {
            try {
                return new java.math.BigInteger(trimmed).signum() < 0 ? min : max;
            } catch (NumberFormatException notANumber) {
                return clamp(fallback, min, max);
            }
        }
    }
}
