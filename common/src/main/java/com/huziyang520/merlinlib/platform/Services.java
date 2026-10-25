// Locates the loader specific implementation of a service interface.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import java.util.ServiceLoader;

public class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IPackBridge PACK = load(IPackBridge.class);
    public static final IRegistrationBridge REGISTRATIONS = load(IRegistrationBridge.class);
    public static final ILifecycleBridge LIFECYCLE = load(ILifecycleBridge.class);
    public static final ILootBridge LOOT = load(ILootBridge.class);
    public static final IModNameBridge MOD_NAMES = load(IModNameBridge.class);

    public static <T> T load(Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
