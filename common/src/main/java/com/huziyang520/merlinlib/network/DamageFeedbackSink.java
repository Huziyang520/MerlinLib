package com.huziyang520.merlinlib.network;

import java.util.function.Consumer;

/**
 * Where the client side hands incoming damage feedback to.
 *
 * <p>The payload handler is registered on both logical sides (the network setup is symmetric in 26.3),
 * but the consumer is client only code. This indirection keeps every class that the dedicated server
 * loads free of client references: the sink is {@code null} on a server, and the handler call is a
 * no-op there.
 */
public final class DamageFeedbackSink {

    private static volatile Consumer<DamageFeedbackPayload> clientSink;

    private DamageFeedbackSink() {
    }

    /**
     * Installs the client side consumer. Called once from the loader's client entrypoint.
     *
     * @param sink the consumer that receives every payload on the render thread's behalf
     */
    public static void setClientSink(Consumer<DamageFeedbackPayload> sink) {
        clientSink = sink;
    }

    /**
     * Delivers one payload to the installed consumer, or does nothing when there is none.
     *
     * @param payload the payload the network layer decoded
     */
    public static void handle(DamageFeedbackPayload payload) {
        Consumer<DamageFeedbackPayload> sink = clientSink;
        if (sink != null) {
            sink.accept(payload);
        }
    }
}
