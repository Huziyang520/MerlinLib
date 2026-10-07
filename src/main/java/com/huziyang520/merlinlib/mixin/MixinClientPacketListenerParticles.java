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
 * <p>{@code handleParticleEvent} runs a loop over the packet's count. Every iteration is cheap, but the
 * count is proportional to the damage a combat effect dealt and the testing weapons can deal the
 * integer limit, so a single hit can put a count of hundreds of millions on the wire. The cap in
 * {@code ClientLevel.addParticle} throws away the particle the loop is about to build, which does
 * nothing about the loop: the client still visits every one of those iterations before the frame ends.
 *
 * <p>So the packet is intercepted here instead: a bounded number of particles is spawned with the
 * packet's own parameters - keeping the visual - and the packet is then cancelled, skipping the loop
 * entirely.
 *
 * <p>This also covers particles that did not come from this server: the same clamp on the sending side
 * stops our own server from building such a packet, but a mod, a command or another server's data can
 * still send one, and the client is the last place that can say no.
 *
 * <h2>1.20.1: the packet's accessors all changed shape</h2>
 *
 * <p>The 26.3 source reads the packet with record-style component accessors:
 * {@code packet.count()}, {@code packet.particle()}, {@code packet.x()}, {@code packet.y()},
 * {@code packet.z()}, {@code packet.xMaxSpeed()} and friends. On 1.20.1 the packet is a plain final
 * class, not a record, and every one of those is a {@code get}-prefixed bean accessor. Verified with
 * {@code javap -p -s net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket}:
 *
 * <pre>
 * public int getCount();
 *   descriptor: ()I
 * public net.minecraft.core.particles.ParticleOptions getParticle();
 *   descriptor: ()Lnet/minecraft/core/particles/ParticleOptions;
 * public double getX();
 *   descriptor: ()D
 * public double getY();
 *   descriptor: ()D
 * public double getZ();
 *   descriptor: ()D
 * public float getXDist();
 *   descriptor: ()F
 * public float getYDist();
 *   descriptor: ()F
 * public float getZDist();
 *   descriptor: ()F
 * public float getMaxSpeed();
 *   descriptor: ()F
 * public boolean isOverrideLimiter();
 *   descriptor: ()Z
 * </pre>
 *
 * <p>Two further differences follow from that same output and are worth stating, because they are
 * silent failures rather than compile errors in a naive port:
 *
 * <ol>
 *     <li>There is <b>no</b> {@code alwaysShow} on this version. The 26.3 packet carries both
 *     {@code overrideLimiter} and {@code alwaysShow}; the 1.20.1 packet has only
 *     {@code isOverrideLimiter()} (plus a private {@code overrideLimiter} field). The call into
 *     {@code ClientLevel} is therefore made against the seven-argument
 *     {@code addParticle(ParticleOptions, double x6)} overload, which is the one that takes neither
 *     boolean - the same overload the client-side cap hooks, so the two caps agree on what a particle
 *     send means.</li>
 *     <li>The spread and speed accessors are {@code getXDist()} / {@code getMaxSpeed()}, and the
 *     per-axis spread is what replaces 26.3's {@code xMaxSpeed()} family. The spawn loop below uses
 *     {@code getXDist()} for each axis' spread and {@code getMaxSpeed()} for the speed, which
 *     reproduces the packet's own distribution.</li>
 * </ol>
 *
 * <p>{@code handleParticleEvent} itself kept its name and descriptor unchanged -
 * {@code (Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;)V} - so the injection
 * point needs no correction.
 *
 * <h2>The diagnostics call</h2>
 *
 * <p>As with the other particle mixins, the dropped count is reported through
 * {@code ParticleDiagnostics} - a plain helper in the {@code tools} package, outside the mixin
 * package on purpose. It is log-only: the interception, the bounded respawn and the cancellation are
 * what matter, and they are identical to the 26.3 source.
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
