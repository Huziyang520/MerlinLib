// Where the client side hands an incoming "open this screen" request.

package com.huziyang520.merlinlib.network;

import java.util.function.IntConsumer;

public final class ToolOpenSink {
    private static volatile IntConsumer clientSink;

    private ToolOpenSink() {
    }

    public static void setClientSink(IntConsumer sink) {
        clientSink = sink;
    }

    public static void handle(OpenToolPayload payload) {
        IntConsumer sink = clientSink;
        if (sink != null) {
            sink.accept(payload.tool());
        }
    }
}
