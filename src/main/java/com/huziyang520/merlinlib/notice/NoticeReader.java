// Reads notices out of data packs.

package com.huziyang520.merlinlib.notice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.NoticeMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class NoticeReader {
    private static final String DIRECTORY = "merlinlib/notices";

    private NoticeReader() {
    }

    static List<NoticeEntry> read(MinecraftServer server) {
        ResourceManager resources = server.getResourceManager();
        Map<ResourceLocation, Resource> found = resources.listResources(DIRECTORY,
                id -> id.getPath().endsWith(".json"));
        List<NoticeEntry> entries = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Resource> pair : found.entrySet()) {
            ResourceLocation file = pair.getKey();
            ResourceLocation id = toNoticeId(file);
            if (id == null) {
                continue;
            }
            try (BufferedReader reader = pair.getValue().openAsReader()) {
                NoticeEntry entry = parse(id, JsonParser.parseReader(reader));
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (Exception e) {
                Constants.LOG.warn("[MerlinLib] the join notice {} could not be read from {} and was skipped: {}",
                        id, file, e.getMessage());
            }
        }
        return entries;
    }

    private static ResourceLocation toNoticeId(ResourceLocation file) {
        String path = file.getPath();
        String name = path.substring(DIRECTORY.length() + 1, path.length() - ".json".length());
        if (name.isEmpty() || name.indexOf('/') >= 0) {
            return null;
        }
        return new ResourceLocation(file.getNamespace(), name);
    }

    private static NoticeEntry parse(ResourceLocation id, JsonElement json) {
        if (!json.isJsonObject()) {
            Constants.LOG.warn("[MerlinLib] the join notice {} is not a JSON object and was skipped", id);
            return null;
        }
        JsonObject root = json.getAsJsonObject();
        NoticeMode mode = parseMode(id, string(root, "mode", "every_join"));
        boolean enabledByDefault = bool(root, "enabled_by_default", true);
        List<Component> lines = new ArrayList<>();
        if (root.has("lines") && root.get("lines").isJsonArray()) {
            for (JsonElement line : root.getAsJsonArray("lines")) {
                Component parsed = parseLine(line);
                if (parsed != null) {
                    lines.add(parsed);
                }
            }
        }
        if (lines.isEmpty()) {
            Constants.LOG.warn("[MerlinLib] the join notice {} has no usable line and was skipped", id);
            return null;
        }
        return new NoticeEntry(id, mode, lines, enabledByDefault);
    }

    private static Component parseLine(JsonElement line) {
        if (line.isJsonPrimitive()) {
            return Component.literal(line.getAsString());
        }
        if (!line.isJsonArray()) {
            return null;
        }
        MutableComponent result = Component.empty();
        for (JsonElement segment : line.getAsJsonArray()) {
            if (segment.isJsonPrimitive()) {
                result.append(Component.literal(segment.getAsString()));
                continue;
            }
            if (!segment.isJsonObject()) {
                continue;
            }
            JsonObject object = segment.getAsJsonObject();
            MutableComponent part = Component.literal(string(object, "text", ""));
            Style style = Style.EMPTY;
            if (object.has("color")) {
                TextColor color = parseColor(string(object, "color", ""));
                if (color != null) {
                    style = style.withColor(color);
                }
            }
            if (bool(object, "bold", false)) {
                style = style.withBold(true);
            }
            if (bool(object, "italic", false)) {
                style = style.withItalic(true);
            }
            if (bool(object, "underlined", false)) {
                style = style.withUnderlined(true);
            }
            result.append(part.withStyle(style));
        }
        return result;
    }

    private static final Map<String, Integer> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", 0x000000), Map.entry("dark_blue", 0x0000AA),
            Map.entry("dark_green", 0x00AA00), Map.entry("dark_aqua", 0x00AAAA),
            Map.entry("dark_red", 0xAA0000), Map.entry("dark_purple", 0xAA00AA),
            Map.entry("gold", 0xFFAA00), Map.entry("gray", 0xAAAAAA),
            Map.entry("dark_gray", 0x555555), Map.entry("blue", 0x5555FF),
            Map.entry("green", 0x55FF55), Map.entry("aqua", 0x55FFFF),
            Map.entry("red", 0xFF5555), Map.entry("light_purple", 0xFF55FF),
            Map.entry("yellow", 0xFFFF55), Map.entry("white", 0xFFFFFF));

    private static TextColor parseColor(String text) {
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }
        Integer named = NAMED_COLORS.get(value.toLowerCase(Locale.ROOT));
        if (named != null) {
            return TextColor.fromRgb(named);
        }
        try {
            if (value.startsWith("#")) {
                return TextColor.fromRgb(Integer.parseInt(value.substring(1), 16));
            }
            if (value.startsWith("0x") || value.startsWith("0X")) {
                return TextColor.fromRgb(Integer.parseInt(value.substring(2), 16));
            }
            return TextColor.fromRgb(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static NoticeMode parseMode(ResourceLocation id, String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "once", "once_per_save" -> NoticeMode.ONCE_PER_SAVE;
            case "first_join" -> NoticeMode.FIRST_JOIN;
            default -> NoticeMode.EVERY_JOIN;
        };
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsBoolean() : fallback;
    }
}
