// Registration and inspection entry point for enchantments.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.Identifier;
import java.util.Optional;
import java.util.Set;

public interface EnchantmentApi {
    EnchantmentBuilder register(Identifier id);
    boolean disable(Identifier id);
    boolean enable(Identifier id);
    Set<Identifier> disabledIds();
    Set<Identifier> registeredIds();
    Optional<EnchantmentInfo> describe(Identifier id);
}
