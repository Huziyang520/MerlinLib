package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.config.ClientConfig;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.tools.ParticleDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops an oversized particle packet before the client walks it.
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListenerParticles {

    /**
     * Consumes a particle packet whose count is above the limit.
     *
     * @param packet the packet being handled
     * @param info   the injection callback, cancelled to skip vanilla's loop
 */
    @Inject(method = "handleParticleEvent(Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$capPacketCount(ClientboundLevelParticlesPacket packet, CallbackInfo info) {
        ClientConfig config = ConfigManager.client();
        if (!config.particleLimitEnabled()) {
            return;
        }
        int count = packet.getCount();
        int limit = config.particleLimit();
        if (count <= limit) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            // Spawn the limited number with the packet's own spread and speed, so a big hit still looks
            // like a big hit - it simply stops at the number of particles this client agreed to draw.
            RandomSource random = level.getRandom();
            for (int index = 0; index < limit; index++) {
                level.addParticle(packet.getParticle(),
                        packet.getX(), packet.getY(), packet.getZ(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.getXDist(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.getYDist(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.getZDist());
            }
        }
        ParticleDiagnostics.recordDrop("packet walk", count - limit);
        info.cancel();
    }
}
