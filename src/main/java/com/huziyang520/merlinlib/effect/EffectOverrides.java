package com.huziyang520.merlinlib.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime overrides applied to effects.
 *
 * <p>A code registry cannot lose an entry, so "disabling" and "recolouring" are implemented as
 * read-time overrides: {@link MerlinMobEffect} consults this table instead of its declared values.
 *
 * <p>Two independent layers are kept so that neither side has to know about the other's bookkeeping:
 * <ul>
 *     <li>the <b>api layer</b>, written by {@code MerlinApi.effects()}</li>
 *     <li>the <b>config layer</b>, rebuilt from {@code config/MerlinLib/effects.json} at startup</li>
 * </ul>
 * The config layer wins, matching the rule that a config file overrides code declarations. Layers are
 * replaced wholesale, never mutated in place, so a reader can never observe a half applied change.
 *
 * <p>Note for 1.20.1: {@code MobEffect#getColor()} does not exist on this version - the colour is a
 * {@code private final int} field read reflectively by the client, and Forge exposes it through
 * nothing public. The declared colour is therefore still visible through
 * {@link MerlinMobEffect#getColor()} for anything MerlinLib draws itself, while an override of an
 * <em>already registered</em> effect's colour needs the accessor mixin. Nothing in this project
 * registers an effect yet, so the override path is wired but not exercised; it is kept because the
 * api promises it and the config file documents it.
 */
public final class EffectOverrides {

    private static volatile Map<ResourceLocation, Entry> apiLayer = Map.of();
    private static volatile Map<ResourceLocation, Entry> configLayer = Map.of();

    private EffectOverrides() {
    }

    /**
     * @param color   replacement colour, empty to keep the declared one
     * @param enabled {@code false} turns the effect into a no-op
     */
    public record Entry(Optional<Integer> color, boolean enabled) {
    }

    /**
     * @param id            effect id
     * @param declaredColor the colour the effect was registered with
     * @return the colour that should be used right now
     */
    public static int colorOf(ResourceLocation id, int declaredColor) {
        Entry entry = merged().get(id);
        if (entry != null && entry.color().isPresent()) {
            return entry.color().get();
        }
        return declaredColor;
    }

    /**
     * @param id effect id
     * @return {@code true} when the effect was disabled, by code or by configuration
     */
    public static boolean isDisabled(ResourceLocation id) {
        Entry entry = merged().get(id);
        return entry != null && !entry.enabled();
    }

    public static Optional<Entry> get(ResourceLocation id) {
        return Optional.ofNullable(merged().get(id));
    }

    /** Replaces the layer written by the java api. */
    public static void setApiLayer(Map<ResourceLocation, Entry> entries) {
        apiLayer = Map.copyOf(entries);
        pushDisabledState();
    }

    /** Replaces the layer read from the configuration files, then refreshes live effect instances. */
    public static void setConfigLayer(Map<ResourceLocation, Entry> entries) {
        configLayer = Map.copyOf(entries);
        pushDisabledState();
    }

    /** @return the effective table after both layers are merged, for diagnostics. */
    public static Map<ResourceLocation, Entry> current() {
        return Map.copyOf(merged());
    }

    private static Map<ResourceLocation, Entry> merged() {
        Map<ResourceLocation, Entry> merged = new LinkedHashMap<>(apiLayer);
        merged.putAll(configLayer);
        return merged;
    }

    /**
     * Walks the effect registry and tells every MerlinLib effect whether it is disabled.
     *
     * <p>Guarded because {@link BuiltInRegistries} throws when the registry has not been frozen yet,
     * and a config file can be read before that (the sample file is written on first launch, when the
     * mod is still constructing). A read that arrives before the registry exists simply has nothing to
     * push to: the effect instance is created later and reads {@link #isDisabled} itself.
     */
    private static void pushDisabledState() {
        Map<ResourceLocation, Entry> current = merged();
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            if (effect instanceof MerlinMobEffect merlinEffect) {
                Entry entry = current.get(merlinEffect.id());
                merlinEffect.setDisabled(entry != null && !entry.enabled());
            }
        }
    }
}
