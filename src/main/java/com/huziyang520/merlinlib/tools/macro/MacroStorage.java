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

/**
 * Client local command macros: named command lists bound to a combination of keys, stored next to the
 * other MerlinLib files so they follow this computer rather than the server.
 *
 * <h2>Keys</h2>
 *
 * <p>A macro is bound to a {@link KeyCombo}, not to a single key: any number of keys held together. The
 * file stores them as a list, and still reads the older single {@code "key"} field, so a file written by an
 * earlier version keeps working.
 *
 * <h2>Running</h2>
 *
 * <p>Running a macro means sending its commands as the player through the vanilla command path, exactly as
 * if they had been typed: permissions, command syntax and server side checks all apply unchanged.
 *
 * <p>The bindings themselves live in {@link HotkeyRegistry}; this class owns the data and re-registers the
 * bindings whenever the data changes.
 */
public final class MacroStorage {

    /**
     * One macro.
     *
     * @param name     the label shown in the macro screen
     * @param keys     the GLFW key codes that have to be held together
     * @param commands the commands to run, one per line, without a leading slash
     */
    public record Macro(String name, List<Integer> keys, List<String> commands) {

        /**
         * @return the combination these keys form, empty when the macro is not bound
         */
        public KeyCombo combo() {
            return KeyCombo.of(keys).orElseGet(KeyCombo::of);
        }

        /**
         * @param newKeys the new key codes
         * @return a copy of this macro bound to those keys
         */
        public Macro withKeys(List<Integer> newKeys) {
            return new Macro(this.name, List.copyOf(newKeys), this.commands);
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", Constants.CONFIG_DIRECTORY, "macros.json");
    private static final List<Macro> MACROS = new ArrayList<>();

    private MacroStorage() {
    }

    /** @return the live list of macros, in the order the screen shows them. */
    public static List<Macro> macros() {
        return List.copyOf(MACROS);
    }

    /**
     * Replaces every macro and re-registers the bindings.
     *
     * @param macros the new macros
     */
    public static void replaceAll(List<Macro> macros) {
        MACROS.clear();
        MACROS.addAll(macros);
        rebind();
    }

    /**
     * Appends one unbound, empty macro, which is what the "new macro" button does.
     */
    public static void add() {
        MACROS.add(new Macro("Macro " + (MACROS.size() + 1), List.of(), List.of()));
        rebind();
    }

    /**
     * Removes one macro.
     *
     * @param index the index to remove, ignored when out of range
     */
    public static void remove(int index) {
        if (index >= 0 && index < MACROS.size()) {
            MACROS.remove(index);
            rebind();
        }
    }

    /**
     * Writes the macros to disk.
     *
     * <p>Failures are logged rather than thrown: a read only file must not take the game down, and the
     * macros keep working for this session.
     */
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

    /** Reads the macros from disk and registers their bindings. */
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

    /**
     * The key codes that two or more macros share.
     *
     * <p>Used by the macro screen to mark the affected rows in red: overlapping combinations cannot both
     * be completed unambiguously, and the player should see that before wondering why a key does not work.
     *
     * @return every key code involved in a clash
     */
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

    /**
     * The name of a key code, as vanilla labels it.
     *
     * @param keyCode the GLFW key code, negative for unbound
     * @return the display name
     */
    public static String keyDisplayName(int keyCode) {
        return HotkeyRegistry.keyName(keyCode);
    }

    /**
     * Advances the hotkey layer by one client tick.
     *
     * @param minecraft the client
     */
    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null) {
            return;
        }
        HotkeyRegistry.tick();
    }

    /**
     * @param combo     the combination
     * @param commands  the commands
     * @return the commands joined for display, one per line
     */
    public static String describe(List<String> commands) {
        return commands.stream().filter(command -> !command.isBlank()).collect(Collectors.joining(" / "));
    }

    /** Re-registers every binding from the current macro list. */
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

    /**
     * Runs one macro as the player.
     *
     * @param macro the macro to run
     */
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

    /**
     * The on disk shape of one macro.
     *
     * <p>{@code key} is the field earlier versions wrote; it is still read so upgrading does not lose a
     * binding, and it is never written back.
     */
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
