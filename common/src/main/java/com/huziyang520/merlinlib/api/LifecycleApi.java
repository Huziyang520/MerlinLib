package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Server lifecycle hooks, reached through {@link MerlinApi#lifecycle()}.
 *
 * <p>The intended use is the one thing every mod has to do and none of the loaders make easy: run a piece of
 * setup once per server, on both loaders, without writing the same two platform hooks twice.
 *
 * <pre>{@code
 * MerlinApi.lifecycle().onServerStarting(() -> {
 *     MyConfig.load();
 *     MyNotice.register();
 * });
 * }</pre>
 *
 * <p>Registration is safe from a mod constructor, before MerlinLib has bootstrapped, and safe to call more
 * than once: the callbacks are collected and run at each server start, in registration order. A callback that
 * throws is logged and does not stop the others.
 */
public final class LifecycleApi {

    /** Single instance, handed out by {@link MerlinApi#lifecycle()}. */
    public static final LifecycleApi INSTANCE = new LifecycleApi();

    private LifecycleApi() {
    }

    /**
     * Runs the callback every time a server starts.
     *
     * <p>This is a server side moment on both loaders, so it is the right place for work that sends system
     * messages, reads server configuration or touches the world.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onServerStarting(Runnable callback) {
        Services.LIFECYCLE.onServerStarting(server -> callback.run());
    }

    /**
     * Runs the callback every time a server starts, handing it the server.
     *
     * <p>The form for work that needs the running server: its recipe manager, its player list, its levels.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStarting(callback);
    }

    /**
     * Runs the callback every time a server stops.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onServerStopping(Runnable callback) {
        Services.LIFECYCLE.onServerStopping(server -> callback.run());
    }

    /**
     * Runs the callback every time a server stops, handing it the server.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStopping(callback);
    }

    /**
     * Runs the callback once the server has finished starting, handing it the server.
     *
     * <p>The moment to use when the work resolves anything out of the data driven registries - enchantments,
     * for instance: they are empty at {@link #onServerStarting(Runnable)} because the data packs have not been
     * read yet, and populated by the time this runs.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        Services.LIFECYCLE.onServerStarted(callback);
    }

    /**
     * Runs the callback every time a player joins, handing it the player.
     *
     * <p>The moment for anything a player should see as they arrive: a welcome message, a version notice, a
     * first-join reward. Sending from here reaches them right after the world finished loading.
     *
     * @param callback the work to run, never {@code null}
     */
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Services.LIFECYCLE.onPlayerJoin(callback);
    }
}
