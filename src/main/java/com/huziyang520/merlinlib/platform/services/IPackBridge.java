package com.huziyang520.merlinlib.platform.services;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Loader specific glue for resource reloads.
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
