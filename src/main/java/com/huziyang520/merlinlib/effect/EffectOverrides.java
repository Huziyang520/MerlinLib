package com.huziyang520.merlinlib.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime overrides applied to effects.
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
