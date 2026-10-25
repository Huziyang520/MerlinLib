// The health editor item: sneak and use it on a living entity to open the health editor for it.

package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

public final class HealthEditorItem extends Item {
    private static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "health_editor");
    private static volatile Consumer<LivingEntity> opener;

    private HealthEditorItem(Properties properties) {
        super(properties);
    }

    public static void register() {
        Services.REGISTRATIONS.register(BuiltInRegistries.ITEM, ID, () -> new HealthEditorItem(
                new Item.Properties().stacksTo(1)));
    }

    public static void setOpener(Consumer<LivingEntity> consumer) {
        opener = consumer;
    }

    public static ItemStack stack() {
        Item item = BuiltInRegistries.ITEM.get(ID);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                                                  InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            Consumer<LivingEntity> active = opener;
            if (active != null && player.level().isClientSide()) {
                active.accept(target);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            Consumer<LivingEntity> active = opener;
            if (active != null && player.level().isClientSide()) {
                active.accept(player);
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
