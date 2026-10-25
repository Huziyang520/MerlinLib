// The one place where a mod puts an item stack into the world at a death or a block.

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.DropGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class DropApi {
    public static final DropApi INSTANCE = new DropApi();

    private DropApi() {
    }

    public void suppressDrops(LivingEntity victim) {
        DropGate.INSTANCE.suppress(victim);
    }

    public boolean dropsSuppressed(LivingEntity victim) {
        return DropGate.INSTANCE.isSuppressed(victim);
    }

    public void releaseDrops(LivingEntity victim) {
        DropGate.INSTANCE.release(victim);
    }

    public boolean dropAtDeath(ServerLevel level, Vec3 pos, ItemStack stack, LivingEntity victim) {
        if (level == null || pos == null || stack == null || stack.isEmpty()) {
            return false;
        }
        if (victim != null && DropGate.INSTANCE.isSuppressed(victim)) {
            return false;
        }
        Block.popResource(level, blockPos(pos), stack);
        return true;
    }

    private static BlockPos blockPos(Vec3 pos) {
        return new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
    }

    public boolean dropAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (level == null || pos == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Block.popResource(level, pos, stack);
        return true;
    }
}
