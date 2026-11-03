// Immutable result of reading every content definition once.

package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ContentSnapshot(
        Map<ResourceLocation, EnchantmentDraft> enchantments,
        List<ResourceLocation> disabled,
        Map<ResourceLocation, EffectOverrides.Entry> effectOverrides,
        List<ContentError> errors
) {
    public static final ContentSnapshot EMPTY = new ContentSnapshot(Map.of(), List.of(), Map.of(), List.of());

    public ContentSnapshot {
        enchantments = Map.copyOf(enchantments);
        disabled = List.copyOf(disabled);
        effectOverrides = Map.copyOf(effectOverrides);
        errors = List.copyOf(errors);
    }

    public Optional<EnchantmentDraft> enchantment(ResourceLocation id) {
        return Optional.ofNullable(this.enchantments.get(id));
    }
}
