// Fluent description of one enchantment.

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

    public EnchantmentBuilder(EnchantmentRegistry owner, Identifier id) {
        this.owner = owner;
        this.id = id;
    }

    public EnchantmentBuilder description(JsonElement description) {
        this.description = description;
        return this;
    }

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

    public EnchantmentBuilder slots(String... slots) {
        this.slots.clear();
        this.slots.addAll(Arrays.asList(slots));
        return this;
    }

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

    public EnchantmentBuilder exclusiveSet(JsonElement exclusiveSet) {
        this.exclusiveSet = exclusiveSet;
        return this;
    }

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

    public EnchantmentDraft submit() {
        return this.owner.submit(this.build());
    }
}
