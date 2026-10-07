package com.huziyang520.merlinlib.command;

import com.huziyang520.merlinlib.api.EnchantmentInfo;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.config.ConfigDirectory;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ContentSnapshot;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.impl.IdValidator;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;
import java.util.Optional;

/**
 * {@code /merlinlib} diagnostics and helpers.
 *
 * <p>Purpose: make it possible to tell, from inside the game, whether the declared enchantments
 * actually reached the enchantment registry. That distinguishes "the content pipeline is broken" from
 * "the enchantment exists but has no way to be obtained yet", which is otherwise very hard to see.
 *
 * <ul>
 *     <li>{@code /merlinlib list} - every MerlinLib managed enchantment that is really in the registry</li>
 *     <li>{@code /merlinlib info <id>} - effective source, enabled flag, level and weight</li>
 *     <li>{@code /merlinlib book <id> [level]} - hands out the enchanted book, operator only</li>
 *     <li>{@code /merlinlib content} - whether the files on disk are the ones this process registered</li>
 * </ul>
 *
 * <h2>Two changes from the 26.3 line</h2>
 *
 * <p><b>The pack subcommand is gone, replaced by {@code content}.</b> It reported on the generated
 * datapack - whether the pack repository knew it, whether it was selected, which resources it served.
 * On 1.20.1 there is no generated datapack and no data driven enchantment registry, so every one of
 * those questions is meaningless. The question an author actually has on this version is different and
 * worse: the files are read once at startup, so an edit made afterwards has no effect until a restart,
 * and nothing on screen says so. {@code /merlinlib content} answers exactly that, by comparing the
 * fingerprint the process registered against the one the files have now.
 *
 * <p><b>The book is built through {@link EnchantedBookItem}.</b> The 26.3 line wrote a
 * {@code STORED_ENCHANTMENTS} data component; that component system arrives in 1.20.5, and on this
 * version an enchanted book's contents are written with
 * {@code EnchantedBookItem.addEnchantment(ItemStack, EnchantmentInstance)}.
 */
public final class MerlinCommand {

    private MerlinCommand() {
    }

    /**
     * Registers the command tree.
     *
     * <p>Fire from Forge's {@code RegisterCommandsEvent} on the game event bus. The three arguments
     * are the ones the event hands over, and the same three 26.3 got from its own loader hooks, so
     * the signature carried over unchanged.
     *
     * @param dispatcher   the server command dispatcher
     * @param buildContext registry access of the running server
     * @param selection    which command set is being built, unused
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext,
                                Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("merlinlib")
                .then(Commands.literal("list")
                        .executes(MerlinCommand::list))
                .then(Commands.literal("content")
                        .executes(MerlinCommand::contentStatus))
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(MerlinCommand::info)))
                .then(Commands.literal("book")
                        .requires(MerlinCommand::mayManage)
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(context -> giveBook(context, 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                        .executes(context -> giveBook(context, IntegerArgumentType.getInteger(context, "level"))))))
                .then(Commands.literal("editorhotkey")
                        .requires(MerlinCommand::mayManage)
                        .then(Commands.literal("on")
                                .executes(context -> setEditorHotkey(context, true)))
                        .then(Commands.literal("off")
                                .executes(context -> setEditorHotkey(context, false)))));
    }

    /**
     * Switches the item editor hotkey on or off.
     *
     * <p>The binding itself always exists, so this only decides whether pressing it opens the editor. It
     * writes {@code editor.hotkey_enabled} into {@code client.toml} and reloads, which is why the switch
     * takes effect immediately: the client reads that value when the key is pressed, not at startup.
     *
     * <p>On a dedicated server the file that is written is the one next to the server, which belongs to
     * the machine running the command; a player's own hotkey is their own file and their own configuration
     * screen.
     *
     * @param context the command context
     * @param enabled the value to write
     * @return 1, the standard success code
     */
    private static int setEditorHotkey(CommandContext<CommandSourceStack> context, boolean enabled) {
        ConfigManager.saveClient(java.util.Map.of("editor.hotkey_enabled", Boolean.toString(enabled)));
        context.getSource().sendSuccess(() -> Component.translatable(enabled
                ? "command.merlinlib.editorhotkey.on"
                : "command.merlinlib.editorhotkey.off"), false);
        return 1;
    }

    private static boolean mayManage(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        // The console is always allowed; players need operator rights when the server asks for them.
        return player == null || PermissionGate.isOperator(player);
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Registry<Enchantment> registry = enchantmentRegistry(source);
        ContentSnapshot snapshot = ContentManager.current();

        List<ResourceLocation> present = snapshot.enchantments().keySet().stream()
                .sorted()
                .filter(registry::containsKey)
                .toList();

        // Everything else in the registry that is not vanilla's, and that MerlinLib does not manage itself.
        // Another mod's enchantments are registered in that mod's own code, so they never appear in
        // MerlinLib's snapshot - which made this list read as if MerlinLib were the only mod with any. They
        // are listed by registry id, so a mod that failed to load is just as obvious as one that did.
        List<ResourceLocation> others = registry.keySet().stream()
                .filter(id -> !id.getNamespace().equals("minecraft"))
                .filter(id -> !present.contains(id))
                .sorted()
                .toList();

        if (present.isEmpty() && others.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.list.empty", snapshot.enchantments().size()), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.merlinlib.list.header", present.size()), false);
        for (ResourceLocation id : present) {
            EnchantmentDraft draft = snapshot.enchantments().get(id);
            source.sendSuccess(() -> Component.literal(" - " + id + " (" + draft.source().displayName() + ")"), false);
        }

        if (!others.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.list.others", others.size()), false);
            for (ResourceLocation id : others) {
                source.sendSuccess(() -> Component.literal(" - " + id + " (registry)"), false);
            }
        }
        return present.size() + others.size();
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String raw = StringArgumentType.getString(context, "id");

        Optional<ResourceLocation> parsed = IdValidator.parse(raw);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.invalid_id", raw));
            return 0;
        }
        ResourceLocation id = parsed.get();

        Optional<EnchantmentInfo> info = MerlinApi.enchantments().describe(id);
        if (info.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.unknown", id.toString()));
            return 0;
        }

        EnchantmentInfo value = info.get();
        boolean inRegistry = enchantmentRegistry(source).containsKey(id);
        source.sendSuccess(() -> Component.literal(id + ": source=" + value.source().displayName()
                + " enabled=" + value.enabled()
                + " inRegistry=" + inRegistry
                + " maxLevel=" + value.maxLevel()
                + " weight=" + value.weight()), false);
        return 1;
    }

    private static int giveBook(CommandContext<CommandSourceStack> context, int level) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.merlinlib.player_only"));
            return 0;
        }

        String raw = StringArgumentType.getString(context, "id");
        Optional<ResourceLocation> parsed = IdValidator.parse(raw);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.invalid_id", raw));
            return 0;
        }
        ResourceLocation id = parsed.get();

        Enchantment enchantment = enchantmentRegistry(source).get(id);
        if (enchantment == null) {
            source.sendFailure(Component.translatable("command.merlinlib.not_in_registry", id.toString()));
            return 0;
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(enchantment, level));

        player.addItem(book);
        source.sendSuccess(() -> Component.translatable("command.merlinlib.book.given", id.toString(), level), false);
        return 1;
    }

    /**
     * Reports whether the content files on disk are the ones this process registered.
     *
     * <h2>Why this replaced the pack status command</h2>
     *
     * <p>The 26.3 line's {@code /merlinlib pack} walked the delivery chain of its generated datapack:
     * known to the pack repository, selected, visible through the resource manager, present in the
     * registry. Every one of those links exists because the content travelled through a data pack.
     *
     * <p>On 1.20.1 the content does not travel. It is read once while the game starts and written
     * straight into the enchantment registry, which is then frozen for the life of the process. The
     * chain to walk is two links long, and the interesting one is the second: an author who edits
     * {@code config/MerlinLib/enchantments.json} and types {@code /merlinlib list} will see the old
     * definition, because the new one cannot be registered without a restart. Nothing else in the game
     * says so, and a silent no-op is the exact failure this project keeps writing commands to prevent.
     *
     * <p>So this command prints the fingerprint the process registered alongside the one the files have
     * now, and says plainly which of the two situations the caller is in.
     */
    private static int contentStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String registered = ContentManager.registeredFingerprint();
        String current = ConfigDirectory.fingerprint();
        int active = ContentManager.current().enchantments().size();

        source.sendSuccess(() -> Component.literal(
                "content: registered=" + (registered.isEmpty() ? "(nothing)" : summarize(registered))
                        + " onDisk=" + (current.isEmpty() ? "(no files)" : summarize(current))
                        + " active=" + active), false);

        if (registered.equals(current)) {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.content.current"), false);
        } else {
            // Not a failure: it is the normal state after an edit, and the honest instruction is a restart.
            source.sendSuccess(() -> Component.translatable("command.merlinlib.content.stale"), false);
        }
        return 1;
    }

    /**
     * @param fingerprint the opaque fingerprint string
     * @return its length and first characters, which is enough to tell two states apart in chat without
     *         printing a line per file
     */
    private static String summarize(String fingerprint) {
        String trimmed = fingerprint.length() <= 48 ? fingerprint : fingerprint.substring(0, 45) + "...";
        return "[" + fingerprint.length() + "b] " + trimmed;
    }

    private static Registry<Enchantment> enchantmentRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }
}
