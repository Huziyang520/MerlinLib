// Fabric implementation of IPackBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.reload.ContentReloadListener;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public class FabricPackBridge implements IPackBridge {
    @Override
    public void bootstrap(Object loaderContext) {
        registerServerReloadListener(ContentReloadListener.ID, ContentReloadListener.instance());
    }

    @Override
    public void registerServerReloadListener(Identifier id, PreparableReloadListener listener) {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener);
    }
}
