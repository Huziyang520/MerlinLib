package com.huziyang520.merlinlib.tools.ui;

/**
 * Key codes shared by the screens.
 *
 * <p>The codes are GLFW key codes written out as constants rather than taken from LWJGL, because LWJGL is
 * on no module's compile class path here and a GLFW key code is a stable part of the GLFW ABI. They are
 * only ever compared with the codes carried by a key event, so inlining them cannot drift.
 */
public final class MerlinUi {

    /** GLFW key code of Escape (GLFW_KEY_ESCAPE). */
    public static final int KEY_ESCAPE = 256;
    /** GLFW key code of Enter (GLFW_KEY_ENTER). */
    public static final int KEY_ENTER = 257;
    /** GLFW key code of the numeric keypad Enter (GLFW_KEY_KP_ENTER). */
    public static final int KEY_KP_ENTER = 335;
    /** GLFW key code of the left arrow (GLFW_KEY_LEFT), the default binding of the item editor. */
    public static final int KEY_LEFT = 263;
    /** GLFW key code of the right arrow (GLFW_KEY_RIGHT), the default binding of the macro screen. */
    public static final int KEY_RIGHT = 262;

    private MerlinUi() {
    }
}
