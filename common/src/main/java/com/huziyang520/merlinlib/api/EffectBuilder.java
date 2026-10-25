// Fluent description of one mob effect.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import java.util.Optional;

public final class EffectBuilder {
    private final EffectRegistry owner;
    private final Identifier id;
    private MobEffectCategory category = EffectApi.defaultCategory();
    private int color = MerlinColor.GRAY;
    private MerlinMobEffect.TickBehaviour behaviour;
    private boolean instant;

    public EffectBuilder(EffectRegistry owner, Identifier id) {
        this.owner = owner;
        this.id = id;
    }

    public EffectBuilder category(MobEffectCategory category) {
        this.category = category;
        return this;
    }

    public EffectBuilder color(int color) {
        this.color = color;
        return this;
    }

    public EffectBuilder color(String color) {
        this.color = MerlinColor.parse(color)
                .orElseThrow(() -> new IllegalArgumentException(this.id + ": unknown colour '" + color + "', use #RRGGBB or a palette name from MerlinColor.named()"));
        return this;
    }

    public EffectBuilder instant() {
        this.instant = true;
        return this;
    }

    public EffectBuilder onTick(MerlinMobEffect.TickBehaviour behaviour) {
        this.behaviour = behaviour;
        return this;
    }

    public Optional<Integer> declaredColor() {
        return Optional.of(this.color);
    }

    public MerlinMobEffect build() {
        MerlinMobEffect effect = new MerlinMobEffect(this.id, this.category, this.color, this.behaviour);
        effect.setInstant(this.instant);
        return effect;
    }

    public MerlinMobEffect submit() {
        return this.owner.submit(this.id, this);
    }

    public Identifier id() {
        return this.id;
    }
}
