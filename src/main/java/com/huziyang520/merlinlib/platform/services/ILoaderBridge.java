package com.huziyang520.merlinlib.platform.services;

/**
 * Common shape of every loader bridge.
 */
public interface ILoaderBridge {

    /**
     * Installs the loader native hooks this bridge needs.
     *
     * @param loaderContext loader specific handle, the Forge mod event bus; may be {@code null}
     *                      when the caller has no such object at hand
 */
    void bootstrap(Object loaderContext);
}
