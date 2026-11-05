// Reads particle looks from the data pack: data/<namespace>/merlin/particles/<name>.json

package com.huziyang520.merlinlib.particle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleApi {
    public static final ParticleApi INSTANCE = new ParticleApi();
    private static final String FOLDER = "merlin/particles";

    private final Map<Identifier, ParticleProfile> cache = new ConcurrentHashMap<>();

    private ParticleApi() {
    }

    /** Path of the profile, e.g. {@code practical_enchantments:blood_burst}. */
    public static Identifier profileId(String namespace, String name) {
        return Identifier.fromNamespaceAndPath(namespace, FOLDER + "/" + name);
    }

    public ParticleProfile profile(ServerLevel level, Identifier id) {
        return this.cache.computeIfAbsent(id, key -> load(level, key, ParticleProfile.FALLBACK));
    }

    public ParticleProfile profile(ServerLevel level, Identifier id, ParticleProfile fallback) {
        return this.cache.computeIfAbsent(id, key -> load(level, key, fallback));
    }

    public void burst(ServerLevel level, Identifier id, double x, double y, double z, double spread) {
        MerlinParticles.burst(level, profile(level, id), x, y, z, spread);
    }

    public void burst(ServerLevel level, Identifier id, ParticleProfile fallback,
                      double x, double y, double z, double spread) {
        MerlinParticles.burst(level, profile(level, id, fallback), x, y, z, spread);
    }

    /** Drops the cache so an edited data pack is picked up on the next world load. */
    public void clear() {
        this.cache.clear();
    }

    private static ParticleProfile load(ServerLevel level, Identifier id, ParticleProfile fallback) {
        Optional<Resource> resource = level.getServer().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return fallback;
        }
        try (var stream = resource.get().open()) {
            JsonObject json = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return ParticleProfile.fromJson(json, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
