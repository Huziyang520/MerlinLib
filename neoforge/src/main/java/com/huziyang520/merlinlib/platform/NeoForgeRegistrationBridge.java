package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * NeoForge implementation of {@link IRegistrationBridge}.
 *
 * <p>Registries are not writable while mod constructors run, so values are queued and written when
 * {@link RegisterEvent} fires for the matching registry.
 */
public class NeoForgeRegistrationBridge implements IRegistrationBridge {

    private record Pending(Identifier id, Supplier<?> value) {
    }

    private final Map<ResourceKey<? extends Registry<?>>, List<Pending>> pending = new ConcurrentHashMap<>();

    @Override
    public void bootstrap(Object loaderContext) {
        if (loaderContext instanceof IEventBus modBus) {
            modBus.addListener(this::onRegister);
        }
    }

    @Override
    public <T> void register(Registry<T> registry, Identifier id, Supplier<T> value) {
        this.pending.computeIfAbsent(registry.key(), key -> new CopyOnWriteArrayList<>()).add(new Pending(id, value));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void onRegister(RegisterEvent event) {
        List<Pending> queued = this.pending.get(event.getRegistryKey());
        if (queued == null || queued.isEmpty()) {
            return;
        }
        for (Pending entry : queued) {
            // The registry key always matches the registry the value was queued for, so the raw cast is safe.
            event.register((ResourceKey) event.getRegistryKey(), entry.id(), entry.value());
        }
    }
}
