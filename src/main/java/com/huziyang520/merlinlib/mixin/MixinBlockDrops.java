// Fires BuiltInEvents#MODIFY_BLOCK_DROPS on the drops of a block a player broke.

package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public class MixinBlockDrops {
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void merlinlib$modifyBlockDrops(BlockState state, ServerLevel level, BlockPos pos,
                                                   BlockEntity blockEntity, Entity breaker, ItemStack tool,
                                                   CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.MODIFY_BLOCK_DROPS)) {
            return;
        }
        if (!(breaker instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>(cir.getReturnValue());
        MutableInt bonusXp = new MutableInt(0);
        ItemStack toolStack = tool;
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.MODIFY_BLOCK_DROPS,
                new BuiltInEvents.ModifyBlockDropsEvent(level, player, pos, state, toolStack, drops, bonusXp),
                player);
        cir.setReturnValue(drops);
        if (bonusXp.intValue() > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), bonusXp.intValue());
        }
    }
}
