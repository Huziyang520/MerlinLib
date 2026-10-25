// Runtime overrides applied to effects.

package com.huziyang520.merlinlib.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class EffectOverrides {
    private static volatile Map<Identifier, Entry> apiLayer = Map.of();
    private static volatile Map<Identifier, Entry> configLayer = Map.of();

    private EffectOverrides() {
    }

    public record Entry(Optional<Integer> color, boolean enabled) {
    }

    public static int colorOf(Identifier id, int declaredColor) {
        Entry entry = merged().get(id);
        if (entry != null && entry.color().isPresent()) {
            return entry.color().get();
        }
        return declaredColor;
    }

    public static boolean isDisabled(Identifier id) {
        Entry entry = merged().get(id);
        return entry != null && !entry.enabled();
    }

    public static Optional<Entry> get(Identifier id) {
        return Optional.ofNullable(merged().get(id));
    }

    public static void setApiLayer(Map<Identifier, Entry> entries) {
        apiLayer = Map.copyOf(entries);
        pushDisabledState();
    }

    public static void setConfigLayer(Map<Identifier, Entry> entries) {
        configLayer = Map.copyOf(entries);
        pushDisabledState();
    }

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
