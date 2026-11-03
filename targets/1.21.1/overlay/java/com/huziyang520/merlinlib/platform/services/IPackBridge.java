// Loader specific glue for everything that has to do with packs and reloads.

package com.huziyang520.merlinlib.platform.services;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public interface IPackBridge extends ILoaderBridge {
    @Override
    void bootstrap(Object loaderContext);
    void registerServerReloadListener(ResourceLocation id, PreparableReloadListener listener);
}
