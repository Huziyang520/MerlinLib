package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server health edit request from the health editor screen.
 *
 * <p>The server validates the request (toolkit enabled, permission, clamped values) before touching the
 * entity, so the screen is only ever a remote control.
 *
 * @param targetId the registry id of the entity to edit
 * @param current  the new current health
 * @param max      the new maximum health
 */
public record HealthEditPayload(int targetId, int current, int max) implements CustomPacketPayload {

    public static final Type<HealthEditPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "health_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HealthEditPayload> STREAM_CODEC =
            CustomPacketPayload.codec(HealthEditPayload::write, HealthEditPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(this.targetId);
        buffer.writeVarInt(this.current);
        buffer.writeVarInt(this.max);
    }

    private static HealthEditPayload read(RegistryFriendlyByteBuf buffer) {
        return new HealthEditPayload(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
