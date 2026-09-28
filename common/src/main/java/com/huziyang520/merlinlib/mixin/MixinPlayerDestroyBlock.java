package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fires {@link BuiltInEvents#POST_BLOCK_BREAK} after a player breaks a block.
 *
 * <h2>Why {@code ServerPlayerGameMode} and not {@code Player}</h2>
 *
 * <p>{@code Player} has a {@code destroyBlock} on one loader's view of the game and not on the other's - the
 * two patched versions of the class differ - so a mixin aimed at it applies on one side and refuses to apply
 * on the other. The game mode object exists in both, and is the server's own break path, which is the side
 * where an event that changes the world belongs.
 *
 * <h2>Why the state has to be remembered</h2>
 *
 * <p>The event means "the block is gone and the player did it", and by the time the method returns the block
 * at that position is air - so reading it then would hand a listener air, which is useful to nobody. The state
 * is therefore read on the way in and kept until the way out.
 *
 * <p>A stack rather than one field, because breaking a block can break more blocks: a chain logging
 * enchantment responds to this very event by breaking the neighbours, and a single field would have the inner
 * break overwrite the outer one's state. Nested breaks pop in the order they were pushed.
 *
 * <h2>Only successful breaks</h2>
 *
 * <p>The method returns whether the block was actually removed. A protected block, a wrong tool or a cancelled
 * break returns without one, and firing the event for those would be a lie the listener could not detect.
 */
@Mixin(ServerPlayerGameMode.class)
public class MixinPlayerDestroyBlock {

    /** The player this game mode belongs to. */
    @Shadow
    protected ServerPlayer player;

    /** The states of the breaks in progress on the server thread, innermost last. */
    @Unique
    private final Deque<BlockState> merlinlib$breakingStates = new ArrayDeque<>();

    /**
     * Remembers what is about to be broken.
     *
     * @param pos the block position
     * @param cir the injection callback
     */
    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void merlinlib$captureState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_BLOCK_BREAK)) {
            return;
        }
        this.merlinlib$breakingStates.push(this.player.level().getBlockState(pos));
    }

    /**
     * Dispatches the event when the block was really removed.
     *
     * @param pos the block position
     * @param cir the injection callback carrying whether the break succeeded
     */
    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void merlinlib$blockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_BLOCK_BREAK)) {
            return;
        }
        BlockState state = this.merlinlib$breakingStates.poll();
        if (state == null || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!(this.player.level() instanceof ServerLevel level)) {
            return;
        }
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_BLOCK_BREAK,
                new BuiltInEvents.PostBlockBreakEvent(level, this.player, pos, state,
                        this.player.getItemBySlot(EquipmentSlot.MAINHAND)),
                this.player);
    }
}
