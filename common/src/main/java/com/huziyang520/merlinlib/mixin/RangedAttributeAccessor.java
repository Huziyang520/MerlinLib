// Access to the upper bound of a ranged attribute, which vanilla keeps in a final field.

package com.huziyang520.merlinlib.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {
    @Mutable
    @Accessor("maxValue")
    void merlinlib$setMaxValue(double value);
}
