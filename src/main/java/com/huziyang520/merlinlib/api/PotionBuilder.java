package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.PotionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent description of one potion.
 *
 * <p>Effects are declared as {@link ResourceLocation}s rather than {@code Holder}s on purpose: the
 * effect registry is only populated after every mod constructor has run, so the holder can only be
 * resolved when the potion itself is registered. Resolving late keeps the api call order irrelevant.
 *
 * <p>The vanilla naming convention is {@code <effect>}, {@code long_<effect>} and
 * {@code strong_<effect>}; {@link #variants()} generates all three from a single declaration.
 *
 * <p>1.20.1 note: {@code new Potion(String, MobEffectInstance...)} still exists and is deprecated in
 * favour of the holder based constructor that arrived with the holder based registry. The deprecated
 * one is used deliberately - it is the only one that works on this version, and using the registry
 * lookup instead would resolve the potion's own holder during the registry event, which is too early.
 */
public final class PotionBuilder {

    /** Vanilla duration of a normal potion, in ticks. */
    public static final int DURATION_NORMAL = 3600;
    /** Vanilla duration of a {@code long_} potion, in ticks. */
    public static final int DURATION_LONG = 9600;
    /** Vanilla duration of a {@code strong_} potion, in ticks. */
    public static final int DURATION_STRONG = 1800;

    /** One declared effect: either a direct instance or an id resolved at registration time. */
    record EffectSpec(ResourceLocation effectId, MobEffectInstance direct, int durationTicks, int amplifier) {
    }

    private final PotionRegistry owner;
    private final ResourceLocation id;
    private final List<EffectSpec> specs = new ArrayList<>();
    private int normalDuration = DURATION_NORMAL;
    private int longDuration = DURATION_LONG;
    private int strongDuration = DURATION_STRONG;
    private boolean variants;

    public PotionBuilder(PotionRegistry owner, ResourceLocation id) {
        this.owner = owner;
        this.id = id;
    }

    /**
     * Adds an effect resolved by id when the potion is registered.
     *
     * @param effectId      the effect to apply
     * @param durationTicks how long it lasts
     * @param amplifier     0 for level I, 1 for level II, and so on
     */
    public PotionBuilder effect(ResourceLocation effectId, int durationTicks, int amplifier) {
        this.specs.add(new EffectSpec(effectId, null, durationTicks, amplifier));
        return this;
    }

    /** Adds an effect resolved by id at level I. */
    public PotionBuilder effect(ResourceLocation effectId, int durationTicks) {
        return effect(effectId, durationTicks, 0);
    }

    /** Adds a fully built instance; the caller is responsible for holding a valid effect holder. */
    public PotionBuilder effect(MobEffectInstance instance) {
        this.specs.add(new EffectSpec(null, instance, instance.getDuration(), instance.getAmplifier()));
        return this;
    }

    public PotionBuilder normalDuration(int ticks) {
        this.normalDuration = ticks;
        return this;
    }

    public PotionBuilder longDuration(int ticks) {
        this.longDuration = ticks;
        return this;
    }

    public PotionBuilder strongDuration(int ticks) {
        this.strongDuration = ticks;
        return this;
    }

    /**
     * Also registers the {@code long_} and {@code strong_} variants of this potion, following vanilla
     * duration and amplifier conventions.
     */
    public PotionBuilder variants() {
        this.variants = true;
        return this;
    }

    /**
     * Resolves every declared effect into instances.
     *
     * @param durationOverride duration to use instead of the declared one, or {@code -1} to keep it
     * @param amplifierBonus   added to every amplifier, used by the {@code strong_} variant
     * @return the instances, in declaration order
     * @throws IllegalStateException when a declared effect id is not in the effect registry
     */
    public List<MobEffectInstance> buildInstances(int durationOverride, int amplifierBonus) {
        List<MobEffectInstance> instances = new ArrayList<>(this.specs.size());
        for (EffectSpec spec : this.specs) {
            if (spec.direct() != null) {
                instances.add(spec.direct());
                continue;
            }
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(spec.effectId());
            if (effect == null) {
                throw new IllegalStateException(
                        this.id + ": cannot build a potion for effect " + spec.effectId()
                                + " because it is not in the effect registry. Register the effect through MerlinApi.effects() first.");
            }
            int duration = durationOverride >= 0 ? durationOverride : spec.durationTicks();
            instances.add(new MobEffectInstance(effect, duration, spec.amplifier() + amplifierBonus));
        }
        return instances;
    }

    public int normalDuration() {
        return this.normalDuration;
    }

    public int longDuration() {
        return this.longDuration;
    }

    public int strongDuration() {
        return this.strongDuration;
    }

    public boolean withVariants() {
        return this.variants;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /**
     * @return the declared effects, for validation before registration.
     */
    public List<ResourceLocation> declaredEffectIds() {
        return this.specs.stream().filter(spec -> spec.effectId() != null).map(EffectSpec::effectId).toList();
    }

    /**
     * Registers the potion, and the variants when {@link #variants()} was called.
     *
     * @return the id of the main potion
     */
    public ResourceLocation submit() {
        return this.owner.submit(this);
    }

    /** @return the effect type of a spec, used to decide which registry entry a variant belongs to. */
    static MobEffect effectOf(EffectSpec spec) {
        return spec.direct() == null ? null : spec.direct().getEffect();
    }
}
