// How an enchantment can be obtained in survival.

package com.huziyang520.merlinlib.content;

public record Acquisition(
        boolean enchantingTable,
        boolean villagerTrade,
        boolean fishing,
        boolean lootChest,
        boolean treasureOnly,
        boolean curse
) {
    public static final Acquisition DEFAULT = new Acquisition(true, true, true, true, false, false);
    public static final Acquisition TREASURE_ONLY = new Acquisition(false, false, false, false, true, false);

    public boolean commonChannel() {
        return this.enchantingTable || this.villagerTrade;
    }

    public boolean randomLoot() {
        return this.fishing || this.lootChest;
    }
}
