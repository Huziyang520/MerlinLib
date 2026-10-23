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

/**
 * Supplies the creative inventory entries for MerlinLib content.
 */
public final class MerlinCreativeEntries {

    /**
     * The vanilla tab MerlinLib puts its enchanted books into.
 */
    public static final net.minecraft.resources.ResourceKey<CreativeModeTab> TAB =
            net.minecraft.world.item.CreativeModeTabs.INGREDIENTS;

    /** Tab the testing weapons and the health editor are added to. */
    public static final net.minecraft.resources.ResourceKey<CreativeModeTab> TOOLS_TAB =
            net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES;

    private MerlinCreativeEntries() {
    }

    /**
     * The three testing weapons plus the health editor, as fresh stacks, in declaration order.
     *
     * <p>Every entry is checked before it is offered: an empty stack in a creative tab is invisible, and a
     * warning in the log is the only way to tell "the item failed to register" apart from "the item is not
     * where the player is looking".
     *
     * @return the stacks to add, empty when the toolkit is switched off
 */
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

    /**
     * Builds one enchanted book for every MerlinLib enchantment that is present in the registry.
     *
     * @return the stacks to add, in id order
 */
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

    /**
     * @param enchantment the enchantment
     * @param level       level stored in the book
     * @return an enchanted book carrying exactly that enchantment
 */
    public static ItemStack book(Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment, level));
        return stack;
    }
}
