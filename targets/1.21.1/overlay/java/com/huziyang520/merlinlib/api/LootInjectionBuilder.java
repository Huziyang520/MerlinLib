// Builds one LootInjection.

package com.huziyang520.merlinlib.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LootInjectionBuilder {
    private final Set<String> targetTables = new LinkedHashSet<>();
    private final Set<String> enchantments = new LinkedHashSet<>();
    private LootInjection.Form form = LootInjection.Form.BOOK;
    private Item item;
    private float chance = 1.0F;
    private int weight = 1;
    private int quality;
    private boolean moddedTables;

    private LootInjectionBuilder() {
    }

    public static LootInjectionBuilder create() {
        return new LootInjectionBuilder();
    }

    public LootInjectionBuilder toTables(String... tables) {
        return toTables(new LinkedHashSet<>(Arrays.asList(tables)));
    }

    public LootInjectionBuilder toTables(Set<String> tables) {
        this.targetTables.clear();
        this.targetTables.addAll(tables);
        return this;
    }

    public LootInjectionBuilder addTables(String... tables) {
        this.targetTables.addAll(Arrays.asList(tables));
        return this;
    }

    public LootInjectionBuilder asBook() {
        this.form = LootInjection.Form.BOOK;
        this.item = null;
        return this;
    }

    public LootInjectionBuilder asItem(Item item) {
        this.form = LootInjection.Form.ITEM;
        this.item = item;
        return this;
    }

    public LootInjectionBuilder asItem(ItemLike item) {
        return asItem(item.asItem());
    }

    public LootInjectionBuilder withEnchantments(String... enchantments) {
        return withEnchantments(new LinkedHashSet<>(Arrays.asList(enchantments)));
    }

    public LootInjectionBuilder withEnchantments(Set<String> enchantments) {
        this.enchantments.clear();
        this.enchantments.addAll(enchantments);
        return this;
    }

    public LootInjectionBuilder chance(float chance) {
        this.chance = chance;
        return this;
    }

    public LootInjectionBuilder weight(int weight) {
        this.weight = weight;
        return this;
    }

    public LootInjectionBuilder quality(int quality) {
        this.quality = quality;
        return this;
    }

    public LootInjectionBuilder allowModdedTables() {
        this.moddedTables = true;
        return this;
    }

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
                clampChance(this.chance), Math.max(1, this.weight), this.quality, this.moddedTables);
    }

    private static float clampChance(float chance) {
        if (Float.isNaN(chance)) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, chance));
    }
}
