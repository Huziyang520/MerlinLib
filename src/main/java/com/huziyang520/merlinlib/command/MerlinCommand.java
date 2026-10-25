// /merlinlib diagnostics and helpers.

package com.huziyang520.merlinlib.command;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentInfo;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigScope;
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
import net.minecraft.resources.ResourceLocation;
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

public final class MerlinCommand {
    private MerlinCommand() {
    }

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

    private static int setEditorHotkey(CommandContext<CommandSourceStack> context, boolean enabled) {
        ConfigManager.write(ConfigScope.CLIENT,
                java.util.Map.of("editor.hotkey_enabled", Boolean.toString(enabled)));
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

        if (present.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.list.header", 0), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.merlinlib.list.header", present.size()), false);
            for (ResourceLocation id : present) {
                EnchantmentDraft draft = snapshot.enchantments().get(id);
                source.sendSuccess(() -> Component.literal(" - " + id + " (" + draft.source().displayName() + ")"), false);
            }
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
        Registry<Enchantment> registry = enchantmentRegistry(source);
        var holder = registry.getHolder(id).orElse(null);
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

    private static int packStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        PackRepository repository = server.getPackRepository();
        boolean available = repository.isAvailable(Constants.GENERATED_PACK_ID);
        boolean selected = repository.getSelectedPacks().stream()
                .anyMatch(pack -> Constants.GENERATED_PACK_ID.equals(pack.getId()));
        Map<String, byte[]> expected = GeneratedPack.previewFiles();
        ResourceManager resources = server.getResourceManager();
        Map<ResourceLocation, List<Resource>> visible = resources.listResourceStacks("enchantment",
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
        for (Map.Entry<ResourceLocation, List<Resource>> entry : visible.entrySet()) {
            String from = entry.getValue().stream().map(Resource::sourcePackId).collect(Collectors.joining(", "));
            source.sendSuccess(() -> Component.literal("  visible " + entry.getKey() + " from [" + from + "]"), false);
        }
        if (expected.isEmpty()) {
            source.sendFailure(Component.translatable("command.merlinlib.pack.empty"));
        }
        return 1;
    }

    private static Registry<Enchantment> enchantmentRegistry(CommandSourceStack source) {
        return source.getServer().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }
}
