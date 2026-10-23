package com.huziyang520.merlinlib.ui.anim;

/**
 * Interface animation, public and usable by any mod.
 */
public final class UiAnimation {

    private UiAnimation() {
    }

    /**
     * @return the animation for a main screen, which is none
     */
    public static ScreenIntro intro() {
        return screenIntro();
    }

    /**
     * @return the animation for a main screen opening or closing, which is none
     */
    public static ScreenIntro screenIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @return the animation for a tab switch sliding the rows into place, which is none
     */
    public static ScreenIntro tabIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @return the animation for a sub screen opening or closing, which is none
     */
    public static ScreenIntro subIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @param kind the configured choice, for a mod that keeps its own setting
     * @return the animation for that choice
     */
    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
