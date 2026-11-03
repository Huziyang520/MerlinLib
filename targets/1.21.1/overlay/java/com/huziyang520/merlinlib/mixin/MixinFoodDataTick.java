// Fires GlobalEvents#FOOD_REGEN before a player heals from a full hunger bar.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.FoodRegenEvent;
import com.huziyang520.merlinlib.event.GlobalEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FoodData.class)
public class MixinFoodDataTick {
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
