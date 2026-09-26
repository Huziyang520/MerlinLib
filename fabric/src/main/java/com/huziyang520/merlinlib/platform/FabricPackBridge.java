package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.reload.ContentReloadListener;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

/**
 * Fabric implementation of {@link IPackBridge}.
 *
 * <p>The generated pack itself is installed by {@code PackRepositoryMixin}: Fabric has no public way
 * to append a repository source, and the pack has to be known before the world is loaded, which is
 * earlier than any Fabric lifecycle event. Reload listeners do have a public api, so they are
 * registered here.
 */
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
