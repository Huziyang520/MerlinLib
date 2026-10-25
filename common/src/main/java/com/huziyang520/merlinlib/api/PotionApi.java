// Registration entry point for potions.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.Identifier;
import java.util.Set;

public interface PotionApi {
    PotionBuilder register(Identifier id);
    PotionBuilder potionFor(Identifier effectId);
    Set<Identifier> registeredIds();
}
