// The concrete effect type every MerlinLib registered effect uses.

package com.huziyang520.merlinlib.effect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MerlinMobEffect extends MobEffect {
    @FunctionalInterface
    public interface TickBehaviour {
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

    public void setInstant(boolean instant) {
        this.instant = instant;
    }

    public void addAttributeModifier(Attribute attribute, ResourceLocation modifierId, double amount,
                                     AttributeModifier.Operation operation) {
        UUID uuid = UUID.nameUUIDFromBytes((this.id + "/" + modifierId).getBytes(StandardCharsets.UTF_8));
        super.addAttributeModifier(attribute, uuid.toString(), amount, operation);
    }

    @Override
    public boolean isInstantenous() {
        return this.instant;
    }

    public boolean isInstantaneous() {
        return this.instant;
    }

    public int getColor() {
        return EffectOverrides.colorOf(this.id, this.declaredColor);
    }

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

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return !this.instant && !this.disabled && this.behaviour != null;
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

    public ResourceLocation id() {
        return this.id;
    }

    public MobEffectInstance instance(int duration, int amplifier) {
        return new MobEffectInstance(this, duration, amplifier);
    }
}
