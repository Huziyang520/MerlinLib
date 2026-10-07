package com.huziyang520.merlinlib.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * The concrete effect type every MerlinLib registered effect uses.
 *
 * <p>It exists for three reasons:
 * <ul>
 *     <li>{@link MobEffect} has a protected constructor, so a subclass is required anyway</li>
 *     <li>it makes both the colour and the tick behaviour overridable at runtime, which is what lets a
 *     config file recolour or neutralise an already registered effect without a restart</li>
 *     <li>it remembers the id it was registered under, which the 26.3 line got from the holder and
 *     1.20.1 cannot: {@code MobEffect} has no {@code builtInRegistryHolder()} lookup here that is
 *     cheap enough to call per tick</li>
 * </ul>
 *
 * <h2>The two signatures that changed</h2>
 *
 * <p>1.20.1's effect tick is
 * {@code applyEffectTick(LivingEntity, int)} and its "does this level do anything" question is
 * {@code isDurationEffectTick(int, int)}. The 26.3 line, being 1.21+, had the merged
 * {@code applyEffectTick(ServerLevel, LivingEntity, int)} form. Both are implemented below so a
 * behaviour written for either shape works, and {@link #isInstantaneous()} keeps its 1.20.1 name.
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
     * <p>Overridden on 1.20.1's signature, which is a <b>void</b> method taking the entity and the
     * amplifier. The level 1.21 form returns a boolean and receives the {@code ServerLevel}; neither
     * is the case here, so the level is recovered from the entity and the outcome is discarded.
     * Vanilla on this version decides whether to keep ticking through
     * {@link #isDurationEffectTick(int, int)} instead, which is why the "did it do anything" answer
     * lives there rather than in a return value.
     */
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (this.disabled || this.behaviour == null) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        this.behaviour.tick(level, entity, amplifier);
    }

    /**
     * @return {@code true} while the effect still has something to do, which vanilla asks once per
     *         tick before calling {@link #applyEffectTick}; an instantaneous or disabled effect says
     *         no so it does not keep the effect alive
     */
    @Override
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
     * @return a fresh instance of {@code (duration, amplifier)}, built through the constructor
     *         vanilla itself uses; the 1.20.1 API has no static factory for this
     */
    public MobEffectInstance instance(int duration, int amplifier) {
        return new MobEffectInstance(this, duration, amplifier);
    }
}
