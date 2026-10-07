package com.huziyang520.merlinlib.tools.hotkey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * One action bound to one or more keys that have to be held together.
 *
 * <p>Vanilla's {@code KeyMapping} binds exactly one key, so a combination has to be expressed as a set of
 * keys plus the rule "fire when every key of the set is held and one of them was just pressed". This class
 * is that set: immutable, sorted and de-duplicated, so two combinations that contain the same keys are
 * equal no matter in which order the player pressed them.
 *
 * <p>Ordering matters for firing: a combination that is a subset of a longer one is suppressed while the
 * longer one is held ({@link #isStrictSubsetOf}), which is what lets {@code LEFT} and {@code LEFT + A} coexist
 * without both firing on the same press.
 *
 * <p><b>What changed / 1.20.1 note:</b> nothing. This is a direct port. The 26.3 file was already free of
 * loader and Minecraft imports - it models a combination as plain {@code int} GLFW/HID codes and compares
 * them against a set of pressed codes handed in by the caller - so it ports verbatim. The one thing worth
 * saying is about the codes themselves rather than this class: see {@code MerlinUi}.
 */
public final class KeyCombo {

    /** The keys, sorted ascending and without duplicates. Empty means "not bound". */
    private final int[] keys;

    private KeyCombo(int[] keys) {
        this.keys = keys;
    }

    /**
     * Builds a combination from key codes.
     *
     * @param keys GLFW key codes; negative values are dropped
     * @return the combination, possibly empty
     */
    public static KeyCombo of(int... keys) {
        return new KeyCombo(normalise(keys));
    }

    /**
     * Builds a combination from a list of key codes, as stored in a configuration file.
     *
     * @param keys the key codes
     * @return the combination, or empty when there is no usable key
     */
    public static Optional<KeyCombo> of(List<Integer> keys) {
        if (keys == null) {
            return Optional.empty();
        }
        int[] raw = new int[keys.size()];
        for (int index = 0; index < raw.length; index++) {
            Integer value = keys.get(index);
            raw[index] = value == null ? -1 : value;
        }
        KeyCombo combo = new KeyCombo(normalise(raw));
        return combo.keys.length == 0 ? Optional.empty() : Optional.of(combo);
    }

    private static int[] normalise(int[] keys) {
        return Arrays.stream(keys).filter(key -> key >= 0).distinct().sorted().toArray();
    }

    /** @return a copy of the key codes, ascending */
    public int[] keys() {
        return this.keys.clone();
    }

    /** @return the key codes as a list, ready to be written to a file */
    public List<Integer> keyList() {
        List<Integer> list = new ArrayList<>(this.keys.length);
        for (int key : this.keys) {
            list.add(key);
        }
        return list;
    }

    /** @return true when no key is bound */
    public boolean isEmpty() {
        return this.keys.length == 0;
    }

    /** @return how many keys have to be held together */
    public int size() {
        return this.keys.length;
    }

    /**
     * @param key a GLFW key code
     * @return true when the combination contains that key
     */
    public boolean contains(int key) {
        return Arrays.binarySearch(this.keys, key) >= 0;
    }

    /**
     * @param pressed the keys pressed during this tick
     * @return true when at least one of them belongs to this combination
     */
    public boolean containsAny(java.util.Set<Integer> pressed) {
        for (int key : this.keys) {
            if (pressed.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the key when it is missing, removes it when it is already there.
     *
     * <p>This is what the capture button does: the first press adds a key, pressing the same key again
     * takes it back out, so a combination can be built and corrected without a separate undo.
     *
     * @param key the GLFW key code
     * @return the resulting combination
     */
    public KeyCombo toggle(int key) {
        if (key < 0) {
            return this;
        }
        int[] source = contains(key) ? remove(key) : add(key);
        return new KeyCombo(normalise(source));
    }

    private int[] add(int key) {
        int[] result = Arrays.copyOf(this.keys, this.keys.length + 1);
        result[this.keys.length] = key;
        return result;
    }

    private int[] remove(int key) {
        int[] result = new int[this.keys.length - 1];
        int target = 0;
        for (int existing : this.keys) {
            if (existing != key) {
                result[target++] = existing;
            }
        }
        return result;
    }

    /**
     * @param other another combination
     * @return true when every key of this combination is also in the other, and the other has more keys
     */
    public boolean isStrictSubsetOf(KeyCombo other) {
        return other.keys.length > this.keys.length && other.supersetOf(this);
    }

    private boolean supersetOf(KeyCombo other) {
        for (int key : other.keys) {
            if (!contains(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param other another combination
     * @return true when the two combinations share at least one key, which is what makes them clash
     */
    public boolean intersects(KeyCombo other) {
        for (int key : this.keys) {
            if (other.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeyCombo combo && Arrays.equals(this.keys, combo.keys);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.keys);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.keys);
    }
}
