package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client damage feedback, the library's single source of "what did that swing deal".
 *
 * <p>The server computes the real number while resolving a hit - enchantments, the weapon's own bonus
 * (the mace's smash, modded overrides), everything - and hands it to the attacking client. No client
 * side formula can reproduce that: the enchantment pipeline needs a {@code ServerLevel}, which the
 * client does not have.
 *
 * <p>Consumers subscribe through their loader's payload handler; the crosshair overlay is one consumer,
 * not the purpose of this packet.
 *
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>26.3 declared this as a {@code CustomPacketPayload} with a {@code Type} and a
 * {@code StreamCodec} built by {@code CustomPacketPayload.codec(...)}. Neither exists on 1.20.1: the
 * payload system arrives in 1.20.2 and the stream codec abstraction with it. The record is therefore a
 * plain data carrier, and the byte layout that the codec used to describe is expressed as the
 * explicit pair {@link #write}/{@link #read}, which {@link ServerSender#register()} hands to
 * {@code SimpleChannel#registerMessage}.
 *
 * <p>The wire format is unchanged, field for field and in the same order: the encoder wrote a float,
 * a boolean and a var int, and so does this pair. A 26.3 packet and a 1.20.1 packet would be byte
 * identical; the two versions simply disagree about how that byte string is described in code.
 *
 * @param damage   the unclamped swing damage: what the hit would deal to a target with enough health
 * @param critical whether vanilla classified the swing as critical
 * @param targetId the registry id of the entity that was hit, for consumers that colour per target
 */
public record DamageFeedbackPayload(float damage, boolean critical, int targetId) {

    /**
     * The id this payload was known under on the 26.3 line, kept for traceability.
     *
     * <p>It plays no part in the 1.20.1 wire format: a {@code SimpleChannel} identifies a message by
     * its ordinal within one channel, so there is nothing here to name it with. It is retained so a
     * reader arriving from the 26.3 sources can grep for the same string, and so a caller that wants
     * to report or key something by the payload's name has the 26.3 name to use rather than inventing
     * a second one.
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
