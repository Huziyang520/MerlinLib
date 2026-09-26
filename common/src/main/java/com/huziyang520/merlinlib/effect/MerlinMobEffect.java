package com.huziyang520.merlinlib.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * The concrete effect type every MerlinLib registered effect uses.
 *
 * <p>It exists for two reasons:
 * <ul>
 *     <li>{@link MobEffect} has a protected constructor, so a subclass is required anyway</li>
 *     <li>it makes both the colour and the tick behaviour overridable at runtime, which is what lets a
 *     config file recolour or neutralise an already registered effect without a restart</li>
 * </ul>
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

    private final Identifier id;
    private final int declaredColor;
    private final TickBehaviour behaviour;
    private volatile boolean disabled;
    private volatile boolean instant;

    public MerlinMobEffect(Identifier id, MobEffectCategory category, int color, TickBehaviour behaviour) {
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
    public boolean isInstantaneous() {
        return this.instant;
    }

    /**
     * @return the colour in use, which is the declared colour unless a config file overrode it.
     */
    @Override
    public int getColor() {
        return EffectOverrides.colorOf(this.id, this.declaredColor);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (this.disabled || this.behaviour == null) {
            return false;
        }
        return this.behaviour.tick(level, entity, amplifier);
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

    public Identifier id() {
        return this.id;
    }
}
