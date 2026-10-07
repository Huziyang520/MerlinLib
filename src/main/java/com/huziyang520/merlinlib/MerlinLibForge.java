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
import com.huziyang520.merlinlib.tools.ui.MerlinConfigScreen;
import com.huziyang520.merlinlib.util.SmeltingLookup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Forge entrypoint of MerlinLib for Minecraft 1.20.1.
 *
 * <h2>What this class is, next to the 26.3 line's {@code MerlinLib}</h2>
 *
 * <p>The 26.3 line had a loader independent {@code MerlinLib#init(Object)} called by a thin
 * {@code fabric/} and {@code neoforge/} entrypoint each. This project targets one loader, so the
 * entrypoint and the bootstrap are the same class and {@code init} is gone; every step it performed is
 * here, in the order it has to happen.
 *
 * <h2>The three phases, and why the order is not negotiable</h2>
 *
 * <ol>
 *   <li><b>Construction</b> (this constructor). The bridges are installed and the configuration is
 *       read. Nothing here touches a registry: Forge has already frozen them and re-opens them only
 *       for the registry events.</li>
 *   <li><b>Registration</b> ({@code RegisterEvent}, fired per registry by the mod event bus). The
 *       {@code DeferredRegister} entries created above are invoked here, while the registry is
 *       writable.</li>
 *   <li><b>After registration</b> ({@code FMLCommonSetupEvent}). Content is read from the config
 *       files and its effective enchantments are queued for registration. This <em>must</em> run
 *       before the registry events, which is why it is {@code FMLCommonSetupEvent} and not the
 *       common setup's {@code enqueueWork} - see the note on {@link #onCommonSetup}.</li>
 * </ol>
 *
 * <p>The one thing a reader should take away: on this version an enchantment has to be registered
 * while the game is starting, by a {@code DeferredRegister}. The 26.3 line wrote a data pack at
 * runtime instead, which is why its content pipeline could run at any moment and this one cannot.
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

        // The Forge mod list only offers a working "Config" button for a mod that registered a
        // config screen factory; without this line the button is dead. This is 1.20.1's form of the
        // extension point the 26.3 line registers as IConfigScreenFactory, and it keeps that line's
        // property that a dedicated server never loads the screen class: the supplier below is only
        // resolved when a client opens the mod list, so the client-side screen inside the inner
        // lambda is never touched on a server.
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new MerlinConfigScreen(parent)));

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
     * <p>1.20.1 declares {@code Attributes.MAX_HEALTH} as a plain {@link Attribute}, not as a
     * {@code Holder<Attribute>}: the holder based attribute registry arrives in 1.21. The 26.3 line
     * therefore had to unwrap with {@code attribute.value()}, and this line does not.
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
