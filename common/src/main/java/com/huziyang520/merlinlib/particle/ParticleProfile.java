// A data-pack driven particle look: which block the chunks come from, what colour
// the fine droplets are, how big they are and how many to spawn.

package com.huziyang520.merlinlib.particle;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;

public record ParticleProfile(Identifier chunkBlock, int color, float dustScale, int count, float speed) {
    public static final ParticleProfile FALLBACK = new ParticleProfile(
            Identifier.withDefaultNamespace("redstone_block"), 0x8B0000, 1.6F, 12, 0.8F);

    public static ParticleProfile fromJson(JsonObject json, ParticleProfile fallback) {
        if (json == null) {
            return fallback;
        }
        Identifier block = json.has("chunk_block")
                ? Identifier.tryParse(json.get("chunk_block").getAsString())
                : fallback.chunkBlock();
        int color = json.has("color") ? parseColor(json.get("color").getAsString(), fallback.color()) : fallback.color();
        float scale = json.has("dust_scale") ? json.get("dust_scale").getAsFloat() : fallback.dustScale();
        int count = json.has("count") ? json.get("count").getAsInt() : fallback.count();
        float speed = json.has("speed") ? json.get("speed").getAsFloat() : fallback.speed();
        return new ParticleProfile(block == null ? fallback.chunkBlock() : block, color,
                Math.max(0.1F, scale), Math.max(1, count), Math.max(0.0F, speed));
    }

    private static int parseColor(String raw, int fallback) {
        String text = raw.trim();
        try {
            if (text.startsWith("#")) {
                return Integer.parseInt(text.substring(1), 16) & 0xFFFFFF;
            }
            return (int) Long.parseLong(text) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
