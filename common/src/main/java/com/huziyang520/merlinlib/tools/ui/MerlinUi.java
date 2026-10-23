package com.huziyang520.merlinlib.tools.ui;

/**
 * Key codes shared by the screens.
 *
 * <p>These are the GLFW codes this Minecraft range hands to {@code Screen#keyPressed}: the input layer of
 * 26.1 and 26.2 still speaks them, while the later line numbers keys by USB HID usage id.
 */
public final class MerlinUi {

    /** Escape. */
    public static final int KEY_ESCAPE = 256;
    /** Enter. */
    public static final int KEY_ENTER = 257;
    /** Keypad Enter. */
    public static final int KEY_KP_ENTER = 335;
    /** Left arrow, the default binding of the item editor. */
    public static final int KEY_LEFT = 263;
    /** Right arrow, the default binding of the macro screen. */
    public static final int KEY_RIGHT = 262;

    private MerlinUi() {
    }
}
