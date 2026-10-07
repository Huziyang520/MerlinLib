package com.huziyang520.merlinlib.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.Set;

/**
 * Registration and inspection entry point for enchantments.
 *
 * <p>Prefer {@link MerlinApi#enchantments()} over caching an implementation.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>Nothing in this interface's shape. What changed is the moment a registration takes effect: on
 * 26.3 an enchantment became real when the generated data pack was read, so registering at any time -
 * including from a reload - worked. On 1.20.1 the registration is a Forge {@code DeferredRegister}
 * entry that is filled while the registry events run, so it has to be declared from a mod constructor.
 *
 * <p>{@link #disable(ResourceLocation)} is the same flag it always was, but its reach is now wider:
 * it removes the entry before it is ever queued for registration, so a disabled enchantment is not
 * merely inert - it does not exist in the registry at all on this version.
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
     * Inspects the finally effective state of an id, including entries declared by config files.
     *
     * @param id the enchantment to inspect
     * @return the effective state, empty when the id is unknown to MerlinLib
     */
    Optional<EnchantmentInfo> describe(ResourceLocation id);
}
