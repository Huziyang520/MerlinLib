package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fabric implementation of {@link ILifecycleBridge}, on top of {@code ServerLifecycleEvents}.
 *
 * <p>Fabric fires the events whether or not a listener exists, so the only thing worth doing here is to keep
 * the callbacks in lists until MerlinLib's bootstrap installs the listeners. That way the order between a
 * dependent mod's constructor and MerlinLib's initialisation cannot lose a registration.
 */
public class FabricLifecycleBridge implements ILifecycleBridge {

    private final List<Consumer<MinecraftServer>> starting = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> stopping = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> started = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joins = new ArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> run("starting", this.starting, server));
        ServerLifecycleEvents.SERVER_STARTED.register(server -> run("started", this.started, server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> run("stopping", this.stopping, server));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                runJoin(this.joins, handler.getPlayer()));
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        this.started.add(callback);
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        this.starting.add(callback);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        this.stopping.add(callback);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        this.joins.add(callback);
    }

    /**
     * Runs every join callback, isolating the failures.
     *
     * @param callbacks the callbacks to run, in registration order
     * @param player    the player who joined
     */
    private static void runJoin(List<Consumer<ServerPlayer>> callbacks, ServerPlayer player) {
        for (Consumer<ServerPlayer> callback : callbacks) {
            try {
                callback.accept(player);
            } catch (RuntimeException | LinkageError error) {
                Constants.LOG.error("[MerlinLib] a player join callback failed; the remaining ones still run",
                        error);
            }
        }
    }

    /**
     * Runs every callback of one phase, isolating the failures.
     *
     * @param phase     the phase name, for the log line
     * @param callbacks the callbacks to run, in registration order
     * @param server    the server that started or stopped
     */
    private static void run(String phase, List<Consumer<MinecraftServer>> callbacks, MinecraftServer server) {
        for (Consumer<MinecraftServer> callback : callbacks) {
            try {
                callback.accept(server);
            } catch (RuntimeException | LinkageError error) {
                // One broken callback must not stop the others, and must not stop the server from starting.
                Constants.LOG.error("[MerlinLib] a server {} callback failed; the remaining ones still run",
                        phase, error);
            }
        }
    }
}
