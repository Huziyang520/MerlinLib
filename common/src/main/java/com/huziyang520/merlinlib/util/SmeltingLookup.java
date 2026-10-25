// "What does this item smelt into", answered from a cache.

package com.huziyang520.merlinlib.util;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SmeltingLookup {
    private static MinecraftServer server;
    private static RecipeManager cachedFrom;
    private static Map<Item, ItemStack> cache = Map.of();

    private SmeltingLookup() {
    }

    public static void initialize(MinecraftServer server) {
        SmeltingLookup.server = server;
        SmeltingLookup.cachedFrom = null;
    }

    public static Optional<ItemStack> smelt(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        ensureCurrent();
        ItemStack result = cache.get(input.getItem());
        return result == null ? Optional.empty() : Optional.of(result.copy());
    }

    public static ItemStack smeltOrOriginal(ItemStack input) {
        return smelt(input).orElse(input);
    }

    public static void invalidate() {
        SmeltingLookup.cachedFrom = null;
    }

    public static int cacheSize() {
        return cache.size();
    }

    private static void ensureCurrent() {
        RecipeManager current = server == null ? null : server.getRecipeManager();
        if (current == null || current == cachedFrom) {
            return;
        }
        rebuild(current);
    }

    private static void rebuild(RecipeManager manager) {
        Map<Item, ItemStack> built = new HashMap<>();
        int recipes = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof SmeltingRecipe recipe)) {
                continue;
            }
            List<Holder<Item>> inputs = recipe.input().items().toList();
            if (inputs.isEmpty()) {
                continue;
            }

            ItemStack result = recipe.assemble(new SingleRecipeInput(new ItemStack(inputs.getFirst().value())));
            if (result.isEmpty()) {
                continue;
            }
            for (Holder<Item> input : inputs) {
                built.put(input.value(), result.copy());
            }
            recipes++;
        }
        cache = Map.copyOf(built);
        cachedFrom = manager;
        Constants.LOG.info("[MerlinLib] the smelting lookup cached {} recipe(s) covering {} item(s)", recipes,
                cache.size());
    }
}
