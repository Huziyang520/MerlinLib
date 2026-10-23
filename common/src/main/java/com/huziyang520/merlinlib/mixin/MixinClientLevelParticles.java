package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.tools.ParticleDiagnostics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Caps how many particles the client draws, so a huge number cannot freeze it.
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
            ParticleDiagnostics.recordDrop("client draw", 1);
            info.cancel();
            return;
        }
        merlinlib$drawn++;
    }
}
