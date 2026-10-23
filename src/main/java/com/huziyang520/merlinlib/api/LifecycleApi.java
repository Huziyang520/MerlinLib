package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Server lifecycle hooks, reached through {@code MerlinApi.lifecycle()}.
 */
public final class LifecycleApi {

    /** Single instance, handed out by {@code MerlinApi.lifecycle()}. */
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
     * <p>The moment for work that needs the server to be complete - its player list, its levels, anything
     * resolved against the registries the loaders have already frozen by then. Nothing about MerlinLib's own
     * content has to wait for the data packs on 1.20.1, since enchantments, effects and potions are plain
     * registry entries here rather than data driven ones.
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
     * first-join reward. Sending from here reaches them right after the world finished loading, which is what
     * {@code NoticeManager} relies on.
     *
     * @param callback the work to run, never {@code null}
 */
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        Services.LIFECYCLE.onPlayerJoin(callback);
    }
}
