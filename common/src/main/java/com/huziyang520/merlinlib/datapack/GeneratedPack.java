// In-memory datapack holding the enchantments registered through the MerlinLib api plus the testing weapon tags.

package com.huziyang520.merlinlib.datapack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.tools.TestWeapons;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
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

public final class GeneratedPack implements PackResources {
    private static final int PACK_FORMAT = 121;
    private static final int RESOURCE_FORMAT = 97;

    private static final String PACK_META = "{\"pack\":{\"description\":\"MerlinLib generated content\","
            + "\"pack_format\":" + PACK_FORMAT
            + ",\"supported_formats\":[" + RESOURCE_FORMAT + "," + PACK_FORMAT + "]}}";

    private final PackLocationInfo location;
    private Map<String, byte[]> files;

    public GeneratedPack(PackLocationInfo location) {
        this.location = location;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        byte[] content = this.files().get(key(type, id));
        return content == null ? null : () -> new ByteArrayInputStream(content);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
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
            output.accept(Identifier.fromNamespaceAndPath(namespace, relative), () -> new ByteArrayInputStream(content));
        }
    }

    public static Map<String, byte[]> previewFiles() {
        return build();
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (type != PackType.SERVER_DATA) {
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
    public <T> T getMetadataSection(MetadataSectionType<T> type) throws IOException {
        JsonElement section = JsonParser.parseString(PACK_META).getAsJsonObject().get(type.name());
        if (section == null) {
            return null;
        }
        return type.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, section).result().orElse(null);
    }

    @Override
    public void close() {
    }

    private static String key(PackType type, Identifier id) {
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
        Map<Identifier, EnchantmentDraft> served = ContentManager.current();
        if (served.isEmpty()) {
            Constants.LOG.warn("[MerlinLib] the generated datapack was opened but contains no enchantment at all; "
                    + "no enchantment is registered through the MerlinLib api, or server.toml switched the pack off");
        }
        for (EnchantmentDraft draft : served.values()) {
            files.put(
                    "data/" + draft.id().getNamespace() + "/enchantment/" + draft.id().getPath() + ".json",
                    EnchantmentJsonWriter.toBytes(draft)
            );
        }
        EnchantmentJsonWriter.acquisitionTags(served.values()).forEach((tag, json) -> files.put(
                "data/" + tag.getNamespace() + "/tags/enchantment/" + tag.getPath() + ".json",
                EnchantmentJsonWriter.tagBytes(json)
        ));
        addWeaponTags(files);
        Constants.LOG.info("[MerlinLib] generated datapack opened with {} file(s) for {} enchantment(s): {}",
                files.size(), served.size(), served.keySet());
        return files;
    }

    private static void addWeaponTags(Map<String, byte[]> files) {
        Map<Identifier, List<String>> byTag = new LinkedHashMap<>();
        for (Identifier weapon : TestWeapons.ids()) {
            TestWeapons.tagMembership(weapon).ifPresent(tags -> {
                for (Identifier tag : tags) {
                    byTag.computeIfAbsent(tag, key -> new java.util.ArrayList<>()).add(weapon.toString());
                }
            });
        }
        for (Map.Entry<Identifier, List<String>> entry : byTag.entrySet()) {
            Identifier tag = entry.getKey();
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
