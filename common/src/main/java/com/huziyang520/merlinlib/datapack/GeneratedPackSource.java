// Repository source that offers the generated datapack to the game.

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

public final class GeneratedPackSource implements RepositorySource {
    public static final GeneratedPackSource INSTANCE = new GeneratedPackSource();
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

                Optional.of(new net.minecraft.server.packs.repository.KnownPack(
                        Constants.MOD_ID, "generated", KNOWN_PACK_VERSION))
        );

        Pack.Metadata metadata = new Pack.Metadata(
                Component.translatable("pack." + Constants.MOD_ID + ".generated.description"),
                PackCompatibility.COMPATIBLE,
                FeatureFlags.VANILLA_SET,
                List.<String>of()
        );
        PackSelectionConfig selection = new PackSelectionConfig(true, Pack.Position.TOP, true);
        consumer.accept(new Pack(location, SUPPLIER, metadata, selection));
    }

    public static PackType packType() {
        return PackType.SERVER_DATA;
    }
}
