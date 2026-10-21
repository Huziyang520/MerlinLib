package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.content.ContentSource;
import net.minecraft.resources.ResourceLocation;

/**
 * Read-only view of the finally effective state of an enchantment, used for diagnostics such as
 * {@code /merlinlib info <id>}.
 *
 * @param id       enchantment id
 * @param source   who won the arbitration for this id
 * @param enabled  whether the entry is active in game
 * @param maxLevel effective max level
 * @param weight   effective weight
 */
public record EnchantmentInfo(
        ResourceLocation id,
        ContentSource source,
        boolean enabled,
        int maxLevel,
        int weight
) {
}
