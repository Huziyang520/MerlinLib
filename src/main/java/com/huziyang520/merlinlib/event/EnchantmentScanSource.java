// 额外附魔来源：让模组把自己容器里的附魔（饰品栏、背包等）接进事件派发。

package com.huziyang520.merlinlib.event;

import java.util.List;
import net.minecraft.world.entity.LivingEntity;

public interface EnchantmentScanSource {
    List<EnchantmentContext> collect(LivingEntity entity, EnchantmentEventType<?> type);
}
