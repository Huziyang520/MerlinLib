package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Raises the upper bound of an enchantment level from 255 to the integer limit.
 */
@Mixin(ItemEnchantments.class)
public class MixinItemEnchantments {

    /** Set once so the log does not repeat for every class load attempt. */
    private static boolean merlinlib$logged;

    /**
     * Widens the level range to the integer limit.
     *
     * @param args the arguments of {@code Codec.intRange(min, max)}; index 1 is the upper bound
     */
    @ModifyArgs(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"))
    private static void merlinlib$widenLevelRange(Args args) {
        args.set(1, Integer.MAX_VALUE);
        if (!merlinlib$logged) {
            merlinlib$logged = true;
            Constants.LOG.info("[MerlinLib] enchantment levels are no longer capped at 255 when an item is saved");
        }
    }

    /**
     * Lifts the same bound in the constructor's validation.
     *
     * <p>The setters clamp on their own class (see {@link MixinItemEnchantmentsMutable}); this one is the
     * constructor's check, which throws for any entry above its bound and would otherwise undo a widened
     * level as soon as the map is turned back into an immutable component.
     *
     * @param bound the {@code 255} literal in the constructor
     * @return the integer limit
     */
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255))
    private int merlinlib$liftValidationBound(int bound) {
        return Integer.MAX_VALUE;
    }
}
