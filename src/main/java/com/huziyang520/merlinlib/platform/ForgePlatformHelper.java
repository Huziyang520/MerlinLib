package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Forge implementation of {@link IPlatformHelper}.
 *
 * <p>1.20.1 still allows the static FML entry points: {@code FMLLoader.isProduction()} is a static
 * method here (the 26.3 line had to move to {@code FMLEnvironment} because the call became
 * instance bound).
 */
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
