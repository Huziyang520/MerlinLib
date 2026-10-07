package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import net.minecraftforge.fml.ModList;

/**
 * Forge implementation of {@link IModNameBridge}.
 *
 * <p>{@code ModContainer.getModInfo().getDisplayName()} is the same string Forge shows in its mod
 * list, so the notice settings screen can list "Practical Enchantments" instead of
 * {@code practical_enchantments}.
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
