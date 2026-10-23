package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.command.MerlinCommand;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.config.ConfigScope;
import com.huziyang520.merlinlib.config.NeoConfigs;
import com.huziyang520.merlinlib.config.NoticeSettings;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.network.DamageFeedbackPayload;
import com.huziyang520.merlinlib.network.DamageFeedbackSink;
import com.huziyang520.merlinlib.network.HealthEditPayload;
import com.huziyang520.merlinlib.network.ItemEditPayload;
import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import com.huziyang520.merlinlib.tools.HealthCeiling;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge entrypoint. Hands the mod event bus to the shared bootstrap, registers the diagnostic
 * command on the game bus and adds the creative inventory entries.
 */
@Mod(Constants.MOD_ID)
public class MerlinLibNeoForge {

    public MerlinLibNeoForge(IEventBus eventBus, ModContainer modContainer) {

        // The per mod join notice switches moved to config/MerlinLib/notices.toml, and the old
        // notices.per_mod string of the common file is no longer declared in the specification. Reading it
        // therefore has to happen here: the loader rewrites a configuration file whose entries do not match
        // the specification while it loads that file, which would drop the old key before any listener could
        // see it. Calling it first also covers a fresh install, where notices.toml is simply created.
        NoticeSettings.init();

        // The switches are declared as loader specifications, which is what makes the mod list show the
        // "Config" button and the game build the screen from them.
        NeoConfigs.register(modContainer);
        ConfigManager.setBackend(ConfigScope.CLIENT, NeoConfigs.clientBackend());
        ConfigManager.setBackend(ConfigScope.SERVER, NeoConfigs.commonBackend());
        // The files are read after this constructor returns, so the values are picked up from these events:
        // without them the mod would run on the declared defaults until the next /reload.
        eventBus.addListener(this::onConfigLoading);
        eventBus.addListener(this::onConfigReloading);

        eventBus.addListener(this::onBuildCreativeTab);
        eventBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        // A death respawn rebuilds the player and copies attribute base values only (restoreFrom calls
        // assignPermanentModifiers solely when restoreAll is true), so the ceiling is carried over here.
        NeoForge.EVENT_BUS.addListener(this::onPlayerClone);
        // The mods list screen picks up the configuration screen from the CLIENT entrypoint, not from here: a
        // lambda that implements IConfigScreenFactory names Screen in its signature, so merely creating
        // one makes the dedicated server load a client-only class and NeoForge's RuntimeDistCleaner
        // aborts mod construction with "Attempted to load class net/minecraft/client/gui/screens/Screen
        // for invalid dist DEDICATED_SERVER". See MerlinLibNeoForgeClient.
        MerlinLib.init(eventBus);
    }

    /**
     * Picks the values up the first time the loader reads each of the two files.
     *
     * <p>This is also the moment the file left behind by a build from before the loader stored the switches is
     * imported: a value cannot be written into a specification before the loader has attached the file to it.
     *
     * @param event the loading event, fired once per file
     */
    private void onConfigLoading(ModConfigEvent.Loading event) {

        ConfigManager.onConfigLoaded(event.getConfig());
    }

    /**
     * Picks the values up again whenever a file changes on disk.
     *
     * <p>The loader watches both files, so a hand edit is applied the moment it is saved, without a restart
     * and without {@code /reload}.
     *
     * @param event the reloading event, fired once per changed file
     */
    private void onConfigReloading(ModConfigEvent.Reloading event) {

        ConfigManager.reload();
    }

    /**
     * Registers the damage feedback payload. The handler delegates to the common sink, which is a
     * no-op on a dedicated server, so this class stays free of client references.
     *
     * @param event the payload registration event
     */
    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {

        // Codec and handler together: 1.21.1 has no separate client-payload registration event, so the
        // handler is registered here and only ever runs on a client (playToClient handlers are skipped
        // on a dedicated server).
        event.registrar("1").playToClient(DamageFeedbackPayload.TYPE, DamageFeedbackPayload.STREAM_CODEC,
                (payload, context) -> DamageFeedbackSink.handle(payload));
        // Item edit: client -> server, applied to the main hand of the requesting player. The server is the
        // only side that can persist it and the only side that can check permission.
        event.registrar("1").playToServer(ItemEditPayload.TYPE, ItemEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    if (!PermissionGate.canOpenEditor(serverPlayer)) {
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
                                .translatable("message.merlinlib.no_permission"));
                        return;
                    }
                    if (!payload.stack().isEmpty()) {
                        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, payload.stack());
                    }
                });

        event.registrar("1").playToServer(HealthEditPayload.TYPE, HealthEditPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        applyHealthEdit(serverPlayer, payload);
                    }
                });
    }

    /**
     * Applies a validated health edit on the server: the screen is only a remote control.
     *
     * @param player  the requesting player
     * @param payload the requested edit
     */
    private static void applyHealthEdit(ServerPlayer player, HealthEditPayload payload) {

        if (!PermissionGate.canOpenHealthEditor(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("message.merlinlib.no_permission"));
            return;
        }
        if (player.level().getEntity(payload.targetId()) instanceof LivingEntity target) {
            int max = Math.max(0, payload.max());
            int current = Math.max(0, Math.min(max, payload.current()));
            HealthCeiling.apply(target, max);
            target.setHealth(Math.min(current, target.getMaxHealth()));
        }
    }

    /**
     * Carries the health ceiling over to the player that replaces the old one on respawn.
     *
     * @param event the clone event, fired on respawn and on a dimension change
     */
    private void onPlayerClone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        HealthCeiling.copyTo(event.getOriginal(), event.getEntity());
    }

    /**
     * Publishes the resolved damage of every player dealt hit to the attacking client.
     *
     * <p>The event amount is the damage before the target's remaining health caps it, which is exactly
     * the "theoretical damage of this swing" the overlay shows.
     *
     * @param event the incoming damage event
     */
    private void onIncomingDamage(LivingIncomingDamageEvent event) {

        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            attacker.connection.send(new DamageFeedbackPayload(
                    event.getAmount(), false, event.getEntity().getId()));
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {

        MerlinCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    /**
     * Adds one enchanted book per MerlinLib enchantment to the vanilla ingredients tab.
     *
     * <p>The stacks are built from the parameters' registry access, which is only populated once the
     * data driven enchantment registry has been loaded; outside of that window the list is simply empty.
     */
    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {

        if (MerlinCreativeEntries.TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.enchantedBooks(event.getParameters().holders())) {
                // NeoForge rejects a stack that the tab already holds with
                // "Itemstack ... already exists in the tab's list", and a tab is rebuilt whenever the
                // creative screen is opened, so the same book arrives more than once per session.
                if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) {
                    continue;
                }
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            return;
        }
        if (MerlinCreativeEntries.TOOLS_TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.testWeapons()) {
                if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) {
                    continue;
                }
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }
}
