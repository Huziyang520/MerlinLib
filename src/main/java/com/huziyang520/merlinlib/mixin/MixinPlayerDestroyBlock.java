// Fires BuiltInEvents#POST_BLOCK_BREAK after a player breaks a block.

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

@Mixin(ServerPlayerGameMode.class)
public class MixinPlayerDestroyBlock {
    @Shadow
    protected ServerPlayer player;

    @Unique
    private final Deque<BlockState> merlinlib$breakingStates = new ArrayDeque<>();

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void merlinlib$captureState(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.POST_BLOCK_BREAK)) {
            return;
        }
        this.merlinlib$breakingStates.push(this.player.level().getBlockState(pos));
    }

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
