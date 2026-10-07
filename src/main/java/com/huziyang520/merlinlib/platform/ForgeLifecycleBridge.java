package com.huziyang520.merlinlib.platform;

import com.huziyang520.merlinlib.platform.services.ILifecycleBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Forge implementation of {@link ILifecycleBridge}.
 *
 * <p>EventBus is 6.2.x on 1.20.1, so the classic {@code MinecraftForge.EVENT_BUS.addListener(method
 * reference)} form is correct here. The EventBus 7 rule ("only one listener per class, use the
 * event class's own BUS") applies from 1.21.8 onwards and does not apply to this line.
 *
 * <p>Callbacks are collected in lists because several mods may register for the same moment, and
 * bootstrap installs the four Forge listeners exactly once.
 */
public final class ForgeLifecycleBridge implements ILifecycleBridge {

    public static final ForgeLifecycleBridge INSTANCE = new ForgeLifecycleBridge();

    private final List<Consumer<MinecraftServer>> startingCallbacks = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> startedCallbacks = new ArrayList<>();
    private final List<Consumer<MinecraftServer>> stoppingCallbacks = new ArrayList<>();
    private final List<Consumer<ServerPlayer>> joinCallbacks = new ArrayList<>();

    private boolean installed;

    private ForgeLifecycleBridge() {
    }

    @Override
    public void bootstrap(Object loaderContext) {
        if (installed) {
            return;
        }
        installed = true;
        // The handlers are named `handle*` and not `onServer*` on purpose: this class also has
        // `onServerStarting` / `onServerStarted` registration methods of its own, and a method
        // reference to an overloaded name is ambiguous - javac cannot choose between the
        // registration method and the event handler, and the whole bootstrap fails to compile.
        MinecraftForge.EVENT_BUS.addListener(this::handleServerAboutToStart);
        MinecraftForge.EVENT_BUS.addListener(this::handleServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::handleServerStopping);
        MinecraftForge.EVENT_BUS.addListener(this::handlePlayerLoggedIn);
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> callback) {
        startingCallbacks.add(callback);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> callback) {
        stoppingCallbacks.add(callback);
    }

    @Override
    public void onServerStarted(Consumer<MinecraftServer> callback) {
        startedCallbacks.add(callback);
    }

    @Override
    public void onPlayerJoin(Consumer<ServerPlayer> callback) {
        joinCallbacks.add(callback);
    }

    /**
     * {@code ServerAboutToStartEvent} is the earliest moment the server object exists, which is the
     * 1.20.1 equivalent of 26.3's "before the data packs are read".
     */
    private void handleServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        for (Consumer<MinecraftServer> callback : startingCallbacks) {
            callback.accept(server);
        }
    }

    private void handleServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        for (Consumer<MinecraftServer> callback : startedCallbacks) {
            callback.accept(server);
        }
    }

    private void handleServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        for (Consumer<MinecraftServer> callback : stoppingCallbacks) {
            callback.accept(server);
        }
    }

    private void handlePlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (Consumer<ServerPlayer> callback : joinCallbacks) {
                callback.accept(player);
            }
        }
    }
}
