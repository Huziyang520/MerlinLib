// Supplies the creative inventory entries for MerlinLib content.

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

public final class MerlinCreativeEntries {
    public static final ResourceKey<CreativeModeTab> TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("ingredients"));

    public static final ResourceKey<CreativeModeTab> TOOLS_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

    private MerlinCreativeEntries() {
    }

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

    public static ItemStack book(Holder<Enchantment> holder, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(holder, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return stack;
    }
}
