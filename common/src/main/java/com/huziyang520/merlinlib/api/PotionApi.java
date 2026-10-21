package com.huziyang520.merlinlib.api;

import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Registration entry point for potions.
 *
 * <p>Like mob effects, potions are a plain code registry: a config file can only override or disable
 * an existing potion, never create one.
 */
public interface PotionApi {

    /**
     * Starts a new potion registration.
     *
     * @param id potion id, the namespace is normally your own mod id
     * @return a builder, nothing is registered until {@link PotionBuilder#submit()}
     */
    PotionBuilder register(Identifier id);

    /**
     * Convenience used right after an effect was registered: builds the single effect potion and,
     * unless disabled, the {@code long_} and {@code strong_} variants vanilla players expect.
     *
     * <pre>{@code
     * EffectBuilder frost = MerlinApi.effects().register(id("frostbite")).color(MerlinColor.CYAN);
     * frost.submit();
     * MerlinApi.potions().potionFor(id("frostbite")).submit();
     * }</pre>
     *
     * @param effectId the effect the potion is built from
     * @return a builder already carrying the effect
     */
    PotionBuilder potionFor(Identifier effectId);

    /** @return ids of every potion registered through this api. */
    Set<Identifier> registeredIds();
}
