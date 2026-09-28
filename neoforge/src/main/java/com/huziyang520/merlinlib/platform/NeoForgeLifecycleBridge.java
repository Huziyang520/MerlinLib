package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * NeoForge implementation of {@link ILifecycleBridge}, on top of the game bus.
 *
 * <p>The server events live on the game bus rather than on the mod bus that the mod constructor receives, so
 * they are installed from here rather than from the entrypoint. Callbacks are buffered until then, which is
 * what makes the registration order between mods irrelevant.
 */
public class NeoForgeLifecycleBridge implements ILifecycleBridge {

    private final List<Consumer<MinecraftServer>> starting = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> stopping = new ArrayList<>();

    @Override
    public void bootstrap(Object loaderContext) {
        // Written as lambdas with the event type spelled out: the bus takes a Consumer whose type parameter
        // cannot be inferred from a method reference outside an event class.
        NeoForge.EVENT_BUS.addListener((ServerStartingEvent event) ->
                run("starting", this.starting, event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) ->
                run("stopping", this.stopping, event.getServer()));
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
