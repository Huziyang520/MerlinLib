// Raises the upper bound of an enchantment level from 255 to the integer limit.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ItemEnchantments.class)
public class MixinItemEnchantments {
    private static boolean merlinlib$logged;

    @ModifyArgs(method = "<clinit>", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;intRange(II)Lcom/mojang/serialization/Codec;"))
    private static void merlinlib$widenLevelRange(Args args) {
        args.set(1, Integer.MAX_VALUE);
        if (!merlinlib$logged) {
            merlinlib$logged = true;
            Constants.LOG.info("[MerlinLib] enchantment levels are no longer capped at 255 when an item is saved");
        }
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 255))
    private int merlinlib$liftValidationBound(int bound) {
        return Integer.MAX_VALUE;
    }
}
