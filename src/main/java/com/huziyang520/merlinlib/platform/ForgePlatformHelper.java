// Forge implementation of IPlatformHelper.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ForgePlatformHelper implements IPlatformHelper {
    public static final ForgePlatformHelper INSTANCE = new ForgePlatformHelper();

    private ForgePlatformHelper() {
    }

    @Override
    public String getPlatformName() {
        return "forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
