package com.huziyang520.merlinlib.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
 *     <li>the <b>config layer</b>, rebuilt from {@code config/MerlinLib/effects.json} on every reload</li>
 * </ul>
 * The config layer wins, matching the rule that a config file overrides code declarations. Layers are
 * replaced wholesale, never mutated in place, so a reload can never be observed half applied.
 */
public final class EffectOverrides {

    private static volatile Map<Identifier, Entry> apiLayer = Map.of();
    private static volatile Map<Identifier, Entry> configLayer = Map.of();

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
    public static int colorOf(Identifier id, int declaredColor) {
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
    public static boolean isDisabled(Identifier id) {
        Entry entry = merged().get(id);
        return entry != null && !entry.enabled();
    }

    public static Optional<Entry> get(Identifier id) {
        return Optional.ofNullable(merged().get(id));
    }

    /** Replaces the layer written by the java api. */
    public static void setApiLayer(Map<Identifier, Entry> entries) {
        apiLayer = Map.copyOf(entries);
        pushDisabledState();
    }

    /** Replaces the layer read from the configuration files, then refreshes live effect instances. */
    public static void setConfigLayer(Map<Identifier, Entry> entries) {
        configLayer = Map.copyOf(entries);
        pushDisabledState();
    }

    /** @return the effective table after both layers are merged, for diagnostics. */
    public static Map<Identifier, Entry> current() {
        return Map.copyOf(merged());
    }

    private static Map<Identifier, Entry> merged() {
        Map<Identifier, Entry> merged = new LinkedHashMap<>(apiLayer);
        merged.putAll(configLayer);
        return merged;
    }

    private static void pushDisabledState() {
        Map<Identifier, Entry> current = merged();
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            if (effect instanceof MerlinMobEffect merlinEffect) {
                Entry entry = current.get(merlinEffect.id());
                merlinEffect.setDisabled(entry != null && !entry.enabled());
            }
        }
    }
}
