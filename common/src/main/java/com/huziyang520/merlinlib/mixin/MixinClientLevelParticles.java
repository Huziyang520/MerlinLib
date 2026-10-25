// Caps how many particles the client draws, so a huge number cannot freeze it.

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

@Mixin(ClientLevel.class)
public class MixinClientLevelParticles {
    private static final long WINDOW_MILLIS = 50L;

    @Unique
    private static long merlinlib$windowStart;

    @Unique
    private static int merlinlib$drawn;

    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    private void merlinlib$capParticles(ParticleOptions options, boolean overrideLimiter, boolean alwaysShow,
                                        double x, double y, double z, double xSpeed, double ySpeed,
                                        double zSpeed, CallbackInfo info) {
        ClientConfig config = ConfigManager.client();
        if (!config.particleLimitEnabled()) {
            return;
        }

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
