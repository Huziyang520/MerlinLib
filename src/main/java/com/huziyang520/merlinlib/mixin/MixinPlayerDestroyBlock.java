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
 * <p>The two loaders view {@code Player} differently and one of them does not have a
 * {@code destroyBlock} on it at all, so a mixin aimed there applies on one side and refuses to apply
 * on the other. The game mode object is present regardless, and is the server's own break path, which
 * is the side where an event that changes the world belongs.
 *
 * <h2>Why the state has to be remembered</h2>
 *
 * <p>The event means "the block is gone and the player did it", and by the time the method returns the
 * block at that position is air - so reading it then would hand a listener air, which is useful to
 * nobody. The state is therefore read on the way in and kept until the way out.
 *
 * <p>A stack rather than one field, because breaking a block can break more blocks: a chain logging
 * enchantment responds to this very event by breaking the neighbours, and a single field would have
 * the inner break overwrite the outer one's state. Nested breaks pop in the order they were pushed.
 *
 * <h2>Only successful breaks</h2>
 *
 * <p>The method returns whether the block was actually removed. A protected block, a wrong tool or a
 * cancelled break returns without one, and firing the event for those would be a lie the listener
 * could not detect.
 *
 * <h2>1.20.1: the injection points are unchanged, the player lookup had to change</h2>
 *
 * <p>Verified with {@code javap -p -s net.minecraft.server.level.ServerPlayerGameMode}:
 *
 * <pre>
 * public boolean destroyBlock(net.minecraft.core.BlockPos);
 *   descriptor: (Lnet/minecraft/core/BlockPos;)Z
 * </pre>
 *
 * <p>The method takes a single {@code BlockPos} and returns {@code boolean}, so both injections keep
 * the {@code CallbackInfoReturnable<Boolean>} shape and the {@code Boolean.TRUE.equals} check on the
 * return value - direct ports.
 *
 * <p>The <b>player lookup</b> is what changed. The first port read the player through a
 * {@code @Shadow ServerPlayer player} field, and opening a world failed with
 *
 * <pre>
 * InvalidMixinException @Shadow field player was not located in the target class
 *   net.minecraft.server.level.ServerPlayerGameMode. Using refmap merlinlib.refmap.json
 * </pre>
 *
 * <p>The target field exists and is named exactly as the shadow wrote it - {@code javap -p -s} shows
 * {@code protected final ServerPlayer player} - and the build's TSRG table maps it to {@code f_9245_}.
 * What the runtime consults first is the generated refmap (the file named in the message), and a
 * {@code @Shadow} member does not get an entry written into this project's refmap on this line. The
 * player is therefore read through {@link ServerPlayerGameModeAccessor}, an interface mixin whose
 * reference does make it into the refmap - the same route {@link AvoidEntityGoalAccessor} takes.
 */
@Mixin(ServerPlayerGameMode.class)
public class MixinPlayerDestroyBlock {

    /** The states of the breaks in progress on the server thread, innermost last. */
    @Unique
    private final Deque<BlockState> merlinlib$breakingStates = new ArrayDeque<>();

    /**
     * Remembers what is about to be broken.
     *
     * @param pos the block position
     * @param cir the injection callback
     */
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), require = 1)
    private void merlinlib$captureState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_BLOCK_BREAK)) {
            return;
        }
        ServerPlayerGameModeAccessor self = (ServerPlayerGameModeAccessor) this;
        this.merlinlib$breakingStates.push(self.merlinlib$player().level().getBlockState(pos));
    }

    /**
     * Dispatches the event when the block was really removed.
     *
     * @param pos the block position
     * @param cir the injection callback carrying whether the break succeeded
     */
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"), require = 1)
    private void merlinlib$blockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_BLOCK_BREAK)) {
            return;
        }
        BlockState state = this.merlinlib$breakingStates.poll();
        if (state == null || !Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        ServerPlayerGameModeAccessor self = (ServerPlayerGameModeAccessor) this;
        ServerPlayer player = self.merlinlib$player();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.POST_BLOCK_BREAK,
                new BuiltInEvents.PostBlockBreakEvent(level, player, pos, state,
                        player.getItemBySlot(EquipmentSlot.MAINHAND)),
                player);
    }
}
