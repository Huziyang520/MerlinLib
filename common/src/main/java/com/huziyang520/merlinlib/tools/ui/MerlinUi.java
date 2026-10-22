package com.huziyang520.merlinlib.tools.ui;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Key codes shared by the screens.
 *
 * <h2>Where these numbers come from</h2>
 *
 * <p>Every constant is read from {@code com.mojang.blaze3d.platform.InputConstants} of this Minecraft line, not
 * typed in. The numbering is <b>not</b> the same across the 26.x releases: 26.3 numbers keys by USB HID usage
 * ids (Escape 41, Enter 40, left arrow 80, right arrow 79), while 26.1 and 26.2 still use the GLFW codes
 * (Escape 256, Enter 257, keypad Enter 335, left arrow 263, right arrow 262).
 *
 * <p>This class was copied from the 26.3 line with the HID numbers baked in, which is exactly what broke the
 * two hot keys here: the bindings were created with 80 and 79, so the controls screen listed them on two
 * ordinary letter keys and the arrow keys did nothing - the "the hot keys are not implemented" report. Reading
 * the constants makes the numbers follow whatever the running version actually uses.
 *
 * <p>They are only ever compared with the codes carried by a key event, and a key event carries the same
 * numbering as {@code InputConstants}, so they cannot drift from the source they are read from.
 */
public final class MerlinUi {

    /** Escape. */
    public static final int KEY_ESCAPE = InputConstants.KEY_ESCAPE;
    /** Enter. */
    public static final int KEY_ENTER = InputConstants.KEY_RETURN;
    /** Keypad Enter. */
    public static final int KEY_KP_ENTER = InputConstants.KEY_NUMPADENTER;
    /** Left arrow, the default binding of the item editor. */
    public static final int KEY_LEFT = InputConstants.KEY_LEFT;
    /** Right arrow, the default binding of the macro screen. */
    public static final int KEY_RIGHT = InputConstants.KEY_RIGHT;

    private MerlinUi() {
    }
}
