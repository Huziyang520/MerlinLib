// Server to client damage feedback, the library's single source of "what did that swing deal".

package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DamageFeedbackPayload(float damage, boolean critical, int targetId) implements CustomPacketPayload {
    public int displayed() {
        return Math.max(1, Math.round(this.damage));
    }

    public static final Type<DamageFeedbackPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "damage_feedback"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageFeedbackPayload> STREAM_CODEC =
            CustomPacketPayload.codec(DamageFeedbackPayload::write, DamageFeedbackPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeFloat(this.damage);
        buffer.writeBoolean(this.critical);
        buffer.writeVarInt(this.targetId);
    }

    private static DamageFeedbackPayload read(RegistryFriendlyByteBuf buffer) {
        return new DamageFeedbackPayload(buffer.readFloat(), buffer.readBoolean(), buffer.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
