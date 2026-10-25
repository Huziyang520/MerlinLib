// Client local command macros: named command lists bound to a combination of keys, stored next to the other MerlinLib ...

package com.huziyang520.merlinlib.tools.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.tools.hotkey.HotkeyRegistry;
import com.huziyang520.merlinlib.tools.hotkey.KeyCombo;
import net.minecraft.client.Minecraft;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class MacroStorage {
    public record Macro(String name, List<Integer> keys, List<String> commands) {
        public KeyCombo combo() {
            return KeyCombo.of(keys).orElseGet(KeyCombo::of);
        }

        public Macro withKeys(List<Integer> newKeys) {
            return new Macro(this.name, List.copyOf(newKeys), this.commands);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "macros.json");
    private static final List<Macro> MACROS = new ArrayList<>();

    private MacroStorage() {
    }

    public static List<Macro> macros() {
        return List.copyOf(MACROS);
    }

    public static void replaceAll(List<Macro> macros) {
        MACROS.clear();
        MACROS.addAll(macros);
        rebind();
    }

    public static void add() {
        MACROS.add(new Macro("Macro " + (MACROS.size() + 1), List.of(), List.of()));
        rebind();
    }

    public static void remove(int index) {
        if (index >= 0 && index < MACROS.size()) {
            MACROS.remove(index);
            rebind();
        }
    }

    public static void save() {
        List<StoredMacro> stored = new ArrayList<>(MACROS.size());
        for (Macro macro : MACROS) {
            StoredMacro value = new StoredMacro();
            value.name = macro.name();
            value.keys = macro.keys();
            value.commands = macro.commands();
            stored.add(value);
        }
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(stored), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not write {}: {}", FILE, exception.getMessage());
        }
    }

    public static void load() {
        MACROS.clear();
        if (Files.isRegularFile(FILE)) {
            try {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                List<StoredMacro> stored = GSON.fromJson(json, new TypeToken<List<StoredMacro>>() { }.getType());
                if (stored != null) {
                    for (StoredMacro value : stored) {
                        MACROS.add(value.toMacro());
                    }
                }
            } catch (IOException | RuntimeException exception) {
                Constants.LOG.warn("[MerlinLib] could not read {}: {}", FILE, exception.getMessage());
            }
        }
        rebind();
    }

    public static Set<Integer> conflictingKeys() {
        Set<Integer> clashing = new HashSet<>();
        for (int first = 0; first < MACROS.size(); first++) {
            KeyCombo a = MACROS.get(first).combo();
            if (a.isEmpty()) {
                continue;
            }
            for (int second = first + 1; second < MACROS.size(); second++) {
                KeyCombo b = MACROS.get(second).combo();
                if (!b.isEmpty() && a.intersects(b)) {
                    clashing.addAll(a.keyList());
                    clashing.addAll(b.keyList());
                }
            }
        }
        return clashing;
    }

    public static String keyDisplayName(int keyCode) {
        return HotkeyRegistry.keyName(keyCode);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        HotkeyRegistry.tick();
    }

    public static String describe(List<String> commands) {
        return commands.stream().filter(command -> !command.isBlank()).collect(Collectors.joining(" / "));
    }

    private static void rebind() {
        HotkeyRegistry.clear();
        for (Macro macro : MACROS) {
            KeyCombo combo = macro.combo();
            if (combo.isEmpty() || macro.commands().isEmpty()) {
                continue;
            }
            HotkeyRegistry.add(combo, () -> run(macro));
        }
    }

    private static void run(Macro macro) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        for (String command : macro.commands()) {
            String trimmed = command.trim();
            if (!trimmed.isEmpty()) {
                minecraft.player.connection.sendCommand(trimmed);
            }
        }
    }

    private static final class StoredMacro {
        private String name;
        private List<Integer> keys;
        private Integer key;
        private List<String> commands;

        private Macro toMacro() {
            List<Integer> resolved = this.keys != null
                    ? this.keys
                    : this.key != null ? List.of(this.key) : List.of();
            return new Macro(this.name == null || this.name.isBlank() ? "Macro" : this.name,
                    List.copyOf(resolved),
                    this.commands == null ? List.of() : List.copyOf(this.commands));
        }
    }
}
