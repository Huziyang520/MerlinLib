package com.huziyang520.merlinlib.ui.anim;

/**
 * Interface animation, public and usable by any mod.
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
