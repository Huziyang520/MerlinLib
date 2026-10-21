package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Optional;
import java.util.Set;

/**
 * Registration entry point for mob effects.
 *
 * <p>Important boundary that the documentation repeats on purpose: {@code MobEffect} is a plain code
 * registry, so a config file <b>cannot create</b> a new effect. It can only recolour or disable one
 * that was declared in code.
 */
public interface EffectApi {

    /**
     * Starts a new effect registration.
     *
     * @param id effect id, the namespace is normally your own mod id
     * @return a builder, nothing is registered until {@link EffectBuilder#submit()}
     */
    EffectBuilder register(Identifier id);

    /**
     * Overrides the colour of any effect, including vanilla ones.
     *
     * @param id    effect id
     * @param color packed ARGB colour, see {@link MerlinColor}
     * @return {@code true} when the override was stored
     */
    boolean overrideColor(Identifier id, int color);

    /**
     * Disables an effect: it stops doing anything and no potion is generated for it. The registry
     * entry itself stays, because the game does not allow removing one.
     *
     * @param id effect id
     * @return {@code true} when the state changed
     */
    boolean disable(Identifier id);

    /** @return ids of every effect registered through this api. */
    Set<Identifier> registeredIds();

    /** @return the registered effect instance, empty when the id is unknown. */
    Optional<MerlinMobEffect> get(Identifier id);

    /**
     * Convenience for building a potion for an effect that was just registered. Uses the
     * {@code <effect path>} naming convention vanilla expects.
     *
     * @param effectId the effect to derive a potion from
     * @return a potion builder pre-filled with the effect
     */
    PotionBuilder potionFor(Identifier effectId);

    /** @return the vanilla category used when a builder does not specify one. */
    static MobEffectCategory defaultCategory() {
        return MobEffectCategory.BENEFICIAL;
    }

    /** @return the registry instance effects are written to. */
    static net.minecraft.core.Registry<net.minecraft.world.effect.MobEffect> registry() {
        return BuiltInRegistries.MOB_EFFECT;
    }
}
