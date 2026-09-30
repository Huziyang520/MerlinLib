package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Loader specific bridge for the server lifecycle.
 *
 * <h2>What it is for</h2>
 *
 * <p>A dependent mod often has work that must happen once per server: reading its configuration, sending a
 * notice when a player joins, warming a cache. The place to do that is not the mod constructor - there is no
 * server yet - and not a client only hook, because those run on the wrong side. Each loader has its own event
 * for it ({@code ServerLifecycleEvents} and {@code ServerPlayConnectionEvents} on Fabric,
 * {@code ServerStartingEvent} and {@code PlayerEvent.PlayerLoggedInEvent} on NeoForge), and this bridge is the
 * single shape all of them are hidden behind.
 *
 * <h2>Registration order does not matter</h2>
 *
 * <p>Callbacks are collected by the bridge and only run when the loader event fires, so a dependent mod may
 * register from its own constructor whether that runs before or after MerlinLib's bootstrap. Every callback is
 * isolated: one that throws is logged and the remaining ones still run, because a broken listener in one mod
 * must not take the others - or the server - down with it.
 *
 * <p>The callbacks receive the server, or the player, because that is what the interesting work at that moment
 * needs. A callback that only wants "run this" can ignore the argument - {@code LifecycleApi} offers shorter
 * forms as well.
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
