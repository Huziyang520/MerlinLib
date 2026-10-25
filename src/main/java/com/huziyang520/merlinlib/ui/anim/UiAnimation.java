// Interface animation, public and usable by any mod.

package com.huziyang520.merlinlib.ui.anim;

public final class UiAnimation {
    private static final int DEFAULT_KIND = 0;

    private UiAnimation() {
    }

    public static ScreenIntro intro() {
        return screenIntro();
    }

    public static ScreenIntro screenIntro() {
        return ScreenIntro.of(DEFAULT_KIND);
    }

    public static ScreenIntro tabIntro() {
        return ScreenIntro.sideSlide();
    }

    public static ScreenIntro subIntro() {
        return ScreenIntro.of(DEFAULT_KIND);
    }

    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
