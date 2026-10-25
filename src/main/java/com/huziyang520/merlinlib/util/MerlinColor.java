// Colour helpers for potions and effects.

package com.huziyang520.merlinlib.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MerlinColor {
    private static final Map<String, Integer> NAMED = new LinkedHashMap<>();
    public static final int WHITE = rgb(0xFFFFFF);
    public static final int LIGHT_GRAY = rgb(0xC0C0C0);
    public static final int GRAY = rgb(0x808080);
    public static final int DARK_GRAY = rgb(0x404040);
    public static final int BLACK = rgb(0x1D1D1D);
    public static final int RED = rgb(0xFF5555);
    public static final int DARK_RED = rgb(0xAA0000);
    public static final int ORANGE = rgb(0xFFAA00);
    public static final int YELLOW = rgb(0xFFFF55);
    public static final int LIME = rgb(0x55FF55);
    public static final int GREEN = rgb(0x00AA00);
    public static final int CYAN = rgb(0x55FFFF);
    public static final int LIGHT_BLUE = rgb(0x55AAFF);
    public static final int BLUE = rgb(0x5555FF);
    public static final int PURPLE = rgb(0xAA00AA);
    public static final int MAGENTA = rgb(0xFF55FF);
    public static final int PINK = rgb(0xFFAAD4);
    public static final int BROWN = rgb(0x8B5A2B);

    static {
        NAMED.put("white", WHITE);
        NAMED.put("light_gray", LIGHT_GRAY);
        NAMED.put("gray", GRAY);
        NAMED.put("dark_gray", DARK_GRAY);
        NAMED.put("black", BLACK);
        NAMED.put("red", RED);
        NAMED.put("dark_red", DARK_RED);
        NAMED.put("orange", ORANGE);
        NAMED.put("yellow", YELLOW);
        NAMED.put("lime", LIME);
        NAMED.put("green", GREEN);
        NAMED.put("cyan", CYAN);
        NAMED.put("light_blue", LIGHT_BLUE);
        NAMED.put("blue", BLUE);
        NAMED.put("purple", PURPLE);
        NAMED.put("magenta", MAGENTA);
        NAMED.put("pink", PINK);
        NAMED.put("brown", BROWN);
    }

    private MerlinColor() {
    }

    public static int rgb(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    public static Optional<Integer> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String text = value.trim().toLowerCase(Locale.ROOT);
        Integer named = NAMED.get(text);
        if (named != null) {
            return Optional.of(named);
        }
        try {
            if (text.startsWith("#")) {
                return Optional.of(parseHex(text.substring(1)));
            }
            if (text.startsWith("0x")) {
                return Optional.of(parseHex(text.substring(2)));
            }
            if (text.matches("[0-9a-f]{6}|[0-9a-f]{8}")) {
                return Optional.of(parseHex(text));
            }
            if (text.matches("\\d+")) {
                return Optional.of(rgb(Integer.parseInt(text)));
            }
        } catch (NumberFormatException ignored) {
        }
        return Optional.empty();
    }

    private static int parseHex(String hex) {
        long packed = Long.parseLong(hex, 16);
        int value = (int) packed;
        return hex.length() >= 8 ? value : rgb(value);
    }

    public static int[] gradient(int base) {
        return new int[] { brighten(base, 0.28F), base, darken(base, 0.32F) };
    }

    public static int brighten(int color, float amount) {
        return mix(color, WHITE, amount);
    }

    public static int darken(int color, float amount) {
        return mix(color, BLACK, amount);
    }

    public static int mix(int from, int to, float t) {
        float factor = clamp(t, 0.0F, 1.0F);
        int r = channel(from, 16) + Math.round((channel(to, 16) - channel(from, 16)) * factor);
        int g = channel(from, 8) + Math.round((channel(to, 8) - channel(from, 8)) * factor);
        int b = channel(from, 0) + Math.round((channel(to, 0) - channel(from, 0)) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    public static Map<String, Integer> named() {
        return Map.copyOf(NAMED);
    }
}
