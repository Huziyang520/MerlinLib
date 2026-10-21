package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.PotionApi;
import com.huziyang520.merlinlib.api.PotionBuilder;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.alchemy.Potion;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Registration state of every potion MerlinLib knows about.
 *
 * <p>Variant generation is centralised here so the {@code long_} / {@code strong_} naming, durations
 * and amplifiers stay consistent no matter which api path created the potion.
 */
public final class PotionRegistry implements PotionApi {

    public static final PotionRegistry INSTANCE = new PotionRegistry();

    private final Map<Identifier, PotionBuilder> registered = new LinkedHashMap<>();
    private final Set<Identifier> disabled = new LinkedHashSet<>();

    private PotionRegistry() {
    }

    @Override
    public PotionBuilder register(Identifier id) {
        Objects.requireNonNull(id, "id");
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("illegal potion id '" + id + "'");
        }
        if (this.registered.containsKey(id)) {
            throw new IllegalStateException(id + " is already registered by MerlinLib");
        }
        if (BuiltInRegistries.POTION.containsKey(id)) {
            throw new IllegalStateException(id + " already exists in the potion registry");
        }
        PotionBuilder builder = new PotionBuilder(this, id);
        this.registered.put(id, builder);
        return builder;
    }

    @Override
    public PotionBuilder potionFor(Identifier effectId) {
        return EffectRegistry.INSTANCE.potionFor(effectId);
    }

    @Override
    public Set<Identifier> registeredIds() {
        return Set.copyOf(this.registered.keySet());
    }

    /**
     * @param id potion id
     * @return {@code true} when the potion was disabled and is therefore not registered at all.
     */
    public boolean isDisabled(Identifier id) {
        return this.disabled.contains(id);
    }

    /**
     * Disables a potion so it is never written into the registry.
     *
     * @param id potion id
     * @return {@code true} when the state changed
     */
    public boolean disable(Identifier id) {
        return this.disabled.add(id);
    }

    /**
     * Registers the potion and, when requested, its {@code long_} and {@code strong_} variants.
     *
     * @param builder the declaring builder
     * @return the id of the main potion
     */
    public Identifier submit(PotionBuilder builder) {
        Identifier id = builder.id();
        if (this.isDisabled(id)) {
            Constants.LOG.info("[MerlinLib] potion {} is disabled, it will not be registered", id);
            return id;
        }
        registerOne(id, builder, -1, 0);
        if (builder.withVariants()) {
            Identifier longId = Identifier.fromNamespaceAndPath(id.getNamespace(), "long_" + id.getPath());
            Identifier strongId = Identifier.fromNamespaceAndPath(id.getNamespace(), "strong_" + id.getPath());
            registerOne(longId, builder, builder.longDuration(), 0);
            registerOne(strongId, builder, builder.strongDuration(), 1);
        }
        return id;
    }

    private void registerOne(Identifier id, PotionBuilder builder, int durationOverride, int amplifierBonus) {
        if (this.disabled.contains(id) || BuiltInRegistries.POTION.containsKey(id)) {
            Constants.LOG.warn("[MerlinLib] potion {} was not registered because the id is already taken", id);
            return;
        }
        String baseName = id.getPath();
        Services.REGISTRATIONS.register(BuiltInRegistries.POTION, id, () -> new Potion(
                baseName,
                builder.buildInstances(durationOverride, amplifierBonus).toArray(net.minecraft.world.effect.MobEffectInstance[]::new)
        ));
        Constants.LOG.debug("[MerlinLib] registered potion {}", id);
    }
}
