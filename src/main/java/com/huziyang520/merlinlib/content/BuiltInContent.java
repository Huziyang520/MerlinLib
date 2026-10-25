// Content shipped by MerlinLib itself.

package com.huziyang520.merlinlib.content;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentBuilder;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.impl.EnchantmentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public final class BuiltInContent {
    public static final ResourceLocation UNBREAKABLE = new ResourceLocation(Constants.MOD_ID, "unbreakable");

    private BuiltInContent() {
    }

    public static void register() {
        registerUnbreakable();
    }

    private static void registerUnbreakable() {
        EnchantmentBuilder builder = MerlinApi.enchantments()
                .register(UNBREAKABLE)
                .maxLevel(1)
                .weight(1)
                .anvilCost(4)
                .cost(25, 25, 75, 25)
                .slots("any")
                .supportedItems("#minecraft:enchantable/durability")
                .category(EnchantmentCategory.BREAKABLE)
                .acquisition(Acquisition.TREASURE_ONLY);
        EnchantmentRegistry.submitUnbreakable(builder);
    }
}
