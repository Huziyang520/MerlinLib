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
 *
 * <h2>1.20.1: the stored key codes had to be migrated once, and this is where that happens</h2>
 *
 * <p>This is the most dangerous part of the port, because getting it wrong is silent: a macro whose key
 * codes mean something different loads without error, shows a plausible key name, and simply never fires
 * (or fires on the wrong key).</p>
 *
 * <p>The 26.3 line stored <b>USB HID usage ids</b> (escape 41, enter 40, keypad enter 88, left 80,
 * right 79). It had to, because that was the encoding its key events carried. On 1.20.1 the key event
 * carries <b>GLFW</b> codes (escape 256, enter 257, keypad enter 335, left 263, right 262), and
 * {@code InputConstants} no longer ships the HID table at all - so the mapping cannot be looked up, it
 * has to be written down. {@link #HID_TO_GLFW} is that table, covering the keys a macro can realistically
 * be bound to.</p>
 *
 * <p>The migration is done <b>here, on read</b>, and not in the macro screen. This is the only place that
 * can tell a file written by 26.3 from one written by this build, so doing it here means every reader of
 * {@code Macro.keys()} - the screen, the hotkey layer, a future exporter - is correct by construction. A
 * reader that had to remember to convert would eventually be written without it.</p>
 *
 * <p>How the two are told apart, and why it is safe: the file carries a {@code codec} marker once this
 * build has written it. A file <b>without</b> the marker was written by 26.3 (or is older still), so its
 * codes are converted and the marker is added. A file <b>with</b> the marker is left exactly as it is.
 * The marker is what makes this a one-off rather than a conversion applied on every load, which would
 * re-map an already-migrated code into nonsense the second time the game started.</p>
 *
 * <p>A code the table does not know is kept as it is rather than guessed at, and reported in the log: an
 * unusual binding is better left visible and possibly wrong than silently changed to a key the player
 * never chose.</p>
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

    /**
     * The value written into the file's {@code codec} field once the key codes are GLFW.
     *
     * <p>Any file without this marker predates the marker, so its codes are HID and are converted on
     * read. See the class Javadoc for why the marker - rather than a heuristic on every load - is what
     * makes the conversion a one-off.
     */
    private static final String CODEC_GLFW = "glfw";

    /** The marker 26.3 files implicitly carried, written out so the intent is readable in the file. */
    private static final String CODEC_HID = "hid";

    /**
     * USB HID usage id to GLFW key code, for the keys a macro can realistically be bound to.
     *
     * <p>Hand written because {@code InputConstants} no longer carries the HID table on this version.
     * The table is deliberately conservative: it covers letters, digits, the function row, the modifier
     * and navigation keys, and the numpad. A code outside it is left alone and logged rather than
     * guessed at - see the class Javadoc.
     */
    private static final java.util.Map<Integer, Integer> HID_TO_GLFW = buildHidTable();

    private MacroStorage() {
    }

    /**
     * Builds the HID to GLFW table.
     *
     * <p>Written from the two published layouts rather than copied from a library, because the library
     * that used to expose the HID table is gone on this version. The regularities are used where they
     * genuinely exist - the letters and the digits are contiguous runs in both layouts - and every
     * irregular key is listed once, explicitly. There are no "guess then correct" overrides: a table
     * whose later lines silently undo earlier ones cannot be checked by reading it.
     *
     * @return the mapping, immutable
     */
    private static java.util.Map<Integer, Integer> buildHidTable() {
        java.util.Map<Integer, Integer> table = new java.util.HashMap<>();

        // Letters: HID 4..29 = A..Z, GLFW 65..90 = A..Z. Same order.
        for (int index = 0; index < 26; index++) {
            table.put(4 + index, 65 + index);
        }
        // Digits on the top row: HID 30..38 = 1..9 and 39 = 0; GLFW 49..57 = 1..9 and 48 = 0.
        for (int index = 0; index < 9; index++) {
            table.put(30 + index, 49 + index);
        }
        table.put(39, 48);
        // Function row: HID 58..69 = F1..F12, GLFW 290..301 = F1..F12.
        for (int index = 0; index < 12; index++) {
            table.put(58 + index, 290 + index);
        }

        // Everything else, one line per key: {HID, GLFW}.
        int[][] irregular = {
                // Punctuation and the main typing keys.
                {40, 257},  // enter
                {41, 256},  // escape
                {42, 259},  // backspace
                {43, 258},  // tab
                {44, 32},   // space
                {45, 45},   // minus
                {46, 61},   // equals
                {47, 91},   // left bracket
                {48, 93},   // right bracket
                {49, 92},   // backslash
                {51, 59},   // semicolon
                {52, 39},   // apostrophe
                {53, 96},   // grave accent
                {54, 44},   // comma
                {55, 46},   // period
                {56, 47},   // slash
                // Lock and system keys.
                {57, 280},  // caps lock
                {70, 283},  // print screen
                {71, 281},  // scroll lock
                {72, 284},  // pause
                // Navigation cluster. GLFW numbers these in a different order from HID, which is the
                // single easiest thing to get wrong here: HID runs insert, home, page up, delete, end,
                // page down, and GLFW runs insert, delete, right, left, down, up, page up, page down,
                // home, end.
                {73, 260},  // insert
                {74, 268},  // home
                {75, 266},  // page up
                {76, 261},  // delete
                {77, 269},  // end
                {78, 267},  // page down
                {79, 262},  // right arrow
                {80, 263},  // left arrow
                {81, 264},  // down arrow
                {82, 265},  // up arrow
                // Keypad.
                {83, 282},  // num lock
                {84, 331},  // keypad divide
                {85, 332},  // keypad multiply
                {86, 333},  // keypad subtract
                {87, 334},  // keypad add
                {88, 335},  // keypad enter
                {89, 321},  // keypad 1
                {90, 322},  // keypad 2
                {91, 323},  // keypad 3
                {92, 324},  // keypad 4
                {93, 325},  // keypad 5
                {94, 326},  // keypad 6
                {95, 327},  // keypad 7
                {96, 328},  // keypad 8
                {97, 329},  // keypad 9
                {98, 320},  // keypad 0
                {99, 330},  // keypad decimal
                {100, 92},  // keypad non-US backslash (no distinct GLFW key; the main backslash)
                {101, 348}, // application / menu
                // Modifiers. GLFW puts the left and right pairs in an order that is not the HID order:
                // left shift/control/alt/super are 340..343 and right shift/control/alt/super are
                // 344..347, while HID runs control, shift, alt, super on the left and control, shift,
                // alt, super on the right.
                {224, 341}, // left control
                {225, 340}, // left shift
                {226, 342}, // left alt
                {227, 343}, // left super
                {228, 345}, // right control
                {229, 344}, // right shift
                {230, 346}, // right alt
                {231, 347}, // right super
        };
        for (int[] pair : irregular) {
            table.put(pair[0], pair[1]);
        }

        return java.util.Map.copyOf(table);
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
     *
     * <p>The file is always written with the {@code codec} marker, because everything this method writes
     * is already in GLFW codes. That marker is what stops the migration in {@link #load()} from running a
     * second time on the next launch.
     */
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

    /**
     * Reads the macros from disk, migrating the key codes when the file predates this build, and
     * registers their bindings.
     *
     * <p>The file may be either shape: a bare array (what 26.3 wrote) or the object with a {@code codec}
     * marker that this build writes. Both are accepted so an upgrade does not lose anything.
     */
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
                    // The 26.3 shape: a bare array, which means HID codes.
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
            // Written back at once so the conversion happens exactly once. If this write fails the
            // macros still work for this session, and the next launch converts again from the same
            // original file - which is the safe direction to fail in.
            Constants.LOG.info("[MerlinLib] {} macro(s) were read from a file written before 1.20.1; their "
                    + "key codes were converted from USB HID to GLFW and the file was rewritten", MACROS.size());
            save();
        }
        rebind();
    }

    /**
     * Converts one macro's key codes from USB HID to GLFW.
     *
     * <p>A code the table does not know is kept unchanged and named in the log. Guessing would be worse
     * than leaving it: a wrong code is a macro that fires on a key the player never chose, while an
     * unconverted one is a macro that visibly does not fire and can be re-bound by hand.
     *
     * @param macro the macro as it was stored
     * @return the same macro with GLFW codes
     */
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
     * @param commands the commands
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
     * The on disk file.
     *
     * <p>{@code codec} names the key code encoding. Its absence means the file predates this build and
     * its codes are USB HID; see the class Javadoc.
     */
    private static final class StoredFile {
        private String codec;
        private List<StoredMacro> macros;
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