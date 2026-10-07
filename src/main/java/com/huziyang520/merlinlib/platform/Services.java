package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import com.huziyang520.merlinlib.platform.services.ILootBridge;
import com.huziyang520.merlinlib.platform.services.IModNameBridge;
import com.huziyang520.merlinlib.platform.services.IPackBridge;
import com.huziyang520.merlinlib.platform.services.IPlatformHelper;
import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;

/**
 * Locates the loader specific implementation of a service interface.
 *
 * <p>The 26.3 line used {@link java.util.ServiceLoader} with a {@code META-INF/services} declaration
 * per interface, because the loader implementations lived in separate Gradle modules. This project
 * is a single module that targets one loader, so the implementations are referenced directly and
 * the {@code META-INF/services} files are gone. The interfaces are kept: they are what lets the
 * shared code stay free of {@code net.minecraftforge} imports.
 */
public final class Services {

    /**
     * Information about the platform MerlinLib is currently running on.
     */
    public static final IPlatformHelper PLATFORM = ForgePlatformHelper.INSTANCE;

    /**
     * Loader specific hooks for resource reload listeners.
     */
    public static final IPackBridge PACK = ForgePackBridge.INSTANCE;

    /**
     * Loader specific way of writing into the plain (non datapack) game registries. On 1.20.1 this
     * carries enchantments, mob effects and potions alike.
     */
    public static final IRegistrationBridge REGISTRATIONS = ForgeRegistrationBridge.INSTANCE;

    /**
     * Loader specific hooks for the server lifecycle, used by
     * {@link com.huziyang520.merlinlib.api.MerlinApi#lifecycle()}.
     */
    public static final ILifecycleBridge LIFECYCLE = ForgeLifecycleBridge.INSTANCE;

    /**
     * Loader specific hooks for loot table loading, used by
     * {@link com.huziyang520.merlinlib.api.MerlinApi#loot()}.
     */
    public static final ILootBridge LOOT = ForgeLootBridge.INSTANCE;

    /**
     * Loader specific lookup of a mod's display name, used by the notice settings screen.
     */
    public static final IModNameBridge MOD_NAMES = ForgeModNameBridge.INSTANCE;

    private Services() {
    }
}
