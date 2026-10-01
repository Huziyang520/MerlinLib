package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemInstance;
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
 *
 * <h2>Why the static drop method</h2>
 *
 * <p>{@code Block.getDrops} is the one place where the block being broken, the player who broke it, the tool
 * in their hand and the list of drops are all present at once. Hooking there means a dependent mod can add,
 * remove or replace drops, and ask for extra experience, without a second hook for experience orbs: the extra
 * experience is awarded by this mixin, at the block, as the last step.
 *
 * <h2>Only player breaks</h2>
 *
 * <p>The overload without a breaker entity is used by pipes, pistons and other machinery. There is no player
 * to deliver the event to there, so that overload is not hooked at all rather than displacing the drops of a
 * machine onto a player who was not involved.
 *
 * <h2>Why the injection is cancellable</h2>
 *
 * <p>Replacing the returned list means calling {@code setReturnValue}, and Mixin only allows that on a
 * cancellable injection. Without {@code cancellable = true} the call throws inside the drop path itself, which
 * does not merely lose the event: it aborts the break, so the player sees a block that refuses to drop
 * anything, and the exception surfaces as a crash report naming this class. The flag is not a formality.
 */
@Mixin(Block.class)
public class MixinBlockDrops {

    /**
     * Lets the breaker's enchantments change the drops, then awards any extra experience.
     *
     * @param state      the block that was broken
     * @param level      the level
     * @param pos        where the block was
     * @param blockEntity the block entity, when the block had one
     * @param breaker    the entity that broke it, when there was one
     * @param tool       the tool as an item instance
     * @param cir        the injection callback carrying vanilla's drops
     */
    @Inject(method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void merlinlib$modifyBlockDrops(BlockState state, ServerLevel level, BlockPos pos,
                                                   BlockEntity blockEntity, Entity breaker, ItemInstance tool,
                                                   CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.MODIFY_BLOCK_DROPS)) {
            return;
        }
        if (!(breaker instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>(cir.getReturnValue());
        MutableInt bonusXp = new MutableInt(0);
        // The tool is handed over as a plain stack when it is one, and as an empty stack otherwise: the event
        // promises a stack, and a broken or missing tool is exactly "no tool".
        ItemStack toolStack = tool instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        EnchantmentEventDispatcher.dispatch(BuiltInEvents.MODIFY_BLOCK_DROPS,
                new BuiltInEvents.ModifyBlockDropsEvent(level, player, pos, state, toolStack, drops, bonusXp),
                player);
        cir.setReturnValue(drops);
        if (bonusXp.intValue() > 0) {
            ExperienceOrb.award(level, Vec3.atCenterOf(pos), bonusXp.intValue());
        }
    }
}
