package com.huziyang520.merlinlib.platform.services;

/**
 * Common shape of every loader bridge.
 *
 * <p>The 26.3 line discovered implementations through {@code ServiceLoader} because three Gradle
 * modules had to be wired together. This project is a single module, so the implementations are
 * instantiated directly by {@link com.huziyang520.merlinlib.platform.Services}; the interface is
 * kept so the shared code still talks to an abstraction rather than to Forge.
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
