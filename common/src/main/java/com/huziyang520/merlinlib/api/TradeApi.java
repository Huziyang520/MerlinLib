// The villager trade registry api, reached through MerlinApi#trades().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.impl.TradeRegistry;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public final class TradeApi {
    public static final TradeApi INSTANCE = new TradeApi();

    private TradeApi() {
    }

    public record Trade(String profession, int level, ItemStack cost, ItemStack result,
                        int maxUses, int xp) {
        public Trade {
            profession = profession == null ? "" : profession;
            cost = cost == null ? ItemStack.EMPTY : cost;
            result = result == null ? ItemStack.EMPTY : result;
            maxUses = Math.max(1, maxUses);
            xp = Math.max(0, xp);
        }
    }

    public void register(String profession, int level, ItemStack cost, ItemStack result,
                         int maxUses, int xp) {
        TradeRegistry.INSTANCE.register(new Trade(profession, level, cost, result, maxUses, xp));
    }

    public void register(Trade trade) {
        TradeRegistry.INSTANCE.register(trade);
    }

    public List<Trade> trades() {
        return TradeRegistry.INSTANCE.all();
    }

    public List<Trade> trades(String profession) {
        return TradeRegistry.INSTANCE.ofProfession(profession);
    }

    public List<Trade> trades(String profession, int level) {
        return TradeRegistry.INSTANCE.ofProfession(profession, level);
    }

    public List<String> professions() {
        return TradeRegistry.INSTANCE.professions();
    }

    public int size() {
        return TradeRegistry.INSTANCE.size();
    }

    public void clear() {
        TradeRegistry.INSTANCE.clear();
    }
}
