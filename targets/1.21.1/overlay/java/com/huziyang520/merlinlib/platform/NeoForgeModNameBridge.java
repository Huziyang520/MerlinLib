// NeoForge side of IModNameBridge, answered out of the loader's own mod list.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import net.neoforged.fml.ModList;

public class NeoForgeModNameBridge implements IModNameBridge {
    @Override
    public String modName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }

    @Override
    public void bootstrap(Object loaderContext) {
    }
}
