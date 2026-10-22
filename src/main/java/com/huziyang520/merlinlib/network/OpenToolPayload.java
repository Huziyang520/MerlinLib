package com.huziyang520.merlinlib.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server to client request to open one of the toolkit screens.
 *
 * <h2>Why a packet and not a plain call</h2>
 *
 * <p>The entry points that open these screens are {@code /merlinlib editor}, {@code /merlinlib health} and
 * {@code /merlinlib macro}. A command runs on the server - on a dedicated server, in another process
 * entirely - while a {@code Screen} only exists on the client. The command therefore validates the request
 * (toolkit switch, command permission) on the side that is authoritative and then asks the player's own
 * client to open the screen, which it does after re-checking its local switches.
 *
 * <p>The alternative - a client side command tree - was rejected: the server's tree is merged into the
 * client's by Forge, and a second {@code /merlinlib} root would either be refused as a duplicate or silently
 * replace one of the two.
 *
 * <p>Only an ordinal travels: what to open, nothing else. The client opens the screen for its own player,
 * which is the only thing a screen could sensibly act on.
 */
public record OpenToolPayload(int tool) {

    /** The item editor, for the item in the main hand. */
    public static final int EDITOR = 0;

    /** The health editor, for the player who ran the command. */
    public static final int HEALTH = 1;

    /** The command macro screen. */
    public static final int MACRO = 2;

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
