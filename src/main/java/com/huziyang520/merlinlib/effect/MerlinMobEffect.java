package com.huziyang520.merlinlib.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * The concrete effect type every MerlinLib registered effect uses.
 */
public class MerlinMobEffect extends MobEffect {

    /** Behaviour hook, called on every tick the effect is active. */
    @FunctionalInterface
    public interface TickBehaviour {
        /**
         * @param level     the server level
         * @param entity    the affected entity
         * @param amplifier effect amplifier, 0 for level I
         * @return {@code true} when the effect did something, matching vanilla semantics
         */
        boolean tick(ServerLevel level, LivingEntity entity, int amplifier);
    }

    private final ResourceLocation id;
    private final int declaredColor;
    private final TickBehaviour behaviour;
    private volatile boolean disabled;
    private volatile boolean instant;

    public MerlinMobEffect(ResourceLocation id, MobEffectCategory category, int color, TickBehaviour behaviour) {
        super(category, color);
        this.id = id;
        this.declaredColor = color;
        this.behaviour = behaviour;
    }

    /**
     * Marks the effect as instantaneous. Set through the builder before registration; the value is
     * read by the game whenever the effect is applied.
     */
    public void setInstant(boolean instant) {
        this.instant = instant;
    }

    @Override
    public boolean isInstantenous() {
        return this.instant;
    }

    /** 1.21 renamed {@code isInstantenous} to {@code isInstantaneous}; both spellings are offered. */
    public boolean isInstantaneous() {
        return this.instant;
    }

    /**
     * @return the colour in use, which is the declared colour unless a config file overrode it.
     */
    public int getColor() {
        return EffectOverrides.colorOf(this.id, this.declaredColor);
    }

    /**
     * Runs the behaviour.
     *
     * <p>1.21.1's signature: {@code boolean applyEffectTick(LivingEntity, int)} - the {@code ServerLevel}
     * parameter of the 26.x form is gone, so the level is recovered from the entity, and the boolean asks
     * vanilla to keep the effect alive, which is what {@code isDurationEffectTick} used to answer.
     */
    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (this.disabled || this.behaviour == null) {
            return false;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        return this.behaviour.tick(level, entity, amplifier);
    }

    /**
     * @return {@code true} while the effect still has something to do. An instantaneous or disabled
     *         effect says no so it does not keep the effect alive.
     */
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return !this.instant && !this.disabled && this.behaviour != null;
    }

    /**
     * Marks this effect as disabled at runtime. The registry entry itself cannot be removed, so the
     * effect is turned into a no-op and its potions are not generated.
     *
     * @param disabled whether the effect should stop doing anything
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    /** @return the colour as declared at registration time, ignoring overrides. */
    public int declaredColor() {
        return this.declaredColor;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /**
     * @return a fresh instance of {@code (duration, amplifier)}. 1.21.1's {@code MobEffectInstance}
     *         constructor takes a {@code Holder<MobEffect>}, so the registered instance is wrapped
     *         through the registry it lives in.
     */
    public MobEffectInstance instance(int duration, int amplifier) {
        return new MobEffectInstance(
                net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(this), duration, amplifier);
    }
}
