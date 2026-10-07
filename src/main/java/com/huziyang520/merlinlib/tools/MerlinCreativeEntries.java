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
 *
 * <h2>What changed from the 26.3 line, and why it got simpler</h2>
 *
 * <p>26.3 built every entry from a {@code HolderLookup.Provider}, because the enchantment registry was
 * data driven and on the client it only existed once the server had sent it. On 1.20.1 the
 * enchantment registry is a plain static registry that exists on both sides as soon as the game has
 * started, so {@code BuiltInRegistries.ENCHANTMENT} is simply read - no provider is threaded through,
 * and no entry can silently go missing because a lookup was asked one phase too early.
 *
 * <p>The book is likewise built with {@link EnchantedBookItem#addEnchantment}, the API this version
 * has, instead of writing a {@code STORED_ENCHANTMENTS} data component that does not exist here.
 *
 * <p>Which switches matter here: only the toolkit master switch. The other switches decide how a screen
 * behaves once it is open - whether the health editor item must be held, for instance - and none of them
 * may make an item vanish from the inventory, because an item that cannot be found is indistinguishable
 * from a broken registration.
 *
 * <h2>How these entries reach a tab</h2>
 *
 * <p>Through Forge's {@code BuildCreativeModeTabContentsEvent}, which is what {@code MerlinLibForge}
 * listens for. The 26.3 line had one registration path per loader; here there is one, on the mod event
 * bus, and the deduplication it needs is done by the caller.
 */
public final class MerlinCreativeEntries {

    /**
     * The vanilla tab MerlinLib puts its enchanted books into.
     *
     * <p>{@code minecraft:ingredients}, which on 1.20.1 is still reachable as
     * {@code CreativeModeTabs.INGREDIENTS} - the 26.3 line had to build the key by hand because those
     * constants had been made private there.
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
