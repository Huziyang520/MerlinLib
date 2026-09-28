package com.huziyang520.merlinlib.platform.services;

import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

/**
 * Loader specific bridge for the server lifecycle.
 *
 * <h2>What it is for</h2>
 *
 * <p>A dependent mod often has work that must happen once per server: reading its configuration, sending a
 * notice when a player joins, warming a cache. The place to do that is not the mod constructor - there is no
 * server yet - and not a client only hook, because those run on the wrong side. Each loader has its own event
 * for it ({@code ServerLifecycleEvents} on Fabric, {@code ServerStartingEvent} on NeoForge), and this bridge
 * is the single shape both of them are hidden behind.
 *
 * <h2>Registration order does not matter</h2>
 *
 * <p>Callbacks are collected by the bridge and only run when the loader event fires, so a dependent mod may
 * register from its own constructor whether that runs before or after MerlinLib's bootstrap. Every callback is
 * isolated: one that throws is logged and the remaining ones still run, because a broken listener in one mod
 * must not take the others - or the server - down with it.
 *
 * <p>The callback receives the server, because the interesting work at that moment needs it: reading the
 * recipe manager, listing the players, touching the world. A callback that only wants "run this" can ignore
 * the argument - {@code LifecycleApi} offers that shorter form as well.
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
}
