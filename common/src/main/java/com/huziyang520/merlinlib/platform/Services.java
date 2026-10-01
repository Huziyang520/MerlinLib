package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;

import java.util.ServiceLoader;

/**
 * Locates the loader specific implementation of a service interface.
 *
 * <p>Java's built-in {@link ServiceLoader} is used so that the common code can call loader features
 * through a thin interface while each loader project supplies the real implementation. The binding
 * is declared in {@code META-INF/services/<service interface FQN>}.
 */
public class Services {

    /**
     * Information about the platform MerlinLib is currently running on.
     */
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * Loader specific hooks for the generated datapack and for resource reload listeners.
     */
    public static final IPackBridge PACK = load(IPackBridge.class);

    /**
     * Loader specific way of writing into the plain (non datapack) game registries, needed for mob
     * effects and potions.
     */
    public static final IRegistrationBridge REGISTRATIONS = load(IRegistrationBridge.class);

    /**
     * Loader specific hooks for the server lifecycle, used by
     * {@link com.huziyang520.merlinlib.api.MerlinApi#lifecycle()}.
     */
    public static final ILifecycleBridge LIFECYCLE = load(ILifecycleBridge.class);

    /**
     * Loader specific hooks for adding pools to loot tables while they load, used by
     * {@link com.huziyang520.merlinlib.api.MerlinApi#loot()}.
     */
    public static final ILootBridge LOOT = load(ILootBridge.class);

    /**
     * Loader specific way of naming a mod, used by the join notice settings screen to show
     * {@code Practical Enchantments} instead of {@code practical_enchantments}.
     */
    public static final IModNameBridge MOD_NAMES = load(IModNameBridge.class);

    /**
     * Loads the implementation of the requested service for the current environment.
     *
     * @param clazz the service interface to load
     * @param <T>   the service interface type
     * @return the loaded implementation
     * @throws NullPointerException when no implementation is declared for this environment
     */
    public static <T> T load(Class<T> clazz) {

        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.debug("Loaded {} for service {}", loadedService, clazz);
        return loadedService;
    }
}
