// One selectable row of the enchantment picker: a concrete enchantment at a concrete level.

package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentLevel(Holder<Enchantment> enchantment, int level) {
    public String label() {
        return this.enchantment.value().description().getString() + " " + VanillaUi.roman(this.level);
    }
}
