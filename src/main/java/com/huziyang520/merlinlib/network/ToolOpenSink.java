package com.huziyang520.merlinlib.network;

import java.util.function.IntConsumer;

/**
 * Where the client side hands an incoming "open this screen" request.
 *
 * <p>The same indirection {@link DamageFeedbackSink} exists for: the payload handler is registered for the
 * whole process, because a {@code SimpleChannel} has no per side handler registration on 1.20.1, while the
 * consumer has to open a {@code Screen} and is therefore client only. The sink is {@code null} on a
 * dedicated server and the handler call is a no-op there.
 */
public final class ToolOpenSink {

    private static volatile IntConsumer clientSink;

    private ToolOpenSink() {
    }

    /**
     * Installs the client side consumer. Called once from the loader's client entrypoint.
     *
     * @param sink receives the tool ordinal of every request
     */
    public static void setClientSink(IntConsumer sink) {
        clientSink = sink;
    }

    /**
     * Delivers one request to the installed consumer, or does nothing when there is none.
     *
     * @param payload the payload the network layer decoded
     */
    public static void handle(OpenToolPayload payload) {
        IntConsumer sink = clientSink;
        if (sink != null) {
            sink.accept(payload.tool());
        }
    }
}
