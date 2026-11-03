// Loader independent bootstrap of MerlinLib.

package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.BuiltInContent;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.TestWeapons;
import com.huziyang520.merlinlib.mixin.RangedAttributeAccessor;
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.loot.LootInjector;
import com.huziyang520.merlinlib.api.NoticeMode;
import com.huziyang520.merlinlib.notice.NoticeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.serialization.Lifecycle;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.util.SmeltingLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MerlinLib {
    private static boolean initialized;

    private MerlinLib() {
    }

    private static void registerOwnNotices() {
        MerlinApi.notices().register(
                ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "config_ui"),
                NoticeMode.EVERY_JOIN,
                Component.translatable("merlinlib.notice.config_ui"));
    }

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
        NoticeManager.install();
        registerOwnNotices();

        Services.LIFECYCLE.onServerStarted(server -> {
            Lifecycle lifecycle = server.getWorldData().worldGenSettingsLifecycle();
            FeatureFlagSet flags = server.getWorldData().enabledFeatures();
            if (lifecycle != Lifecycle.stable()) {
                StringBuilder unstable = new StringBuilder();
                server.registryAccess().registries().forEach(entry -> {
                    Lifecycle registryLifecycle = entry.value().registryLifecycle();
                    if (registryLifecycle != Lifecycle.stable()) {
                        unstable.append(entry.key().location()).append('(').append(registryLifecycle)
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

    private static void widenAttributeCeilings() {
        raiseCeiling(Attributes.MAX_HEALTH, "max_health");
        raiseCeiling(Attributes.ATTACK_DAMAGE, "attack_damage");
    }

    private static void raiseCeiling(Holder<Attribute> attribute, String name) {
        if (attribute.value() instanceof RangedAttributeAccessor accessor) {
            accessor.merlinlib$setMaxValue(Integer.MAX_VALUE);
            Constants.LOG.info("[MerlinLib] the {} ceiling was raised to the integer limit", name);
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
