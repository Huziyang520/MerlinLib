// Registration entry point for potions.

package com.huziyang520.merlinlib.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Set;

public interface PotionApi {
    PotionBuilder register(ResourceLocation id);
    PotionBuilder potionFor(ResourceLocation effectId);
    Set<ResourceLocation> registeredIds();
}
