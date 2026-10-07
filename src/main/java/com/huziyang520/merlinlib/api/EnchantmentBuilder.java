package com.huziyang520.merlinlib.api;

import com.google.gson.JsonObject;
import com.huziyang520.merlinlib.content.Acquisition;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent description of one enchantment.
 *
 * <p>Typical use:
 * <pre>{@code
 * MerlinApi.enchantments()
 *         .register(new ResourceLocation("mymod", "frostbite"))
 *         .maxLevel(3)
 *         .weight(5)
 *         .slots("mainhand")
 *         .supportedItems("#minecraft:enchantable/weapon")
 *         .submit();
 * }</pre>
 *
 * <p>{@link #build()} only produces the immutable draft, {@link #submit()} registers it.
 *
 * <h2>What still takes json, and why</h2>
 *
 * <p>{@link #supportedItems(String)}, {@link #primaryItems(String)} and {@link #exclusiveSet} keep
 * the vanilla datapack spellings - {@code "#namespace:tag"}, {@code "namespace:item"} or a list -
 * because that is what the documentation and the existing config files write. They are parsed into
 * code by {@code EnchantmentRegistry} at registration time; nothing on 1.20.1 reads a tag for these
 * fields.
 *
 * <p>{@link #description(JsonElement)} and {@link #effects(JsonObject)} are kept for source
 * compatibility with the 26.3 line. On 1.20.1 the description is a translation key
 * ({@link #translationKey(String)}) and the numeric effect system does not exist
 * ({@link #effects(JsonObject)} records the value and reports that it cannot be applied).
 */
public final class EnchantmentBuilder {

    private final EnchantmentRegistry owner;
    private final ResourceLocation id;

    private boolean enabled = true;
    private String translationKey;
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
    private net.minecraft.world.item.enchantment.EnchantmentCategory category;

    /**
     * Public only so the registry implementation in another package can construct it; use
     * {@link EnchantmentApi#register(ResourceLocation)} instead of calling this directly.
     */
    public EnchantmentBuilder(EnchantmentRegistry owner, ResourceLocation id) {
        this.owner = owner;
        this.id = id;
        this.translationKey = EnchantmentDraft.defaultTranslationKey(id);
    }

    /** Sets the language key the tooltip reads, e.g. {@code enchantment.mymod.frostbite}. */
    public EnchantmentBuilder translationKey(String translationKey) {
        this.translationKey = translationKey;
        return this;
    }

    /**
     * Accepts the vanilla {@code description} component shape of the 26.3 line and keeps only its
     * {@code translate} value, which is the only part 1.20.1 has a place for.
     *
     * <p>A literal {@code text} description has nowhere to go on this version: the enchantment's name
     * is a translation key looked up by the tooltip, and a hard coded string would show up untranslated
     * in every language. It is accepted and ignored rather than rejected, so a file carried over from
     * 26.3 still loads.
     */
    public EnchantmentBuilder description(JsonElement description) {
        if (description != null && description.isJsonObject()) {
            JsonElement key = description.getAsJsonObject().get("translate");
            if (key != null && key.isJsonPrimitive()) {
                this.translationKey = key.getAsString();
            }
        }
        return this;
    }

    /** Uses {@link EnchantmentDraft#defaultTranslationKey(ResourceLocation)} as description. */
    public EnchantmentBuilder defaultDescription() {
        this.translationKey = EnchantmentDraft.defaultTranslationKey(this.id);
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

    /**
     * Records the declared numeric effects.
     *
     * <p>Kept so a caller written for the 26.3 line still compiles and so {@code /merlinlib info} can
     * show what was declared. It is <b>not applied</b>: the numeric effect system it describes
     * ({@code minecraft:add}, {@code LevelBasedValue}) is 1.20.5+, and on this version behaviour comes
     * from the event api. A non-empty value is reported once, by id, at registration time - see
     * {@code EnchantmentRegistry#submit}.
     */
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
     * Sets the vanilla {@code EnchantmentCategory}.
     *
     * <p>1.20.1 takes a category in the enchantment's constructor and uses it for two things: the
     * enchanting table asks {@code EnchantmentCategory#canEnchant(Item)} before it offers an entry,
     * and the anvil's book rules look at it. A MerlinLib enchantment declares its own item set
     * through {@code supported_items}, so the default ({@code VANISHABLE}, which accepts anything) is
     * normally right and this is only needed to opt out of a vanilla list - which is exactly what
     * {@code merlinlib:unbreakable} does with {@code BREAKABLE}.
     *
     * @param category the vanilla category
     * @return this builder
     */
    public EnchantmentBuilder category(net.minecraft.world.item.enchantment.EnchantmentCategory category) {
        this.category = category;
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
                this.translationKey,
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
                this.acquisition,
                this.category
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
