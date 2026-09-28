package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

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

    @Override
    public void bootstrap(Object loaderContext) {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> run("starting", this.starting, server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> run("stopping", this.stopping, server));
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        this.starting.add(callback);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        this.stopping.add(callback);
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
