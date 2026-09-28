package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.BuiltInContent;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.TestWeapons;
import com.huziyang520.merlinlib.mixin.RangedAttributeAccessor;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.loot.LootInjector;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.util.SmeltingLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Loader independent bootstrap of MerlinLib.
 *
 * <p>This class lives in the common project, so it may only touch vanilla code, libraries bundled
 * with vanilla and optional third party libraries that ship loader independent binaries. Anything
 * that needs loader concepts (events, networking, datapack mounting, permissions, key bindings)
 * has to go through the {@code platform.services} interfaces.
 *
 * <p>Every loader entrypoint calls {@link #init(Object)} exactly once when the loader is ready.
 */
public class MerlinLib {

    private static boolean initialized;

    private MerlinLib() {
    }

    /**
     * Boots the shared part of the mod. Safe to call more than once; only the first call has any
     * effect.
     *
     * @param loaderContext loader specific handle, currently the NeoForge mod event bus; pass
     *                      {@code null} on loaders without one
     */
    public static void init(Object loaderContext) {
        if (initialized) {
            return;
        }
        initialized = true;

        Constants.LOG.info(
                "MerlinLib is starting on {} ({})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName()
        );

        Services.PACK.bootstrap(loaderContext);
        Services.REGISTRATIONS.bootstrap(loaderContext);
        Services.LIFECYCLE.bootstrap(loaderContext);
        Services.LOOT.bootstrap(loaderContext);
        LootInjector.install();
        ConfigManager.reload();
        widenAttributeCeilings();
        MerlinApi.lifecycle().onServerStarting(SmeltingLookup::initialize);
        BuiltInContent.register();
        TestWeapons.register();
        HealthEditorItem.register();
        ContentManager.refresh();
    }

    /**
     * Raises the upper bound of the attributes the editors can reach, to the integer limit.
     *
     * <p>Vanilla declares these as ranged attributes with a ceiling of their own - 1024 for
     * {@code max_health} and 2048 for {@code attack_damage} - and every value a holder computes is clamped
     * to that ceiling, so an editor that lets the player type one million would silently end up at the
     * ceiling instead. The ceiling is a private final field with no setter, which is what the accessor
     * mixin is for; widening them once here is what makes the advertised range real.
     */
    private static void widenAttributeCeilings() {
        raiseCeiling(Attributes.MAX_HEALTH, "max_health");
        raiseCeiling(Attributes.ATTACK_DAMAGE, "attack_damage");
    }

    /**
     * Raises one attribute's ceiling.
     *
     * @param attribute the attribute holder
     * @param name      its name, for the log line that proves the widening happened
     */
    private static void raiseCeiling(Holder<Attribute> attribute, String name) {
        if (attribute.value() instanceof RangedAttributeAccessor accessor) {
            accessor.merlinlib$setMaxValue(Integer.MAX_VALUE);
            Constants.LOG.info("[MerlinLib] the {} ceiling was raised to the integer limit", name);
        }
    }

    /**
     * @return {@code true} once {@link #init(Object)} has completed.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
