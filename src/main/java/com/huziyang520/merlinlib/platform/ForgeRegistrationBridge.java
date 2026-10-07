package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Forge implementation of {@link IRegistrationBridge}.
 *
 * <p>Forge freezes its registries before mod constructors run and re-opens them for the length of
 * the registry events, so every value has to go through {@code DeferredRegister}: the supplier is
 * stored now and invoked by Forge while the registry is writable. Calling {@code new Enchantment(..)}
 * or {@code new MobEffect(..)} at mod construction time throws
 * {@code IllegalStateException: Registry is already frozen} - that failure mode was one of the three
 * crash layers documented for the 26.3 line and it is exactly why this bridge exists.
 *
 * <p>One {@code DeferredRegister} is kept per (registry, namespace) pair, because
 * {@code DeferredRegister.create(registry, namespace)} derives the final id from the namespace.
 * The namespace comes from the id the caller passed, so a downstream mod can register under its own
 * namespace through MerlinLib.
 */
public final class ForgeRegistrationBridge implements IRegistrationBridge {

    public static final ForgeRegistrationBridge INSTANCE = new ForgeRegistrationBridge();

    private static final Map<String, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();

    private static IEventBus modBus;

    private ForgeRegistrationBridge() {
    }

    /**
     * Remembers the mod event bus and flushes every register that was created before it arrived.
     *
     * @param loaderContext the Forge mod event bus
     */
    @Override
    public void bootstrap(Object loaderContext) {
        if (!(loaderContext instanceof IEventBus bus)) {
            return;
        }
        modBus = bus;
        for (DeferredRegister<?> register : REGISTERS.values()) {
            register.register(bus);
        }
    }

    @Override
    public <T> void register(Registry<T> registry, ResourceLocation id, Supplier<T> value) {
        deferred(registry, id.getNamespace()).register(id.getPath(), value);
    }

    /**
     * Returns (and creates on first use) the deferred register backing one registry and namespace.
     *
     * @param registry  the target registry
     * @param namespace the namespace entries are created under
     * @param <T>       registry element type
     */
    @SuppressWarnings("unchecked")
    private static <T> DeferredRegister<T> deferred(Registry<T> registry, String namespace) {
        String key = registry.key().location() + "@" + namespace;
        return (DeferredRegister<T>) REGISTERS.computeIfAbsent(key, ignored -> {
            DeferredRegister<T> created = DeferredRegister.create(registry.key(), namespace);
            if (modBus != null) {
                created.register(modBus);
            }
            return created;
        });
    }
}
