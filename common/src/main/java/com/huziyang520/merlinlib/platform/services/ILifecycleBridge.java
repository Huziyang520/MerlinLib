package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Loader specific bridge for the server lifecycle.
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
     * <p>The difference from {@link #onServerStarting} matters: at start, the data packs have not been read
     * yet, so the dynamic registries - enchantments among them - are not populated. Work that resolves
     * anything out of those registries must wait for this moment, while work that has to be in place
     * <em>before</em> the packs are read (loot table rules, for instance) must use the other one.
     *
     * @param callback the work to run, never {@code null}
     */
    void onServerStarted(Consumer<MinecraftServer> callback);

    /**
     * Runs the callback every time a player joins, after the connection is accepted.
     *
     * <p>This is the moment a welcome message can be sent: the player exists, their connection is up, and
     * messages sent now arrive before the loading screen is over on most setups.
     *
     * @param callback the work to run, never {@code null}
     */
    void onPlayerJoin(Consumer<ServerPlayer> callback);
}
