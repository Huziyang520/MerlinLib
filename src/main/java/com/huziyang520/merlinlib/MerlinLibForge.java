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

/**
 * Forge entrypoint of MerlinLib for Minecraft 1.20.1.
 */
@Mod(Constants.MOD_ID)
public class MerlinLibForge {

    public MerlinLibForge() {
        Constants.LOG.info(
                "MerlinLib is starting on {} ({})",
                Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.getEnvironmentName()
        );

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // The mod list's Config button. Forge draws it only for a mod that registered a
        // ConfigScreenHandler.ConfigScreenFactory, so the extension point is registered here, on the client
        // only, and only when there is a screen to show: Cloth Config's when Cloth is installed, Jupiter's
        // own otherwise (see MerlinConfigEntry). With neither installed the button is absent and the
        // switches are edited in config/MerlinLib/client.toml and server.toml, which are commented for it.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            com.huziyang520.merlinlib.config.ForgeConfig.install();
            com.huziyang520.merlinlib.client.MerlinConfigEntry.register();
        }

        // Every bridge that needs a loader handle gets one here, before anything can ask it to
        // register. REGISTRATIONS keeps the bus so that a DeferredRegister created later - which is
        // every one the content layer makes - is attached and flushed correctly.
        Services.PACK.bootstrap(modBus);
        Services.REGISTRATIONS.bootstrap(modBus);
        Services.LIFECYCLE.bootstrap(modBus);
        Services.LOOT.bootstrap(modBus);

        // Loot rules are read when a table loads, so the injector only has to be attached once.
        LootInjector.install();
        // Join notices: registered from here, so a business mod may have registered its own before or after.
        NoticeManager.install();
        // The network channel has to exist before anything sends on it, and before FML locks the
        // registry once the network phase is over - a channel registered after that throws
        // "Registration of impl channels is locked". register() is idempotent.
        com.huziyang520.merlinlib.network.ServerSender.register();
        // The /reload listener, for the switch files. It deliberately does not re-register content:
        // the 1.20.1 enchantment registry is frozen after startup (see the class for why).
        com.huziyang520.merlinlib.reload.ContentReloadListener.install();

        // The configuration has to be read before the content files are, because the server switches
        // decide whether the config file enchantments are registered at all.
        ConfigManager.reload();
        // After the switches, not before: the notice's own default is the client side one.
        com.huziyang520.merlinlib.notice.BuiltInNotices.register();
        widenAttributeCeilings();
        MerlinApi.lifecycle().onServerStarting(SmeltingLookup::initialize);

        // Built-in content goes through the public api, exactly like a dependent mod's content.
        BuiltInContent.register();
        TestWeapons.register();
        HealthEditorItem.register();

        // The content files are read and their effective enchantments queued for registration. This is
        // done during construction rather than in the common setup event, and the reason is worth
        // stating: FMLCommonSetupEvent is fired through enqueueWork on the main thread, and the
        // registry events run on the mod loading thread while that work is still queued. Queuing
        // content there would produce DeferredRegister entries that are created after the registry
        // event has already passed, which Forge reports as a missing registry value rather than as
        // the ordering mistake it is. Reading them here has none of that risk: nothing in this phase
        // touches a registry, it only builds objects and hands them to the deferred registers.
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
     * @param attribute the attribute
     * @param name      its name, for the log line that proves the widening happened
 */
    private static void raiseCeiling(net.minecraft.world.entity.ai.attributes.Attribute attribute, String name) {
        if (attribute instanceof RangedAttributeAccessor accessor) {
            accessor.merlinlib$setMaxValue(Integer.MAX_VALUE);
            Constants.LOG.info("[MerlinLib] the {} ceiling was raised to the integer limit", name);
        }
    }
}
