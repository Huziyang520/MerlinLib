// The concrete effect type every MerlinLib registered effect uses.

package com.huziyang520.merlinlib.effect;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class MerlinMobEffect extends MobEffect {
    @FunctionalInterface
    public interface TickBehaviour {
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

    public void setInstant(boolean instant) {
        this.instant = instant;
    }

    @Override
    public boolean isInstantaneous() {
        return this.instant;
    }

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

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public int declaredColor() {
        return this.declaredColor;
    }

    public Identifier id() {
        return this.id;
    }
}
