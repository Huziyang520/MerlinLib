package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Supplies the creative inventory entries for MerlinLib content.
 *
 * <p>Why the entries are built from a {@link HolderLookup.Provider} instead of a plain registry: the
 * enchantment registry is data driven, so on the client it only exists once the server has sent it.
 * The creative screen is built while a world is loaded, which is exactly the moment the provider is
 * available; asking earlier would silently produce nothing.
 *
 * <p>Which switches matter here: only the toolkit master switch. The other switches decide how a screen
 * behaves once it is open - whether the health editor item must be held, for instance - and none of them
 * may make an item vanish from the inventory, because an item that cannot be found is indistinguishable
 * from a broken registration.
 */
public final class MerlinCreativeEntries {

    /**
     * The vanilla tab MerlinLib puts its enchanted books into.
     *
     * <p>Built by hand because 26.3 turned the tab key constants in {@code CreativeModeTabs} private;
     * the id itself is still the vanilla one, {@code minecraft:ingredients}.
     */
    public static final ResourceKey<CreativeModeTab> TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));

    /** Tab the testing weapons and the health editor are added to. */
    public static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

    private MerlinCreativeEntries() {
    }

    /**
     * The four testing weapons plus the health editor, as fresh stacks, in declaration order.
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
        for (Identifier id : TestWeapons.ids()) {
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
     * Builds one enchanted book for every MerlinLib enchantment that is present in the given registry.
     *
     * @param holders registry access of the running game, may be {@code null} outside of a world
     * @return the stacks to add, empty when the enchantment registry is not available yet
     */
    public static List<ItemStack> enchantedBooks(HolderLookup.Provider holders) {
        if (holders == null) {
            return List.of();
        }
        Optional<? extends HolderLookup.RegistryLookup<Enchantment>> lookup = holders.lookup(Registries.ENCHANTMENT);
        if (lookup.isEmpty()) {
            return List.of();
        }
        HolderLookup.RegistryLookup<Enchantment> registry = lookup.get();

        List<ItemStack> stacks = new ArrayList<>();
        ContentManager.current().enchantments().keySet().stream().sorted().forEach(id -> registry
                .get(ResourceKey.create(Registries.ENCHANTMENT, id))
                .map(holder -> book(holder, holder.value().getMaxLevel()))
                .ifPresent(stacks::add));
        return stacks;
    }

    /**
     * @param holder the enchantment
     * @param level  level stored in the book
     * @return an enchanted book carrying exactly that enchantment
     */
    public static ItemStack book(Holder<Enchantment> holder, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return stack;
    }
}
