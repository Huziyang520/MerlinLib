package com.huziyang520.merlinlib.tools.ui;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Key codes shared by the screens.
 */
public final class MerlinUi {

    /** Escape. The GLFW code, 256; the screen closes on it. */
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    /** Enter on the main keyboard, 257. Confirms a dialog the way vanilla does. */
    public static final int KEY_ENTER = InputConstants.KEY_RETURN;
    /** Keypad Enter, 335. Accepted wherever {@link #KEY_ENTER} is, as vanilla does. */
    public static final int KEY_KP_ENTER = InputConstants.KEY_NUMPADENTER;
    /** Left arrow, 263, the default binding of the item editor. */
    public static final int KEY_LEFT = InputConstants.KEY_LEFT;
    /** Right arrow, 262, the default binding of the macro screen. */
    public static final int KEY_RIGHT = InputConstants.KEY_RIGHT;

    private MerlinUi() {
    }
}
