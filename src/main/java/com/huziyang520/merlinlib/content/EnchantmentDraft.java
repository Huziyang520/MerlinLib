package com.huziyang520.merlinlib.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.List;
import java.util.Objects;

/**
 * Loader independent description of one enchantment, regardless of whether it was written in java
 * or in a config file.
 *
 * <h2>What changed from the 26.3 line, and why it had to</h2>
 *
 * <p>On 26.3 this record was the shape of a vanilla enchantment <em>datapack file</em>: the field
 * names mirrored {@code data/<ns>/enchantment/<name>.json} one for one, the values that datapack
 * could not express were carried as raw {@link JsonElement}s and translated on the way out, and a
 * generated datapack was what made them real.
 *
 * <p>1.20.1 has no data driven enchantment registry. An enchantment is a code object
 * ({@code DeferredRegister<Enchantment>}), created once during the registry event, and the things a
 * datapack would have said are said by overriding its methods. The draft therefore keeps the
 * <em>documented public field set</em> - so a {@code config/MerlinLib/*.json} file still looks and
 * reads exactly the same, and every value keeps its meaning - but the values are the plain java
 * types the constructor needs, and the ones that cannot exist here are resolved instead of passed
 * through:
 *
 * <ul>
 *   <li>{@code slots} becomes {@link #equipmentSlots()}, the {@link EquipmentSlot} array
 *       {@code Enchantment}'s constructor takes</li>
 *   <li>{@code supported_items} becomes a predicate, because
 *       {@code Enchantment#canEnchant(ItemStack)} is a method rather than a tag reference</li>
 *   <li>{@code exclusive_set} becomes a set of ids, because
 *       {@code Enchantment#checkCompatibility(Enchantment)} is a method rather than a tag</li>
 *   <li>{@code effects} is kept verbatim for the record and for {@code /merlinlib info}, and is
 *       reported as unusable rather than silently dropped: the numeric effect system it describes
 *       ({@code minecraft:add}, {@code LevelBasedValue}) arrives in 1.20.5. Behaviour on this
 *       version comes from the event api, which is what Practical Enchantments uses</li>
 * </ul>
 *
 * <p>Kept deliberately absent: the 26.3 {@code description} component. Enchantment names in 1.20.1
 * are translation keys ({@code enchantment.<namespace>.<path>}), which is what the config file's
 * {@code translation_key} already produces, and the vanilla tooltip reads exactly that key. See
 * {@link #translationKey()}.
 *
 * @param id              full identifier, namespace defaulted to {@code merlinlib}
 * @param source          who declared it, decides override priority
 * @param enabled         {@code false} removes the entry from the game no matter who else declares it
 * @param translationKey  the language key the tooltip reads, normally
 *                        {@code enchantment.<namespace>.<path>}
 * @param weight          rarity weight, 1..1024, exposed through {@code Enchantment#getRarity().getWeight()}
 * @param maxLevel        maximum level, 1..255, returned by {@code Enchantment#getMaxLevel()}
 * @param minCostBase     {@code min_cost.base}
 * @param minCostPerLevel {@code min_cost.per_level_above_first}
 * @param maxCostBase     {@code max_cost.base}
 * @param maxCostPerLevel {@code max_cost.per_level_above_first}
 * @param anvilCost       {@code anvil_cost}
 * @param slots           {@code slots}, e.g. {@code ["mainhand"]} or {@code ["any"]}
 * @param supportedItems  {@code supported_items}, item id, {@code #tag} or a json array
 * @param primaryItems    {@code primary_items}, optional; on 1.20.1 it decides whether the
 *                        enchantment may be offered at the enchanting table
 * @param exclusiveSet    {@code exclusive_set}, tag, id or array; matched against registered ids
 * @param effects         the declared numeric effects, kept for the record and never applied here
 * @param acquisition     how it can be obtained, translated onto the enchantment methods
 */
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
        // VANISHABLE is the category whose own canEnchant(Item) says "anything", which is the right
        // default for an enchantment that declared its own item set: the real filter is
        // canEnchant(ItemStack), driven by supported_items. A caller that needs the category for
        // something else - the enchanting table filters its offers by category - sets it explicitly.
        category = category == null ? EnchantmentCategory.VANISHABLE : category;
    }

    /** Sensible defaults matching a plain level 1..3 weapon enchantment. */
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

    /**
     * @return the equipment slots the declared {@link #slots()} names resolve to.
     *
     * <p>{@code any} and unknown names mean "all slots", which is what
     * {@code EquipmentSlot.values()} is. The list is never empty: an empty slot set would make an
     * enchantment unreachable through every equipment lookup, and a config typo must not be able to
     * do that.
     */
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

    /**
     * Maps a slot name written in a config file onto {@link EquipmentSlot}.
     *
     * <p>Accepts both the vanilla datapack spelling ({@code any}, {@code hand}, {@code armor},
     * {@code mainhand}, ...) and the enum's own constant names, so a file written for the 26.3 line
     * keeps working unchanged.
     *
     * @param name the name as written
     * @return the slot, or {@code null} for {@code any} and for names with no 1.20.1 equivalent
     */
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
            // `body` is the 1.21+ horse armour slot and has no 1.20.1 equivalent, so it resolves to
            // "no particular slot" like `any` rather than to a wrong one.
            case "body" -> null;
            default -> null;
        };
    }

    /**
     * @return the rarity the declared {@link #weight()} corresponds to.
     *
     * <p>1.20.1 takes a {@code Rarity} enum, not a numeric weight, so the number is mapped onto the
     * four vanilla bands. The bands are the vanilla ones (COMMON 10, UNCOMMON 5, RARE 2, VERY_RARE 1)
     * and the mapping is the nearest band, so a file that says {@code "weight": 5} gets exactly the
     * rarity that number would have meant on the data driven version.
     */
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

    /**
     * @return the translation key used for the enchantment's name.
     */
    public String translationKey() {
        return this.translationKey;
    }

    public static String defaultTranslationKey(ResourceLocation id) {
        return "enchantment." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }
}
