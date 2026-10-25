// /merlinlib diagnostics and helpers.

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
import com.huziyang520.merlinlib.network.OpenToolPayload;
import com.huziyang520.merlinlib.network.ServerSender;
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

public final class MerlinCommand {
    private MerlinCommand() {
    }

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
                .then(Commands.literal("editor")
                        .executes(context -> openTool(context, OpenToolPayload.EDITOR)))
                .then(Commands.literal("health")
                        .executes(context -> openTool(context, OpenToolPayload.HEALTH)))
                .then(Commands.literal("macro")
                        .executes(context -> openTool(context, OpenToolPayload.MACRO)))
                .then(Commands.literal("notices")
                        .executes(context -> openTool(context, OpenToolPayload.NOTICES)))
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

    private static int openTool(CommandContext<CommandSourceStack> context, int tool) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.merlinlib.player_only"));
            return 0;
        }
        boolean allowed = switch (tool) {
            case OpenToolPayload.EDITOR -> PermissionGate.canOpenEditor(player);
            case OpenToolPayload.HEALTH -> PermissionGate.canOpenHealthEditor(player);
            case OpenToolPayload.NOTICES -> PermissionGate.canUseTools(player);
            default -> PermissionGate.canUseTools(player);
        };
        if (!allowed) {
            source.sendFailure(Component.translatable("message.merlinlib.no_permission"));
            return 0;
        }
        ServerSender.sendToPlayer(player, new OpenToolPayload(tool));
        return 1;
    }

    private static int setEditorHotkey(CommandContext<CommandSourceStack> context, boolean enabled) {
        ConfigManager.saveClient(java.util.Map.of("editor.hotkey_enabled", Boolean.toString(enabled)));
        context.getSource().sendSuccess(() -> Component.translatable(enabled
                ? "command.merlinlib.editorhotkey.on"
                : "command.merlinlib.editorhotkey.off"), false);
        return 1;
    }

    private static boolean mayManage(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
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
            source.sendSuccess(() -> Component.translatable("command.merlinlib.content.stale"), false);
        }
        return 1;
    }

    private static String summarize(String fingerprint) {
        String trimmed = fingerprint.length() <= 48 ? fingerprint : fingerprint.substring(0, 45) + "...";
        return "[" + fingerprint.length() + "b] " + trimmed;
    }

    private static Registry<Enchantment> enchantmentRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }
}
