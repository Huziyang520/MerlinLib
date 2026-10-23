package com.huziyang520.merlinlib.tools;

import net.minecraft.world.item.TridentItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The testing trident, with the one extra hook a custom trident needs to render like the vanilla one.
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
