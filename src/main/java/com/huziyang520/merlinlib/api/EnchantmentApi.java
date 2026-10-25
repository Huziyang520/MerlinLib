// Registration and inspection entry point for enchantments.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import java.util.Set;

public interface EnchantmentApi {
    EnchantmentBuilder register(ResourceLocation id);
    boolean disable(ResourceLocation id);
    boolean enable(ResourceLocation id);
    Set<ResourceLocation> disabledIds();
    Set<ResourceLocation> registeredIds();
    Optional<EnchantmentInfo> describe(ResourceLocation id);
}
