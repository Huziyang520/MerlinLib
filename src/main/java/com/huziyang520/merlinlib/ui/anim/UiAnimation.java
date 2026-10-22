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
 * <h2>No switches of its own any more</h2>
 *
 * <p>The three animated places this class used to configure - a main screen opening, a tab switch and a sub
 * screen opening - belonged to MerlinLib's own hand written screens, and those are gone: the configuration
 * screen is the loader's now, and the toolkit screens simply appear. The matching switches went with them, so
 * the three methods below answer with {@link ScreenIntro#finished()} - an animation that is already over -
 * instead of reading a key that no longer exists.
 *
 * <p>Nothing was removed from the api: {@link #intro(int)} and {@link ScreenIntro#of(int)} still build a real
 * animation for a mod that keeps its own setting, and {@link Easing} is untouched. A mod that wants animated
 * screens keeps working exactly as before, with its own switch, which is where that choice belonged anyway.
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
