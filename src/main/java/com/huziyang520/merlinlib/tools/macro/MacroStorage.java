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
    private static final String CODEC_GLFW = "glfw";
    private static final String CODEC_HID = "hid";
    private static final java.util.Map<Integer, Integer> HID_TO_GLFW = buildHidTable();

    private MacroStorage() {
    }

    private static java.util.Map<Integer, Integer> buildHidTable() {
        java.util.Map<Integer, Integer> table = new java.util.HashMap<>();

        for (int index = 0; index < 26; index++) {
            table.put(4 + index, 65 + index);
        }

        for (int index = 0; index < 9; index++) {
            table.put(30 + index, 49 + index);
        }
        table.put(39, 48);

        for (int index = 0; index < 12; index++) {
            table.put(58 + index, 290 + index);
        }

        int[][] irregular = {
                {40, 257},
                {41, 256},
                {42, 259},
                {43, 258},
                {44, 32},
                {45, 45},
                {46, 61},
                {47, 91},
                {48, 93},
                {49, 92},
                {51, 59},
                {52, 39},
                {53, 96},
                {54, 44},
                {55, 46},
                {56, 47},

                {57, 280},
                {70, 283},
                {71, 281},
                {72, 284},

                {73, 260},
                {74, 268},
                {75, 266},
                {76, 261},
                {77, 269},
                {78, 267},
                {79, 262},
                {80, 263},
                {81, 264},
                {82, 265},

                {83, 282},
                {84, 331},
                {85, 332},
                {86, 333},
                {87, 334},
                {88, 335},
                {89, 321},
                {90, 322},
                {91, 323},
                {92, 324},
                {93, 325},
                {94, 326},
                {95, 327},
                {96, 328},
                {97, 329},
                {98, 320},
                {99, 330},
                {100, 92},
                {101, 348},

                {224, 341},
                {225, 340},
                {226, 342},
                {227, 343},
                {228, 345},
                {229, 344},
                {230, 346},
                {231, 347},
        };
        for (int[] pair : irregular) {
            table.put(pair[0], pair[1]);
        }

        return java.util.Map.copyOf(table);
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
        StoredFile file = new StoredFile();
        file.codec = CODEC_GLFW;
        file.macros = new ArrayList<>(MACROS.size());
        for (Macro macro : MACROS) {
            StoredMacro value = new StoredMacro();
            value.name = macro.name();
            value.keys = macro.keys();
            value.commands = macro.commands();
            file.macros.add(value);
        }
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(file), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Constants.LOG.warn("[MerlinLib] could not write {}: {}", FILE, exception.getMessage());
        }
    }

    public static void load() {
        MACROS.clear();
        boolean migrated = false;

        if (Files.isRegularFile(FILE)) {
            try {
                String json = Files.readString(FILE, StandardCharsets.UTF_8);
                List<StoredMacro> stored = null;
                String codec = null;
                com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(json);
                if (root.isJsonArray()) {
                    stored = GSON.fromJson(root, new TypeToken<List<StoredMacro>>() { }.getType());
                } else if (root.isJsonObject()) {
                    StoredFile file = GSON.fromJson(root, StoredFile.class);
                    if (file != null) {
                        stored = file.macros;
                        codec = file.codec;
                    }
                }

                boolean needsMigration = !CODEC_GLFW.equals(codec);
                if (stored != null) {
                    for (StoredMacro value : stored) {
                        Macro macro = value.toMacro();
                        MACROS.add(needsMigration ? migrate(macro) : macro);
                    }
                    migrated = needsMigration && !MACROS.isEmpty();
                }
            } catch (IOException | RuntimeException exception) {
                Constants.LOG.warn("[MerlinLib] could not read {}: {}", FILE, exception.getMessage());
            }
        }

        if (migrated) {
            Constants.LOG.info("[MerlinLib] {} macro(s) were read from a file written before 1.20.1; their "
                    + "key codes were converted from USB HID to GLFW and the file was rewritten", MACROS.size());
            save();
        }
        rebind();
    }

    private static Macro migrate(Macro macro) {
        List<Integer> converted = new ArrayList<>(macro.keys().size());
        for (Integer code : macro.keys()) {
            if (code == null) {
                continue;
            }
            Integer glfw = HID_TO_GLFW.get(code);
            if (glfw != null) {
                converted.add(glfw);
            } else {
                converted.add(code);
                Constants.LOG.warn("[MerlinLib] macro '{}' is bound to key code {}, which this build has no "
                        + "HID to GLFW mapping for; it was left as it is and will need re-binding by hand",
                        macro.name(), code);
            }
        }
        return macro.withKeys(converted);
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

    private static final class StoredFile {
        private String codec;
        private List<StoredMacro> macros;
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
