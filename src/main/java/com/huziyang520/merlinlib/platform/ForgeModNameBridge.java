// Forge implementation of IModNameBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import net.minecraftforge.fml.ModList;

public final class ForgeModNameBridge implements IModNameBridge {
    public static final ForgeModNameBridge INSTANCE = new ForgeModNameBridge();

    private ForgeModNameBridge() {
    }

    @Override
    public void bootstrap(Object loaderContext) {
    }

    @Override
    public String modName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId);
    }
}
