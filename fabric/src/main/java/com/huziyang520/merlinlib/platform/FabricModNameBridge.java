package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric side of {@link IModNameBridge}, answered out of the loader's own mod list.
 */
public class FabricModNameBridge implements IModNameBridge {

    @Override
    public String modName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    @Override
    public void bootstrap(Object loaderContext) {
        // Nothing to install: the mod list is already there when this is asked.
    }
}
