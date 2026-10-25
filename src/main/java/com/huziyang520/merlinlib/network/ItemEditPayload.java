// Client to server item edit request from the item editor screen.

package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record ItemEditPayload(ItemStack stack) {
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "item_edit");

    public static void write(ItemEditPayload payload, FriendlyByteBuf buffer) {
        buffer.writeItem(payload.stack);
    }

    public static ItemEditPayload read(FriendlyByteBuf buffer) {
        return new ItemEditPayload(buffer.readItem());
    }
}
