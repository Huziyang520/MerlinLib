package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Loader specific bridge for registering content into a plain (non datapack) game registry.
 */
public interface IRegistrationBridge extends ILoaderBridge {

    /**
     * Registers a value, or queues it until the loader opens the registry.
     *
     * @param registry the target registry, e.g. {@code BuiltInRegistries.MOB_EFFECT}
     * @param id       id the value is registered under, the namespace may differ from MerlinLib
     * @param value    creates the value; on NeoForge it is called when the registry is open
     * @param <T>      registry element type
     */
    <T> void register(Registry<T> registry, Identifier id, Supplier<T> value);
}
