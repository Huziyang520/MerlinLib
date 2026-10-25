// Watches key combinations and runs the action bound to each of them.

package com.huziyang520.merlinlib.tools.hotkey;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HotkeyRegistry {
    public static final int MAX_MOUSE_CODE = 7;
    private static final String CATEGORY = "key.categories.misc";
    private static final Map<Integer, KeyMapping> MAPPINGS = new HashMap<>();
    private static final List<Binding> BINDINGS = new ArrayList<>();

    private record Binding(KeyCombo combo, Runnable action) {
    }

    private HotkeyRegistry() {
    }

    public static void clear() {
        BINDINGS.clear();
    }

    public static void add(KeyCombo combo, Runnable action) {
        if (combo == null || combo.isEmpty() || action == null) {
            return;
        }
        BINDINGS.add(new Binding(combo, action));
        BINDINGS.sort(Comparator.comparingInt((Binding binding) -> binding.combo().size()).reversed());
    }

    public static boolean isHeld(KeyCombo combo) {
        if (combo == null || combo.isEmpty()) {
            return false;
        }
        for (int key : combo.keys()) {
            if (!mapping(key).isDown()) {
                return false;
            }
        }
        return true;
    }

    public static void tick() {
        if (BINDINGS.isEmpty()) {
            return;
        }
        Set<Integer> justPressed = drainClicks();
        if (justPressed.isEmpty()) {
            return;
        }
        List<KeyCombo> fired = new ArrayList<>();
        for (Binding binding : BINDINGS) {
            KeyCombo combo = binding.combo();
            if (!combo.containsAny(justPressed) || !isHeld(combo)) {
                continue;
            }
            boolean superseded = false;
            for (KeyCombo already : fired) {
                if (combo.isStrictSubsetOf(already)) {
                    superseded = true;
                    break;
                }
            }
            if (superseded) {
                continue;
            }
            fired.add(combo);
            binding.action().run();
        }
    }

    private static Set<Integer> drainClicks() {
        Set<Integer> pressed = new HashSet<>();
        for (Map.Entry<Integer, KeyMapping> entry : MAPPINGS.entrySet()) {
            boolean clicked = false;
            while (entry.getValue().consumeClick()) {
                clicked = true;
            }
            if (clicked) {
                pressed.add(entry.getKey());
            }
        }
        return pressed;
    }

    public static KeyMapping mapping(int keyCode) {
        return MAPPINGS.computeIfAbsent(keyCode, code -> code <= MAX_MOUSE_CODE
                ? new KeyMapping("key.merlinlib.hotkey.mouse." + code, InputConstants.Type.MOUSE, code, CATEGORY)
                : new KeyMapping("key.merlinlib.hotkey." + code, InputConstants.Type.KEYSYM, code, CATEGORY));
    }

    public static void release(int keyCode) {
        KeyMapping mapping = mapping(keyCode);
        mapping.setDown(false);
        while (mapping.consumeClick()) {
        }
    }

    public static String keyName(int keyCode) {
        if (keyCode < 0) {
            return Component.translatable("key.merlinlib.unbound").getString();
        }
        return mapping(keyCode).getTranslatedKeyMessage().getString();
    }

    public static String comboName(KeyCombo combo) {
        if (combo == null || combo.isEmpty()) {
            return Component.translatable("key.merlinlib.unbound").getString();
        }
        StringBuilder builder = new StringBuilder();
        for (int key : combo.keys()) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append(keyName(key));
        }
        return builder.toString();
    }
}
