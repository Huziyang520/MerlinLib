package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Caps the amount of health the heart row is drawn from.
 *
 * <h2>The hang this prevents</h2>
 *
 * <p>Vanilla draws one heart per two points of health by looping from
 * {@code ceil(maxHealth / 2) - 1} down to zero. That is fine while the ceiling is vanilla's own 1024, but
 * the health editor can set the ceiling to the integer limit, and a max health of two billion asks the loop
 * for a billion hearts: the game stops responding while it draws them, which is exactly what happened when
 * the editor was used on the player themselves.
 *
 * <p>Vanilla already knows this shape of problem - vehicle health is capped at thirty hearts for the same
 * reason - so capping is not a deviation, it is the same guard applied to the player. Only what the HUD
 * <em>draws</em> is capped: the attribute, the health value, the damage dealt and the network are all
 * untouched, and a health of two billion still works, it is simply shown as a full row of hearts.
 */
@Mixin(Hud.class)
public class MixinHud {

    /** The most health the heart row represents: twenty hearts, which is one full vanilla row. */
    private static final float MAX_DRAWN_HEALTH = 40.0F;

    /**
     * Clamps the health the heart row is built from.
     *
     * <p>{@code maxHealth} is the only {@code float} this method receives, so the first float argument is it;
     * the loop inside can then never ask for more than a row of hearts.
     *
     * @param maxHealth the health the HUD is about to draw
     * @return the same value, capped at {@link #MAX_DRAWN_HEALTH}
     */
    @ModifyVariable(method = "extractHearts", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float merlinlib$capDrawnHealth(float maxHealth) {
        return Math.min(maxHealth, MAX_DRAWN_HEALTH);
    }
}
