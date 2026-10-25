// Forge implementation of IRegistrationBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ForgeRegistrationBridge implements IRegistrationBridge {
    public static final ForgeRegistrationBridge INSTANCE = new ForgeRegistrationBridge();
    private static final Map<String, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static IEventBus modBus;

    private ForgeRegistrationBridge() {
    }

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
