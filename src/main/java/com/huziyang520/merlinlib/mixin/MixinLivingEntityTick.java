package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.event.BuiltInEvents;
import com.huziyang520.merlinlib.event.EnchantmentEventDispatcher;
import com.huziyang520.merlinlib.event.GlobalEvents;
import com.huziyang520.merlinlib.event.LivingEntityTickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BuiltInEvents#ENTITY_TICK} and {@link GlobalEvents#LIVING_ENTITY_TICK}.
 *
 * <h2>Why this is the most carefully gated trigger in the library</h2>
 *
 * <p>Every living entity ticks twenty times a second, on both sides. A hook here that does any work by
 * default would be the single most expensive thing the library adds, so the method checks, in this
 * order:
 *
 * <ol>
 *     <li>the per entity tick type's mask, which is one bit test when nobody listens;</li>
 *     <li>the global switch, which a dependent mod turns on with
 *     {@link GlobalEvents#enableLivingEntityTick()} and which nothing turns on by itself;</li>
 *     <li>the side - the client's tick is not where server effects belong.</li>
 * </ol>
 *
 * <p>Only when one of the two is live does the method build an event. A server with neither pays a bit
 * test per entity per tick and nothing more.
 *
 * <h2>1.20.1 evidence and what changed from 26.3</h2>
 *
 * <p>The injection point is unchanged; only the order of the internal checks differs, and that is a
 * cosmetic tidy rather than a behavioural port. Verified with
 * {@code javap -p -s net.minecraft.world.entity.LivingEntity}:
 *
 * <pre>
 * public void tick();
 *   descriptor: ()V
 * </pre>
 *
 * <p>The descriptor matches the 26.3 line and the callback keeps the {@code void} shape.
 * {@code tickCount} is a public field on {@code Entity} on this version as it was on 26.3, so the
 * event can still report it.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityTick {

    /**
     * Dispatches the tick events.
     *
     * @param info the injection callback
     */
    @Inject(method = "tick()V", at = @At("HEAD"), require = 1)
    private void merlinlib$tick(CallbackInfo info) {
        boolean perEnchantment = EnchantmentEventDispatcher.hasCallbacks(BuiltInEvents.ENTITY_TICK);
        boolean global = GlobalEvents.isLivingEntityTickEnabled();
        if (!perEnchantment && !global) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (perEnchantment) {
            EnchantmentEventDispatcher.dispatch(BuiltInEvents.ENTITY_TICK,
                    new BuiltInEvents.EntityTickEvent(level, entity, entity.tickCount), entity);
        }
        if (global) {
            GlobalEvents.LIVING_ENTITY_TICK.invoker()
                    .onTick(new LivingEntityTickEvent(level, entity, entity.tickCount));
        }
    }
}
