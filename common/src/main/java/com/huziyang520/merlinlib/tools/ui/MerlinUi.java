package com.huziyang520.merlinlib.tools.ui;

/**
 * Key codes shared by the screens.
 *
 * <h2>Why these numbers look unfamiliar</h2>
 *
 * <p>26.3 numbers keyboard keys by {@code InputConstants}, and that table is built from <b>USB HID usage
 * ids</b>, not from GLFW key codes: Escape is 41, Enter is 40, and the arrow keys are 79 and 80. The old GLFW
 * values are still present in the same table, but they now mean something else entirely - 262 and 263 are
 * "media play" and "media pause", which is what a binding written with the GLFW values shows up as in the
 * controls screen.
 *
 * <p>The values below are read from {@code com.mojang.blaze3d.platform.InputConstants} of 26.3. They are only
 * ever compared with the codes carried by a key event, and a key event carries the same numbering, so they
 * cannot drift from the source they were copied from without the comparison failing loudly.
 */
public final class MerlinUi {

    /** Escape (HID usage 0x29). */
    public static final int KEY_ESCAPE = 41;
    /** Enter (HID usage 0x28). */
    public static final int KEY_ENTER = 40;
    /** Keypad Enter (HID usage 0x58). */
    public static final int KEY_KP_ENTER = 88;
    /** Left arrow (HID usage 0x50), the default binding of the item editor. */
    public static final int KEY_LEFT = 80;
    /** Right arrow (HID usage 0x4F), the default binding of the macro screen. */
    public static final int KEY_RIGHT = 79;

    private MerlinUi() {
    }
}
