package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lifts the 255 ceiling the enchantment level setters clamp to.
 *
 * <h2>Why this is the wall that mattered</h2>
 *
 * <p>{@code ItemEnchantments.Mutable#set} and {@code #merge} write {@code Math.min(level, 255)}, so a level
 * typed into the item editor came back as 255 no matter what {@code max_enchantment_level} allowed. The
 * level codec in the outer class only decides what can be written to disk, and the private constructor only
 * validates; this is the bound the editor actually hits, and it lives on the mutable view, which is a class
 * of its own, so it needs a mixin of its own.
 *
 * <p>The bound is a literal in the bytecode ({@code sipush 255} followed by {@code Math.min}), which is what
 * makes a constant injection the right tool: the surrounding logic, including the "level 0 removes" branch,
 * is left exactly as vanilla wrote it.
 */
@Mixin(ItemEnchantments.Mutable.class)
public class MixinItemEnchantmentsMutable {

    /**
     * Replaces the literal bound with the integer limit.
     *
     * @param bound the {@code 255} literal in the method
     * @return the integer limit
     */
    @ModifyConstant(method = {"set", "merge"}, constant = @Constant(intValue = 255))
    private int merlinlib$liftLevelBound(int bound) {
        return Integer.MAX_VALUE;
    }
}
