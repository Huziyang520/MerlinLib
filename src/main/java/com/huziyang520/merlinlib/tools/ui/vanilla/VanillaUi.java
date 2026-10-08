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

/**
 * The vanilla look and feel of every MerlinLib screen, expressed as drawing primitives plus the few
 * measurements screens must not invent for themselves.
 *
 * <h2>Where every number comes from</h2>
 *
 * <p>Nothing here is invented. Each value is either a colour sampled from the vanilla texture that owns it
 * or copied from the vanilla class that owns it, so a MerlinLib screen cannot drift away from the game's own
 * look:
 *
 * <ul>
 *   <li><b>panel</b>: the container panel, drawn in its own colours (black frame, {@code #C6C6C6} body,
 *       white top and left edge, {@code #555555} bottom and right);</li>
 *   <li><b>slot</b>: the vanilla slot frame - {@code #373737} along the top and left, {@code #FFFFFF} along
 *       the bottom and right, {@code #8B8B8B} body - sampled from
 *       {@code textures/gui/container/inventory.png}, where a slot occupies an 18x18 square starting at
 *       (7, 83);</li>
 *   <li><b>list area</b>: opaque black with a one pixel outline, exactly what
 *       {@code AbstractSelectionList} draws ({@code -16777216} fill, {@code -8355712} outline, white when
 *       the list is focused). These two numbers were read out of that class's bytecode, not guessed;</li>
 *   <li><b>text</b>: always drawn with a shadow, which is what makes white text readable on a light
 *       panel - the same combination vanilla uses for its own dialogs.</li>
 * </ul>
 *
 * <h2>Layout is measured, never hard coded</h2>
 *
 * <p>Every helper that takes text also takes the width it may occupy, and {@link #measureWidth} derives a
 * panel width from the actual translated labels. A screen built from these helpers therefore fits Chinese,
 * English or any other language without a per-language constant.
 *
 * <p>This is library surface: other mods may build screens on it, so signatures change only for good
 * reason, and every rule encoded here was chosen to keep screens pixel aligned at GUI scale 1, 2 and 3.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>The drawing type is {@link GuiGraphics}, not the 26.3 {@code GuiGraphicsExtractor}: on this version
 * every fill, every string and the pose stack go through one class, and there is no separate "extract"
 * pass. That rename touches the first parameter of nearly every method here and is the only systematic
 * difference.
 *
 * <p>Three things this file used to borrow from the game cannot be borrowed on 1.20.1, and all three are
 * <b>drawn by hand instead of blitted</b>:
 *
 * <ul>
 *   <li>{@code graphics.blitSprite(...)} <b>does not exist</b> - the sprite-atlas API for GUIs arrived in
 *       1.20.2 and every overload of it is absent from this version's {@code GuiGraphics}. The only blit
 *       overloads here take a whole {@code ResourceLocation} texture plus a u/v rectangle, so there is no
 *       nine slice to hand it;</li>
 *   <li>the sprites the 26.3 line named do not exist as files either. 1.20.1 has no
 *       {@code textures/gui/sprites/} tree at all (it appears in 1.20.4), so {@code popup/background},
 *       {@code widget/scroller} and {@code widget/scroller_background} have no 1.20.1 counterpart - the
 *       scrollbar is not even drawn from a texture on this version: {@code AbstractSelectionList} has no
 *       reference to any scroller texture, only to {@code Screen#BACKGROUND_LOCATION};</li>
 *   <li>{@code container/slot} is not a standalone texture on this version: the slot frame is baked into
 *       each container's own PNG, so sampling it is the only way to reproduce it.</li>
 * </ul>
 *
 * <p>Consequently the public sprite constants ({@code PANEL_SPRITE}, {@code SCROLLER_SPRITE},
 * {@code SCROLLER_TRACK_SPRITE}, {@code SLOT_SPRITE}) are gone. They were the resource locations of
 * sprites that do not resolve on this version, so keeping them would have handed callers a name that draws
 * nothing. Every method that consumed them now paints the same pixels with {@code fill}.
 *
 * <p>The 26.3 panel already painted itself with fills rather than a sprite, so {@link #panel} is a direct
 * port; only {@link #slot} and {@link #scroller} changed implementation.
 */
public final class VanillaUi {

    // ------------------------------------------------------------------ metrics (vanilla grid)

    /** Vanilla widget height. Every control is this tall. */
    public static final int WIDGET_HEIGHT = 20;
    /** Vertical distance between two control rows: the widget plus a 4 pixel gap. */
    public static final int ROW_STEP = 24;
    /** Padding between the panel frame and its content. */
    public static final int PADDING = 8;
    /** Horizontal gap between two controls on the same row. */
    public static final int GAP = 6;
    /**
     * Width and height of the compact - / + / X buttons used inside list rows.
     *
     * <p>Deliberately smaller than {@link #LIST_ROW_HEIGHT} by four pixels: at the same size the buttons
     * touched (and at some GUI scales appeared to cut through) the row's outline, which is what the
     * "the buttons run into the frame" report was about. Sixteen against twenty leaves a two pixel gap
     * above and below once the row centres them.
     */
    public static final int STEP_BUTTON = 16;
    /** Width of the vanilla scrollbar. */
    public static final int SCROLLBAR_WIDTH = 6;
    /** Height of one compact list row. */
    public static final int LIST_ROW_HEIGHT = 20;
    /** Smallest handle the scrollbar keeps, so it stays grabbable. */
    public static final int MIN_HANDLE_HEIGHT = 20;

    // ------------------------------------------------------------------ colours (copied from vanilla)

    /** Panel frame: the black line around a container panel. */
    public static final int PANEL_OUTLINE = 0xFF000000;
    /** Panel body: the container grey. */
    public static final int PANEL_BODY = 0xFFC6C6C6;
    /** Panel top and left edge: white, as the container textures have it. */
    public static final int PANEL_HIGHLIGHT = 0xFFFFFFFF;
    /** Panel bottom and right edge. */
    public static final int PANEL_SHADOW = 0xFF555555;
    /** Body text: white, the colour vanilla uses on that panel. */
    public static final int TEXT = 0xFFFFFFFF;
    /** Secondary or hint text. Also white; the shadow provides the separation. */
    public static final int TEXT_HINT = 0xFFFFFFFF;
    /** Text of a disabled control: vanilla's grey. */
    public static final int TEXT_DISABLED = 0xFFA0A0A0;
    /** Error text. */
    public static final int TEXT_ERROR = 0xFFFF5555;
    /** Numeric highlights such as enchantment levels: vanilla yellow. */
    public static final int TEXT_HIGHLIGHT = 0xFFFFFF55;
    /** The separator line under a panel header: vanilla's list button grey. */
    public static final int SEPARATOR = 0xFF8B8B8B;
    /** The recessed list area: vanilla draws exactly this opaque black. */
    public static final int LIST_BACKGROUND = 0xFF000000;
    /** The list's one pixel outline; {@code -8355712} in {@code AbstractSelectionList}. */
    public static final int LIST_OUTLINE = 0xFF808080;
    /** The outline of the focused or selected row; {@code -1} in {@code AbstractSelectionList}. */
    public static final int LIST_OUTLINE_FOCUSED = 0xFFFFFFFF;
    /** Slot frame top and left edge, sampled from the vanilla inventory texture. */
    public static final int SLOT_EDGE_DARK = 0xFF373737;
    /** Slot frame bottom and right edge, sampled from the vanilla inventory texture. */
    public static final int SLOT_EDGE_LIGHT = 0xFFFFFFFF;
    /** Slot body, sampled from the vanilla inventory texture. */
    public static final int SLOT_BODY = 0xFF8B8B8B;
    /** The scrollbar track: vanilla's recessed black. */
    public static final int SCROLLER_TRACK = 0xFF000000;
    /** The scrollbar handle: the same grey vanilla outlines a list with. */
    public static final int SCROLLER_HANDLE = 0xFF808080;

    private VanillaUi() {
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Draws the vanilla container panel over the given rectangle.
     *
     * <p>Call it from {@code Screen#renderBackground} so it lands after the world dimming and before the
     * widgets: drawing it later would cover the controls, drawing it earlier would let the world show
     * through.
     *
     * <p><b>Drawn by code, in the container colours.</b> 1.20.1 has no generic panel sprite at all - the
     * dark {@code popup/background} sprite the 26.3 line could have used is a 1.20.4+ file, and nine-slice
     * blitting does not exist before 1.20.2. A library screen that is supposed to read as a vanilla
     * container panel therefore draws it: black frame, {@code #C6C6C6} body, white edge along the top and
     * left, {@code #555555} along the bottom and right. Nothing a resource pack or a UI mod does to the
     * sprite atlas can change it.
     *
     * @param graphics the graphics object
     * @param x        panel left
     * @param y        panel top
     * @param width    panel width, at least 4
     * @param height   panel height, at least 4
     */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL_OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, PANEL_BODY);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, PANEL_HIGHLIGHT);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, PANEL_SHADOW);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, PANEL_SHADOW);
    }

    /**
     * Draws a vanilla slot frame, the square border used for item slots.
     *
     * <p><b>What changed / 1.20.1 note:</b> the 26.3 line blitted the {@code container/slot} sprite here.
     * That sprite is a 1.20.4+ file - this version has no {@code textures/gui/sprites/} tree - and the slot
     * frame it was cut from lives baked into each container's own PNG. The fills below reproduce it
     * <em>pixel for pixel</em>, in the colours sampled from {@code textures/gui/container/inventory.png},
     * where an 18x18 slot square starts at (7, 83): {@code #373737} along the top and left,
     * {@code #FFFFFF} along the bottom and right, {@code #8B8B8B} over the body - and over the two
     * <em>opposite corners</em>, which vanilla paints with the body colour rather than the frame colour.
     * Those corners are why this is five fills and not four.
     *
     * @param graphics the graphics object
     * @param x        left edge
     * @param y        top edge
     */
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT_EDGE_DARK);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BODY);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, SLOT_EDGE_LIGHT);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, SLOT_EDGE_LIGHT);
        graphics.fill(x, y + 17, x + 1, y + 18, SLOT_BODY);
        graphics.fill(x + 17, y, x + 18, y + 1, SLOT_BODY);
    }

    /**
     * Draws a list scrollbar: the track plus the handle at its current position.
     *
     * <p><b>What changed / 1.20.1 note:</b> the 26.3 line blitted the {@code widget/scroller} and
     * {@code widget/scroller_background} sprites. Neither exists on this version, and this version's
     * {@code AbstractSelectionList} does not reference a scroller texture at all - it draws its scrollbar
     * from the same flat colours used below. The bar is therefore two fills: a recessed black track and a
     * grey handle, matching the {@code #808080} outline vanilla uses for a list.
     *
     * @param graphics     the graphics object
     * @param x            scrollbar left, the handle and track are {@link #SCROLLBAR_WIDTH} wide
     * @param y            scrollbar top
     * @param height       scrollbar height, which is the viewport height
     * @param handleTop    absolute y of the handle
     * @param handleHeight height of the handle, at least {@link #MIN_HANDLE_HEIGHT}
     */
    public static void scroller(GuiGraphics graphics, int x, int y, int height,
                                int handleTop, int handleHeight) {
        graphics.fill(x, y, x + SCROLLBAR_WIDTH, y + height, SCROLLER_TRACK);
        graphics.fill(x, handleTop, x + SCROLLBAR_WIDTH, handleTop + handleHeight, SCROLLER_HANDLE);
    }

    /**
     * Draws a thin separator line, the way a panel header is separated from its content.
     *
     * @param graphics the graphics object
     * @param x1       left edge
     * @param x2       right edge
     * @param y        vertical position
     */
    public static void separator(GuiGraphics graphics, int x1, int x2, int y) {
        graphics.fill(x1, y, x2, y + 1, SEPARATOR);
    }

    /**
     * Draws a recessed list area: opaque black inside, one pixel outline around it.
     *
     * <p>This is one fill and one outline rather than a translucent panel colour, because a translucent
     * background turns into mud over the light dialog panel and stops looking like the game.
     *
     * @param graphics the graphics object
     * @param x        left edge
     * @param y        top edge
     * @param width    width
     * @param height   height
     */
    public static void listArea(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, LIST_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, LIST_OUTLINE);
        graphics.fill(x, y + height - 1, x + width, y + height, LIST_OUTLINE);
        graphics.fill(x, y, x + 1, y + height, LIST_OUTLINE);
        graphics.fill(x + width - 1, y, x + width, y + height, LIST_OUTLINE);
    }

    /**
     * Outlines one row of a list, the way vanilla marks the row under the cursor.
     *
     * @param graphics the graphics object
     * @param x        row left
     * @param y        row top
     * @param width    row width
     * @param height   row height
     * @param focused  {@code true} for the selected row, {@code false} for a hovered one
     */
    public static void rowOutline(GuiGraphics graphics, int x, int y, int width, int height,
                                  boolean focused) {
        int colour = focused ? LIST_OUTLINE_FOCUSED : LIST_OUTLINE;
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    // ------------------------------------------------------------------ text

    /**
     * Draws one line of text, shadowed and clipped to a maximum width instead of overflowing its control.
     *
     * <p>The shadow is not optional: white text on a light panel relies on it for contrast, and it is what
     * vanilla does for every label it draws. Clipping is what makes the screens language independent: a
     * long translation loses its tail rather than running over the next widget.
     *
     * @param graphics the graphics object
     * @param font     the font
     * @param text     the text
     * @param x        left edge
     * @param y        baseline row top
     * @param color    the ARGB colour
     * @param maxWidth maximum pixel width before an ellipsis is appended
     */
    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y,
                            int color, int maxWidth) {
        graphics.drawString(font, Component.literal(clip(font, text.getString(), maxWidth)), x, y,
                color, true);
    }

    /**
     * Draws one line of shadowed text without clipping. Only for strings that are known to fit.
     *
     * @param graphics the graphics object
     * @param font     the font
     * @param text     the text
     * @param x        left edge
     * @param y        baseline row top
     * @param color    the ARGB colour
     */
    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    /**
     * Draws shadowed text whose right edge is pinned to a coordinate, clipped from the left as needed.
     *
     * @param graphics the graphics object
     * @param font     the font
     * @param text     the text
     * @param right    the x coordinate the text ends at
     * @param y        baseline row top
     * @param color    the ARGB colour
     * @param maxWidth maximum pixel width
     * @return the x coordinate the text starts at
     */
    public static int textRight(GuiGraphics graphics, Font font, Component text, int right, int y,
                                int color, int maxWidth) {
        String clipped = clip(font, text.getString(), maxWidth);
        int width = font.width(clipped);
        graphics.drawString(font, Component.literal(clipped), right - width, y, color, true);
        return right - width;
    }

    /**
     * Breaks a line of text into as many lines as it needs to fit a width.
     *
     * <p>Used for the hint lines under a screen: clipping them to one line turned every longer hint into
     * "...". Splitting is done by the font's own splitter rather than by spaces, so a language without
     * spaces breaks where it should - and the caller then reserves room for the number of lines it gets
     * back, which is what keeps the layout measured rather than guessed.
     *
     * <p><b>What changed / 1.20.1 note:</b> 26.3 called {@code Font#splitIgnoringLanguage} and got
     * {@code FormattedText} lines back. That method is a 1.20.5+ addition and does not exist here;
     * {@code Font#split} does, but it returns {@code FormattedCharSequence}, which cannot be turned back
     * into a string. The equivalent call on this version is
     * {@code Font#getSplitter()#splitLines(FormattedText, int, Style)}, which returns {@code FormattedText}
     * and therefore still has {@code getString()}. It is the <em>language aware</em> splitter - the
     * "ignoring language" variant exists upstream precisely to opt out of these rules - which is the
     * behaviour the comment above asks for anyway, and is what lets Chinese text break between characters.
     *
     * @param font     the font
     * @param text     the text to lay out
     * @param maxWidth the width each line may occupy
     * @return the lines, at least one
     */
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

    /**
     * Shortens text so it never overflows, appending an ellipsis when it had to be cut.
     *
     * @param font     the font
     * @param text     the text to fit
     * @param maxWidth maximum pixel width
     * @return the original text, or a clipped version ending in an ellipsis
     */
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

    // ------------------------------------------------------------------ measurement

    /**
     * The width needed for the widest of the given labels, which is what makes a label column line up
     * across rows in any language.
     *
     * @param font   the font
     * @param labels the labels of the panel
     * @return the widest label width in pixels, 0 when there is none
     */
    public static int labelWidth(Font font, List<Component> labels) {
        int widest = 0;
        for (Component label : labels) {
            widest = Math.max(widest, font.width(label));
        }
        return widest;
    }

    /**
     * Derives a content width from the text that has to fit, instead of guessing a constant.
     *
     * @param font         the font
     * @param labels       every label that shares the panel's left column
     * @param controlWidth the widest control on the right of a row
     * @param minWidth     a lower bound for very sparse panels
     * @return a content width that fits both columns
     */
    public static int measureWidth(Font font, List<Component> labels, int controlWidth, int minWidth) {
        return Math.max(minWidth, labelWidth(font, labels) + GAP + controlWidth + PADDING);
    }

    // ------------------------------------------------------------------ controls

    /**
     * Builds a vanilla button.
     *
     * @param label   the button text
     * @param onPress the press handler
     * @param x       left edge
     * @param y       top edge
     * @param width   width, height is {@link #WIDGET_HEIGHT}
     * @return the button
     */
    public static Button button(Component label, Button.OnPress onPress, int x, int y, int width) {
        return Button.builder(label, onPress).bounds(x, y, width, WIDGET_HEIGHT).build();
    }

    /**
     * Builds a compact button for a list row.
     *
     * @param label   the button text
     * @param onPress the press handler
     * @param x       left edge
     * @param y       top edge
     * @return the button
     */
    public static Button compact(Component label, Button.OnPress onPress, int x, int y) {
        return Button.builder(label, onPress).bounds(x, y, STEP_BUTTON, STEP_BUTTON).build();
    }

    /**
     * Builds an edit box.
     *
     * @param font      the font
     * @param x         left edge
     * @param y         top edge
     * @param width     width, height is {@link #WIDGET_HEIGHT}
     * @param label     the narration label
     * @param value     the initial value
     * @param maxLength maximum character count
     * @return the edit box
     */
    public static EditBox field(Font font, int x, int y, int width, Component label, String value,
                                int maxLength) {
        EditBox box = new EditBox(font, x, y, width, WIDGET_HEIGHT, label);
        box.setMaxLength(maxLength);
        box.setValue(value);
        return box;
    }

    // ------------------------------------------------------------------ numbers & text helpers

    /**
     * Converts a level to a roman numeral, the way vanilla shows enchantment levels.
     *
     * @param value the level, expected to be positive
     * @return the roman numeral, or the plain number above 10
     */
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

    /**
     * Clamps a value into a range.
     *
     * <p><b>What changed / 1.20.1 note:</b> implemented with {@code Math.max}/{@code Math.min} rather than
     * 26.3's {@code Math.clamp}, which is a Java 21 addition and is not available under this project's Java
     * 17 target. The behaviour is identical.
     *
     * @param value the value to clamp
     * @param min   lower bound
     * @param max   upper bound
     * @return the clamped value
     */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parses a signed decimal integer, accepting surrounding spaces.
     *
     * @param text     the text to parse
     * @param fallback the value to return when the text is not a number
     * @return the parsed value, or the fallback
     */
    public static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /**
     * Reads a number that may be larger than the field allows, and brings it inside the range.
     *
     * <p>This exists because of a real bug: {@code Integer.parseInt} throws on anything above the integer limit,
     * and the editor fell back to 1 when it did - so typing one more than the maximum turned a huge damage into
     * a single point. A number that is merely too large is a request for the largest value allowed, and a
     * number below the minimum is a request for the smallest; only something that is not a number at all uses
     * the fallback.
     *
     * @param text     the text to read
     * @param fallback the value to use when the text is not a number
     * @param min      the smallest value allowed
     * @param max      the largest value allowed
     * @return the value, inside the range
     */
    public static int parseClamped(String text, int fallback, int min, int max) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return clamp(fallback, min, max);
        }
        try {
            return clamp(Integer.parseInt(trimmed), min, max);
        } catch (NumberFormatException exception) {
            // Either it is not a number, or it is one the integer type cannot hold; the second case is decided
            // by its sign, which is all a number too large to read can tell us about itself.
            try {
                return new java.math.BigInteger(trimmed).signum() < 0 ? min : max;
            } catch (NumberFormatException notANumber) {
                return clamp(fallback, min, max);
            }
        }
    }
}
