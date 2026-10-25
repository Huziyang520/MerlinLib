// Server to client damage feedback, the library's single source of "what did that swing deal".

package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record DamageFeedbackPayload(float damage, boolean critical, int targetId) {
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "damage_feedback");

    public int displayed() {
        return Math.max(1, Math.round(this.damage));
    }

    public static void write(DamageFeedbackPayload payload, FriendlyByteBuf buffer) {
        buffer.writeFloat(payload.damage);
        buffer.writeBoolean(payload.critical);
        buffer.writeVarInt(payload.targetId);
    }

    public static DamageFeedbackPayload read(FriendlyByteBuf buffer) {
        return new DamageFeedbackPayload(buffer.readFloat(), buffer.readBoolean(), buffer.readVarInt());
    }
}
