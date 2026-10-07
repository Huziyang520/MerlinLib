package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.tools.ParticleDiagnostics;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Caps the particle count a single call asks the server to send.
 *
 * <h2>Why the client side cap was not enough</h2>
 *
 * <p>The count a combat effect asks for is proportional to the damage dealt, and the testing weapons
 * can deal the integer limit. One swing at a high health mob therefore produced a packet carrying a
 * count in the hundreds of millions. The client does not merely create a particle per unit of that
 * count: it runs a loop that many times, and the cap on the client only throws away the object the
 * loop is about to build - the loop itself still runs. That is what a freeze lasting minutes on a
 * single hit is.
 *
 * <h2>Why clamp here</h2>
 *
 * <p>{@code sendParticles} is the one door every particle a server sends goes through, and its count
 * parameter is the whole problem, so clamping it covers damage, healing, explosions, blocks and every
 * mod that calls the same method. The count is the only {@code int} argument of the overload, which
 * is why the injection targets it by type and position rather than by name.
 *
 * <p>Clamping to the configured limit is not a visual loss in practice: the limit is what the player
 * asked the client to draw per tick anyway, so the particles that would have been dropped on arrival
 * are simply never created. With the limit switched off this does nothing at all.
 *
 * <h2>1.20.1: naming the overload explicitly, because there are two</h2>
 *
 * <p>The target corrections this port was handed say {@code sendParticles} has two overloads and that
 * a {@code @ModifyVariable} must carry the full descriptor. Verified with
 * {@code javap -p -s net.minecraft.server.level.ServerLevel}, and the correction is right - there are
 * two public overloads, both of which contain exactly one {@code int}:
 *
 * <pre>
 * public &lt;T extends ParticleOptions&gt; int sendParticles(T, double, double, double, int, double, double, double, double);
 *   descriptor: (Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I
 *
 * public &lt;T extends ParticleOptions&gt; boolean sendParticles(net.minecraft.server.level.ServerPlayer, T, boolean, double, double, double, int, double, double, double, double);
 *   descriptor: (Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/particles/ParticleOptions;ZDDDIDDDD)Z
 *
 * private boolean sendParticles(net.minecraft.server.level.ServerPlayer, boolean, double, double, double, net.minecraft.network.protocol.Packet);
 *   descriptor: (Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z
 * </pre>
 *
 * <p>The method name alone would therefore be ambiguous across three entries. Note in particular that
 * the private third one has no {@code int} at all, so a name-only injection could match a method where
 * the intended variable does not exist. The full descriptor of the first overload is given below,
 * where the count is argument index 4 and the only {@code int} - hence {@code ordinal = 0}.
 *
 * <p>The single-player overload is deliberately not hooked. It is the "send this to one player"
 * path, and its count is bounded by whatever the caller already decided is appropriate for one
 * client; clamping both would mean the same particle send could be capped twice at two different
 * points in the same call.
 *
 * <h2>The diagnostics call</h2>
 *
 * <p>The dropped count is reported through {@code ParticleDiagnostics}, a plain helper in the
 * {@code tools} package - outside the mixin package on purpose, because a class in the package named
 * by {@code merlinlib.mixins.json} may not be referenced directly from game code. It is log-only:
 * the clamp is the behaviour that matters, and it is identical to the 26.3 source.
 */
@Mixin(ServerLevel.class)
public class MixinServerLevelParticles {

    /**
     * Clamps the count of one particle send.
     *
     * @param count the count the caller asked for
     * @return the count to actually send
     */
    @ModifyVariable(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1)
    private int merlinlib$capSendCount(int count) {
        if (!ConfigManager.client().particleLimitEnabled()) {
            return count;
        }
        int limit = ConfigManager.client().particleLimit();
        if (count <= limit) {
            return count;
        }
        ParticleDiagnostics.recordDrop("server send", count - limit);
        return limit;
    }
}
