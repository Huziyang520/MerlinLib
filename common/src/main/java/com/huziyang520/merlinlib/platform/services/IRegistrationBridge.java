package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Loader specific bridge for registering content into a plain (non datapack) game registry.
 *
 * <p>Enchantments are data driven and need no bridge at all, but {@code MobEffect} and {@code Potion}
 * are still code registries, and the moment they may be written to differs per loader:
 * <ul>
 *     <li>Fabric keeps the registries open for the whole mod initialisation phase, so the value is
 *     registered immediately</li>
 *     <li>NeoForge fires a mod bus event after every mod constructor, so registrations are queued and
 *     flushed there</li>
 * </ul>
 * Both behaviours are hidden behind this interface; callers just call {@link #register}.
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
