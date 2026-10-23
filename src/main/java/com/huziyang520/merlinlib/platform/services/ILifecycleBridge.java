package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Loader specific hooks for the server lifecycle.
 */
public interface ILifecycleBridge extends ILoaderBridge {

    /**
     * Runs the callback every time a server starts.
     *
     * @param callback the work to run, never {@code null}
 */
    void onServerStarting(Consumer<MinecraftServer> callback);

    /**
     * Runs the callback every time a server stops.
     *
     * @param callback the work to run, never {@code null}
 */
    void onServerStopping(Consumer<MinecraftServer> callback);

    /**
     * Runs the callback every time a server has finished starting.
     *
     * @param callback the work to run, never {@code null}
 */
    void onServerStarted(Consumer<MinecraftServer> callback);

    /**
     * Runs the callback every time a player joins, after the connection is accepted.
     *
     * @param callback the work to run, never {@code null}
 */
    void onPlayerJoin(Consumer<ServerPlayer> callback);
}
