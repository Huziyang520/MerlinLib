package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
 * <h2>What changed from the 26.3 line</h2>
 *
 * <p>Two changes, and only one of them is about the payload system.
 *
 * <p>First, as with the other two payloads, the {@code CustomPacketPayload}/{@code StreamCodec} pair
 * is gone and is replaced by the explicit {@link #write}/{@link #read} methods that
 * {@link ServerSender#register()} hands to {@code SimpleChannel#registerMessage}.
 *
 * <p>Second, the stack itself. 26.3 wrote it with {@code ItemStack.STREAM_CODEC}, which encodes
 * through the data component system. 1.20.1 predates data components entirely: the equivalent is
 * {@link FriendlyByteBuf#writeItem} and {@link FriendlyByteBuf#readItem}, which write the item's
 * registry id, a count byte and the stack's share tag (the NBT the client is allowed to see). That is
 * the same triple the game uses for every ordinary inventory packet on this version, so it is the
 * transport vanilla itself trusts for an {@code ItemStack}, not an approximation of the 26.3 one.
 *
 * @param stack the edited item, as it should be placed in the player's main hand
 */
public record ItemEditPayload(ItemStack stack) {

    /**
     * The id this payload was known under on the 26.3 line, kept for traceability.
     *
     * <p>{@code SimpleChannel} addresses messages by ordinal, so the id plays no part in the 1.20.1
     * wire format; it is retained so the 26.3 identifier can still be grepped for and so callers have
     * a name to report the payload under.
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
