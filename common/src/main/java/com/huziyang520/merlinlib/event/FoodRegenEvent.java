package com.huziyang520.merlinlib.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * A player was about to regenerate health from having a full hunger bar.
 *
 * <p>Cancellable, because the natural shape of the enchantment using it is "you no longer heal on your own, and
 * something else happens instead". Note that this is the <em>food</em> regeneration only: healing from a potion,
 * a beacon or another mod's effect never passes through here, so cancelling it cannot silently break those.
 *
 * @see GlobalEvents#FOOD_REGEN
 */
public final class FoodRegenEvent implements EnchantmentEvent {

    private final ServerPlayer player;
    private final float originalAmount;
    private boolean cancelled;

    /**
     * @param player         the player about to heal
     * @param originalAmount the amount of health that would be restored
     */
    public FoodRegenEvent(ServerPlayer player, float originalAmount) {
        this.player = player;
        this.originalAmount = originalAmount;
    }

    /** @return the player about to heal */
    public ServerPlayer player() {
        return this.player;
    }

    /** @return the amount of health that would be restored without interference */
    public float originalAmount() {
        return this.originalAmount;
    }

    /**
     * Stops the regeneration.
     *
     * @param cancelled {@code true} to stop it; cancelling twice is harmless
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /** @return {@code true} when the regeneration was stopped */
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public LivingEntity getEntity() {
        return this.player;
    }

    @Override
    public ServerLevel getLevel() {
        // A food regeneration only happens on the server side, where the level is a ServerLevel.
        return (ServerLevel) this.player.level();
    }
}
