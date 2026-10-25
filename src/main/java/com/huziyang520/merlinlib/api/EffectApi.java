// Registration entry point for mob effects.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import java.util.Optional;
import java.util.Set;

public interface EffectApi {
    EffectBuilder register(ResourceLocation id);
    boolean overrideColor(ResourceLocation id, int color);
    boolean disable(ResourceLocation id);
    Set<ResourceLocation> registeredIds();
    Optional<MerlinMobEffect> get(ResourceLocation id);
    PotionBuilder potionFor(ResourceLocation effectId);

    static MobEffectCategory defaultCategory() {
        return MobEffectCategory.BENEFICIAL;
    }

    static net.minecraft.core.Registry<net.minecraft.world.effect.MobEffect> registry() {
        return BuiltInRegistries.MOB_EFFECT;
    }
}
