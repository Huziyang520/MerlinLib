// A player was about to regenerate health from having a full hunger bar.

package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class FoodRegenEvent implements EnchantmentEvent {
    private final ServerPlayer player;
    private final float originalAmount;
    private boolean cancelled;

    public FoodRegenEvent(ServerPlayer player, float originalAmount) {
        this.player = player;
        this.originalAmount = originalAmount;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public float originalAmount() {
        return this.originalAmount;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public LivingEntity getEntity() {
        return this.player;
    }

    @Override
    public ServerLevel getLevel() {
        return (ServerLevel) this.player.level();
    }
}
