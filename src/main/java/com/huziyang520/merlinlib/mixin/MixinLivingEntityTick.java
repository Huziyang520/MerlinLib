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
