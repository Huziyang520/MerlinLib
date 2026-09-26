package com.huziyang520.merlinlib.platform.services;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Loader specific glue for everything that has to do with packs and reloads.
 *
 * <p>Implementations are discovered through {@code META-INF/services}, so they must have a public
 * no-argument constructor. Anything the loader needs at bootstrap time is passed through
 * {@link #bootstrap(Object)} instead of the constructor.
 */
public interface IPackBridge extends ILoaderBridge {

    /**
     * Installs the loader native hooks for the generated pack and for the content reload listener.
     *
     * <p>Called exactly once, while the loader entrypoint is running.
     *
     * @param loaderContext loader specific handle, currently the NeoForge mod event bus; may be
     *                      {@code null} when the loader has no such object
     */
    @Override
    void bootstrap(Object loaderContext);

    /**
     * Registers a listener that runs on every server resource reload.
     *
     * <p>Loaders that expose a single bootstrap hook implement this by remembering the listener
     * instead; calling it twice with the same id must be a no-op.
     *
     * @param id       id the listener is known under
     * @param listener the listener to run
     */
    void registerServerReloadListener(Identifier id, PreparableReloadListener listener);
}
