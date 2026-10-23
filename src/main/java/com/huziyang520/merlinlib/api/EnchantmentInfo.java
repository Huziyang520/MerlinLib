package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.content.ContentSource;
import net.minecraft.resources.ResourceLocation;

public record EnchantmentInfo(
        ResourceLocation id,
        ContentSource source,
        boolean enabled,
        int maxLevel,
        int weight
) {
}
