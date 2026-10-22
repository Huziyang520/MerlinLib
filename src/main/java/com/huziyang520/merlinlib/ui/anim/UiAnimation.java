package com.huziyang520.merlinlib.ui.anim;

/**
 * Interface animation, public and usable by any mod.
 *
 * <h2>What a mod gets</h2>
 *
 * <p>A screen asks for an animation and applies it in three lines - translate by
 * {@link ScreenIntro#offsetX(int)} / {@link ScreenIntro#offsetY(int)}, scale by {@link ScreenIntro#scale()} - or
 * uses {@link Easing} on its own for something completely different. The library draws nothing for the caller
 * and does not know which screen it is for.
 *
 * <h2>No switch of MerlinLib's own</h2>
 *
 * <p>These helpers used to be gated by four switches of this mod's configuration. They are not any more, for
 * two reasons: MerlinLib does not animate its own screens at all, so the switches had nothing left to govern,
 * and a helper that silently answers {@link ScreenIntro#finished()} depending on someone else's configuration
 * is a trap for the mod calling it. The three methods below always answer with an animation, and a mod that
 * wants a switch of its own passes its choice to {@link #intro(int)} or {@link ScreenIntro#of(int)}.
 *
 * <p><b>What changed / 1.20.1 note:</b> the signatures are unchanged from the 26.3 line; only the bodies stopped
 * reading {@code ClientConfig}.
 */
public final class UiAnimation {

    /** The kind {@link #screenIntro()} and {@link #subIntro()} answer with: 0 = pop. */
    private static final int DEFAULT_KIND = 0;

    private UiAnimation() {
    }

    /**
     * @return the animation for a main screen
     */
    public static ScreenIntro intro() {
        return screenIntro();
    }

    /**
     * @return the animation for a main screen opening or closing
     */
    public static ScreenIntro screenIntro() {
        return ScreenIntro.of(DEFAULT_KIND);
    }

    /**
     * @return the animation for a tab switch sliding the rows into place
     *
     * <p>Always a sideways slide, never the opening kind: letting the opening kind drive the tab switch too
     * meant that choosing how a screen appears changed what a tab switch looked like - the two became the same
     * setting, which is not what either of them says.
     */
    public static ScreenIntro tabIntro() {
        return ScreenIntro.sideSlide();
    }

    /**
     * @return the animation for a sub screen opening or closing
     */
    public static ScreenIntro subIntro() {
        return ScreenIntro.of(DEFAULT_KIND);
    }

    /**
     * @param kind the choice of a mod that keeps its own setting
     * @return the animation for that choice
     */
    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
