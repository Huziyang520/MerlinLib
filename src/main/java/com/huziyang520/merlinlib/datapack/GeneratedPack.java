package com.huziyang520.merlinlib.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.tools.TestWeapons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory datapack holding everything MerlinLib derives from the configuration files.
 *
 * <p>The content is built lazily on the first read and cached for the lifetime of this instance. A new
 * instance is created for every {@code Pack#open()} call, which is exactly what happens on startup and
 * on every {@code /reload}, so the pack always serves the newest configuration without any reload
 * ordering requirement.
 */
public final class GeneratedPack implements PackResources {

    /** Datapack format of the target version (1.21.1 = 48). */
    private static final int PACK_FORMAT = 48;

    /**
     * Resource format of the same version (1.21.1 = 34).
     *
     * <p>The same pack is asked for its metadata once per pack type, and a pack that answers with the data
     * format only looks like a pack from the future on the resource side. Declaring the range covers both
     * readings while the pack itself stays data only.
     */
    private static final int RESOURCE_FORMAT = 34;

    private static final String PACK_META = "{\"pack\":{\"description\":\"MerlinLib generated content\","
            + "\"pack_format\":" + PACK_FORMAT
            + ",\"supported_formats\":[" + RESOURCE_FORMAT + "," + PACK_FORMAT + "]}}";

    private final PackLocationInfo location;
    private Map<String, byte[]> files;

    public GeneratedPack(PackLocationInfo location) {
        this.location = location;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation id) {
        byte[] content = this.files().get(key(type, id));
        return content == null ? null : () -> new ByteArrayInputStream(content);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        // The ids handed to the output MUST be "<namespace>:<path from the pack root>" including the
        // file extension, e.g. "merlinlib:enchantment/unbreakable.json". The registry loaders cut the
        // requested directory and the ".json" suffix off that path to derive the element id; emitting a
        // bare "merlinlib:unbreakable" makes every element invisible.
        String namespacePrefix = type.getDirectory() + "/" + namespace + "/";
        String requestedPrefix = path + "/";
        for (Map.Entry<String, byte[]> entry : this.files().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(namespacePrefix)) {
                continue;
            }
            String relative = key.substring(namespacePrefix.length());
            if (!relative.startsWith(requestedPrefix)) {
                continue;
            }
            byte[] content = entry.getValue();
            output.accept(ResourceLocation.fromNamespaceAndPath(namespace, relative), () -> new ByteArrayInputStream(content));
        }
    }

    /**
     * @return the files this pack would currently contain, keyed by their position from the pack root.
     *         Used by the diagnostics command to show what the pack is about to serve.
     */
    public static Map<String, byte[]> previewFiles() {
        return build();
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) {
            // The generated content is data only; claiming client namespaces would make the pack show up
            // as an empty resource pack.
            return Set.of();
        }
        Set<String> namespaces = new LinkedHashSet<>();
        for (String key : this.files().keySet()) {
            String[] parts = key.split("/", 3);
            if (parts.length >= 2) {
                namespaces.add(parts[1]);
            }
        }
        return namespaces;
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        if (path.length == 1 && PackResources.PACK_META.equals(path[0])) {
            return () -> new ByteArrayInputStream(PACK_META.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
        // 1.21.1 takes the serializer itself (a getMetadataSectionName()/fromJson pair) rather than the
        // 26.x MetadataSectionType with its codec.
        JsonElement section = JsonParser.parseString(PACK_META).getAsJsonObject()
                .get(serializer.getMetadataSectionName());
        if (section == null || !section.isJsonObject()) {
            return null;
        }
        return serializer.fromJson(section.getAsJsonObject());
    }

    @Override
    public void close() {
        // Nothing to release: the pack is a plain in-memory byte map.
    }

    private static String key(PackType type, ResourceLocation id) {
        return type.getDirectory() + "/" + id.getNamespace() + "/" + id.getPath();
    }

    private Map<String, byte[]> files() {
        Map<String, byte[]> local = this.files;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (this.files == null) {
                this.files = build();
            }
            return this.files;
        }
    }

    private static Map<String, byte[]> build() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        var snapshot = ContentManager.current();
        if (snapshot.enchantments().isEmpty()) {
            Constants.LOG.warn("[MerlinLib] the generated datapack was opened but contains no enchantment at all; "
                    + "check config/MerlinLib/enchantments.json and the 'content reloaded' log line above");
        }
        for (EnchantmentDraft draft : snapshot.enchantments().values()) {
            files.put(
                    "data/" + draft.id().getNamespace() + "/enchantment/" + draft.id().getPath() + ".json",
                    EnchantmentJsonWriter.toBytes(draft)
            );
        }
        EnchantmentJsonWriter.acquisitionTags(snapshot.enchantments().values()).forEach((tag, json) -> files.put(
                "data/" + tag.getNamespace() + "/tags/enchantment/" + tag.getPath() + ".json",
                EnchantmentJsonWriter.tagBytes(json)
        ));
        addWeaponTags(files);
        Constants.LOG.info("[MerlinLib] generated datapack opened with {} file(s) for {} enchantment(s): {}",
                files.size(), snapshot.enchantments().size(), snapshot.enchantments().keySet());
        return files;
    }

    /**
     * Adds the item tag files that make the testing weapons real weapons.
     *
     * <p>Enchantability is granted through tags of tags, so without these entries the enchanting table
     * and the anvil treat the weapons as unknown item categories and refuse every enchantment. The
     * vanilla tags are extended, never replaced: the file is written with {@code "replace": false}, so
     * vanilla's own values stay intact.
     *
     * @param files the file map being built, modified in place
     */
    private static void addWeaponTags(Map<String, byte[]> files) {
        Map<ResourceLocation, List<String>> byTag = new LinkedHashMap<>();
        for (ResourceLocation weapon : TestWeapons.ids()) {
            TestWeapons.tagMembership(weapon).ifPresent(tags -> {
                for (ResourceLocation tag : tags) {
                    byTag.computeIfAbsent(tag, key -> new java.util.ArrayList<>()).add(weapon.toString());
                }
            });
        }
        for (Map.Entry<ResourceLocation, List<String>> entry : byTag.entrySet()) {
            ResourceLocation tag = entry.getKey();
            files.put(
                    "data/" + tag.getNamespace() + "/tags/item/" + tag.getPath() + ".json",
                    EnchantmentJsonWriter.tagBytes(values(entry.getValue()))
            );
        }
        Constants.LOG.info("[MerlinLib] generated datapack adds the testing weapons to {} vanilla item tag(s)",
                byTag.size());
    }

    private static com.google.gson.JsonObject values(List<String> ids) {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        ids.forEach(array::add);
        root.addProperty("replace", false);
        root.add("values", array);
        return root;
    }
}
