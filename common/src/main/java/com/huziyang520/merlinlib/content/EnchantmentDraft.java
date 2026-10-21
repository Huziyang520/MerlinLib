package com.huziyang520.merlinlib.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * Loader independent description of one enchantment, regardless of whether it was written in java
 * or in a config file.
 *
 * <p>The field names intentionally mirror the vanilla enchantment datapack format so that the
 * generated JSON needs no translation layer. {@code acquisition} and {@code enabled} are MerlinLib
 * extensions and never written into the vanilla schema directly.
 *
 * @param id              full identifier, namespace defaulted to {@code merlinlib}
 * @param source          who declared it, decides override priority
 * @param enabled         {@code false} removes the entry from the game no matter who else declares it
 * @param description     vanilla {@code description} component, e.g. {@code {"translate": "..."}}
 * @param weight          vanilla {@code weight}, 1..1024
 * @param maxLevel        vanilla {@code max_level}, 1..255
 * @param minCostBase     vanilla {@code min_cost.base}
 * @param minCostPerLevel vanilla {@code min_cost.per_level_above_first}
 * @param maxCostBase     vanilla {@code max_cost.base}
 * @param maxCostPerLevel vanilla {@code max_cost.per_level_above_first}
 * @param anvilCost       vanilla {@code anvil_cost}
 * @param slots           vanilla {@code slots}, e.g. {@code ["mainhand"]} or {@code ["any"]}
 * @param supportedItems  vanilla {@code supported_items}, item id, {@code #tag} or a json array
 * @param primaryItems    vanilla {@code primary_items}, optional
 * @param exclusiveSet    vanilla {@code exclusive_set}, tag, id or array
 * @param effects         vanilla {@code effects} object, passed through untouched
 * @param acquisition     MerlinLib extension translated into vanilla {@code tags/enchantment/*}
 */
public record EnchantmentDraft(
        Identifier id,
        ContentSource source,
        boolean enabled,
        JsonElement description,
        int weight,
        int maxLevel,
        int minCostBase,
        int minCostPerLevel,
        int maxCostBase,
        int maxCostPerLevel,
        int anvilCost,
        List<String> slots,
        JsonElement supportedItems,
        JsonElement primaryItems,
        JsonElement exclusiveSet,
        JsonObject effects,
        Acquisition acquisition
) {

    public EnchantmentDraft {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        slots = List.copyOf(slots);
        effects = effects == null ? new JsonObject() : effects;
        acquisition = acquisition == null ? Acquisition.DEFAULT : acquisition;
    }

    /** Sensible defaults matching a plain level 1..3 weapon enchantment. */
    public static EnchantmentDraft defaults(Identifier id, ContentSource source) {
        return new EnchantmentDraft(
                id,
                source,
                true,
                EnchantmentJson.defaultDescription(id),
                10,
                1,
                1,
                11,
                21,
                11,
                1,
                List.of("any"),
                EnchantmentJson.tag("minecraft:enchantable/durability"),
                null,
                null,
                new JsonObject(),
                Acquisition.DEFAULT
        );
    }

    public EnchantmentDraft withSource(ContentSource newSource) {
        return new EnchantmentDraft(this.id, newSource, this.enabled, this.description, this.weight, this.maxLevel,
                this.minCostBase, this.minCostPerLevel, this.maxCostBase, this.maxCostPerLevel, this.anvilCost,
                this.slots, this.supportedItems, this.primaryItems, this.exclusiveSet, this.effects, this.acquisition);
    }

    public EnchantmentDraft withAcquisition(Acquisition newAcquisition) {
        return new EnchantmentDraft(this.id, this.source, this.enabled, this.description, this.weight, this.maxLevel,
                this.minCostBase, this.minCostPerLevel, this.maxCostBase, this.maxCostPerLevel, this.anvilCost,
                this.slots, this.supportedItems, this.primaryItems, this.exclusiveSet, this.effects, newAcquisition);
    }

    /**
     * @return the translation key used by the generated {@code description} component.
     */
    public String translationKey() {
        if (this.description != null && this.description.isJsonObject()) {
            JsonElement key = this.description.getAsJsonObject().get("translate");
            if (key != null && key.isJsonPrimitive()) {
                return key.getAsString();
            }
        }
        return defaultTranslationKey(this.id);
    }

    public static String defaultTranslationKey(Identifier id) {
        return "enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
