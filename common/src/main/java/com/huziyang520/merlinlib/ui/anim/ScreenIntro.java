// The opening animation of a screen: where it is, how big it is and how visible it is, over a short moment.

package com.huziyang520.merlinlib.ui.anim;

public final class ScreenIntro {
    public enum Transition {
        SCALE_POP,

        SLIDE_UP,

        SLIDE_SIDE,

        FADE
    }

    private final Transition transition;
    private final Easing easing;
    private final long durationMillis;
    private final long startedAt;

    public ScreenIntro(Transition transition, Easing easing, long durationMillis) {
        this.transition = transition;
        this.easing = easing;
        this.durationMillis = Math.max(1L, durationMillis);
        this.startedAt = System.currentTimeMillis();
    }

    public static ScreenIntro finished() {
        return new ScreenIntro(Transition.FADE, Easing.LINEAR, 0L);
    }

    public static ScreenIntro of(int kind) {
        return switch (kind) {
            case 1 -> new ScreenIntro(Transition.SLIDE_UP, Easing.EASE_OUT_CUBIC, 220L);
            case 2 -> new ScreenIntro(Transition.SLIDE_SIDE, Easing.EASE_OUT_CUBIC, 200L);
            case 3 -> new ScreenIntro(Transition.FADE, Easing.LINEAR, 220L);
            default -> new ScreenIntro(Transition.SCALE_POP, Easing.EASE_OUT_BACK, 240L);
        };
    }

    public static ScreenIntro sideSlide() {
        return new ScreenIntro(Transition.SLIDE_SIDE, Easing.EASE_OUT_CUBIC, 180L);
    }

    public boolean running() {
        return this.progress() < 1.0D;
    }

    public double progress() {
        long elapsed = System.currentTimeMillis() - this.startedAt;
        return this.easing.applyClamped((double) elapsed / (double) this.durationMillis);
    }

    public boolean fades() {
        return this.transition == Transition.FADE;
    }

    public double veil() {
        return this.veil(this.progress());
    }

    public double veil(double progress) {
        return this.fades() ? 1.0D - progress : 0.0D;
    }

    public double scale() {
        return this.scale(this.progress());
    }

    public double scale(double progress) {
        if (this.transition != Transition.SCALE_POP) {
            return 1.0D;
        }
        return 0.92D + 0.08D * progress;
    }

    public int offsetX(int width) {
        return this.offsetX(width, this.progress());
    }

    public int offsetX(int width, double progress) {
        if (this.transition != Transition.SLIDE_SIDE) {
            return 0;
        }
        return (int) Math.round((1.0D - progress) * width * 0.12D);
    }

    public int offsetY(int height) {
        return this.offsetY(height, this.progress());
    }

    public int offsetY(int height, double progress) {
        if (this.transition != Transition.SLIDE_UP) {
            return 0;
        }
        return (int) Math.round((1.0D - progress) * height * 0.10D);
    }
}
