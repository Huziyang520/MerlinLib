package com.huziyang520.merlinlib.content;

/**
 * How an enchantment can be obtained in survival.
 */
public record Acquisition(
        boolean enchantingTable,
        boolean villagerTrade,
        boolean fishing,
        boolean lootChest,
        boolean treasureOnly,
        boolean curse
) {

    /** Vanilla-like default: obtainable everywhere except treasure/curse channels. */
    public static final Acquisition DEFAULT = new Acquisition(true, true, true, true, false, false);

    /** Treasure-only default, used by e.g. {@code merlinlib:unbreakable}. */
    public static final Acquisition TREASURE_ONLY = new Acquisition(false, false, false, false, true, false);

    /**
     * @return the channel used by the enchanting table and villager trades, {@code false} when the
     *         enchantment is treasure only.
     */
    public boolean commonChannel() {
        return this.enchantingTable || this.villagerTrade;
    }

    public boolean randomLoot() {
        return this.fishing || this.lootChest;
    }
}
