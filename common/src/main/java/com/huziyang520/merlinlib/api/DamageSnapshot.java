// The damage one living entity dealt to another, captured as it landed.

package com.huziyang520.merlinlib.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record DamageSnapshot(LivingEntity attacker, LivingEntity victim, DamageSource source,
                             ItemStack weapon, float amount, boolean projectile) {
    public DamageSnapshot {
        weapon = weapon == null ? ItemStack.EMPTY : weapon;
    }
}
