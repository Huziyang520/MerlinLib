package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Client to server item edit request from the item editor screen.
 */
public record ItemEditPayload(ItemStack stack) {

    /**
 */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "item_edit");

    /**
     * Writes this payload into a buffer.
     *
     * @param payload the payload to encode
     * @param buffer  the buffer to write into
 */
    public static void write(ItemEditPayload payload, FriendlyByteBuf buffer) {
        buffer.writeItem(payload.stack);
    }

    /**
     * Reads a payload back out of a buffer.
     *
     * @param buffer the buffer to read from
     * @return the decoded payload
 */
    public static ItemEditPayload read(FriendlyByteBuf buffer) {
        return new ItemEditPayload(buffer.readItem());
    }
}
