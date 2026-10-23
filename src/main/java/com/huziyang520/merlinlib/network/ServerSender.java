package com.huziyang520.merlinlib.network;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.impl.PermissionGate;
import com.huziyang520.merlinlib.tools.HealthCeiling;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The library's network channel, and the sink client screens send through.
 */
public final class ServerSender {

    /**
     * Protocol version advertised to the other side.
 */
    private static final String PROTOCOL_VERSION = "1";

    /** The one channel every MerlinLib message travels on. */
    private static final String CHANNEL_NAME = "main";

    /** Message ordinals. These are the wire format: they must never be renumbered or reused. */
    private static final int ID_DAMAGE_FEEDBACK = 0;
    private static final int ID_HEALTH_EDIT = 1;
    private static final int ID_ITEM_EDIT = 2;
    private static final int ID_OPEN_TOOL = 3;

    private static volatile SimpleChannel channel;

    private static volatile Consumer<Object> sink;

    private ServerSender() {
    }

    /**
     * Builds the channel and registers every message on it.
     *
     * <p>Idempotent by design: the channel registry cannot be reopened, so a second call would throw
     * rather than overwrite, and the likeliest way to reach that is two entrypoints both asking to
     * install the network.
 */
    public static synchronized void register() {
        if (channel != null) {
            return;
        }

        SimpleChannel built = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Constants.MOD_ID, CHANNEL_NAME),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        // The handler is dispatched on the main thread through enqueueWork, and the context has to be
        // told the packet was handled: without that the connection stalls waiting for a reply that
        // never comes. Both calls are what the 1.20.1 API asks for, not decoration.
        built.registerMessage(ID_DAMAGE_FEEDBACK, DamageFeedbackPayload.class,
                DamageFeedbackPayload::write,
                DamageFeedbackPayload::read,
                ServerSender::onDamageFeedback,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        built.registerMessage(ID_HEALTH_EDIT, HealthEditPayload.class,
                HealthEditPayload::write,
                HealthEditPayload::read,
                ServerSender::onHealthEdit,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        built.registerMessage(ID_ITEM_EDIT, ItemEditPayload.class,
                ItemEditPayload::write,
                ItemEditPayload::read,
                ServerSender::onItemEdit,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        // The one clientbound message that is not about damage: the answer to /merlinlib editor / health /
        // macro, which have to be executed on the server but open a screen on the client.
        built.registerMessage(ID_OPEN_TOOL, OpenToolPayload.class,
                OpenToolPayload::write,
                OpenToolPayload::read,
                ServerSender::onOpenTool,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        channel = built;
        Constants.LOG.info("[MerlinLib] network channel '{}' registered with {} message(s)",
                CHANNEL_NAME, 4);
    }

    /**
     * Installs the client side sender.
 *
     * @param sender accepts a payload and sends it to the server
 */
    public static void setSink(Consumer<Object> sender) {
        sink = sender;
    }

    /**
     * Sends a client to server payload.
     *
     * @param payload the payload to send
     * @throws IllegalStateException when the channel was never registered and no sink is installed
 */
    public static void send(Object payload) {
        Consumer<Object> active = sink;
        if (active != null) {
            active.accept(payload);
            return;
        }
        SimpleChannel local = channel;
        if (local == null) {
            // Loud rather than silent: a dropped editor request looks like "the button does nothing",
            // which is the single hardest failure in this subsystem to diagnose from a log.
            throw new IllegalStateException(
                    "[MerlinLib] a payload was sent before the network channel was registered");
        }
        local.sendToServer(payload);
    }

    /**
     * Sends damage feedback to one player.
 *
     * @param player  the receiving player, typically the attacker
     * @param payload the feedback to send
 */
    public static void sendToPlayer(ServerPlayer player, DamageFeedbackPayload payload) {
        SimpleChannel local = channel;
        if (local == null || player == null) {
            return;
        }
        local.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    /**
     * Asks one player's client to open a toolkit screen.
     *
     * <p>Clientbound for the same reason as the damage feedback: the decision belongs to the server, the
     * screen belongs to the client. A missing channel is logged rather than thrown here, because the caller
     * is a command and a command that throws prints a stack trace into the player's chat.
     *
     * @param player  the receiving player
     * @param payload which screen to open
 */
    public static void sendToPlayer(ServerPlayer player, OpenToolPayload payload) {
        SimpleChannel local = channel;
        if (local == null || player == null) {
            Constants.LOG.warn("[MerlinLib] the toolkit request could not be sent: the network channel is absent");
            return;
        }
        local.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    /**
     * Delivers one screen request to the client sink.
     *
     * <p>Runs on the main thread. The sink is {@code null} on a dedicated server, which is what keeps this
     * handler safe to have registered unconditionally.
     *
     * @param payload  the decoded payload
     * @param supplier the network context
 */
    private static void onOpenTool(OpenToolPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ToolOpenSink.handle(payload));
        context.setPacketHandled(true);
    }

    /**
     * Delivers one damage feedback payload to the client sink.
     *
     * <p>Runs on the main thread. The sink is {@code null} on a dedicated server, which is what keeps
     * this handler safe to have registered unconditionally.
     *
     * @param payload  the decoded payload
     * @param supplier the network context
 */
    private static void onDamageFeedback(DamageFeedbackPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DamageFeedbackSink.handle(payload));
        context.setPacketHandled(true);
    }

    /**
     * Handles a health edit request.
     *
     * <p>The screen is only a remote control: everything is re-validated here, on the server, against
     * the server's own configuration and the server's own permission set.
     *
     * @param payload  the requested edit
     * @param supplier the network context
 */
    private static void onHealthEdit(HealthEditPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> applyHealthEdit(player, payload));
        context.setPacketHandled(true);
    }

    /**
     * Handles an item edit request.
     *
     * <p>Permission is checked before the stack is touched, and an empty stack is refused rather than
     * written: an empty main hand is what the player has when the edit failed, not a request to clear
     * the slot.
     *
     * @param payload  the requested stack
     * @param supplier the network context
 */
    private static void onItemEdit(ItemEditPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player == null) {
                return;
            }
            if (!PermissionGate.canOpenEditor(player)) {
                player.sendSystemMessage(Component.translatable("message.merlinlib.no_permission"));
                return;
            }
            if (!payload.stack().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, payload.stack());
            }
        });
        context.setPacketHandled(true);
    }

    /**
     * Applies a validated health edit on the server.
     *
     * <p>The ceiling itself is not written here: {@link HealthCeiling#apply} owns that rule (a
     * permanent attribute modifier under a deterministic id, and "a maximum of zero removes the
     * modifier rather than writing one"), and it is the same object the respawn hook calls. Copying
     * those lines into this class is how the two paths would drift apart.
     *
     * <p>What this method adds on top is the clamp, and the reason it is needed: the screen is a
     * remote control, so a hand crafted or replayed packet may carry any two integers at all. Current
     * health is pinned between zero and the requested maximum, and the result is pinned again against
     * the entity's real maximum, which the requested pair says nothing about.
     *
     * @param player  the requesting player, may be {@code null}
     * @param payload the requested edit
 */
    private static void applyHealthEdit(ServerPlayer player, HealthEditPayload payload) {
        if (player == null) {
            return;
        }
        if (!PermissionGate.canOpenHealthEditor(player)) {
            player.sendSystemMessage(Component.translatable("message.merlinlib.no_permission"));
            return;
        }
        if (!(player.level().getEntity(payload.targetId()) instanceof LivingEntity target)) {
            return;
        }
        int max = Math.max(0, payload.max());
        int current = Math.max(0, Math.min(max, payload.current()));

        HealthCeiling.apply(target, max);
        target.setHealth(Math.min(current, target.getMaxHealth()));
    }
}
