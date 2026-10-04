package com.huziyang520.merlinlib.ui.anim;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;

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
 * <h2>The four switches</h2>
 *
 * <p>MerlinLib uses the animation in three places, each with a switch of its own, plus one master switch that
 * turns all of them off at once:
 *
 * <ul>
 *   <li>{@code gui.animation_enabled} - the master switch, on the general tab;</li>
 *   <li>{@code gui.animation_screen} - a main screen opening and closing ({@link #screenIntro()});</li>
 *   <li>{@code gui.animation_tab} - a tab switch sliding the rows ({@link #tabIntro()});</li>
 *   <li>{@code gui.animation_sub} - a sub screen opening and closing ({@link #subIntro()}).</li>
 * </ul>
 *
 * <p>A business mod may read these through the three methods, pass its own choice to {@link #intro(int)} or
 * {@link ScreenIntro#of(int)}, use {@link Easing} directly, or ignore the feature. Nothing here touches another
 * mod's screens by itself: the switches are MerlinLib's own, which is why they live in MerlinLib's config.
 */
public final class UiAnimation {

    private UiAnimation() {
    }

    /**
     * @return the animation for a main screen, honouring the master and per-place switches
     */
    public static ScreenIntro intro() {
        return screenIntro();
    }

    /**
     * @return the animation for a main screen opening or closing
     */
    public static ScreenIntro screenIntro() {
        ClientConfig config = ConfigManager.client();
        return pick(config.uiAnimationEnabled() && config.animationScreen(), config.uiAnimationKind());
    }

    /**
     * @return the animation for a tab switch sliding the rows into place
     *
     * <p>Always a sideways slide, and never the configured kind: the kind is the open and close animation, and
     * letting it drive the tab switch too meant that choosing a kind for opening a screen changed what a tab
     * switch looked like - the two became the same setting, which is not what either of them says.
     */
    public static ScreenIntro tabIntro() {
        ClientConfig config = ConfigManager.client();
        return config.uiAnimationEnabled() && config.animationTab()
                ? ScreenIntro.sideSlide() : ScreenIntro.finished();
    }

    /**
     * @return the animation for a sub screen opening or closing
     */
    public static ScreenIntro subIntro() {
        ClientConfig config = ConfigManager.client();
        return pick(config.uiAnimationEnabled() && config.animationSub(), config.uiAnimationKind());
    }

    /**
     * @param kind the configured choice, for a mod that keeps its own setting
     * @return the animation for that choice
     */
    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }

    private static ScreenIntro pick(boolean enabled, int kind) {
        return enabled ? ScreenIntro.of(kind) : ScreenIntro.finished();
    }
}
