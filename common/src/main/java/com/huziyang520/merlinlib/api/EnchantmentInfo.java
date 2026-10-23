package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.content.ContentSource;
import net.minecraft.resources.Identifier;

/**
 * Read-only view of the finally effective state of an enchantment, used for diagnostics such as {@code
 * /merlinlib info <id>}.
 */
public record EnchantmentInfo(
        Identifier id,
        ContentSource source,
        boolean enabled,
        int maxLevel,
        int weight
) {
}
