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

/**
 * The health editor item: sneak and use it on a living entity to open the health editor for it.
 *
 * <p>The screen is opened on the client only, through an opener installed by the client entrypoint, so
 * this class stays free of client references and is safe on a dedicated server.
 *
 * <p>1.20.1 carries this over with one deletion: the 26.3 line had to give the item properties an
 * explicit id ({@code Properties#setId(ResourceKey)}), because 1.21 moved an item's registry key into
 * its properties. This version takes the id from the {@code DeferredRegister} call instead, which is
 * both simpler and the only thing the registry event looks at.
 */
public final class HealthEditorItem extends Item {

    private static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "health_editor");

    /** Client side opener; {@code null} on a dedicated server. */
    private static volatile Consumer<LivingEntity> opener;

    private HealthEditorItem(Properties properties) {
        super(properties);
    }

    /** Registers the item; the instance is built inside the registration callback (see {@link TestWeapons}). */
    public static void register() {
        Services.REGISTRATIONS.register(BuiltInRegistries.ITEM, ID, () -> new HealthEditorItem(
                new Item.Properties().stacksTo(1)));
    }

    /**
     * Installs the client side opener.
     *
     * @param consumer opens the health editor screen for the given entity
     */
    public static void setOpener(Consumer<LivingEntity> consumer) {
        opener = consumer;
    }

    /** @return the stack of the health editor item, empty before registration. */
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

    /**
     * Sneak-using with nothing under the crosshair edits the player himself.
     *
     * <p>{@code interactLivingEntity} only fires when an entity is under the crosshair, so "sneak use
     * to edit yourself" - the behaviour the screen's own hint line promises - has to be answered here
     * instead. Sneaking also makes the game skip the block interaction, so aiming at a block arrives
     * here as well: the description's "no target in reach" case.
     *
     * <p>Only the sneak path is answered; without it the item stays out of the way and vanilla's own
     * right click runs, which is why this returns {@code PASS} rather than opening anything.
     *
     * <p>{@code use} hands back an {@link InteractionResultHolder} on this version (the plain
     * {@code InteractionResult} return arrives later), so success and pass are wrapped in it - the
     * holder carries the stack the swing was made with, which is this item's own hand.
     *
     * @param level  the level
     * @param player the player using the item
     * @param hand   the hand holding it
     * @return a successful holder when the editor was opened for the player himself
     */
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
