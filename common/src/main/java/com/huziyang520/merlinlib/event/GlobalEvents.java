package com.huziyang520.merlinlib.event;

import java.util.ArrayList;
import java.util.List;

/**
 * The events that do not belong to a single enchantment.
 */
public final class GlobalEvents {

    /** Called for every living entity tick, when {@link #enableLivingEntityTick()} was asked for. */
    public static final SimpleEvent<LivingEntityTickCallback> LIVING_ENTITY_TICK =
            SimpleEvent.createArrayBacked(LivingEntityTickCallback.class, listeners -> event -> {
                for (LivingEntityTickCallback listener : listeners) {
                    listener.onTick(event);
                }
            });

    /** Called before a player regenerates health from a full hunger bar. */
    public static final SimpleEvent<FoodRegenCallback> FOOD_REGEN =
            SimpleEvent.createArrayBacked(FoodRegenCallback.class, listeners -> event -> {
                for (FoodRegenCallback listener : listeners) {
                    listener.onRegen(event);
                }
            });

    /** Whether the per entity tick trigger has been asked for. */
    private static volatile boolean livingEntityTickEnabled;

    private GlobalEvents() {
    }

    /**
     * Asks for the per entity tick trigger to run.
     *
     * <p>Idempotent: calling it from several mods, or several times from one, changes nothing after the first
     * call. It is separate from registering a listener so that a mod can turn the feature on early and add its
     * listener later.
     */
    public static void enableLivingEntityTick() {
        livingEntityTickEnabled = true;
    }

    /** @return {@code true} when the per entity tick trigger should run */
    public static boolean isLivingEntityTickEnabled() {
        return livingEntityTickEnabled;
    }

    /** @return the listeners of {@link #LIVING_ENTITY_TICK}, for diagnostics */
    public static List<String> describeListeners() {
        List<String> described = new ArrayList<>();
        described.add("living_entity_tick=" + LIVING_ENTITY_TICK.listenerCount());
        described.add("food_regen=" + FOOD_REGEN.listenerCount());
        return described;
    }

    /** The listener shape of {@link #LIVING_ENTITY_TICK}. */
    @FunctionalInterface
    public interface LivingEntityTickCallback {

        /**
         * @param event the ticking entity
         */
        void onTick(LivingEntityTickEvent event);
    }

    /** The listener shape of {@link #FOOD_REGEN}. */
    @FunctionalInterface
    public interface FoodRegenCallback {

        /**
         * @param event the regeneration, cancellable
         */
        void onRegen(FoodRegenEvent event);
    }
}
