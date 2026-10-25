package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Fabric implementation of {@link IRegistrationBridge}.
 */
public class FabricRegistrationBridge implements IRegistrationBridge {

    @Override
    public void bootstrap(Object loaderContext) {
        // Nothing to install: registration happens inline while the mod is initialising.
    }

    @Override
    public <T> void register(Registry<T> registry, Identifier id, Supplier<T> value) {
        Registry.register(registry, id, value.get());
    }
}
