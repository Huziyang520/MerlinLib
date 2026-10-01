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
 * <p>The count a combat effect asks for is proportional to the damage dealt, and the testing weapons can deal
 * the integer limit. One swing at a high health mob therefore produced a packet carrying a count in the
 * hundreds of millions. The client does not merely create a particle per unit of that count: it runs a loop
 * that many times, and the cap in {@code ClientLevel.doAddParticle} only throws away the object the loop is
 * about to build - the loop itself still runs. That is what a freeze lasting minutes on a single hit is.
 *
 * <h2>Why clamp here</h2>
 *
 * <p>{@code sendParticles} is the one door every particle a server sends goes through, and its count parameter
 * is the whole problem, so clamping it covers damage, healing, explosions, blocks and every mod that calls the
 * same method. The count is the only {@code int} argument of every overload, which is why the injection targets
 * it by type and position rather than by name.
 *
 * <p>Clamping to the configured limit is not a visual loss in practice: the limit is what the player asked the
 * client to draw per tick anyway, so the particles that would have been dropped on arrival are simply never
 * created. With the limit switched off this does nothing at all.
 */
@Mixin(ServerLevel.class)
public class MixinServerLevelParticles {

    /**
     * Clamps the count of one particle send.
     *
     * @param count the count the caller asked for
     * @return the count to actually send
     */
    @ModifyVariable(method = "sendParticles", at = @At("HEAD"), argsOnly = true, ordinal = 0)
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
