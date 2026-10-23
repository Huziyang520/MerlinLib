package com.huziyang520.merlinlib.content;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentBuilder;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * Content shipped by MerlinLib itself.
 */
public final class BuiltInContent {

    /** Enchantment that keeps an item at full durability forever. */
    public static final ResourceLocation UNBREAKABLE = new ResourceLocation(Constants.MOD_ID, "unbreakable");

    private BuiltInContent() {
    }

    public static void register() {
        registerUnbreakable();
    }

    /**
     * {@code merlinlib:unbreakable}: single level, treasure only, never obtainable from an enchanting
     * table in survival.
 *
     * <p>1.20.1 has no numeric effect system - that arrives in 1.20.5 - so there is no component to
     * declare. The rule is rebuilt at the one place this version offers for it, which is Forge's own
     * durability hook rather than anything of ours:
     * {@code IForgeItem#damageItem(ItemStack, int, LivingEntity, Consumer)}, a default method that
     * {@code ItemStack#hurtAndBreak} calls before it subtracts any durability.
     * {@link UnbreakableItem} below overrides it to return {@code 0}, which is that method's own
     * documented way of saying "consume none of this damage".
 *
     * <p>Everything else about the declaration is carried over verbatim, so the enchantment keeps its
     * name, its single level, its rarity, its cost and its treasure-only status.
 */
    private static void registerUnbreakable() {
        EnchantmentBuilder builder = MerlinApi.enchantments()
                .register(UNBREAKABLE)
                .maxLevel(1)
                .weight(1)
                .anvilCost(4)
                .cost(25, 25, 75, 25)
                .slots("any")
                .supportedItems("#minecraft:enchantable/durability")
                // The enchanting table filters its offers by category, and this enchantment must never
                // be offered there; BREAKABLE is the category that exactly means "items with durability".
                .category(EnchantmentCategory.BREAKABLE)
                .acquisition(Acquisition.TREASURE_ONLY);
        EnchantmentRegistry.submitUnbreakable(builder);
    }
}
