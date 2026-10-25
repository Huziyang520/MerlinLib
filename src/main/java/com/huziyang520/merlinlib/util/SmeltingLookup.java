// "What does this item smelt into", answered from a cache.

package com.huziyang520.merlinlib.util;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import java.util.HashMap;
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
        RegistryAccess registries = server.registryAccess();
        Map<Item, ItemStack> built = new HashMap<>();
        int recipes = 0;
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (!(recipe instanceof AbstractCookingRecipe cooking)) {
                continue;
            }

            ItemStack result = cooking.getResultItem(registries);
            if (result.isEmpty()) {
                continue;
            }
            int matched = 0;
            for (Ingredient ingredient : cooking.getIngredients()) {
                for (ItemStack candidate : ingredient.getItems()) {
                    if (!candidate.isEmpty()) {
                        built.put(candidate.getItem(), result.copy());
                        matched++;
                    }
                }
            }
            if (matched > 0) {
                recipes++;
            }
        }
        cache = Map.copyOf(built);
        cachedFrom = manager;
        Constants.LOG.info("[MerlinLib] the smelting lookup cached {} recipe(s) covering {} item(s)", recipes,
                cache.size());
    }
}
