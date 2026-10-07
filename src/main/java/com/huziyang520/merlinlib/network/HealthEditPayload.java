package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server health edit request from the health editor screen.
 *
 * <p>The server validates the request (toolkit enabled, permission, clamped values) before touching the
 * entity, so the screen is only ever a remote control.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>This was a {@code CustomPacketPayload} with a {@code StreamCodec}. 1.20.1 has neither, so the
 * record is now a plain data carrier with the explicit {@link #write}/{@link #read} pair that
 * {@link ServerSender#register()} gives to {@code SimpleChannel#registerMessage}. The three fields and
 * their order are untouched: three var ints, exactly as the 26.3 codec wrote them.
 *
 * @param targetId the registry id of the entity to edit
 * @param current  the new current health
 * @param max      the new maximum health
 */
public record HealthEditPayload(int targetId, int current, int max) {

    /**
     * The id this payload was known under on the 26.3 line, kept for traceability.
     *
     * <p>A {@code SimpleChannel} addresses a message by its ordinal, not by a resource location, so
     * this id has no part in the wire format on 1.20.1. It is retained so the 26.3 identifier can
     * still be grepped for and so callers have a name to report the payload under.
     */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "health_edit");

    /**
     * Writes this payload into a buffer.
     *
     * @param payload the payload to encode
     * @param buffer  the buffer to write into
     */
    public static void write(HealthEditPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.targetId);
        buffer.writeVarInt(payload.current);
        buffer.writeVarInt(payload.max);
    }

    /**
     * Reads a payload back out of a buffer.
     *
     * @param buffer the buffer to read from
     * @return the decoded payload
     */
    public static HealthEditPayload read(FriendlyByteBuf buffer) {
        return new HealthEditPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }
}
