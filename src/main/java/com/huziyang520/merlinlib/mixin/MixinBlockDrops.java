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

/**
 * Fires {@link BuiltInEvents#MODIFY_BLOCK_DROPS} on the drops of a block a player broke.
 */
@Mixin(Block.class)
public class MixinBlockDrops {

    /**
     * Lets the breaker's enchantments change the drops, then awards any extra experience.
     *
     * @param state       the block that was broken
     * @param level       the level
     * @param pos         where the block was
     * @param blockEntity the block entity, when the block had one
     * @param breaker     the entity that broke it, when there was one
     * @param tool        the tool that broke it, empty when there was none
     * @param cir         the injection callback carrying vanilla's drops
 */
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true, require = 1)
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
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.MODIFY_BLOCK_DROPS,
                new BuiltInEvents.ModifyBlockDropsEvent(level, player, pos, state, tool, drops, bonusXp),
                player);
        cir.setReturnValue(drops);
        if (bonusXp.intValue() > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), bonusXp.intValue());
        }
    }
}
