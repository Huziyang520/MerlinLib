// 通用拼音搜索：中文按全拼、首字母和原文三种方式匹配，供各模组的搜索框复用。

package com.huziyang520.merlinlib.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SearchApi {
    private static volatile Map<Character, String> table;

    private SearchApi() {
    }

    public static boolean matches(String text, String query) {
        if (text == null || query == null) {
            return false;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return true;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains(needle)) {
            return true;
        }
        String full = pinyin(text);
        if (full.contains(needle)) {
            return true;
        }
        return initials(text).contains(needle);
    }

    public static String pinyin(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            String syllable = table().get(c);
            if (syllable != null) {
                out.append(syllable);
            } else if (c > 0x2E80) {
                continue;
            } else if (!Character.isWhitespace(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    public static String initials(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            String syllable = table().get(c);
            if (syllable != null) {
                out.append(syllable.charAt(0));
            } else if (Character.isLetterOrDigit(c)) {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    private static Map<Character, String> table() {
        Map<Character, String> loaded = table;
        if (loaded != null) {
            return loaded;
        }
        Map<Character, String> map = new HashMap<>();
        try (InputStream stream = SearchApi.class.getResourceAsStream("/merlinlib/pinyin.txt")) {
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int space = line.indexOf(' ');
                        if (space > 0) {
                            map.put(line.charAt(0), line.substring(space + 1).trim());
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // An empty table simply falls back to plain substring matching.
        }
        table = map;
        return map;
    }
}
