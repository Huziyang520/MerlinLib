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
 *
 * <h2>Why the static drop method</h2>
 *
 * <p>{@code Block.getDrops} is the one place where the block being broken, the player who broke it,
 * the tool in their hand and the list of drops are all present at once. Hooking there means a
 * dependent mod can add, remove or replace drops, and ask for extra experience, without a second hook
 * for experience orbs: the extra experience is awarded by this mixin, at the block, as the last step.
 *
 * <h2>Only player breaks</h2>
 *
 * <p>The four argument overload without a breaker entity is used by pipes, pistons and other
 * machinery. There is no player to deliver the event to there, so that overload is not hooked at all
 * rather than displacing the drops of a machine onto a player who was not involved.
 *
 * <h2>Why the injection is cancellable</h2>
 *
 * <p>Replacing the returned list means calling {@code setReturnValue}, and Mixin only allows that on a
 * cancellable injection. Without {@code cancellable = true} the call throws inside the drop path
 * itself, which does not merely lose the event: it aborts the break, so the player sees a block that
 * refuses to drop anything, and the exception surfaces as a crash report naming this class. The flag
 * is not a formality.
 *
 * <h2>1.20.1: the second port that had to change shape, and why</h2>
 *
 * <p>The 26.3 line injects into a <b>static</b> method that takes
 * {@code ItemInstance} as its last parameter. On 1.20.1 the method is still static, but the
 * {@code ItemInstance} interface does not exist yet - the parameter is a plain
 * {@code ItemStack}. Verified with {@code javap -p -s net.minecraft.world.level.block.Block}, which
 * shows both overloads and their {@code static} modifier:
 *
 * <pre>
 * public static java.util.List&lt;net.minecraft.world.item.ItemStack&gt; getDrops(
 *         net.minecraft.world.level.block.state.BlockState, net.minecraft.server.level.ServerLevel,
 *         net.minecraft.core.BlockPos, net.minecraft.world.level.block.entity.BlockEntity);
 *   descriptor: (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;
 *
 * public static java.util.List&lt;net.minecraft.world.item.ItemStack&gt; getDrops(
 *         net.minecraft.world.level.block.state.BlockState, net.minecraft.server.level.ServerLevel,
 *         net.minecraft.core.BlockPos, net.minecraft.world.level.block.entity.BlockEntity,
 *         net.minecraft.world.entity.Entity, net.minecraft.world.item.ItemStack);
 *   descriptor: (Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;
 * </pre>
 *
 * <p>Three consequences, all of them forced by the bytecode above rather than chosen:
 *
 * <ol>
 *     <li>The handler is {@code static}. A static target has no {@code this} inside the handler, and
 *     Mixin rejects a non-static handler for a static target. The 26.3 source already had this right,
 *     so the port keeps {@code private static void}.</li>
 *     <li>The last parameter is typed {@code ItemStack}, not {@code ItemInstance}, and the
 *     {@code tool instanceof ItemStack} dance of the 26.3 version collapses to a plain use of the
 *     argument. That pattern existed only because a multi-version API exposed the wider interface;
 *     on 1.20.1 it would be a cast that always succeeds and a branch that is always taken, which is
 *     noise rather than safety. The one case the pattern also covered - a missing or broken tool - is
 *     already handled by vanilla passing {@code ItemStack.EMPTY} into this overload, and an empty
 *     stack is scanned to no enchantments, which is the intended "no tool" answer.</li>
 *     <li>The full descriptor is given in {@code method = "..."} because the class has two
 *     {@code getDrops} overloads. Naming only {@code getDrops} would be ambiguous and would make the
 *     injection depend on Mixin's overload resolution instead of on the bytecode.</li>
 * </ol>
 *
 * <p>The event itself is unchanged: same type, same position at {@code RETURN}, same cancellation and
 * same bonus experience award.
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
