package com.huziyang520.merlinlib.platform.services;

/**
 * Common shape of every loader bridge.
 *
 * <p>Implementations are instantiated by {@code ServiceLoader}, so they cannot receive loader objects
 * through a constructor; {@link #bootstrap(Object)} is called once from the loader entrypoint instead.
 */
public interface ILoaderBridge {

    /**
     * Installs the loader native hooks this bridge needs.
     *
     * @param loaderContext loader specific handle, currently the NeoForge mod event bus; may be
     *                      {@code null} on loaders without an equivalent object
     */
    void bootstrap(Object loaderContext);
}
