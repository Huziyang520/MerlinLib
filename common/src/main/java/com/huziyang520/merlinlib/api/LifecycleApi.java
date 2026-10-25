// Server lifecycle hooks, reached through MerlinApi#lifecycle().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;

public final class LifecycleApi {
    public static final LifecycleApi INSTANCE = new LifecycleApi();

    private LifecycleApi() {
    }

    public void onServerStarting(Runnable callback) {
        Services.LIFECYCLE.onServerStarting(server -> callback.run());
    }

    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStarting(callback);
    }

    public void onServerStopping(Runnable callback) {
        Services.LIFECYCLE.onServerStopping(server -> callback.run());
    }

    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStopping(callback);
    }

    public void onServerStarted(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStarted(callback);
    }

    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Services.LIFECYCLE.onPlayerJoin(callback);
    }
}
