package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.tools.MerlinCreativeEntries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Forge events MerlinLib subscribes to on the <b>mod</b> event bus.
 *
 * <h2>Why this is a separate class from {@link MerlinGameEvents}</h2>
 *
 * <p>Forge has two event buses and an event belongs to exactly one of them. A handler registered on
 * the wrong bus does not fail - it simply never runs, which is the worst failure mode there is: the
 * feature is silently absent and the log says nothing.
 *
 * <p>{@code @Mod.EventBusSubscriber} defaults to {@code Bus.FORGE}, the game bus. This class handles
 * {@link BuildCreativeModeTabContentsEvent}, which is a mod bus event (it implements
 * {@code IModBusEvent}), so the bus is named explicitly here. The first version of this code put this
 * handler and {@code RegisterCommandsEvent} in one class under a single annotation, which would have
 * silently dropped every creative tab entry. They are two classes now, one per bus, and each names
 * its bus on the annotation so the mistake cannot be repeated by accident.
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
     * <p><b>Deduplication is not optional here.</b> The 26.3 line recorded a defect where a mod that
     * added a stack twice produced a duplicate entry, and on Forge the same mistake is worse: the
     * entries live in a keyed map, and re-adding an existing stack replaces the position of the first
     * one, which shows up as an entry that jumps around between openings. The map is therefore asked
     * first and an entry already present is skipped. The check is on the stack, because that is the
     * map's key: {@code ItemStack} implements the equality the map needs by comparing item and NBT.
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
