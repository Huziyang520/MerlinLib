// Forge entrypoint of MerlinLib for Minecraft 1.20.1.

package com.huziyang520.merlinlib;

import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.BuiltInContent;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.loot.LootInjector;
import com.huziyang520.merlinlib.mixin.RangedAttributeAccessor;
import com.huziyang520.merlinlib.notice.NoticeManager;
import com.huziyang520.merlinlib.platform.Services;
import com.huziyang520.merlinlib.tools.HealthEditorItem;
import com.huziyang520.merlinlib.tools.TestWeapons;
import com.huziyang520.merlinlib.util.SmeltingLookup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
public class MerlinLibForge {
    public MerlinLibForge() {
        Constants.LOG.info(
                "MerlinLib is starting on {} ({})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName()
        );
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.huziyang520.merlinlib.config.ForgeConfig.install();
            com.huziyang520.merlinlib.client.MerlinConfigEntry.register();
        }

        Services.PACK.bootstrap(modBus);
        Services.REGISTRATIONS.bootstrap(modBus);
        Services.LIFECYCLE.bootstrap(modBus);
        Services.LOOT.bootstrap(modBus);
        LootInjector.install();
        NoticeManager.install();
        com.huziyang520.merlinlib.network.ServerSender.register();
        com.huziyang520.merlinlib.reload.ContentReloadListener.install();
        ConfigManager.reload();
        com.huziyang520.merlinlib.config.NoticeSettings.init();
        com.huziyang520.merlinlib.notice.BuiltInNotices.register();
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

    private static void raiseCeiling(net.minecraft.world.entity.ai.attributes.Attribute attribute, String name) {
        if (attribute instanceof RangedAttributeAccessor accessor) {
            accessor.merlinlib$setMaxValue(Integer.MAX_VALUE);
            Constants.LOG.info("[MerlinLib] the {} ceiling was raised to the integer limit", name);
        }
    }
}
