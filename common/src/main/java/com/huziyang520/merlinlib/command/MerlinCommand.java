package com.huziyang520.merlinlib.command;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentInfo;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ContentSnapshot;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.datapack.GeneratedPack;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /merlinlib} diagnostics and helpers.
 *
 * <p>Purpose: make it possible to tell, from inside the game, whether the generated enchantments
 * actually reached the enchantment registry. That distinguishes "the content pipeline is broken" from
 * "the enchantment exists but has no way to be obtained yet", which is otherwise very hard to see.
 *
 * <ul>
 *     <li>{@code /merlinlib list} - every MerlinLib managed enchantment that is really in the registry</li>
 *     <li>{@code /merlinlib info <id>} - effective source, enabled flag, level and weight</li>
 *     <li>{@code /merlinlib book <id> [level]} - hands out the enchanted book, operator only</li>
 * </ul>
 */
public final class MerlinCommand {

    private MerlinCommand() {
    }

    /**
     * Registers the command tree. Called from both loaders with the same three arguments, because
     * NeoForge's event and Fabric's callback expose the identical parameter set.
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
                .then(Commands.literal("pack")
                        .executes(MerlinCommand::packStatus))
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

        List<Identifier> present = snapshot.enchantments().keySet().stream()
                .sorted()
                .filter(registry::containsKey)
                .toList();

        if (present.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.list.empty", snapshot.enchantments().size()), false);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.merlinlib.list.header", present.size()), false);
        for (Identifier id : present) {
            EnchantmentDraft draft = snapshot.enchantments().get(id);
            source.sendSuccess(() -> Component.literal(" - " + id + " (" + draft.source().displayName() + ")"), false);
        }
        return present.size();
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String raw = StringArgumentType.getString(context, "id");

        Optional<Identifier> parsed = IdValidator.parse(raw);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.invalid_id", raw));
            return 0;
        }
        Identifier id = parsed.get();

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
        Optional<Identifier> parsed = IdValidator.parse(raw);
        if (parsed.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.invalid_id", raw));
            return 0;
        }
        Identifier id = parsed.get();

        Registry<Enchantment> registry = enchantmentRegistry(source);
        var holder = registry.get(id).orElse(null);
        if (holder == null) {
            source.sendFailure(Component.translatable("command.merlinlib.not_in_registry", id.toString()));
            return 0;
        }

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());

        player.addItem(book);
        source.sendSuccess(() -> Component.translatable("command.merlinlib.book.given", id.toString(), level), false);
        return 1;
    }

    /**
     * Walks the whole delivery chain of the generated datapack and reports every link, so a failure can
     * be pinned down without reading the log:
     * <ol>
     *     <li>is the pack known to the pack repository (the injection hook ran)</li>
     *     <li>is the pack selected (it is declared required, so it must be)</li>
     *     <li>does the server resource manager actually see our resources (the listing is correct)</li>
     *     <li>did the enchantment end up in the registry (the json was accepted)</li>
     * </ol>
     */
    private static int packStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        PackRepository repository = server.getPackRepository();

        boolean available = repository.isAvailable(Constants.GENERATED_PACK_ID);
        boolean selected = repository.getSelectedPacks().stream()
                .anyMatch(pack -> Constants.GENERATED_PACK_ID.equals(pack.getId()));
        Map<String, byte[]> expected = GeneratedPack.previewFiles();

        ResourceManager resources = server.getResourceManager();
        Map<Identifier, List<Resource>> visible = resources.listResourceStacks("enchantment",
                id -> id.getNamespace().equals(Constants.MOD_ID));
        Registry<Enchantment> registry = enchantmentRegistry(source);
        long inRegistry = ContentManager.current().enchantments().keySet().stream().filter(registry::containsKey).count();

        source.sendSuccess(() -> Component.literal(
                "pack available=" + available
                        + " selected=" + selected
                        + " files=" + expected.size()
                        + " visibleResources=" + visible.size()
                        + " inRegistry=" + inRegistry), false);

        for (String key : expected.keySet()) {
            source.sendSuccess(() -> Component.literal("  would serve " + key), false);
        }
        for (Map.Entry<Identifier, List<Resource>> entry : visible.entrySet()) {
            String from = entry.getValue().stream().map(Resource::sourcePackId).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal("  visible " + entry.getKey() + " from [" + from + "]"), false);
        }
        if (expected.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.pack.empty"));
        }
        return 1;
    }

    private static Registry<Enchantment> enchantmentRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    }
}
