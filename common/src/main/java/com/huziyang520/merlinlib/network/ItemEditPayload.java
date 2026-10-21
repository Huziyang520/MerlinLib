package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server item edit request from the item editor screen.
 *
 * <p>Why the edit travels to the server instead of being applied on the client: the client's copy of the
 * inventory is replaced by the next inventory sync, so an edit made only there is lost. Sending the stack
 * and letting the server apply it to the requesting player's main hand is also what gives the permission
 * switch something to enforce - the server refuses the request outright, so the screen really is only a
 * remote control.
 *
 * @param stack the edited item, as it should be placed in the player's main hand
 */
public record ItemEditPayload(ItemStack stack) implements CustomPacketPayload {

    public static final Type<ItemEditPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "item_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemEditPayload> STREAM_CODEC =
            CustomPacketPayload.codec(ItemEditPayload::write, ItemEditPayload::read);

    private void write(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, this.stack);
    }

    private static ItemEditPayload read(RegistryFriendlyByteBuf buffer) {
        return new ItemEditPayload(ItemStack.STREAM_CODEC.decode(buffer));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
