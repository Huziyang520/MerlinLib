package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.FoodRegenEvent;
import com.huziyang520.merlinlib.event.GlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fires {@link GlobalEvents#FOOD_REGEN} before a player heals from a full hunger bar.
 *
 * <h2>Why the heal call is redirected rather than the whole tick</h2>
 *
 * <p>Cancelling {@code FoodData.tick} would be easy and wrong: the same method also burns exhaustion, spends
 * saturation and starves a player at zero hunger. Redirecting the heal instead leaves every other part of
 * eating and hunger exactly as vanilla has it, and gives the event the amount that was about to be restored -
 * which is what a "no natural regeneration, something else instead" enchantment needs.
 *
 * <p>The redirect covers both call sites in the method: the fast regeneration from saturation and the slower
 * one from an empty stomach, so neither can slip past.
 */
@Mixin(FoodData.class)
public class MixinFoodDataTick {

    /**
     * Asks the listeners, then heals unless one of them cancelled.
     *
     * @param player the player about to heal
     * @param amount the health that would be restored
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void merlinlib$foodRegen(ServerPlayer player, float amount) {
        FoodRegenEvent event = new FoodRegenEvent(player, amount);
        GlobalEvents.FOOD_REGEN.invoker().onRegen(event);
        if (!event.isCancelled()) {
            player.heal(amount);
        }
    }
}
