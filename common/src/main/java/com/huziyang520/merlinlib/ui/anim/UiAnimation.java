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
 * <h2>Where the switches went</h2>
 *
 * <p>These helpers used to be driven by MerlinLib's own {@code [gui]} switches. That section is gone: this
 * build draws no configuration screen of its own, the toolkit screens no longer animate, and a setting that
 * governs code nobody calls is only a promise that cannot be kept. The helpers stay - they are part of the
 * public api and a business mod may use them - but they no longer consult MerlinLib's configuration: a caller
 * that wants a switch keeps its own, and passes the choice to {@link #intro(int)} or {@link ScreenIntro#of(int)}.
 */
public final class UiAnimation {

    private UiAnimation() {
    }

    /**
     * @return the default animation for a screen opening or closing
     */
    public static ScreenIntro intro() {
        return screenIntro();
    }

    /**
     * @return the default animation for a screen opening or closing; see {@link ScreenIntro#of(int)}
     */
    public static ScreenIntro screenIntro() {
        return ScreenIntro.of(0);
    }

    /**
     * @return the default animation for a tab switch sliding the rows into place
     *
     * <p>Always a sideways slide, and never a configured kind: a tab switch is a different motion from a
     * screen opening, and letting one setting drive both made choosing a kind for opening a screen change what
     * a tab switch looked like.
     */
    public static ScreenIntro tabIntro() {
        return ScreenIntro.sideSlide();
    }

    /**
     * @return the default animation for a second level screen opening or closing
     */
    public static ScreenIntro subIntro() {
        return ScreenIntro.of(0);
    }

    /**
     * @param kind the choice of a mod that keeps its own setting
     * @return the animation for that choice
     */
    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
