package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Loader specific hooks for the server lifecycle.
 *
 * <p>On 1.20.1 the four moments map onto Forge's {@code ServerAboutToStartEvent},
 * {@code ServerStartedEvent}, {@code ServerStoppingEvent} and {@code PlayerEvent.PlayerLoggedInEvent}.
 *
 * <p>The distinction between {@link #onServerStarting} and {@link #onServerStarted} still matters,
 * but for a different reason than on 26.3: 1.20.1 has no data driven enchantment registry, so
 * nothing about enchantments has to wait for the packs. What does have to wait is anything that
 * touches the finished server (config load, loot rules that resolve registry ids).
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
