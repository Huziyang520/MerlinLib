package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caps how many particles the client draws, so a huge number cannot freeze it.
 *
 * <h2>The problem this solves</h2>
 *
 * <p>Several parts of the game ask for a particle per point of damage, and the health editor can put a mob's
 * health, and a test weapon's damage, into the thousands. One hit then asks for thousands of particles in a
 * single tick: the client spends the whole frame building particle objects, never finishes it, and the game
 * stops responding - which is what a "freeze on hitting a high health mob" turns out to be.
 *
 * <h2>Why the cap is here and not at the source</h2>
 *
 * <p>{@code doAddParticle} is the one door every particle goes through, on the client, closest to the cost that
 * actually hurts. Dropping a particle there costs nothing; dropping it at the source would mean guessing which
 * of the game's own paths - damage, healing, breeding, explosions, blocks - is the one to limit, and being
 * wrong the moment another one appears.
 *
 * <h2>The budget</h2>
 *
 * <p>The count is kept over a rolling window of one tick: whatever the limit is, it is the number of particles
 * <em>per tick</em>, and the window is only reset when a particle actually arrives, so an idle client does no
 * work at all. The limit lives in {@code client.toml} under {@code [particles]} and can be switched off, in
 * which case vanilla behaviour is restored exactly.
 */
@Mixin(ClientLevel.class)
public class MixinClientLevelParticles {

    /** Length of the counting window, in milliseconds; one tick at the usual twenty per second. */
    private static final long WINDOW_MILLIS = 50L;

    /** When the current window started. */
    @Unique
    private static long merlinlib$windowStart;

    /** How many particles have been let through in the current window. */
    @Unique
    private static int merlinlib$drawn;

    /**
     * Lets a particle through, or drops it when this tick's budget is spent.
     *
     * @param options         the particle and its options
     * @param overrideLimiter vanilla's own reduced-particle setting, unused here
     * @param alwaysShow      whether vanilla marked the particle as always visible, unused here
     * @param x               the x coordinate
     * @param y               the y coordinate
     * @param z               the z coordinate
     * @param xSpeed          the x velocity
     * @param ySpeed          the y velocity
     * @param zSpeed          the z velocity
     * @param info            the injection callback, cancelled to drop the particle
     */
    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    private void merlinlib$capParticles(ParticleOptions options, boolean overrideLimiter, boolean alwaysShow,
                                        double x, double y, double z, double xSpeed, double ySpeed,
                                        double zSpeed, CallbackInfo info) {
        ClientConfig config = ConfigManager.client();
        if (!config.particleLimitEnabled()) {
            return;
        }
        // Wall clock rather than a game clock: the window only has to measure elapsed time between two
        // particle spawns, and the game's own time helper has moved between versions more than once.
        long now = System.currentTimeMillis();
        if (now - merlinlib$windowStart >= WINDOW_MILLIS) {
            merlinlib$windowStart = now;
            merlinlib$drawn = 0;
        }
        if (merlinlib$drawn >= config.particleLimit()) {
            info.cancel();
            return;
        }
        merlinlib$drawn++;
    }
}
