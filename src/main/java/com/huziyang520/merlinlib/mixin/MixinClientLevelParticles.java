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
 *
 * <h2>The problem this solves</h2>
 *
 * <p>Several parts of the game ask for a particle per point of damage, and the health editor can put a
 * mob's health, and a test weapon's damage, into the thousands. One hit then asks for thousands of
 * particles in a single tick: the client spends the whole frame building particle objects, never
 * finishes it, and the game stops responding - which is what a "freeze on hitting a high health mob"
 * turns out to be.
 *
 * <h2>Why the cap is here and not at the source</h2>
 *
 * <p>{@code addParticle} is the one door every particle goes through, on the client, closest to the
 * cost that actually hurts. Dropping a particle there costs nothing; dropping it at the source would
 * mean guessing which of the game's own paths - damage, healing, breeding, explosions, blocks - is the
 * one to limit, and being wrong the moment another one appears.
 *
 * <h2>The budget</h2>
 *
 * <p>The count is kept over a rolling window of one tick: whatever the limit is, it is the number of
 * particles <em>per tick</em>, and the window is only reset when a particle actually arrives, so an
 * idle client does no work at all. The limit can be switched off, in which case vanilla behaviour is
 * restored exactly.
 *
 * <h2>1.20.1: the target method was renamed, and there are two overloads</h2>
 *
 * <p>The 26.3 source injects into {@code ClientLevel.doAddParticle}. The correction this port was
 * handed says that method does not exist on 1.20.1 and that the target is
 * {@code addParticle(ParticleOptions, double x6)} and/or {@code addParticle(ParticleOptions, boolean,
 * double x6)}, with a note to verify which. Verified with
 * {@code javap -p -s net.minecraft.client.multiplayer.ClientLevel} - both overloads are present, and
 * there is no {@code doAddParticle}:
 *
 * <pre>
 * public void addParticle(net.minecraft.core.particles.ParticleOptions, double, double, double, double, double, double);
 *   descriptor: (Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V
 * public void addParticle(net.minecraft.core.particles.ParticleOptions, boolean, double, double, double, double, double, double);
 *   descriptor: (Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V
 * </pre>
 *
 * <p>The answer to "which one" is <b>this one only</b> - the seven-argument form. The reasoning is
 * what matters, because injecting into both would be a bug:
 *
 * <ol>
 *     <li>On 26.3, {@code doAddParticle} was the single funnel and the boolean overload called it, so
 *     hooking the funnel covered both entry points exactly once. In 1.20.1 the boolean overload is the
 *     <em>outer</em> method and the seven-argument one without the boolean is what it delegates to
 *     after resolving the vanilla particle setting. Hooking the seven-argument form therefore still
 *     covers both public entry points - and covers them once.</li>
 *     <li>Injecting into both overloads would double-count. A particle arriving through the boolean
 *     overload would be counted by the outer hook and again by the inner one, so the effective limit
 *     would be half of what the configuration says, and the accounting would depend on which entry
 *     point a caller used. Clamping at the single funnel is what makes the budget mean "particles per
 *     tick" rather than "particles per tick per entry point".</li>
 * </ol>
 *
 * <p>This is also the injection point whose absence would be most expensive, which is why
 * {@code require = 1} is explicit here as everywhere else: a silently missing cap is a client that
 * still freezes on a large hit, with nothing in the log to say why.
 *
 * <h2>The diagnostics call</h2>
 *
 * <p>Dropped counts are reported through {@code ParticleDiagnostics}, a plain helper in the
 * {@code tools} package - deliberately outside the mixin package, because a class in the package named
 * by {@code merlinlib.mixins.json} may not be referenced directly from game code. It is log-only; the
 * cap itself is what matters and it is identical to the 26.3 source.
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
     * @param options  the particle and its options
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param z        the z coordinate
     * @param xSpeed   the x velocity
     * @param ySpeed   the y velocity
     * @param zSpeed   the z velocity
     * @param info     the injection callback, cancelled to drop the particle
     */
    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$capParticles(ParticleOptions options, double x, double y, double z,
                                        double xSpeed, double ySpeed, double zSpeed,
                                        CallbackInfo info) {
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
