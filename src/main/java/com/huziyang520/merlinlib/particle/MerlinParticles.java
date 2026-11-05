// Spawns the two halves of a particle burst: fine coloured droplets and heavier
// block chunks that fall. Both are vanilla particles, so a data pack only picks
// the colour and the block.

package com.huziyang520.merlinlib.particle;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class MerlinParticles {
    private MerlinParticles() {
    }

    public static ParticleOptions dust(int color, float scale) {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        return new DustParticleOptions(new org.joml.Vector3f(red, green, blue), scale);
    }

    public static void burst(ServerLevel level, ParticleProfile profile,
                             double x, double y, double z, double spread) {
        Block block = BuiltInRegistries.BLOCK.get(profile.chunkBlock());
        if (block == null || block == Blocks.AIR) {
            block = Blocks.REDSTONE_BLOCK;
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()),
                x, y, z, profile.count(), spread, spread * 0.75D, spread, profile.speed());
        level.sendParticles(dust(profile.color(), profile.dustScale()),
                x, y, z, profile.count() * 2, spread * 0.8D, spread * 0.6D, spread * 0.8D, profile.speed() * 0.5D);
    }
}
