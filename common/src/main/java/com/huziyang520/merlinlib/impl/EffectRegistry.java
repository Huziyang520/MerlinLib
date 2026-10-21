package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EffectApi;
import com.huziyang520.merlinlib.api.EffectBuilder;
import com.huziyang520.merlinlib.api.PotionBuilder;
import com.huziyang520.merlinlib.effect.EffectOverrides;
import com.huziyang520.merlinlib.effect.MerlinMobEffect;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Registration state of every effect MerlinLib knows about, plus the runtime colour overrides.
 */
public final class EffectRegistry implements EffectApi {

    public static final EffectRegistry INSTANCE = new EffectRegistry();

    private final Map<Identifier, MerlinMobEffect> registered = new LinkedHashMap<>();
    private final Map<Identifier, Integer> colorOverrides = new LinkedHashMap<>();
    private final Set<Identifier> disabled = new LinkedHashSet<>();

    private EffectRegistry() {
    }

    @Override
    public EffectBuilder register(Identifier id) {
        Objects.requireNonNull(id, "id");
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("illegal effect id '" + id + "'");
        }
        if (this.registered.containsKey(id)) {
            throw new IllegalStateException(id + " is already registered by MerlinLib");
        }
        if (BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
            throw new IllegalStateException(id + " already exists in the effect registry");
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
    public MerlinMobEffect submit(Identifier id, EffectBuilder builder) {
        MerlinMobEffect effect = builder.build();
        this.registered.put(id, effect);
        // The very same instance is handed to the loader, so disabling it later affects the live object
        // even on loaders that register deferred.
        Services.REGISTRATIONS.register(BuiltInRegistries.MOB_EFFECT, id, () -> effect);
        this.pushOverrides();
        Constants.LOG.debug("[MerlinLib] registered effect {}", id);
        return effect;
    }

    @Override
    public boolean overrideColor(Identifier id, int color) {
        Objects.requireNonNull(id, "id");
        Integer previous = this.colorOverrides.put(id, color);
        this.pushOverrides();
        return previous == null || previous != color;
    }

    @Override
    public boolean disable(Identifier id) {
        Objects.requireNonNull(id, "id");
        if (!this.disabled.add(id)) {
            return false;
        }
        this.pushOverrides();
        Constants.LOG.info("[MerlinLib] effect {} is disabled, it will not do anything and no potion is generated for it", id);
        return true;
    }

    @Override
    public Set<Identifier> registeredIds() {
        synchronized (this.registered) {
            return Set.copyOf(this.registered.keySet());
        }
    }

    @Override
    public Optional<MerlinMobEffect> get(Identifier id) {
        synchronized (this.registered) {
            return Optional.ofNullable(this.registered.get(id));
        }
    }

    @Override
    public PotionBuilder potionFor(Identifier effectId) {
        MerlinMobEffect effect = this.get(effectId).orElseThrow(() -> new IllegalArgumentException(
                effectId + " is not managed by MerlinLib; register it through MerlinApi.effects() before deriving a potion from it"));
        Identifier potionId = Identifier.fromNamespaceAndPath(effectId.getNamespace(), effectId.getPath());
        return PotionRegistry.INSTANCE.register(potionId)
                .effect(effectId, PotionBuilder.DURATION_NORMAL, 0)
                .variants();
    }

    /**
     * @return {@code true} when the effect was disabled through the api or a config file.
     */
    public boolean isDisabled(Identifier id) {
        return this.disabled.contains(id);
    }

    private void pushOverrides() {
        Map<Identifier, EffectOverrides.Entry> merged = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Integer> entry : this.colorOverrides.entrySet()) {
            merged.put(entry.getKey(), new EffectOverrides.Entry(Optional.of(entry.getValue()), !this.disabled.contains(entry.getKey())));
        }
        for (Identifier id : this.disabled) {
            merged.computeIfAbsent(id, key -> new EffectOverrides.Entry(Optional.empty(), false));
        }
        EffectOverrides.setApiLayer(merged);
    }
}
