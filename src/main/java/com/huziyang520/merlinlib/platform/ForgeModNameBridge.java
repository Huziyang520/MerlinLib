package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import net.minecraftforge.fml.ModList;

/**
 * Forge implementation of {@link IModNameBridge}.
 */
public final class ForgeModNameBridge implements IModNameBridge {

    public static final ForgeModNameBridge INSTANCE = new ForgeModNameBridge();

    private ForgeModNameBridge() {
    }

    @Override
    public void bootstrap(Object loaderContext) {
        // Nothing to install: the lookup below is a plain query.
    }

    @Override
    public String modName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
