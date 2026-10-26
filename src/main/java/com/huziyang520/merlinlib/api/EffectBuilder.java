// Fluent description of one mob effect.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.impl.EffectRegistry;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EffectBuilder {
    private final EffectRegistry owner;
    private final ResourceLocation id;
    private MobEffectCategory category = EffectApi.defaultCategory();
    private int color = MerlinColor.GRAY;
    private MerlinMobEffect.TickBehaviour behaviour;
    private final List<AttributeSpec> attributes = new ArrayList<>();
    private boolean instant;

    record AttributeSpec(Holder<Attribute> attribute, ResourceLocation id, double amount,
                         AttributeModifier.Operation operation) {
    }

    public EffectBuilder(EffectRegistry owner, ResourceLocation id) {
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

    public EffectBuilder attribute(Holder<Attribute> attribute, ResourceLocation id, double amount,
                                   AttributeModifier.Operation operation) {
        this.attributes.add(new AttributeSpec(attribute, id, amount, operation));
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
        for (AttributeSpec spec : this.attributes) {
            effect.addAttributeModifier(spec.attribute(), spec.id(), spec.amount(), spec.operation());
        }
        return effect;
    }

    public MerlinMobEffect submit() {
        return this.owner.submit(this.id, this);
    }

    public ResourceLocation id() {
        return this.id;
    }
}
