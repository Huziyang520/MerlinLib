package com.huziyang520.merlinlib.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Builds one {@link LootInjection}.
 *
 * <pre>{@code
 * MerlinApi.loot().register(LootInjectionBuilder.create()
 *         .toTables(LootTables.ANCIENT_CITY)
 *         .asBook()
 *         .withEnchantments("practical_enchantments:chinese")
 *         .chance(0.10F)
 *         .quality(2));
 * }</pre>
 *
 * <p>Defaults are the quiet ones: no table, a book, no enchantment, always rolled, weight 1, quality 0. A rule
 * with no table or no enchantment is rejected when it is built, because it could never do anything and a
 * silent no-op is the hardest kind of mistake to find.
 */
public final class LootInjectionBuilder {

    private final Set<String> targetTables = new LinkedHashSet<>();
    private final Set<String> enchantments = new LinkedHashSet<>();
    private LootInjection.Form form = LootInjection.Form.BOOK;
    private Item item;
    private float chance = 1.0F;
    private int weight = 1;
    private int quality;

    private LootInjectionBuilder() {
    }

    /** @return a new builder */
    public static LootInjectionBuilder create() {
        return new LootInjectionBuilder();
    }

    /**
     * Sets the target tables, replacing any earlier ones.
     *
     * @param tables full table ids, e.g. {@code minecraft:chests/ancient_city}
     * @return this builder
     */
    public LootInjectionBuilder toTables(String... tables) {
        return toTables(new LinkedHashSet<>(Arrays.asList(tables)));
    }

    /**
     * Sets the target tables, replacing any earlier ones.
     *
     * @param tables full table ids
     * @return this builder
     */
    public LootInjectionBuilder toTables(Set<String> tables) {
        this.targetTables.clear();
        this.targetTables.addAll(tables);
        return this;
    }

    /**
     * Adds target tables without dropping the ones already set.
     *
     * @param tables full table ids
     * @return this builder
     */
    public LootInjectionBuilder addTables(String... tables) {
        this.targetTables.addAll(Arrays.asList(tables));
        return this;
    }

    /**
     * Makes the drop an enchanted book.
     *
     * @return this builder
     */
    public LootInjectionBuilder asBook() {
        this.form = LootInjection.Form.BOOK;
        this.item = null;
        return this;
    }

    /**
     * Makes the drop the given item, carrying one of the enchantments.
     *
     * @param item the item to drop
     * @return this builder
     */
    public LootInjectionBuilder asItem(Item item) {
        this.form = LootInjection.Form.ITEM;
        this.item = item;
        return this;
    }

    /**
     * Makes the drop the given item, carrying one of the enchantments.
     *
     * @param item the item to drop
     * @return this builder
     */
    public LootInjectionBuilder asItem(ItemLike item) {
        return asItem(item.asItem());
    }

    /**
     * Sets the enchantments the drop may carry, replacing any earlier ones.
     *
     * @param enchantments full enchantment ids, e.g. {@code practical_enchantments:venom}
     * @return this builder
     */
    public LootInjectionBuilder withEnchantments(String... enchantments) {
        return withEnchantments(new LinkedHashSet<>(Arrays.asList(enchantments)));
    }

    /**
     * Sets the enchantments the drop may carry, replacing any earlier ones.
     *
     * @param enchantments full enchantment ids
     * @return this builder
     */
    public LootInjectionBuilder withEnchantments(Set<String> enchantments) {
        this.enchantments.clear();
        this.enchantments.addAll(enchantments);
        return this;
    }

    /**
     * Sets the chance of the drop rolling at all.
     *
     * @param chance 0.0 to 1.0; 1.0 means the entry always rolls
     * @return this builder
     */
    public LootInjectionBuilder chance(float chance) {
        this.chance = chance;
        return this;
    }

    /**
     * Sets the entry's weight against the table's own entries.
     *
     * @param weight at least 1
     * @return this builder
     */
    public LootInjectionBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    /**
     * Sets the entry's quality, the number that raises its weight when the player has luck.
     *
     * @param quality any value; negative values lower the weight with luck
     * @return this builder
     */
    public LootInjectionBuilder quality(int quality) {
        this.quality = quality;
        return this;
    }

    /**
     * @return the rule
     * @throws IllegalStateException when the rule has no target table or no enchantment
     */
    public LootInjection build() {
        if (this.targetTables.isEmpty()) {
            throw new IllegalStateException("[MerlinLib] a loot injection needs at least one target table");
        }
        if (this.enchantments.isEmpty()) {
            throw new IllegalStateException("[MerlinLib] a loot injection needs at least one enchantment");
        }
        if (this.form == LootInjection.Form.ITEM && this.item == null) {
            throw new IllegalStateException("[MerlinLib] an item loot injection needs the item to drop");
        }
        return new LootInjection(Set.copyOf(this.targetTables), this.form, this.item, Set.copyOf(this.enchantments),
                clampChance(this.chance), Math.max(1, this.weight), this.quality);
    }

    /**
     * @param chance the raw chance
     * @return the chance, held between 0 and 1 so a bad number cannot make an entry unreachable or always roll
     */
    private static float clampChance(float chance) {
        if (Float.isNaN(chance)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, chance));
    }
}
