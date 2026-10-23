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

/**
 * "What does this item smelt into", answered from a cache.
 */
public final class SmeltingLookup {

    /** The server the lookup belongs to, set when it starts. */
    private static MinecraftServer server;

    /** The recipe manager the cache was built from; a different one means the cache is stale. */
    private static RecipeManager cachedFrom;

    /** Item to smelting result, one entry per item that can be smelted. */
    private static Map<Item, ItemStack> cache = Map.of();

    private SmeltingLookup() {
    }

    /**
     * Points the lookup at a server.
     *
     * <p>Called when a server starts. Safe to call again; the cache is rebuilt on the next lookup.
     *
     * @param server the running server
 */
    public static void initialize(MinecraftServer server) {
        SmeltingLookup.server = server;
        SmeltingLookup.cachedFrom = null;
    }

    /**
     * Looks up what an item smelts into.
     *
     * @param input the item, may be empty
     * @return the result as a fresh stack, or empty when the item does not smelt
 */
    public static Optional<ItemStack> smelt(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        ensureCurrent();
        ItemStack result = cache.get(input.getItem());
        return result == null ? Optional.empty() : Optional.of(result.copy());
    }

    /**
     * Looks up what an item smelts into, falling back to the item itself.
     *
     * @param input the item, may be empty
     * @return the smelting result, or the input unchanged when it does not smelt
 */
    public static ItemStack smeltOrOriginal(ItemStack input) {
        return smelt(input).orElse(input);
    }

    /**
     * Drops the cache, so the next lookup rebuilds it.
     *
     * <p>Not needed for a recipe reload - the cache notices that by itself - but useful when a caller has
     * changed what it expects to find.
 */
    public static void invalidate() {
        SmeltingLookup.cachedFrom = null;
    }

    /** @return how many items currently have a smelting answer */
    public static int cacheSize() {
        return cache.size();
    }

    /** Rebuilds the cache when the server's recipe manager is not the one it was built from. */
    private static void ensureCurrent() {
        RecipeManager current = server == null ? null : server.getRecipeManager();
        if (current == null || current == cachedFrom) {
            return;
        }
        rebuild(current);
    }

    /**
     * Collects every cooking recipe into the cache.
 *
     * @param manager the recipe manager to read
 */
    private static void rebuild(RecipeManager manager) {
        RegistryAccess registries = server.registryAccess();
        Map<Item, ItemStack> built = new HashMap<>();
        int recipes = 0;
        for (Recipe<?> recipe : manager.getRecipes()) {
            if (!(recipe instanceof AbstractCookingRecipe cooking)) {
                continue;
            }
            // getResultItem is the accessor that answers without a container; assemble would need one
            // built from the ingredient, and this class has no reason to build a container it never
            // uses for anything else.
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
