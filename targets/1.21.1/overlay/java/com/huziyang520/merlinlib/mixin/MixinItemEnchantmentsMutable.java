// Lifts the 255 ceiling the enchantment level setters clamp to.

package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ItemEnchantments.Mutable.class)
public class MixinItemEnchantmentsMutable {
    @ModifyConstant(method = {"set", "upgrade"}, constant = @Constant(intValue = 255))
    private int merlinlib$liftLevelBound(int bound) {
        return Integer.MAX_VALUE;
    }
}
