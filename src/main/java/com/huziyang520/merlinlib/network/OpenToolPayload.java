package com.huziyang520.merlinlib.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client request to open one of the toolkit screens.
 */
public record OpenToolPayload(int tool) {

    /** The item editor, for the item in the main hand. */
    public static final int EDITOR = 0;

    /** The health editor, for the player who ran the command. */
    public static final int HEALTH = 1;

    /** The command macro screen. */
    public static final int MACRO = 2;

    /**
     * The join notice switches.
     *
     * <p>Not a toolkit screen: it edits a configuration file and touches no item, so it is here for the same
     * reason the other three are - a {@code Screen} exists on the client, and the command runs on the server.
 */
    public static final int NOTICES = 3;

    /**
     * Writes this payload into a buffer.
     *
     * @param payload the payload to encode
     * @param buffer  the buffer to write into
 */
    public static void write(OpenToolPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.tool);
    }

    /**
     * Reads a payload back out of a buffer.
     *
     * @param buffer the buffer to read from
     * @return the decoded payload
 */
    public static OpenToolPayload read(FriendlyByteBuf buffer) {
        return new OpenToolPayload(buffer.readVarInt());
    }
}
