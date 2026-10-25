// Client to server health edit request from the health editor screen.

package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HealthEditPayload(int targetId, int current, int max) {
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "health_edit");

    public static void write(HealthEditPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.targetId);
        buffer.writeVarInt(payload.current);
        buffer.writeVarInt(payload.max);
    }

    public static HealthEditPayload read(FriendlyByteBuf buffer) {
        return new HealthEditPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }
}
