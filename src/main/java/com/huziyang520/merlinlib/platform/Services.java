// Locates the loader specific implementation of a service interface.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;

public final class Services {
    public static final IPlatformHelper PLATFORM = ForgePlatformHelper.INSTANCE;
    public static final IPackBridge PACK = ForgePackBridge.INSTANCE;
    public static final IRegistrationBridge REGISTRATIONS = ForgeRegistrationBridge.INSTANCE;
    public static final ILifecycleBridge LIFECYCLE = ForgeLifecycleBridge.INSTANCE;
    public static final ILootBridge LOOT = ForgeLootBridge.INSTANCE;
    public static final IModNameBridge MOD_NAMES = ForgeModNameBridge.INSTANCE;

    private Services() {
    }
}
