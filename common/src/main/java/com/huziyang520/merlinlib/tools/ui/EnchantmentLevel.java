package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * One selectable row of the enchantment picker: a concrete enchantment at a concrete level.
 *
 * <p>Levels are separate rows on purpose, exactly like the vanilla enchanting table lists them, so
 * picking "Sharpness V" is a single click instead of two adjustments.
 *
 * @param enchantment the enchantment
 * @param level       the level carried by this row
 */
public record EnchantmentLevel(Holder<Enchantment> enchantment, int level) {

    /**
     * @return the label shown for this row, e.g. {@code Sharpness V}
     */
    public String label() {
        return this.enchantment.value().description().getString() + " " + VanillaUi.roman(this.level);
    }
}
