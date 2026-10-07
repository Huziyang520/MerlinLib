package com.huziyang520.merlinlib.config;

import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable result of reading every content definition once.
 *
 * <p>The 26.3 line called this a snapshot because a data driven registry could change while the game
 * ran and {@code /reload} rebuilt it. On 1.20.1 the registry is frozen before mod constructors run,
 * so this is read once per launch; the name and the shape are kept because the diagnostics, the
 * command output and the arbitration rules all speak in terms of it.
 *
 * @param enchantments    the finally effective enchantments, after arbitration and after removing
 *                        everything that is explicitly disabled
 * @param disabled        ids that were explicitly disabled, kept for diagnostics
 * @param effectOverrides effect recolour and disable entries read from {@code effects.json}
 * @param errors          locatable problems; a snapshot with errors is still usable for the parts that
 *                        parsed successfully
 */
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
