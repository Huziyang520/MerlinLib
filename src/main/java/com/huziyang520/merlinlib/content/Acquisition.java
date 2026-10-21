package com.huziyang520.merlinlib.content;

/**
 * How an enchantment can be obtained in survival.
 *
 * <p>Vanilla has no such field on the enchantment itself; every channel is expressed through
 * {@code minecraft:tags/enchantment/*}. MerlinLib translates this record into tag entries inside the
 * generated datapack, which is why changing a channel only requires {@code /reload}.
 *
 * @param enchantingTable  can be offered by the enchanting table
 * @param villagerTrade    librarian trades may offer it
 * @param fishing          can be found while fishing
 * @param lootChest        can appear in generated loot chests
 * @param treasureOnly     excluded from enchanting tables and villager trades
 * @param curse            treated as a curse (red tooltip, removable only by grindstone/anvil)
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
