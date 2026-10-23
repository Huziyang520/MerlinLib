package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client damage feedback, the library's single source of "what did that swing deal".
 */
public record DamageFeedbackPayload(float damage, boolean critical, int targetId) {

    /**
 */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "damage_feedback");

    /** @return the damage rounded for display. */
    public int displayed() {
        return Math.max(1, Math.round(this.damage));
    }

    /**
     * Writes this payload into a buffer.
     *
     * <p>Named as a static method rather than an instance one because that is the shape
     * {@code SimpleChannel#registerMessage} asks for: its encoder is a
     * {@code BiConsumer<MSG, FriendlyByteBuf>}, so the parameter order here is the encoder's order.
     *
     * @param payload the payload to encode
     * @param buffer  the buffer to write into
 */
    public static void write(DamageFeedbackPayload payload, FriendlyByteBuf buffer) {
        buffer.writeFloat(payload.damage);
        buffer.writeBoolean(payload.critical);
        buffer.writeVarInt(payload.targetId);
    }

    /**
     * Reads a payload back out of a buffer.
     *
     * <p>The read order must stay the exact mirror of {@link #write}; a mismatch is not a compile
     * error and does not even throw immediately, it silently shifts every later field of the packet.
     *
     * @param buffer the buffer to read from
     * @return the decoded payload
 */
    public static DamageFeedbackPayload read(FriendlyByteBuf buffer) {
        return new DamageFeedbackPayload(buffer.readFloat(), buffer.readBoolean(), buffer.readVarInt());
    }
}
