package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.BuiltInContent;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.TestWeapons;
import com.huziyang520.merlinlib.mixin.RangedAttributeAccessor;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.loot.LootInjector;
import com.huziyang520.merlinlib.notice.NoticeManager;
import com.mojang.serialization.Lifecycle;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
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
        // Join notices: registered from here, so a business mod may have registered its own before or after.
        NoticeManager.install();
        // Says which feature flags put a world in the "experimental settings" category. The generated datapack
        // declares none, so when this line appears the flag came from a data pack the world has selected - and
        // the log names it, instead of leaving it to be guessed.
        Services.LIFECYCLE.onServerStarted(server -> {
            // The screen the player sees ("this world uses experimental settings") is driven by the world's
            // generation lifecycle, not by the feature flags: WorldOpenFlows.confirmWorldCreation compares the
            // lifecycle, and that value is built from the registered dimensions and registries of the selected
            // packs (WorldDimensions.checkStability). Both are reported here, because the two have different
            // fixes and guessing between them costs a round trip each time.
            Lifecycle lifecycle = server.getWorldData().worldGenSettingsLifecycle();
            FeatureFlagSet flags = server.getWorldData().enabledFeatures();
            if (lifecycle != Lifecycle.stable()) {
                // Name the registries that are not stable: the lifecycle is the maximum over the dimensions and
                // over every registry the selected packs provide, and knowing which one is unstable is the whole
                // difference between fixing this and guessing at it again.
                StringBuilder unstable = new StringBuilder();
                server.registryAccess().registries().forEach(entry -> {
                    Lifecycle registryLifecycle = entry.value().registryLifecycle();
                    if (registryLifecycle != Lifecycle.stable()) {
                        unstable.append(entry.key().identifier()).append('(').append(registryLifecycle)
                                .append(") ");
                    }
                });
                Constants.LOG.warn("[MerlinLib] this save reports world gen lifecycle {}; non-stable registries: "
                                + "{}", lifecycle, unstable.isEmpty() ? "(none - it is the dimensions)" : unstable);
            }
            if (FeatureFlags.isExperimental(flags) || lifecycle != Lifecycle.stable()) {
                Constants.LOG.warn("[MerlinLib] this save reports world gen lifecycle {} and feature flags "
                                + "outside vanilla: {}. The lifecycle is what drives the 'experimental settings' "
                                + "warning; it becomes non-stable when the selected data packs register dimensions "
                                + "or registry entries that are not vanilla-like.",
                        lifecycle, FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, flags));
            }
        });
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
