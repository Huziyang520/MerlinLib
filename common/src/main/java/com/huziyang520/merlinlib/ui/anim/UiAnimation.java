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
 * <h2>Why nothing comes back switched on</h2>
 *
 * <p>MerlinLib used to run these helpers on its own screens behind four configuration switches in the
 * {@code [gui]} section of {@code client.toml}. Both the drawn configuration screens that owned those
 * switches and the switches themselves are gone, so there is nothing left here to configure: MerlinLib's
 * own screens appear and disappear at once, and the helpers below return a finished animation unless a
 * caller asks for a kind explicitly.
 *
 * <p>A business mod still gets the whole toolkit - {@link Easing}, {@link ScreenIntro}, and
 * {@link #intro(int)} - and may keep its own switch for it. Nothing here touches another mod's screens by
 * itself.
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
