package com.huziyang520.merlinlib.tools.ui;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Key codes shared by the screens.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>The 26.3 line could not use {@code InputConstants} constants for these: that line's table is built
 * from <b>USB HID usage ids</b> rather than GLFW key codes, so Escape was 41, Enter 40 and the arrow keys
 * 79 and 80, and the numbers had to be written out as literals with a comment explaining why they looked
 * unfamiliar.
 *
 * <p>On 1.20.1 the situation is the reverse and much simpler: {@code InputConstants} holds the <b>GLFW</b>
 * codes, which is exactly what a key event carries into {@code Screen#keyPressed(int keyCode, int scanCode,
 * int modifiers)}. The constants below therefore reference {@code InputConstants} directly instead of
 * numbering the keys by hand - they cannot drift from the values the events carry, and the reader does not
 * have to trust a comment. The numbers were confirmed by probing this version's {@code InputConstants}:
 * {@code KEY_ESCAPE = 256}, {@code KEY_RETURN = 257}, {@code KEY_NUMPADENTER = 335},
 * {@code KEY_LEFT = 263} and {@code KEY_RIGHT = 262}.
 *
 * <p>Because the numbering changed, so did the <em>values</em> - a macro stored by the 26.3 line with a HID
 * code means something else here. That is a one-off conversion of the saved file, not something this class
 * can paper over: the constants are only ever compared with the codes carried by an event.
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
