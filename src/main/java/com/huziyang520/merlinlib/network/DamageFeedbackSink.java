// Where the client side hands incoming damage feedback to.

package com.huziyang520.merlinlib.network;

import java.util.function.Consumer;

public final class DamageFeedbackSink {
    private static volatile Consumer<DamageFeedbackPayload> clientSink;

    private DamageFeedbackSink() {
    }

    public static void setClientSink(Consumer<DamageFeedbackPayload> sink) {
        clientSink = sink;
    }

    public static void handle(DamageFeedbackPayload payload) {
        Consumer<DamageFeedbackPayload> sink = clientSink;
        if (sink != null) {
            sink.accept(payload);
        }
    }
}
