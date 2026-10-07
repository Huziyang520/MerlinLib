package com.huziyang520.merlinlib.platform.services;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Loader specific glue for resource reloads.
 *
 * <p>On 26.3 this interface also mounted the runtime generated datapack. That whole mechanism is
 * gone on 1.20.1: enchantments are code registered (see the project plan §3), so there is no pack
 * to inject, no {@code KnownPack} to declare and no "experimental settings" warning to fix. What
 * remains is the ability to register a reload listener.
 *
 * <p>Note the 1.20.1 rename: {@code Identifier} is called {@link ResourceLocation} here.
 */
public interface IPackBridge extends ILoaderBridge {

    /**
     * Installs the loader native hooks this bridge needs.
     *
     * @param loaderContext the Forge mod event bus; may be {@code null} when not available
     */
    @Override
    void bootstrap(Object loaderContext);

    /**
     * Registers a listener that runs on every server resource reload.
     *
     * @param id       id the listener is known under
     * @param listener the listener to run
     */
    void registerServerReloadListener(ResourceLocation id, PreparableReloadListener listener);
}
