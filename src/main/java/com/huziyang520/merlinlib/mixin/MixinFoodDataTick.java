package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.FoodRegenEvent;
import com.huziyang520.merlinlib.event.GlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fires {@link GlobalEvents#FOOD_REGEN} before a player heals from a full hunger bar.
 *
 * <p>The hook sits on the {@code Player.heal(float)} call inside {@code FoodData#tick}: that is the
 * only place where "this heal came from saturation and a full hunger bar" is still knowable, and
 * cancelling it there cannot affect potions, beacons or any other source of healing.
 *
 * <h2>1.21.1 names, read out of the bytecode</h2>
 *
 * <p>The 26.x line redirected {@code ServerPlayer.heal(F)V}. On 1.21.1 the invocation owner is the
 * static type, {@code net.minecraft.world.entity.player.Player}, and {@code ServerPlayer} does not
 * override {@code heal} at all - verified with {@code javap -p -c net.minecraft.world.food.FoodData},
 * where both heal call sites read
 * {@code invokevirtual net/minecraft/world/entity/player/Player.heal:(F)V}. Targeting
 * {@code ServerPlayer} would therefore match nothing.
 *
 * <p>The redirect receives a {@code Player}; the event promises a {@code ServerPlayer} because the
 * food tick only runs on the server, so the handler passes the call through untouched for anything
 * that is not one (a client-side prediction, a fake player).
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
            target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void merlinlib$foodRegen(Player player, float amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            player.heal(amount);
            return;
        }
        FoodRegenEvent event = new FoodRegenEvent(serverPlayer, amount);
        GlobalEvents.FOOD_REGEN.invoker().onRegen(event);
        if (!event.isCancelled()) {
            serverPlayer.heal(amount);
        }
    }
}
