// Loader independent description of one enchantment, regardless of whether it was written in java or in a config file.

package com.huziyang520.merlinlib.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import java.util.List;
import java.util.Objects;

public record EnchantmentDraft(
        ResourceLocation id,
        ContentSource source,
        boolean enabled,
        String translationKey,
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
        Acquisition acquisition,
        EnchantmentCategory category
) {
    public EnchantmentDraft {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
        slots = List.copyOf(slots);
        effects = effects == null ? new JsonObject() : effects;
        acquisition = acquisition == null ? Acquisition.DEFAULT : acquisition;
        translationKey = translationKey == null || translationKey.isBlank()
                ? defaultTranslationKey(id) : translationKey;

        category = category == null ? EnchantmentCategory.VANISHABLE : category;
    }

    public static EnchantmentDraft defaults(ResourceLocation id, ContentSource source) {
        return new EnchantmentDraft(
                id,
                source,
                true,
                defaultTranslationKey(id),
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
                Acquisition.DEFAULT,
                null
        );
    }

    public EnchantmentDraft withSource(ContentSource newSource) {
        return new EnchantmentDraft(this.id, newSource, this.enabled, this.translationKey, this.weight, this.maxLevel,
                this.minCostBase, this.minCostPerLevel, this.maxCostBase, this.maxCostPerLevel, this.anvilCost,
                this.slots, this.supportedItems, this.primaryItems, this.exclusiveSet, this.effects, this.acquisition,
                this.category);
    }

    public EnchantmentDraft withAcquisition(Acquisition newAcquisition) {
        return new EnchantmentDraft(this.id, this.source, this.enabled, this.translationKey, this.weight, this.maxLevel,
                this.minCostBase, this.minCostPerLevel, this.maxCostBase, this.maxCostPerLevel, this.anvilCost,
                this.slots, this.supportedItems, this.primaryItems, this.exclusiveSet, this.effects, newAcquisition,
                this.category);
    }

    public EquipmentSlot[] equipmentSlots() {
        List<EquipmentSlot> resolved = new java.util.ArrayList<>();
        for (String name : this.slots) {
            EquipmentSlot slot = slotByName(name);
            if (slot != null && !resolved.contains(slot)) {
                resolved.add(slot);
            }
        }
        if (resolved.isEmpty()) {
            return EquipmentSlot.values();
        }
        return resolved.toArray(new EquipmentSlot[0]);
    }

    private static EquipmentSlot slotByName(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "any", "hand", "armor" -> null;
            case "mainhand", "main_hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off_hand" -> EquipmentSlot.OFFHAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            case "body" -> null;
            default -> null;
        };
    }

    public Enchantment.Rarity rarity() {
        if (this.weight >= 10) {
            return Enchantment.Rarity.COMMON;
        }
        if (this.weight >= 5) {
            return Enchantment.Rarity.UNCOMMON;
        }
        if (this.weight >= 2) {
            return Enchantment.Rarity.RARE;
        }
        return Enchantment.Rarity.VERY_RARE;
    }

    public String translationKey() {
        return this.translationKey;
    }

    public static String defaultTranslationKey(ResourceLocation id) {
        return "enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
