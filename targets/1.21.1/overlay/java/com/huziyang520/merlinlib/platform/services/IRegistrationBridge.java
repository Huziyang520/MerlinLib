// Loader specific bridge for registering content into a plain (non datapack) game registry.

package com.huziyang520.merlinlib.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Supplier;

public interface IRegistrationBridge extends ILoaderBridge {
    <T> void register(Registry<T> registry, ResourceLocation id, Supplier<T> value);
}
