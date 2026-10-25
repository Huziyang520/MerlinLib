// Easing curves for interface animation, reachable by any mod.

package com.huziyang520.merlinlib.ui.anim;

public enum Easing {
    LINEAR {
        @Override
        public double apply(double progress) {
            return progress;
        }
    },

    EASE_OUT_CUBIC {
        @Override
        public double apply(double progress) {
            double inverted = 1.0D - progress;
            return 1.0D - inverted * inverted * inverted;
        }
    },

    EASE_IN_OUT_SINE {
        @Override
        public double apply(double progress) {
            return -(Math.cos(Math.PI * progress) - 1.0D) / 2.0D;
        }
    },

    EASE_OUT_BACK {
        @Override
        public double apply(double progress) {
            double overshoot = 1.70158D;
            double shifted = progress - 1.0D;
            return shifted * shifted * ((overshoot + 1.0D) * shifted + overshoot) + 1.0D;
        }
    },

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
    public abstract double apply(double progress);

    public final double applyClamped(double progress) {
        return this.apply(Math.max(0.0D, Math.min(1.0D, progress)));
    }
}
