// Fabric implementation of IRegistrationBridge.

package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.IRegistrationBridge;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import java.util.function.Supplier;

public class FabricRegistrationBridge implements IRegistrationBridge {
    @Override
    public void bootstrap(Object loaderContext) {
    }

    @Override
    public <T> void register(Registry<T> registry, Identifier id, Supplier<T> value) {
        Registry.register(registry, id, value.get());
    }
}
