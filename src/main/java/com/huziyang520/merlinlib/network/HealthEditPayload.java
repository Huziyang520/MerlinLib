package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server health edit request from the health editor screen.
 */
public record HealthEditPayload(int targetId, int current, int max) {

    /**
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
