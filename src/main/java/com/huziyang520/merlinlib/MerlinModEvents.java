package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Forge events MerlinLib subscribes to on the <b>mod</b> event bus.
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MerlinModEvents {

    private MerlinModEvents() {
    }

    /**
     * Adds MerlinLib's own entries to the vanilla creative tabs.
 *
     * <p>Fired once per tab while the creative screen's contents are being built.
 *
     * <p>Note that this map is Forge's own {@code MutableHashedLinkedMap} rather than a
     * {@code Map}: its membership test is {@code contains}, not {@code containsKey}, and it is the
     * map the event hands over rather than a copy.
 *
     * @param event the tab being built
 */
    @SubscribeEvent
    public static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (MerlinCreativeEntries.TOOLS_TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.testWeapons()) {
                addOnce(event, stack);
            }
            return;
        }
        if (MerlinCreativeEntries.TAB.equals(event.getTabKey())) {
            for (ItemStack stack : MerlinCreativeEntries.enchantedBooks()) {
                addOnce(event, stack);
            }
        }
    }

    /**
     * Adds a stack to a tab unless the tab already holds it.
     *
     * @param event the tab being built
     * @param stack the stack to offer
 */
    private static void addOnce(BuildCreativeModeTabContentsEvent event, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (event.getEntries().contains(stack)) {
            Constants.LOG.debug("[MerlinLib] {} is already in the creative tab, not adding it twice", stack);
            return;
        }
        event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
