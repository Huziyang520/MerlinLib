package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * The health editor item: sneak and use it on a living entity to open the health editor for it.
 *
 * <p>The screen is opened on the client only, through an opener installed by the loader's client
 * entrypoint, so this class stays free of client references and is safe on a dedicated server.
 */
public final class HealthEditorItem extends Item {

    private static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "health_editor");

    /** Client side opener; {@code null} on a dedicated server. */
    private static volatile Consumer<LivingEntity> opener;

    private HealthEditorItem(Properties properties) {
        super(properties);
    }

    /** Registers the item; the instance is built inside the registration callback (see TestWeapons). */
    public static void register() {
        Services.REGISTRATIONS.register(BuiltInRegistries.ITEM, ID, () -> new HealthEditorItem(
                new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).stacksTo(1)));
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
        Item item = BuiltInRegistries.ITEM.getValue(ID);
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
}
