package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.ai.AiRouter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The same veto as {@link MixinAvoidEntityGoal}, for the cat's own avoid goal.
 *
 * <p>The cat does not use {@code AvoidEntityGoal} directly: it has a nested goal of its own, and a
 * nested goal that overrides the two decision methods never reaches the parent's version of them. One
 * file per target is the price of mixin not being able to inject into "a method of that shape
 * somewhere in this hierarchy".
 *
 * <h2>1.20.1: the target string changed, and this is the file most likely to be mis-ported</h2>
 *
 * <p>The 26.3 line names {@code net.minecraft.world.entity.animal.feline.Cat$CatAvoidEntityGoal}.
 * On 1.20.1 the cat has not been moved into the {@code feline} package yet, so that string matches
 * nothing. The correction this port was handed says the right target is
 * {@code net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal}, and the bytecode confirms both
 * halves of it. {@code javap -p -s net.minecraft.world.entity.animal.Cat} shows the field that holds
 * the goal and the package it is spelled with:
 *
 * <pre>
 * private net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal&lt;net.minecraft.world.entity.player.Player&gt; avoidPlayersGoal;
 *   descriptor: Lnet/minecraft/world/entity/animal/Cat$CatAvoidEntityGoal;
 * </pre>
 *
 * <p>and the nested class itself is reached by its {@code Outer$Inner} name:
 *
 * <pre>
 * class net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal&lt;T extends LivingEntity&gt; extends AvoidEntityGoal&lt;T&gt; {
 *   private final net.minecraft.world.entity.animal.Cat cat;
 *     descriptor: Lnet/minecraft/world/entity/animal/Cat;
 *   public boolean canUse();
 *     descriptor: ()Z
 *   public boolean canContinueToUse();
 *     descriptor: ()Z
 * }
 * </pre>
 *
 * <p>Two facts about that output decide how this file is written:
 *
 * <ol>
 *     <li>The nested class is <b>package private</b> - the {@code class} keyword carries no
 *     {@code public}. It is therefore not reachable as a class literal from this package, which is
 *     exactly why the 26.3 source targets it by name and why this port keeps doing so. A
 *     {@code @Mixin(Cat.CatAvoidEntityGoal.class)} would not compile.</li>
 *     <li>It extends {@code AvoidEntityGoal<T>} and <b>overrides both</b> {@code canUse} and
 *     {@code canContinueToUse}. That is the fact that forces a second mixin to exist at all: mixin
 *     injections are not inherited into an override, so the hooks in
 *     {@link MixinAvoidEntityGoal} genuinely do not cover the cat. Had the nested class merely
 *     inherited them, this file would be redundant.</li>
 * </ol>
 *
 * <p>The handlers still read the two fields through {@link AvoidEntityGoalAccessor}. That works
 * because the interface mixin is applied to {@code AvoidEntityGoal}, which this class extends, so a
 * cast to the accessor from inside the subclass resolves - the same reason the 26.3 source does it
 * that way, and the nested class keeps both fields under their original names and modifiers.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatAvoidEntityGoal")
public class MixinCatAvoidEntityGoal {

    /**
     * Refuses to start the cat's avoid goal when a hook says so.
     *
     * @param info the injection callback
     */
    @Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoCatAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    /**
     * Stops the cat's avoid goal when it is already running.
     *
     * @param info the injection callback
     */
    @Inject(method = "canContinueToUse()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void merlinlib$vetoCatContinuedAvoid(CallbackInfoReturnable<Boolean> info) {
        merlinlib$veto(info);
    }

    /**
     * The shared veto, identical to the one in {@link MixinAvoidEntityGoal}.
     *
     * @param info the injection callback
     */
    private void merlinlib$veto(CallbackInfoReturnable<Boolean> info) {
        if (!AiRouter.hasHooks()) {
            return;
        }
        AvoidEntityGoalAccessor self = (AvoidEntityGoalAccessor) this;
        PathfinderMob mob = self.merlinlib$mob();
        LivingEntity avoid = self.merlinlib$toAvoid();
        if (mob != null && avoid != null && AiRouter.vetoesAvoiding(mob, avoid)) {
            info.setReturnValue(false);
        }
    }
}
