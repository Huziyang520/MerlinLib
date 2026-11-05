// /merlinlib layers：直接给分层效果叠层，用于测试（原版 /effect 不会叠同一实例）。

package com.huziyang520.merlinlib.command;

import com.huziyang520.merlinlib.effect.LayeredEffect;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;

public final class LayerCommand {
    private LayerCommand() {
    }

    public static int apply(CommandSourceStack source, String effectId, int layers, int seconds) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("command.merlinlib.layers.player_only"));
            return 0;
        }
        LayeredEffect effect = LayeredEffect.find(effectId);
        if (effect == null) {
            source.sendFailure(Component.translatable("command.merlinlib.layers.unknown", effectId));
            return 0;
        }
        Holder<MobEffect> holder = effect.display();
        if (holder == null) {
            source.sendFailure(Component.translatable("command.merlinlib.layers.unknown", effectId));
            return 0;
        }
        int ticks = seconds * 20;
        for (int i = 0; i < layers; i++) {
            effect.push(player, ticks);
        }
        effect.syncDisplay(player, holder);
        int total = effect.count(player);
        source.sendSuccess(() -> Component.translatable("command.merlinlib.layers.done",
                effectId, total, seconds), false);
        return total;
    }
}
