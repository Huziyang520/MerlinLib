package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EffectApi;
import com.huziyang520.merlinlib.api.EffectBuilder;
import com.huziyang520.merlinlib.api.PotionBuilder;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Registration state of every effect MerlinLib knows about, plus the runtime colour overrides.
 *
 * <p>The duplicate check that used to ask {@code BuiltInRegistries.MOB_EFFECT.containsKey} is gone:
 * on Forge the registry is frozen before mod constructors run, and touching it this early throws
 * {@code IllegalStateException: Registry is already frozen}. The bookkeeping here is what is left,
 * and it is the check that matters in practice: two registrations of the same id from the same
 * process are the mistake a mod actually makes.
 */
public final class EffectRegistry implements EffectApi {

    public static final EffectRegistry INSTANCE = new EffectRegistry();

    private final Map<ResourceLocation, MerlinMobEffect> registered = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> colorOverrides = new LinkedHashMap<>();
    private final Set<ResourceLocation> disabled = new LinkedHashSet<>();

    private EffectRegistry() {
    }

    @Override
    public EffectBuilder register(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("illegal effect id '" + id + "'");
        }
        if (this.registered.containsKey(id)) {
            throw new IllegalStateException(id + " is already registered by MerlinLib");
        }
        return new EffectBuilder(this, id);
    }

    /**
     * Registers the built effect through the loader bridge.
     *
     * @param id      effect id
     * @param builder the builder that produced it
     * @return the created instance, the same object the game will hold
     */
    public MerlinMobEffect submit(ResourceLocation id, EffectBuilder builder) {
        MerlinMobEffect effect = builder.build();
        this.registered.put(id, effect);
        // The very same instance is handed to the loader, so disabling it later affects the live object
        // even though Forge only puts it in the registry during the registry event.
        Services.REGISTRATIONS.register(BuiltInRegistries.MOB_EFFECT, id, () -> effect);
        this.pushOverrides();
        Constants.LOG.debug("[MerlinLib] registered effect {}", id);
        return effect;
    }

    @Override
    public boolean overrideColor(ResourceLocation id, int color) {
        Objects.requireNonNull(id, "id");
        Integer previous = this.colorOverrides.put(id, color);
        this.pushOverrides();
        return previous == null || previous != color;
    }

    @Override
    public boolean disable(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!this.disabled.add(id)) {
            return false;
        }
        this.pushOverrides();
        Constants.LOG.info("[MerlinLib] effect {} is disabled, it will not do anything and no potion is generated for it", id);
        return true;
    }

    @Override
    public Set<ResourceLocation> registeredIds() {
        synchronized (this.registered) {
            return Set.copyOf(this.registered.keySet());
        }
    }

    @Override
    public Optional<MerlinMobEffect> get(ResourceLocation id) {
        synchronized (this.registered) {
            return Optional.ofNullable(this.registered.get(id));
        }
    }

    @Override
    public PotionBuilder potionFor(ResourceLocation effectId) {
        MerlinMobEffect effect = this.get(effectId).orElseThrow(() -> new IllegalArgumentException(
                effectId + " is not managed by MerlinLib; register it through MerlinApi.effects() before deriving a potion from it"));
        ResourceLocation potionId = new ResourceLocation(effectId.getNamespace(), effectId.getPath());
        return PotionRegistry.INSTANCE.register(potionId)
                .effect(effectId, PotionBuilder.DURATION_NORMAL, 0)
                .variants();
    }

    /**
     * @return {@code true} when the effect was disabled through the api or a config file.
     */
    public boolean isDisabled(ResourceLocation id) {
        return this.disabled.contains(id);
    }

    private void pushOverrides() {
        Map<ResourceLocation, EffectOverrides.Entry> merged = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : this.colorOverrides.entrySet()) {
            merged.put(entry.getKey(), new EffectOverrides.Entry(Optional.of(entry.getValue()), !this.disabled.contains(entry.getKey())));
        }
        for (ResourceLocation id : this.disabled) {
            merged.computeIfAbsent(id, key -> new EffectOverrides.Entry(Optional.empty(), false));
        }
        EffectOverrides.setApiLayer(merged);
    }
}
