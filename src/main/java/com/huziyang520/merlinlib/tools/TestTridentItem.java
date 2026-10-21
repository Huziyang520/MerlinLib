package com.huziyang520.merlinlib.tools;

import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The testing trident, with the one extra hook a custom trident needs to render like the vanilla one.
 *
 * <h2>Why a subclass is required at all</h2>
 *
 * <p>The vanilla trident's <em>in-hand</em> appearance is not a model file: {@code trident.json} is a flat
 * {@code item/generated} sprite, and the 3-D look in the hand comes from a hard-coded branch of
 * {@code ItemRenderer}/{@code BlockEntityWithoutLevelRenderer} that fires only for
 * {@code stack.is(Items.TRIDENT)}. Verified on 1.21.1 with
 * {@code javap -p -c net.minecraft.client.renderer.entity.ItemRenderer}: the branch reads
 * {@code getstatic Items.TRIDENT} and then {@code TRIDENT_IN_HAND_MODEL}. A modded trident is a
 * different item, so it falls through to the flat sprite everywhere - the "held trident is a texture,
 * thrown trident is fine" report.
 *
 * <p>NeoForge's door for exactly this is {@code Item#initializeClient} (still present on 1.21.1,
 * verified with {@code javap -p net.minecraft.world.item.Item}): it hands the item an
 * {@link IClientItemExtensions}, and a non-null {@code getCustomRenderer()} from there replaces the
 * baked-model path with a {@code BlockEntityWithoutLevelRenderer} of our own -
 * {@code com.huziyang520.merlinlib.client.TestTridentRenderer}.
 *
 * <h2>Where the client-only types live</h2>
 *
 * <p>{@code initializeClient} itself is executed on the client only, and the anonymous extensions object
 * it creates is what names the client-only renderer - so a dedicated server loads this class, never runs
 * the hook, and never touches the renderer.
 */
public class TestTridentItem extends TridentItem {

    /**
     * @param properties the item properties, as passed by {@link TestWeapons}
     */
    public TestTridentItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.huziyang520.merlinlib.client.TestTridentRenderer.instance();
            }
        });
    }
}
