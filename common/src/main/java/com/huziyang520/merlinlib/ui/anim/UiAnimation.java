package com.huziyang520.merlinlib.ui.anim;

/**
 * Interface animation, public and usable by any mod.
 */
public final class UiAnimation {

    private UiAnimation() {
    }

    /**
     * @return a finished animation: MerlinLib's screens no longer animate on their own
     */
    public static ScreenIntro intro() {
        return ScreenIntro.finished();
    }

    /**
     * @return a finished animation: MerlinLib's screens no longer animate on their own
     */
    public static ScreenIntro screenIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @return a finished animation: no tabbed MerlinLib screen is left to slide its rows
     */
    public static ScreenIntro tabIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @return a finished animation: the toolkit screens no longer animate on their own
     */
    public static ScreenIntro subIntro() {
        return ScreenIntro.finished();
    }

    /**
     * @param kind the choice of the caller, for a mod that keeps its own setting
     * @return the animation for that choice
     */
    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
