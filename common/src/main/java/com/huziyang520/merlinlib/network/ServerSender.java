// Client side sender for client to server payloads.

package com.huziyang520.merlinlib.network;

import java.util.function.Consumer;

public final class ServerSender {
    private static volatile Consumer<Object> sink;

    private ServerSender() {
    }

    public static void setSink(Consumer<Object> sender) {
        sink = sender;
    }

    public static void send(Object payload) {
        Consumer<Object> active = sink;
        if (active != null) {
            active.accept(payload);
        }
    }
}
