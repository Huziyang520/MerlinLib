package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;

import java.util.Optional;

/**
 * Fluent description of one mob effect.
 */
public final class EffectBuilder {

    private final EffectRegistry owner;
    private final ResourceLocation id;
    private MobEffectCategory category = EffectApi.defaultCategory();
    private int color = MerlinColor.GRAY;
    private MerlinMobEffect.TickBehaviour behaviour;
    private boolean instant;

    public EffectBuilder(EffectRegistry owner, ResourceLocation id) {
        this.owner = owner;
        this.id = id;
    }

    public EffectBuilder category(MobEffectCategory category) {
        this.category = category;
        return this;
    }

    /** Sets the colour from a packed ARGB value, see {@link MerlinColor}. */
    public EffectBuilder color(int color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the colour from text, accepting {@code "#RRGGBB"}, {@code "RRGGBB"} or a palette name such
     * as {@code "cyan"}.
     *
     * @throws IllegalArgumentException when the text cannot be understood
 */
    public EffectBuilder color(String color) {
        this.color = MerlinColor.parse(color)
                .orElseThrow(() -> new IllegalArgumentException(this.id + ": unknown colour '" + color + "', use #RRGGBB or a palette name from MerlinColor.named()"));
        return this;
    }

    /** Marks the effect as instant, applied once on the tick it starts. */
    public EffectBuilder instant() {
        this.instant = true;
        return this;
    }

    /** Behaviour run on every tick the effect is active. */
    public EffectBuilder onTick(MerlinMobEffect.TickBehaviour behaviour) {
        this.behaviour = behaviour;
        return this;
    }

    public Optional<Integer> declaredColor() {
        return Optional.of(this.color);
    }

    /**
     * @return the effect instance without registering it.
 */
    public MerlinMobEffect build() {
        MerlinMobEffect effect = new MerlinMobEffect(this.id, this.category, this.color, this.behaviour);
        effect.setInstant(this.instant);
        return effect;
    }

    /**
     * Registers the effect through the loader.
     *
     * @return the created effect instance
 */
    public MerlinMobEffect submit() {
        return this.owner.submit(this.id, this);
    }

    public ResourceLocation id() {
        return this.id;
    }
}
