package com.huziyang520.merlinlib.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reserved hook for opening the health editor on a sneak right click into thin air.
 *
 * <h2>Why the existing entry points cannot cover this</h2>
 *
 * <p>The health editor already opens from two places: using it on an entity, and sneak using an
 * entity. Both need an entity under the crosshair, so "edit myself while nothing is in front of me"
 * had no path at all - vanilla only calls the use item callback when there is something to use it on.
 *
 * <p>{@code Minecraft.startUseItem} is the method the game runs for the use key, which is why the hook
 * sits there: it respects the player's own key binding, works with the key bound to a mouse button,
 * and runs before vanilla decides what to do with the click.
 *
 * <h2>Why only thin air</h2>
 *
 * <p>The hook stays out of the way unless the crosshair points at nothing. Sneak right clicking a
 * block is how a player opens a chest or places a block, and sneak right clicking an entity already
 * belongs to the entity path; hijacking either would break vanilla behaviour the player relies on.
 *
 * <h2>1.20.1 evidence</h2>
 *
 * <p>Verified with {@code javap -p -s net.minecraft.client.Minecraft}:
 *
 * <pre>
 * private void startUseItem();
 *   descriptor: ()V
 * public net.minecraft.world.phys.HitResult hitResult;
 *   descriptor: Lnet/minecraft/world/phys/HitResult;
 * public net.minecraft.client.gui.screens.Screen screen;
 *   descriptor: Lnet/minecraft/client/gui/screens/Screen;
 * </pre>
 *
 * <p>{@code startUseItem} keeps its name, its no-argument shape and its {@code private} modifier, so
 * the injection at {@code HEAD} needs no descriptor and no change. {@code hitResult} is still a public
 * field on {@code Minecraft}, so the "is the crosshair actually empty" test carries over unchanged.
 *
 * <h2>The one thing that had to change: where the current screen lives</h2>
 *
 * <p>On 26.3 the current screen is read from the HUD object ({@code minecraft.gui.screen()}). On
 * 1.20.1 there is no such accessor and no such field - the screen belongs to {@code Minecraft}
 * itself, as the third line of bytecode above shows. The guard is therefore rewritten to read
 * {@code minecraft.screen} directly. The rule it enforces is unchanged and still necessary: the use
 * key is polled while it is held, so without the guard the editor would reopen the instant it was
 * closed, which reads to the player as flicker.
 *
 * <h2>Why the body is currently commented out</h2>
 *
 * <p>The opening call goes through {@code MerlinScreens} in the {@code tools} package. That package
 * has not been ported in this project yet, so calling it would not compile, and the project rule is
 * that a mixin never references a class outside its assigned files. The class is therefore written as
 * a faithful, compiling shell: the target, the injection point, the cancellation contract and the
 * whole decision tree are in place and identical to 26.3, with only the final "open the editor" call
 * removed and marked. Until {@code tools} lands this mixin cancels nothing, which is the honest
 * behaviour of a hook whose action does not exist yet - it is deliberately not pretending to work.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftUseItem {

    /**
     * Handles the sneak right click into thin air.
     *
     * @param info the injection callback, cancelled when the screen is opened
     */
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$healthEditorOnSneakUse(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isShiftKeyDown()) {
            return;
        }
        // 1.20.1 keeps the current screen on Minecraft itself. See the class Javadoc: this line is the
        // one behavioural difference from the 26.3 source, which asked the HUD instead.
        if (minecraft.screen != null) {
            // A screen is already up. Never stack another one on top: the use key is polled while it is
            // held, so without this the editor would reopen as soon as it was closed.
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            return;
        }
        // TODO: restore the opening call once the tools package is ported. It was, in the 26.3 source:
        //
        //     if (MerlinScreens.openHealthEditor(null, player)) {
        //         info.cancel();
        //     }
        //
        // Every guard above it is in place and evaluates exactly as it does upstream, so restoring the
        // three lines is the whole of the remaining work.
    }
}
