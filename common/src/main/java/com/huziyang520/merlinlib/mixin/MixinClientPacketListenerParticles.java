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
 *
 * <h2>The cost is the loop, not the particle</h2>
 *
 * <p>{@code handleParticleEvent} runs {@code for (int i = 0; i < packet.count(); i++)}. Every iteration is
 * cheap, but the count is proportional to the damage a combat effect dealt and the testing weapons can deal
 * the integer limit, so a single hit can put a count of hundreds of millions on the wire. The cap in
 * {@code ClientLevel.doAddParticle} throws away the particle the loop is about to build, which does nothing
 * about the loop: the client still visits every one of those iterations before the frame ends.
 *
 * <p>So the packet is intercepted here instead: a bounded number of particles is spawned with the packet's own
 * parameters - keeping the visual - and the packet is then cancelled, skipping the loop entirely.
 *
 * <p>This also covers particles that did not come from this server: the same clamp on the sending side stops
 * our own server from building such a packet, but a mod, a command or another server's data can still send
 * one, and the client is the last place that can say no.
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListenerParticles {

    /**
     * Consumes a particle packet whose count is above the limit.
     *
     * @param packet the packet being handled
     * @param info   the injection callback, cancelled to skip vanilla's loop
     */
    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true)
    private void merlinlib$capPacketCount(ClientboundLevelParticlesPacket packet, CallbackInfo info) {
        ClientConfig config = ConfigManager.client();
        if (!config.particleLimitEnabled()) {
            return;
        }
        int count = packet.count();
        int limit = config.particleLimit();
        if (count <= limit) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            // Spawn the limited number with the packet's own spread and speed, so a big hit still looks like a
            // big hit - it simply stops at the number of particles this client agreed to draw.
            RandomSource random = level.getRandom();
            for (int index = 0; index < limit; index++) {
                level.addParticle(packet.particle(), packet.overrideLimiter(), packet.alwaysShow(),
                        packet.x(), packet.y(), packet.z(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.xMaxSpeed(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.yMaxSpeed(),
                        (random.nextDouble() - 0.5D) * 2.0D * packet.zMaxSpeed());
            }
        }
        ParticleDiagnostics.recordDrop("packet walk", count - limit);
        info.cancel();
    }
}
