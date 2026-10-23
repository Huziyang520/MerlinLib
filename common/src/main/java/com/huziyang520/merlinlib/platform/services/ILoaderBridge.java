package com.huziyang520.merlinlib.platform.services;

/**
 * Common shape of every loader bridge.
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
