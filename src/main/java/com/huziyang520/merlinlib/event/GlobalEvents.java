// The events that do not belong to a single enchantment.

package com.huziyang520.merlinlib.event;

import java.util.ArrayList;
import java.util.List;

public final class GlobalEvents {
    public static final SimpleEvent<LivingEntityTickCallback> LIVING_ENTITY_TICK =
            SimpleEvent.createArrayBacked(LivingEntityTickCallback.class, listeners -> event -> {
                for (LivingEntityTickCallback listener : listeners) {
                    listener.onTick(event);
                }
            });

    public static final SimpleEvent<FoodRegenCallback> FOOD_REGEN =
            SimpleEvent.createArrayBacked(FoodRegenCallback.class, listeners -> event -> {
                for (FoodRegenCallback listener : listeners) {
                    listener.onRegen(event);
                }
            });

    private static volatile boolean livingEntityTickEnabled;

    private GlobalEvents() {
    }

    public static void enableLivingEntityTick() {
        livingEntityTickEnabled = true;
    }

    public static boolean isLivingEntityTickEnabled() {
        return livingEntityTickEnabled;
    }

    public static List<String> describeListeners() {
        List<String> described = new ArrayList<>();
        described.add("living_entity_tick=" + LIVING_ENTITY_TICK.listenerCount());
        described.add("food_regen=" + FOOD_REGEN.listenerCount());
        return described;
    }

    @FunctionalInterface
    public interface LivingEntityTickCallback {
        void onTick(LivingEntityTickEvent event);
    }

    @FunctionalInterface
    public interface FoodRegenCallback {
        void onRegen(FoodRegenEvent event);
    }
}
