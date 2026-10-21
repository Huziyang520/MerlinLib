package com.huziyang520.merlinlib.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.content.Acquisition;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent description of one enchantment.
 *
 * <p>Typical use:
 * <pre>{@code
 * MerlinApi.enchantments()
 *         .register(Identifier.fromNamespaceAndPath("mymod", "frostbite"))
 *         .maxLevel(3)
 *         .weight(5)
 *         .slots("mainhand")
 *         .supportedItems("#minecraft:enchantable/weapon")
 *         .submit();
 * }</pre>
 *
 * <p>{@link #build()} only produces the immutable draft, {@link #submit()} registers it.
 */
public final class EnchantmentBuilder {

    private final EnchantmentRegistry owner;
    private final Identifier id;

    private boolean enabled = true;
    private JsonElement description;
    private int weight = 10;
    private int maxLevel = 1;
    private int minCostBase = 1;
    private int minCostPerLevel = 11;
    private int maxCostBase = 21;
    private int maxCostPerLevel = 11;
    private int anvilCost = 1;
    private final List<String> slots = new ArrayList<>(List.of("any"));
    private JsonElement supportedItems = EnchantmentJson.tag("minecraft:enchantable/durability");
    private JsonElement primaryItems;
    private JsonElement exclusiveSet;
    private JsonObject effects = new JsonObject();
    private Acquisition acquisition = Acquisition.DEFAULT;

    /**
     * Public only so the registry implementation in another package can construct it; use
     * {@link EnchantmentApi#register(Identifier)} instead of calling this directly.
     */
    public EnchantmentBuilder(EnchantmentRegistry owner, Identifier id) {
        this.owner = owner;
        this.id = id;
    }

    /** Overrides the vanilla {@code description} component, e.g. {@code {"translate": "..."}}. */
    public EnchantmentBuilder description(JsonElement description) {
        this.description = description;
        return this;
    }

    /** Uses {@link EnchantmentDraft#defaultTranslationKey(Identifier)} as description. */
    public EnchantmentBuilder defaultDescription() {
        this.description = EnchantmentJson.defaultDescription(this.id);
        return this;
    }

    public EnchantmentBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    public EnchantmentBuilder maxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
        return this;
    }

    /** Sets both cost ranges at once, mirroring the usual vanilla values. */
    public EnchantmentBuilder cost(int minBase, int minPerLevel, int maxBase, int maxPerLevel) {
        this.minCostBase = minBase;
        this.minCostPerLevel = minPerLevel;
        this.maxCostBase = maxBase;
        this.maxCostPerLevel = maxPerLevel;
        return this;
    }

    public EnchantmentBuilder anvilCost(int anvilCost) {
        this.anvilCost = anvilCost;
        return this;
    }

    /** Replaces the slot list, e.g. {@code "mainhand"}, {@code "armor"}, {@code "any"}. */
    public EnchantmentBuilder slots(String... slots) {
        this.slots.clear();
        this.slots.addAll(Arrays.asList(slots));
        return this;
    }

    /** Item id, {@code #tag} reference or a raw json array. */
    public EnchantmentBuilder supportedItems(String items) {
        this.supportedItems = EnchantmentJson.tag(items);
        return this;
    }

    public EnchantmentBuilder supportedItems(JsonElement items) {
        this.supportedItems = items;
        return this;
    }

    public EnchantmentBuilder primaryItems(String items) {
        this.primaryItems = EnchantmentJson.tag(items);
        return this;
    }

    /** Tag reference, single id or raw json array, matches vanilla {@code exclusive_set}. */
    public EnchantmentBuilder exclusiveSet(JsonElement exclusiveSet) {
        this.exclusiveSet = exclusiveSet;
        return this;
    }

    /** Raw vanilla {@code effects} object, passed through to the generated datapack. */
    public EnchantmentBuilder effects(JsonObject effects) {
        this.effects = effects;
        return this;
    }

    public EnchantmentBuilder acquisition(Acquisition acquisition) {
        this.acquisition = acquisition;
        return this;
    }

    public EnchantmentBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * @return the immutable draft, without registering it.
     */
    public EnchantmentDraft build() {
        return new EnchantmentDraft(
                this.id,
                ContentSource.API,
                this.enabled,
                this.description == null ? EnchantmentJson.defaultDescription(this.id) : this.description,
                this.weight,
                this.maxLevel,
                this.minCostBase,
                this.minCostPerLevel,
                this.maxCostBase,
                this.maxCostPerLevel,
                this.anvilCost,
                this.slots,
                this.supportedItems,
                this.primaryItems,
                this.exclusiveSet,
                this.effects,
                this.acquisition
        );
    }

    /**
     * Validates and registers the enchantment.
     *
     * @return the registered draft
     * @throws IllegalArgumentException when a field is out of range; the message names the field
     */
    public EnchantmentDraft submit() {
        return this.owner.submit(this.build());
    }
}
