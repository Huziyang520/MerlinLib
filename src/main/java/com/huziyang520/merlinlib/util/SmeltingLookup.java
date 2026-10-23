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
     * Collects every smelting recipe into the cache.
     *
     * @param manager the recipe manager to read
     */
    private static void rebuild(RecipeManager manager) {
        Map<Item, ItemStack> built = new HashMap<>();
        int recipes = 0;
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof SmeltingRecipe recipe)) {
                continue;
            }
            // 1.21.1: the inputs are gathered from getIngredients(), whose Ingredient#getItems() already
            // resolves tags into concrete stacks. The 26.x Ingredient#items() stream does not exist here.
            List<Item> inputs = new java.util.ArrayList<>();
            for (net.minecraft.world.item.crafting.Ingredient ingredient : recipe.getIngredients()) {
                for (ItemStack candidate : ingredient.getItems()) {
                    if (!candidate.isEmpty()) {
                        inputs.add(candidate.getItem());
                    }
                }
            }
            if (inputs.isEmpty()) {
                continue;
            }
            // The recipe is asked for its result through assemble, because that is the public door: the
            // result field itself is protected, and an empty answer means the recipe is not usable.
            ItemStack result = recipe.assemble(new SingleRecipeInput(new ItemStack(inputs.getFirst())),
                    server.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            for (Item input : inputs) {
                built.put(input, result.copy());
            }
            recipes++;
        }
        cache = Map.copyOf(built);
        cachedFrom = manager;
        Constants.LOG.info("[MerlinLib] the smelting lookup cached {} recipe(s) covering {} item(s)", recipes,
                cache.size());
    }
}
