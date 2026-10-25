// Registration entry point for mob effects.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import java.util.Optional;
import java.util.Set;

public interface EffectApi {
    EffectBuilder register(Identifier id);
    boolean overrideColor(Identifier id, int color);
    boolean disable(Identifier id);
    Set<Identifier> registeredIds();
    Optional<MerlinMobEffect> get(Identifier id);
    PotionBuilder potionFor(Identifier effectId);

    static MobEffectCategory defaultCategory() {
        return MobEffectCategory.BENEFICIAL;
    }

    static net.minecraft.core.Registry<net.minecraft.world.effect.MobEffect> registry() {
        return BuiltInRegistries.MOB_EFFECT;
    }
}
