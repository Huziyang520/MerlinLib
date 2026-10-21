package com.huziyang520.merlinlib.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Set;

/**
 * One rule that puts something into loot tables.
 *
 * <p>Immutable: built once through {@link LootInjectionBuilder} and read whenever a table is loaded. The shape
 * is deliberately the one a datapack author would write by hand - target tables, what to drop, which
 * enchantments, how often, and how it competes with the table's own entries.
 */
public final class LootInjection {

    /** What the injected entry spawns. */
    public enum Form {

        /** An enchanted book carrying one of the listed enchantments. */
        BOOK,

        /** The configured item, carrying one of the listed enchantments. */
        ITEM
    }

    private final Set<String> targetTables;
    private final Form form;
    private final Item item;
    private final Set<String> enchantments;
    private final float chance;
    private final int weight;
    private final int quality;

    LootInjection(Set<String> targetTables, Form form, Item item, Set<String> enchantments, float chance,
                  int weight, int quality) {
        this.targetTables = targetTables;
        this.form = form;
        this.item = item;
        this.enchantments = enchantments;
        this.chance = chance;
        this.weight = weight;
        this.quality = quality;
    }

    /** @return the full ids of the loot tables this rule applies to */
    public Set<String> getTargetTables() {
        return this.targetTables;
    }

    /** @return what the entry spawns */
    public Form getForm() {
        return this.form;
    }

    /** @return the item for {@link Form#ITEM}, or {@code null} for {@link Form#BOOK} */
    public Item getItem() {
        return this.item;
    }

    /** @return the full ids of the enchantments the drop may carry */
    public Set<String> getEnchantments() {
        return this.enchantments;
    }

    /** @return the chance of the entry rolling at all, 0.0 to 1.0 */
    public float getChance() {
        return this.chance;
    }

    /** @return the entry's weight against the table's own entries, at least 1 */
    public int getWeight() {
        return this.weight;
    }

    /** @return the entry's quality, which raises its weight when the player has luck */
    public int getQuality() {
        return this.quality;
    }

    @Override
    public String toString() {
        return "LootInjection[" + this.form + " " + this.enchantments + " into " + this.targetTables
                + ", chance " + this.chance + ", weight " + this.weight + ", quality " + this.quality + "]";
    }

    /** @return the book item, kept here so the builder and the injector agree on it */
    public static Item bookItem() {
        return Items.ENCHANTED_BOOK;
    }
}
