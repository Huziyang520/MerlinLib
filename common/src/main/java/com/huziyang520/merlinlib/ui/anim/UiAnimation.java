// Interface animation, public and usable by any mod.

package com.huziyang520.merlinlib.ui.anim;

public final class UiAnimation {
    private UiAnimation() {
    }

    public static ScreenIntro intro() {
        return ScreenIntro.finished();
    }

    public static ScreenIntro screenIntro() {
        return ScreenIntro.finished();
    }

    public static ScreenIntro tabIntro() {
        return ScreenIntro.finished();
    }

    public static ScreenIntro subIntro() {
        return ScreenIntro.finished();
    }

    public static ScreenIntro intro(int kind) {
        return ScreenIntro.of(kind);
    }
}
