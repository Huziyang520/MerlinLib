package com.huziyang520.merlinlib.content;

/**
 * How an enchantment can be obtained in survival.
 *
 * <p>Vanilla expresses this through {@code minecraft:tags/enchantment/*} -
 * {@code in_enchanting_table}, {@code tradeable}, {@code on_random_loot}, {@code treasure},
 * {@code curse}, {@code prevents_bribing} - and on the 26.3 line MerlinLib wrote those tags into a
 * datapack it generated at runtime.
 *
 * <p><b>That machinery does not exist on 1.20.1.</b> Enchantments are a plain static registry here, a
 * {@code DeferredRegister} entry cannot carry a tag, and an entry cannot be tagged before the
 * registry event has closed. A tag file under {@code data/} could carry the channels a dependent mod
 * declares, but only because <em>this</em> mod ships it: a mod that registers an enchantment through
 * {@link com.huziyang520.merlinlib.api.MerlinApi} at runtime has no file to write to.
 *
 * <p>So the record stays as the description of what was asked for, and the translation happens where
 * 1.20.1 can actually honour it:
 * <ul>
 *     <li>{@link #commonChannel()} - "may the enchanting table offer it" is answered by
 *     {@code Enchantment#isDiscoverable()}, which {@code EnchantmentTableMenu} consults before
 *     offering an enchantment</li>
 *     <li>{@link #treasureOnly()} - {@code Enchantment#isTreasureOnly()}, which the enchanting table
 *     and the anvil both respect</li>
 *     <li>{@link #curse()} - {@code Enchantment#isCurse()}, which drives the red tooltip and the
 *     grindstone/anvil rules</li>
 *     <li>{@link #villagerTrade()} and {@link #randomLoot()} - the two channels vanilla reads from
 *     tags that cannot be set per entry; they are honoured by the business mod's own loot and trade
 *     registration, which is exactly how Practical Enchantments does it on this version</li>
 * </ul>
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

    /**
     * @return whether the enchanting table may offer this enchantment, which is what
     *         {@code Enchantment#isDiscoverable()} is asked.
     */
    public boolean discoverable() {
        return this.enchantingTable && !this.treasureOnly;
    }
}
