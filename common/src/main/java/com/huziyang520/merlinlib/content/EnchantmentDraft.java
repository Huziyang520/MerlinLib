// Loader independent description of one enchantment, regardless of whether it was written in java or in a config file.

package com.huziyang520.merlinlib.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Objects;

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
