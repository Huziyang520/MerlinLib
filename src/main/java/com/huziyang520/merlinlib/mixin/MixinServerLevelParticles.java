package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.tools.ParticleDiagnostics;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Caps the particle count a single call asks the server to send.
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
