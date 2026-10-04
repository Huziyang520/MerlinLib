package com.huziyang520.merlinlib.ui.anim;

/**
 * Easing curves for interface animation, reachable by any mod.
 *
 * <p>An easing function takes a linear progress in {@code [0, 1]} and returns the value to use instead. The
 * curves are the ones a screen actually needs - nothing overshoots past its target except {@link #EASE_OUT_BACK},
 * which is there for the "pop" an opening window wants - and every one of them ends exactly at {@code 1}, so an
 * animation that is told it has finished lands precisely where it should.
 *
 * <p>Pure functions on doubles: they know nothing about Minecraft, screens or tick rates, which is what makes
 * them usable from anywhere (a mod may drive its own animation with them and never touch the rest of the
 * library).
 */
public enum Easing {

    /** No easing: linear progress. */
    LINEAR {
        @Override
        public double apply(double progress) {
            return progress;
        }
    },

    /** Starts fast and settles gently; the default for anything that slides or grows. */
    EASE_OUT_CUBIC {
        @Override
        public double apply(double progress) {
            double inverted = 1.0D - progress;
            return 1.0D - inverted * inverted * inverted;
        }
    },

    /** Slow, fast, slow: for a movement that has a clear start and end. */
    EASE_IN_OUT_SINE {
        @Override
        public double apply(double progress) {
            return -(Math.cos(Math.PI * progress) - 1.0D) / 2.0D;
        }
    },

    /**
     * Overshoots slightly before settling - the "pop" of a window that jumps open.
     *
     * <p>The value exceeds {@code 1} in the middle of the curve on purpose: a caller that scales by it will see
     * the window grow a little past its final size and come back, which is the whole effect.
     */
    EASE_OUT_BACK {
        @Override
        public double apply(double progress) {
            double overshoot = 1.70158D;
            double shifted = progress - 1.0D;
            return shifted * shifted * ((overshoot + 1.0D) * shifted + overshoot) + 1.0D;
        }
    },

    /** Springs in from below zero: for something that should punch in from the bottom of the screen. */
    EASE_OUT_ELASTIC {
        @Override
        public double apply(double progress) {
            if (progress <= 0.0D) {
                return 0.0D;
            }
            if (progress >= 1.0D) {
                return 1.0D;
            }
            double period = 0.3D;
            double eased = Math.pow(2.0D, -10.0D * progress);
            return eased * Math.sin((progress - period / 4.0D) * (2.0D * Math.PI) / period) + 1.0D;
        }
    };

    /**
     * @param progress linear progress, clamped to {@code [0, 1]}
     * @return the eased value
     */
    public abstract double apply(double progress);

    /**
     * @param progress linear progress, may be outside {@code [0, 1]}
     * @return the eased value for the clamped progress
     */
    public final double applyClamped(double progress) {
        return this.apply(Math.max(0.0D, Math.min(1.0D, progress)));
    }
}
