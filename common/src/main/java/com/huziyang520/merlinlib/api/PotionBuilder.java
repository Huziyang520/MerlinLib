// Fluent description of one potion.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.PotionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;

public final class PotionBuilder {
    public static final int DURATION_NORMAL = 3600;
    public static final int DURATION_LONG = 9600;
    public static final int DURATION_STRONG = 1800;

    record EffectSpec(Identifier effectId, MobEffectInstance direct, int durationTicks, int amplifier) {
    }

    private final PotionRegistry owner;
    private final Identifier id;
    private final List<EffectSpec> specs = new ArrayList<>();
    private int normalDuration = DURATION_NORMAL;
    private int longDuration = DURATION_LONG;
    private int strongDuration = DURATION_STRONG;
    private boolean variants;

    public PotionBuilder(PotionRegistry owner, Identifier id) {
        this.owner = owner;
        this.id = id;
    }

    public PotionBuilder effect(Identifier effectId, int durationTicks, int amplifier) {
        this.specs.add(new EffectSpec(effectId, null, durationTicks, amplifier));
        return this;
    }

    public PotionBuilder effect(Identifier effectId, int durationTicks) {
        return effect(effectId, durationTicks, 0);
    }

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

    public PotionBuilder variants() {
        this.variants = true;
        return this;
    }

    public List<MobEffectInstance> buildInstances(int durationOverride, int amplifierBonus) {
        List<MobEffectInstance> instances = new ArrayList<>(this.specs.size());
        for (EffectSpec spec : this.specs) {
            if (spec.direct() != null) {
                instances.add(spec.direct());
                continue;
            }
            var holder = BuiltInRegistries.MOB_EFFECT.get(spec.effectId()).orElseThrow(() -> new IllegalStateException(
                    this.id + ": cannot build a potion for effect " + spec.effectId()
                            + " because it is not in the effect registry. Register the effect through MerlinApi.effects() first."));
            int duration = durationOverride >= 0 ? durationOverride : spec.durationTicks();
            instances.add(new MobEffectInstance(holder, duration, spec.amplifier() + amplifierBonus));
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

    public Identifier id() {
        return this.id;
    }

    public List<Identifier> declaredEffectIds() {
        return this.specs.stream().filter(spec -> spec.effectId() != null).map(EffectSpec::effectId).toList();
    }

    public Identifier submit() {
        return this.owner.submit(this);
    }

    static MobEffect effectOf(EffectSpec spec) {
        return spec.direct() == null ? null : spec.direct().getEffect().value();
    }
}
