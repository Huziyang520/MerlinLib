// Loader specific hooks for the server lifecycle.

package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.Consumer;

public interface ILifecycleBridge extends ILoaderBridge {
    void onServerStarting(Consumer<MinecraftServer> callback);
    void onServerStopping(Consumer<MinecraftServer> callback);
    void onServerStarted(Consumer<MinecraftServer> callback);
    void onPlayerJoin(Consumer<ServerPlayer> callback);
}
