package com.huziyang520.merlinlib.network;

import java.util.function.Consumer;

/**
 * Where the client side hands incoming damage feedback to.
 *
 * <p>The payload handler is registered on both logical sides - a {@code SimpleChannel} is symmetric by
 * construction, there is no per side handler registration on 1.20.1 - but the consumer is client only
 * code. This indirection keeps every class that the dedicated server loads free of client references:
 * the sink is {@code null} on a server, and the handler call is a no-op there.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>Nothing about the sink itself; it is a direct port. What changed is the side of the call.
 *
 * <p>On 26.3 the clientbound <em>handler</em> was registered separately from the codec
 * ({@code RegisterClientPayloadHandlersEvent}), which is why the 26.3 entrypoint could leave the
 * server side handler out and avoid a duplicate registration. On 1.20.1 the handler is part of
 * {@code SimpleChannel#registerMessage} and the channel is built once, for both sides, during common
 * setup. There is only one handler, so it must be safe to call on a server - and this class is what
 * makes it safe, exactly as it did on 26.3.
 *
 * @see ServerSender#register()
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
