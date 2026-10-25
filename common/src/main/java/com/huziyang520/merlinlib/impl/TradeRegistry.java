// The villager trades a dependent mod registered, read by the loader bridges.

package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.TradeApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TradeRegistry {
    public static final TradeRegistry INSTANCE = new TradeRegistry();

    private static final int MAX_ENTRIES = 4096;
    private static final Comparator<TradeApi.Trade> ORDER = Comparator
            .comparing(TradeApi.Trade::profession)
            .thenComparingInt(TradeApi.Trade::level);

    private final Map<String, List<TradeApi.Trade>> byProfession = new ConcurrentHashMap<>();

    private TradeRegistry() {
    }

    public void register(TradeApi.Trade trade) {
        if (trade == null || trade.result().isEmpty()) {
            Constants.LOG.warn("[MerlinLib] a villager trade was ignored: it has no result item");
            return;
        }
        String key = professionKey(trade.profession());
        if (key.isEmpty()) {
            Constants.LOG.warn("[MerlinLib] a villager trade was ignored: it names no profession");
            return;
        }
        if (size() >= MAX_ENTRIES) {
            Constants.LOG.warn("[MerlinLib] the villager trade registry is full; {} was ignored", key);
            return;
        }
        byProfession.computeIfAbsent(key, unused -> new CopyOnWriteArrayList<>()).add(trade);
    }

    public List<TradeApi.Trade> all() {
        List<TradeApi.Trade> trades = new ArrayList<>();
        for (List<TradeApi.Trade> entries : this.byProfession.values()) {
            trades.addAll(entries);
        }
        trades.sort(ORDER);
        return List.copyOf(trades);
    }

    public List<TradeApi.Trade> ofProfession(String profession) {
        List<TradeApi.Trade> entries = this.byProfession.get(professionKey(profession));
        if (entries == null) {
            return List.of();
        }
        List<TradeApi.Trade> trades = new ArrayList<>(entries);
        trades.sort(ORDER);
        return List.copyOf(trades);
    }

    public List<TradeApi.Trade> ofProfession(String profession, int level) {
        List<TradeApi.Trade> trades = new ArrayList<>();
        for (TradeApi.Trade trade : ofProfession(profession)) {
            if (level <= 0 || trade.level() == level) {
                trades.add(trade);
            }
        }
        return List.copyOf(trades);
    }

    public List<String> professions() {
        List<String> keys = new ArrayList<>(this.byProfession.keySet());
        keys.sort(Comparator.naturalOrder());
        return List.copyOf(keys);
    }

    public int size() {
        int total = 0;
        for (List<TradeApi.Trade> entries : this.byProfession.values()) {
            total += entries.size();
        }
        return total;
    }

    public void clear() {
        this.byProfession.clear();
    }

    private static String professionKey(String profession) {
        if (profession == null) {
            return "";
        }
        String key = profession.trim().toLowerCase(Locale.ROOT);
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
