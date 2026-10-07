package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Loader specific bridge for registering content into a plain (non datapack) game registry.
 *
 * <p>On 1.20.1 this is no longer just for mob effects and potions: enchantments are a plain game
 * registry here too (there is no data driven enchantment registry before 1.21), so the same
 * deferred registration path carries the whole content pipeline.
 *
 * <p>Forge opens its registries during the mod event bus phase, so implementations queue the
 * supplier and flush it when the registry event fires. Callers only see {@link #register}.
 */
public interface IRegistrationBridge extends ILoaderBridge {

    /**
     * Registers a value, or queues it until the loader opens the registry.
     *
     * @param registry the target registry, e.g. {@code BuiltInRegistries.MOB_EFFECT}
     * @param id       id the value is registered under, the namespace may differ from MerlinLib
     * @param value    creates the value; it is called when the registry is open
     * @param <T>      registry element type
     */
    <T> void register(Registry<T> registry, ResourceLocation id, Supplier<T> value);
}
