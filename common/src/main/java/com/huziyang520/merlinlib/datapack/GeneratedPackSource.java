package com.huziyang520.merlinlib.datapack;

import com.huziyang520.merlinlib.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlags;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Repository source that offers the generated datapack to the game.
 *
 * <p>The pack is declared as required and hidden: it is always active (so {@code /reload} keeps it),
 * and it never shows up in the datapack or resource pack selection screens.
 */
public final class GeneratedPackSource implements RepositorySource {

    public static final GeneratedPackSource INSTANCE = new GeneratedPackSource();

    /**
     * The version this pack reports to the game alongside its id.
     *
     * <p>Deliberately a fixed string and not the mod's own version: the pair is an identity, and an identity that
     * changes on every release would stop matching the packs older worlds recorded.
     */
    private static final String KNOWN_PACK_VERSION = "1";

    private static final Pack.ResourcesSupplier SUPPLIER = new Pack.ResourcesSupplier() {
        @Override
        public PackMetadataResources openMetadata(PackLocationInfo location) {
            return new GeneratedPack(location);
        }

        @Override
        public Stream<PackResources> openResources(PackLocationInfo location, Pack.Metadata metadata) {
            return Stream.of(new GeneratedPack(location));
        }
    };

    private GeneratedPackSource() {
    }

    private static final java.util.concurrent.atomic.AtomicBoolean ANNOUNCED = new java.util.concurrent.atomic.AtomicBoolean();

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        if (ANNOUNCED.compareAndSet(false, true)) {
            Constants.LOG.info("[MerlinLib] the generated datapack source was accepted by the pack repository");
        }
        PackLocationInfo location = new PackLocationInfo(
                Constants.GENERATED_PACK_ID,
                Component.translatable("pack." + Constants.MOD_ID + ".generated"),
                PackSource.BUILT_IN,
                // The pack has to name itself to the game, and this is not cosmetic: vanilla marks every registry entry
                // with the lifecycle of the pack that provided it, and a pack it does not recognise counts as an
                // experimental datapack. That single empty optional is what made every world using this library report
                // "minecraft:enchantment(Experimental)" and ask for confirmation on every load. Declaring the id keeps
                // this pack's enchantments stable like vanilla's own.
                Optional.of(new net.minecraft.server.packs.repository.KnownPack(
                        Constants.MOD_ID, "generated", KNOWN_PACK_VERSION))
        );
        // The 4 argument constructor is the one vanilla itself provides; the pack is kept out of the
        // selection screens through PackSelectionConfig instead.
        Pack.Metadata metadata = new Pack.Metadata(
                Component.translatable("pack." + Constants.MOD_ID + ".generated.description"),
                PackCompatibility.COMPATIBLE,
                FeatureFlags.VANILLA_SET,
                List.<String>of()
        );
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);
        consumer.accept(new Pack(location, SUPPLIER, metadata, selection));
    }

    /**
     * @return the pack type the generated content belongs to.
     */
    public static PackType packType() {
        return PackType.SERVER_DATA;
    }
}
