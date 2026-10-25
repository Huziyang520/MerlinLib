// Supplies the creative inventory entries for MerlinLib content.

package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import java.util.ArrayList;
import java.util.List;

public final class MerlinCreativeEntries {
    public static final net.minecraft.resources.ResourceKey<CreativeModeTab> TAB =
            net.minecraft.world.item.CreativeModeTabs.INGREDIENTS;

    public static final net.minecraft.resources.ResourceKey<CreativeModeTab> TOOLS_TAB =
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES;

    private MerlinCreativeEntries() {
    }

    public static List<ItemStack> testWeapons() {
        if (!ConfigManager.server().testingToolkitEnabled()) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ResourceLocation id : TestWeapons.ids()) {
            TestWeapons.stack(id).ifPresent(stacks::add);
        }
        ItemStack healthEditor = HealthEditorItem.stack();
        if (healthEditor.isEmpty()) {
            Constants.LOG.warn("[MerlinLib] the health editor item is not registered, so it cannot be offered "
                    + "in the creative tab");
        } else {
            stacks.add(healthEditor);
        }
        Constants.LOG.info("[MerlinLib] creative tab 'tools_and_utilities': {} testing item(s) offered",
                stacks.size());
        return stacks;
    }

    public static List<ItemStack> enchantedBooks() {
        List<ItemStack> stacks = new ArrayList<>();
        ContentManager.current().enchantments().keySet().stream().sorted().forEach(id -> {
            Enchantment enchantment = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(id);
            if (enchantment != null) {
                stacks.add(book(enchantment, enchantment.getMaxLevel()));
            }
        });
        return stacks;
    }

    public static ItemStack book(Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level));
        return stack;
    }
}
