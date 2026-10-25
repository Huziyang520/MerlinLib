// The death of one living entity and who caused it.

package com.huziyang520.merlinlib.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public record KillInfo(LivingEntity killer, LivingEntity victim, DamageSource source, boolean projectile) {
}
