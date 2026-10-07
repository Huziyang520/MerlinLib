package com.huziyang520.merlinlib.tools.ui;

import com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * One selectable row of the enchantment picker: a concrete enchantment at a concrete level.
 *
 * <p>Levels are separate rows on purpose, exactly like the vanilla enchanting table lists them, so
 * picking "Sharpness V" is a single click instead of two adjustments.
 *
 * <h2>What changed / 1.20.1 note</h2>
 *
 * <p>Two things, both forced by the version and both visible to callers:
 *
 * <ul>
 *   <li><b>the component type</b> is a plain {@link Enchantment} rather than the 26.3
 *       {@code Holder<Enchantment>}. 1.20.1 registers enchantments into a static registry that is built
 *       once at startup and never rebuilt, so the holder indirection that exists on 1.21 to survive a
 *       reload has nothing to protect here - the same reasoning the ported
 *       {@code event.EnchantmentContext} records. A record component is part of the public signature, so a
 *       screen that built or read these rows against a holder must drop the {@code .value()} call:
 *       {@code new EnchantmentLevel(enchantment, level)} instead of
 *       {@code new EnchantmentLevel(holder, level)};</li>
 *   <li><b>the display name</b> is reached differently. 1.21 exposes
 *       {@code Enchantment#description()}; this version has no such method, only
 *       {@code getDescriptionId()} and the pre-formatted {@code getFullname(int)}. The label keeps the
 *       26.3 shape - name, a space, then {@link VanillaUi#roman(int)} - rather than switching to
 *       {@code getFullname}, because {@code getFullname} resolves the {@code enchantment.level.N}
 *       translation keys, which a MerlinLib content enchantment does not ship.</li>
 * </ul>
 *
 * @param enchantment the enchantment
 * @param level       the level carried by this row
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
