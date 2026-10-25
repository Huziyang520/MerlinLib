// Server to client request to open one of the toolkit screens.

package com.huziyang520.merlinlib.network;

import net.minecraft.network.FriendlyByteBuf;

public record OpenToolPayload(int tool) {
    public static final int EDITOR = 0;
    public static final int HEALTH = 1;
    public static final int MACRO = 2;
    public static final int NOTICES = 3;

    public static void write(OpenToolPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.tool);
    }

    public static OpenToolPayload read(FriendlyByteBuf buffer) {
        return new OpenToolPayload(buffer.readVarInt());
    }
}
