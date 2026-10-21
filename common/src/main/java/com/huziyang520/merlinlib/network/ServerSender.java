package com.huziyang520.merlinlib.network;

import java.util.function.Consumer;

/**
 * Client side sender for client to server payloads.
 *
 * <p>Each loader's client entrypoint installs one implementation; common code stays free of client
 * classes. The sink is {@code null} on a dedicated server.
 */
public final class ServerSender {

    private static volatile Consumer<Object> sink;

    private ServerSender() {
    }

    /**
     * Installs the client side sender.
     *
     * @param sender accepts a payload and sends it to the server
     */
    public static void setSink(Consumer<Object> sender) {
        sink = sender;
    }

    /**
     * Sends a client to server payload, or does nothing when there is no sender.
     *
     * @param payload the payload to send
     */
    public static void send(Object payload) {
        Consumer<Object> active = sink;
        if (active != null) {
            active.accept(payload);
        }
    }
}
