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

/**
 * Watches key combinations and runs the action bound to each of them.
 *
 * <h2>How keys are read without a mixin</h2>
 *
 * <p>26.3 removed {@code InputConstants.isKeyDown}, and GLFW is on no compile class path here, so the one
 * usable source of key state is {@link KeyMapping} itself: the vanilla keyboard handler feeds every press
 * and release into every mapping it knows about - through the static map the constructor writes to,
 * registered with a loader or not - so a mapping created on the fly still reports {@link KeyMapping#isDown}
 * and still counts {@link KeyMapping#consumeClick clicks}. That is what this class builds on:
 *
 * <ul>
 *     <li>one lazily created mapping per key code, reused for every combination that mentions it;</li>
 *     <li>clicks are drained once per tick into a "just pressed" set, so a combination can be evaluated
 *     without consuming another action's click;</li>
 *     <li>a binding fires when all of its keys are held and one of them was pressed in this tick.</li>
 * </ul>
 *
 * <p>These mappings are deliberately <b>not</b> registered with the loader: they are an implementation
 * detail, the player binds them inside the macro screen, and leaving them out of the vanilla controls
 * screen keeps it readable.
 *
 * <h2>Overlapping combinations</h2>
 *
 * <p>A binding whose combination is a strict subset of one that already fired in the same tick is
 * suppressed, so {@code LEFT} does not also run when {@code LEFT + A} was the combination the player
 * completed.
 */
public final class HotkeyRegistry {

    /**
     * The highest code that stands for a mouse button.
     *
     * <p>Mouse buttons are numbered 0..7 by the input layer, and GLFW leaves that range free for keyboard
     * keys (the lowest keyboard code is 32), so one integer space covers both without a second lookup and
     * without changing the format macros are stored in.
     */
    public static final int MAX_MOUSE_CODE = 7;

    /** One mapping per key code, created on first use. */
    private static final Map<Integer, KeyMapping> MAPPINGS = new HashMap<>();
    /** Every binding, longest combination first so the specific one wins. */
    private static final List<Binding> BINDINGS = new ArrayList<>();

    private record Binding(KeyCombo combo, Runnable action) {
    }

    private HotkeyRegistry() {
    }

    /**
     * Removes every binding, keeping the mappings.
     *
     * <p>Called before a screen or a configuration reload rebuilds the bindings, so a stale action can
     * never fire.
     */
    public static void clear() {
        BINDINGS.clear();
    }

    /**
     * Binds an action to a combination.
     *
     * @param combo  the keys that must be held together; an empty combination is ignored
     * @param action what to run when the combination is completed
     */
    public static void add(KeyCombo combo, Runnable action) {
        if (combo == null || combo.isEmpty() || action == null) {
            return;
        }
        BINDINGS.add(new Binding(combo, action));
        BINDINGS.sort(Comparator.comparingInt((Binding binding) -> binding.combo().size()).reversed());
    }

    /**
     * Whether every key of a combination is held right now.
     *
     * @param combo the combination
     * @return true when all of its keys are down
     */
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

    /**
     * Runs the actions whose combination was completed during this tick. Call once per client tick.
     */
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

    /**
     * Empties the click counters of every watched key and reports which of them were pressed.
     *
     * @return the key codes pressed since the last tick
     */
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

    /**
     * The mapping watching one key code.
     *
     * <p>Also used by the capture button, which needs the key's translated name.
     *
     * @param keyCode a GLFW key code
     * @return the mapping, created on first use
     */
    public static KeyMapping mapping(int keyCode) {
        return MAPPINGS.computeIfAbsent(keyCode, code -> code <= MAX_MOUSE_CODE
                ? new KeyMapping("key.merlinlib.hotkey.mouse." + code, InputConstants.Type.MOUSE, code,
                        KeyMapping.Category.MISC)
                : new KeyMapping("key.merlinlib.hotkey." + code, code, KeyMapping.Category.MISC));
    }

    /**
     * Releases a key's state.
     *
     * <p>Used after a capture: while the macro screen is open the key was pressed to bind it, and without
     * this the binding would fire the moment the screen closes if the player is still holding it.
     *
     * @param keyCode a GLFW key code
     */
    public static void release(int keyCode) {
        KeyMapping mapping = mapping(keyCode);
        mapping.setDown(false);
        while (mapping.consumeClick()) {
            // drained on purpose: the press belonged to the capture, not to the action
        }
    }

    /**
     * The translated name of a key code, as vanilla labels it in the controls screen.
     *
     * @param keyCode a GLFW key code, or a negative value for "unbound"
     * @return the name to show on a button
     */
    public static String keyName(int keyCode) {
        if (keyCode < 0) {
            return Component.translatable("key.merlinlib.unbound").getString();
        }
        return mapping(keyCode).getTranslatedKeyMessage().getString();
    }

    /**
     * The label of a combination, keys joined the way a player reads them.
     *
     * @param combo the combination
     * @return e.g. {@code 左方向键 + A}, or the unbound text when the combination is empty
     */
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
