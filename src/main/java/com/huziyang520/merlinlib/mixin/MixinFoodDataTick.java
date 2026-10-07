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
 * <h2>Why the heal call is redirected rather than the whole tick</h2>
 *
 * <p>Cancelling {@code FoodData.tick} would be easy and wrong: the same method also burns exhaustion,
 * spends saturation and starves a player at zero hunger. Redirecting the heal instead leaves every
 * other part of eating and hunger exactly as vanilla has it, and gives the event the amount that was
 * about to be restored - which is what a "no natural regeneration, something else instead"
 * enchantment needs.
 *
 * <p>The redirect covers both call sites in the method: the fast regeneration from saturation and the
 * slower one from an empty stomach, so neither can slip past.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>The injection point is unchanged, and the bytecode even confirms the "two call sites" claim the
 * 26.3 comment makes. Verified with {@code javap -p -c net.minecraft.world.food.FoodData}, which shows
 * both reaches into {@code Player.heal}:
 *
 * <pre>
 *      157: aload_1
 *      158: fload         4
 *      160: ldc           #127   // float 6.0f
 *      162: fdiv
 *      163: invokevirtual #131   // Method net/minecraft/world/entity/player/Player.heal:(F)V
 *      ...
 *      219: aload_1
 *      220: fconst_1
 *      221: invokevirtual #131   // Method net/minecraft/world/entity/player/Player.heal:(F)V
 * </pre>
 *
 * <p>and with {@code javap -p -s net.minecraft.world.food.FoodData}:
 *
 * <pre>
 * public void tick(net.minecraft.world.entity.player.Player);
 *   descriptor: (Lnet/minecraft/world/entity/player/Player;)V
 * </pre>
 *
 * <ol>
 *     <li>The redirected method is {@code Player.heal(F)V}, not {@code ServerPlayer.heal(F)V}. On this
 *     version {@code Player} declares {@code heal} itself (inherited from {@code LivingEntity}) and
 *     {@code ServerPlayer} does not override it, so the constant pool entry at both call sites names
 *     {@code Player}. Targeting {@code ServerPlayer} here would find no call at all and, with
 *     {@code require = 1}, would abort startup - which is the desired failure, but only if the target
 *     is written correctly in the first place.</li>
 *     <li>{@code heal} returns {@code void}, so the redirect handler returns {@code void} too. On the
 *     26.3 line it was the same, so no shape change is needed.</li>
 * </ol>
 *
 * <h2>The handler's first parameter is the second trap, and it fired on this version</h2>
 *
 * <p>The first port declared the handler as {@code (ServerPlayer player, float amount)} - which reads
 * naturally, because the event only exists for a server player - and opening a world failed with
 *
 * <pre>
 * InvalidInjectionException: @Redirect handler method FoodData::merlinlib$foodRegen has an invalid
 * signature. Found unexpected argument type net.minecraft.server.level.ServerPlayer at index 0,
 * expected net.minecraft.world.entity.player.Player.
 * Handler signature: (Lnet/minecraft/server/level/ServerPlayer;F)V
 * Expected signature: (Lnet/minecraft/world/entity/player/Player;F)V
 * </pre>
 *
 * <p>Mixin validates the handler against the <em>call site</em>, not against the event: both call
 * sites load the receiver from the method's own {@code Player player} parameter ({@code aload_1}), so
 * the handler's first parameter has to be declared {@code Player}. The {@code ServerPlayer} the event
 * needs is obtained inside, by narrowing the parameter; anything that is not a server player - the
 * client's own food tick among them, since {@code Player.tick} calls {@code foodData.tick(this)} on
 * both sides - falls straight through to the vanilla heal untouched.
 *
 * <p>{@code require = 1} is explicit. {@code FoodData.tick} runs for every player twenty times a
 * second, so a silently missing hook here would mean a disabled natural regeneration rule that nobody
 * could see was broken.
 */
@Mixin(FoodData.class)
public class MixinFoodDataTick {

    /**
     * Asks the listeners, then heals unless one of them cancelled.
     *
     * @param player the player about to heal; declared {@code Player} because that is the type the
     *               call site passes, then narrowed to {@code ServerPlayer} before the event is built
     * @param amount the health that would be restored
     */
    @Redirect(method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"),
            require = 1)
    private void merlinlib$foodRegen(Player player, float amount) {
        if (player instanceof ServerPlayer serverPlayer) {
            FoodRegenEvent event = new FoodRegenEvent(serverPlayer, amount);
            GlobalEvents.FOOD_REGEN.invoker().onRegen(event);
            if (event.isCancelled()) {
                return;
            }
        }
        player.heal(amount);
    }
}
