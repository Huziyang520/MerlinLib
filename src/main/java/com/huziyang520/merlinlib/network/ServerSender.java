// The library's network channel, and the sink client screens send through.

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

public final class ServerSender {
    private static final String PROTOCOL_VERSION = "1";
    private static final String CHANNEL_NAME = "main";
    private static final int ID_DAMAGE_FEEDBACK = 0;
    private static final int ID_HEALTH_EDIT = 1;
    private static final int ID_ITEM_EDIT = 2;
    private static final int ID_OPEN_TOOL = 3;
    private static volatile SimpleChannel channel;
    private static volatile Consumer<Object> sink;

    private ServerSender() {
    }

    public static synchronized void register() {
        if (channel != null) {
            return;
        }

        SimpleChannel built = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Constants.MOD_ID, CHANNEL_NAME),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

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

        built.registerMessage(ID_OPEN_TOOL, OpenToolPayload.class,
                OpenToolPayload::write,
                OpenToolPayload::read,
                ServerSender::onOpenTool,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        channel = built;
        Constants.LOG.info("[MerlinLib] network channel '{}' registered with {} message(s)",
                CHANNEL_NAME, 4);
    }

    public static void setSink(Consumer<Object> sender) {
        sink = sender;
    }

    public static void send(Object payload) {
        Consumer<Object> active = sink;
        if (active != null) {
            active.accept(payload);
            return;
        }
        SimpleChannel local = channel;
        if (local == null) {
            throw new IllegalStateException(
                    "[MerlinLib] a payload was sent before the network channel was registered");
        }
        local.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, DamageFeedbackPayload payload) {
        SimpleChannel local = channel;
        if (local == null || player == null) {
            return;
        }
        local.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToPlayer(ServerPlayer player, OpenToolPayload payload) {
        SimpleChannel local = channel;
        if (local == null || player == null) {
            Constants.LOG.warn("[MerlinLib] the toolkit request could not be sent: the network channel is absent");
            return;
        }
        local.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    private static void onOpenTool(OpenToolPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ToolOpenSink.handle(payload));
        context.setPacketHandled(true);
    }

    private static void onDamageFeedback(DamageFeedbackPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DamageFeedbackSink.handle(payload));
        context.setPacketHandled(true);
    }

    private static void onHealthEdit(HealthEditPayload payload, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> applyHealthEdit(player, payload));
        context.setPacketHandled(true);
    }

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
