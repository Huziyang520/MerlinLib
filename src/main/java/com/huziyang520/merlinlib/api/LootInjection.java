// One rule that puts something into loot tables.

package com.huziyang520.merlinlib.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Set;

public final class LootInjection {
    public enum Form {
        BOOK,

        ITEM
    }

    private final Set<String> targetTables;
    private final Form form;
    private final Item item;
    private final Set<String> enchantments;
    private final float chance;
    private final int weight;
    private final int quality;
    private final boolean moddedTables;

    LootInjection(Set<String> targetTables, Form form, Item item, Set<String> enchantments, float chance,
                  int weight, int quality, boolean moddedTables) {
        this.targetTables = targetTables;
        this.form = form;
        this.item = item;
        this.enchantments = enchantments;
        this.chance = chance;
        this.weight = weight;
        this.quality = quality;
        this.moddedTables = moddedTables;
    }

    public boolean allowsModdedTables() {
        return this.moddedTables;
    }

    public Set<String> getTargetTables() {
        return this.targetTables;
    }

    public Form getForm() {
        return this.form;
    }

    public Item getItem() {
        return this.item;
    }

    public Set<String> getEnchantments() {
        return this.enchantments;
    }

    public float getChance() {
        return this.chance;
    }

    public int getWeight() {
        return this.weight;
    }

    public int getQuality() {
        return this.quality;
    }

    @Override
    public String toString() {
        return "LootInjection[" + this.form + " " + this.enchantments + " into " + this.targetTables
                + ", chance " + this.chance + ", weight " + this.weight + ", quality " + this.quality
                + (this.moddedTables ? ", modded tables allowed" : "") + "]";
    }

    public static Item bookItem() {
        return Items.ENCHANTED_BOOK;
    }
}
