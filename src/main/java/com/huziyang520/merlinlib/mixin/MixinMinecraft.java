package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prints the client banner once the game object is fully constructed.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>This is the library's smoke test for the client half of the mixin configuration. The client
 * mixins are not loaded on a dedicated server, so "the mod loaded" is not the same statement as "the
 * client mixins applied"; a line at the tail of the client's own constructor is the cheapest thing
 * that proves the latter, and it names the exact Minecraft version the client half is running
 * against.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>Nothing changed: this is a direct port. Verified with
 * {@code javap -p -s net.minecraft.client.Minecraft}:
 *
 * <pre>
 * public net.minecraft.client.Minecraft(net.minecraft.client.main.GameConfig);
 *   descriptor: (Lnet/minecraft/client/main/GameConfig;)V
 * </pre>
 *
 * <p>The 26.3 source uses the bare {@code method = "<init>"}, and so does this port.
 * {@code <init>} is not a name that can drift between versions, and there is exactly one constructor
 * on this class in the bytecode above, so a name-only match is unambiguous here in a way it is not for
 * an overloaded method such as {@code sendParticles}. The descriptor is still asserted at runtime by
 * {@code require = 1}: if a second constructor were ever added, {@code require = 1} would fail loudly
 * rather than pick one silently.
 *
 * <h2>The one rename, and why it is {@code getName()}</h2>
 *
 * <p>The 26.3 source prints {@code SharedConstants.getCurrentVersion().name()}. On 1.20.1
 * {@code WorldVersion} is not a record, so it has no {@code name()} component accessor - the method is
 * the bean accessor {@code getName()}. Verified with
 * {@code javap -p -s net.minecraft.WorldVersion}, which lists the whole interface:
 *
 * <pre>
 * public interface net.minecraft.WorldVersion {
 *   public abstract java.lang.String getName();
 *     descriptor: ()Ljava/lang/String;
 *   public abstract java.lang.String getId();
 *     descriptor: ()Ljava/lang/String;
 *   public abstract int getProtocolVersion();
 *     descriptor: ()I
 *   public abstract boolean isStable();
 *     descriptor: ()Z
 * }
 * </pre>
 *
 * <p>and with {@code javap -p -s net.minecraft.SharedConstants}, which confirms the static entry point
 * itself kept its name:
 *
 * <pre>
 * public static net.minecraft.WorldVersion getCurrentVersion();
 *   descriptor: ()Lnet/minecraft/WorldVersion;
 * </pre>
 *
 * <p>Note that {@code name()} and {@code getName()} differ by more than case, so a naive port is a
 * compile error rather than a silent substitution - which is how this was caught. This is the only
 * change in this file.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    /**
     * Logs the banner once the client exists.
     *
     * @param info the injection callback
     */
    @Inject(method = "<init>", at = @At("TAIL"), require = 1)
    private void merlinlib$logClientBanner(CallbackInfo info) {
        Constants.LOG.info("This line is printed by a MerlinLib client mixin!");
        Constants.LOG.info("MC Version: {}", SharedConstants.getCurrentVersion().getName());
    }
}
