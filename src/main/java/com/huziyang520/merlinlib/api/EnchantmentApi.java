package com.huziyang520.merlinlib.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

/**
 * Registration and inspection entry point for enchantments.
 *
 * <p>Prefer {@link MerlinApi#enchantments()} over caching an implementation.
 */
public interface EnchantmentApi {

    /**
     * Starts a new registration.
     *
     * @param id full enchantment id, the namespace is normally your own mod id
     * @return a builder, nothing is registered until {@link EnchantmentBuilder#submit()}
     */
    EnchantmentBuilder register(ResourceLocation id);

    /**
     * Disables an enchantment, including one owned by vanilla or another mod. The disable flag wins
     * over every other source, and is the only way a config file can remove content it does not own.
     *
     * @param id the enchantment to disable
     * @return {@code true} when the state changed
     */
    boolean disable(ResourceLocation id);

    /**
     * Re-enables a previously disabled enchantment.
     *
     * @return {@code true} when the state changed
     */
    boolean enable(ResourceLocation id);

    /** @return ids currently disabled through {@link #disable(ResourceLocation)}. */
    Set<ResourceLocation> disabledIds();

    /** @return ids registered through the java api. */
    Set<ResourceLocation> registeredIds();

    /**
     * Inspects the finally effective state of an id, including entries declared by config files or
     * external datapacks when the registry has already been rebuilt.
     *
     * @param id the enchantment to inspect
     * @return the effective state, empty when the id is unknown to MerlinLib
     */
    Optional<EnchantmentInfo> describe(ResourceLocation id);
}
