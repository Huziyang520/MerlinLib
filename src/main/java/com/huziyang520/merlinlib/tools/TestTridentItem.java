// The testing trident, with the one extra hook a custom trident needs to render like the vanilla one.

package com.huziyang520.merlinlib.tools;

import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

public class TestTridentItem extends TridentItem {
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
