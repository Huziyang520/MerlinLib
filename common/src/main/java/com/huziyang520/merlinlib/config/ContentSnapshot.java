package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable result of reading every content definition once.
 *
 * @param enchantments   the finally effective enchantments, after arbitration and after removing
 *                       everything that is explicitly disabled
 * @param disabled       ids that were explicitly disabled, kept for diagnostics
 * @param effectOverrides effect recolour and disable entries read from {@code effects.json}
 * @param errors         locatable problems; a snapshot with errors is still usable for the parts that
 *                       parsed successfully
 */
public record ContentSnapshot(
        Map<Identifier, EnchantmentDraft> enchantments,
        List<Identifier> disabled,
        Map<Identifier, EffectOverrides.Entry> effectOverrides,
        List<ContentError> errors
) {

    public static final ContentSnapshot EMPTY = new ContentSnapshot(Map.of(), List.of(), Map.of(), List.of());

    public ContentSnapshot {
        enchantments = Map.copyOf(enchantments);
        disabled = List.copyOf(disabled);
        effectOverrides = Map.copyOf(effectOverrides);
        errors = List.copyOf(errors);
    }

    public Optional<EnchantmentDraft> enchantment(Identifier id) {
        return Optional.ofNullable(this.enchantments.get(id));
    }
}
