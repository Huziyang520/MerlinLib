package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * One selectable row of the enchantment picker: a concrete enchantment at a concrete level.
 */
public record EnchantmentLevel(Enchantment enchantment, int level) {

    /**
     * @return the label shown for this row, e.g. {@code Sharpness V}
 */
    public String label() {
        return Component.translatable(this.enchantment.getDescriptionId()).getString() + " "
                + VanillaUi.roman(this.level);
    }
}
