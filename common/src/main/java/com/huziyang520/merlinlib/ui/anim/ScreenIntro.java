package com.huziyang520.merlinlib.ui.anim;

/**
 * The opening animation of a screen: where it is, how big it is and how visible it is, over a short moment.
 *
 * <p>A mod may drive one itself, or ignore the whole thing. It knows nothing about which screen it belongs to,
 * so the same object animates any of them:
 *
 * <pre>{@code
 * ScreenIntro intro = UiAnimation.intro();
 * graphics.pose().translate(intro.offsetX(width), intro.offsetY(height));
 * graphics.pose().scale((float) intro.scale(), (float) intro.scale());
 * }</pre>
 *
 * <h2>Why it is driven by the clock, not by ticks</h2>
 *
 * <p>The first version advanced once per tick and could only be started from a place that ticks. Screens do not
 * tick, and drawing runs many times per tick, so it counted frames and finished several times too fast. Reading
 * the wall clock in {@link #progress()} makes the animation independent of both: a caller asks for the current
 * value whenever it draws, and nothing has to be pumped.
 *
 * <h2>Why it returns offsets rather than drawing</h2>
 *
 * <p>A screen already knows how to draw itself; asking it for its width and height and giving back a
 * translation keeps the animation out of the drawing code, and lets a mod that wants a different effect use the
 * same object with its own transform.
 */
public final class ScreenIntro {

    /** How a screen arrives. */
    public enum Transition {
        /** Grows from 92% with a slight bounce. */
        SCALE_POP,
        /** Slides in from the bottom while fading in. */
        SLIDE_UP,
        /** Slides in from the side it was opened from. */
        SLIDE_SIDE,
        /** Only fades in. */
        FADE
    }

    private final Transition transition;
    private final Easing easing;
    private final long durationMillis;
    private final long startedAt;

    /**
     * Starts an animation now.
     *
     * @param transition     how the screen arrives
     * @param easing         the curve to use
     * @param durationMillis how long it lasts
     */
    public ScreenIntro(Transition transition, Easing easing, long durationMillis) {
        this.transition = transition;
        this.easing = easing;
        this.durationMillis = Math.max(1L, durationMillis);
        this.startedAt = System.currentTimeMillis();
    }

    /**
     * @return an animation that is already over, for a caller that has the feature switched off
     */
    public static ScreenIntro finished() {
        // A one millisecond animation started now: by the time anything asks, progress is already 1, so the
        // caller draws the screen at its final size and position without a branch of its own.
        return new ScreenIntro(Transition.FADE, Easing.LINEAR, 0L);
    }

    /**
     * @param kind the configured choice; any number outside the known ones falls back to the first
     * @return the animation for that choice
     */
    public static ScreenIntro of(int kind) {
        return switch (kind) {
            case 1 -> new ScreenIntro(Transition.SLIDE_UP, Easing.EASE_OUT_CUBIC, 220L);
            case 2 -> new ScreenIntro(Transition.SLIDE_SIDE, Easing.EASE_OUT_CUBIC, 200L);
            case 3 -> new ScreenIntro(Transition.FADE, Easing.LINEAR, 220L);
            default -> new ScreenIntro(Transition.SCALE_POP, Easing.EASE_OUT_BACK, 240L);
        };
    }

    /**
     * A sideways slide, always, whatever the configured kind is.
     *
     * <p>This is what a tab switch uses. The kind setting is the open and close animation and only that: it was
     * a real bug that it also drove the tab switch, because then choosing "slide from the side" for opening a
     * screen silently changed what a tab switch looked like, and the two settings read as one.
     *
     * @return a sideways slide
     */
    public static ScreenIntro sideSlide() {
        return new ScreenIntro(Transition.SLIDE_SIDE, Easing.EASE_OUT_CUBIC, 180L);
    }

    /** @return whether the animation still has frames to play */
    public boolean running() {
        return this.progress() < 1.0D;
    }

    /** @return the eased progress of the animation, between 0 and 1 (the "pop" curve may exceed 1) */
    public double progress() {
        long elapsed = System.currentTimeMillis() - this.startedAt;
        return this.easing.applyClamped((double) elapsed / (double) this.durationMillis);
    }

    /** @return whether this transition fades the screen rather than moving it */
    public boolean fades() {
        return this.transition == Transition.FADE;
    }

    /**
     * How opaque the veil drawn over the screen should be, 1 hiding it completely.
     *
     * <h2>Why it is a veil and not an alpha</h2>
     *
     * <p>The interface is drawn through a render state extractor, which has no colour multiplier: every fill
     * and every piece of text takes its own colour and nothing can scale them all at once. Vanilla widgets are
     * the real problem - they draw themselves, in their own colours, from a place a screen cannot reach. A fade
     * that only dimmed what this library draws would leave the buttons at full brightness, which is exactly the
     * half-working fade this replaces. Painting a dark veil over the finished frame fades everything that is on
     * screen, the player's own widgets included, and it is the closest thing to a fade the interface allows.
     *
     * @return the veil opacity, 0 for a transition that does not fade
     */
    public double veil() {
        return this.veil(this.progress());
    }

    /**
     * @param progress how far the animation has run, 0 to 1
     * @return the veil opacity at that moment
     */
    public double veil(double progress) {
        return this.fades() ? 1.0D - progress : 0.0D;
    }

    /** @return the scale the screen should be drawn at, 1 meaning its final size */
    public double scale() {
        return this.scale(this.progress());
    }

    /**
     * @param progress how far the animation has run, 0 to 1
     * @return the scale for that moment
     */
    public double scale(double progress) {
        if (this.transition != Transition.SCALE_POP) {
            return 1.0D;
        }
        return 0.92D + 0.08D * progress;
    }

    /**
     * @param width the screen width, for transitions that move the whole screen
     * @return how far the screen is pushed to the right
     */
    public int offsetX(int width) {
        return this.offsetX(width, this.progress());
    }

    /**
     * @param width    the screen width, for transitions that move the whole screen
     * @param progress how far the animation has run, 0 to 1
     * @return how far the screen is pushed to the right at that moment
     */
    public int offsetX(int width, double progress) {
        if (this.transition != Transition.SLIDE_SIDE) {
            return 0;
        }
        return (int) Math.round((1.0D - progress) * width * 0.12D);
    }

    /**
     * @param height the screen height, for transitions that move the whole screen
     * @return how far the screen is pushed down
     */
    public int offsetY(int height) {
        return this.offsetY(height, this.progress());
    }

    /**
     * @param height   the screen height, for transitions that move the whole screen
     * @param progress how far the animation has run, 0 to 1
     * @return how far the screen is pushed down at that moment
     */
    public int offsetY(int height, double progress) {
        if (this.transition != Transition.SLIDE_UP) {
            return 0;
        }
        return (int) Math.round((1.0D - progress) * height * 0.10D);
    }
}
