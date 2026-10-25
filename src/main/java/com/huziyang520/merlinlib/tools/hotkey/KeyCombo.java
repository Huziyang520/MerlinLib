// One action bound to one or more keys that have to be held together.

package com.huziyang520.merlinlib.tools.hotkey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class KeyCombo {
    private final int[] keys;

    private KeyCombo(int[] keys) {
        this.keys = keys;
    }

    public static KeyCombo of(int... keys) {
        return new KeyCombo(normalise(keys));
    }

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

    public int[] keys() {
        return this.keys.clone();
    }

    public List<Integer> keyList() {
        List<Integer> list = new ArrayList<>(this.keys.length);
        for (int key : this.keys) {
            list.add(key);
        }
        return list;
    }

    public boolean isEmpty() {
        return this.keys.length == 0;
    }

    public int size() {
        return this.keys.length;
    }

    public boolean contains(int key) {
        return Arrays.binarySearch(this.keys, key) >= 0;
    }

    public boolean containsAny(java.util.Set<Integer> pressed) {
        for (int key : this.keys) {
            if (pressed.contains(key)) {
                return true;
            }
        }
        return false;
    }

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
