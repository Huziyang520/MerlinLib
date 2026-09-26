package com.huziyang520.merlinlib.mixin;

import com.huziyang520.merlinlib.datapack.GeneratedPackSource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adds the MerlinLib generated pack to every pack repository.
 *
 * <p>Why a mixin: the pack has to be discovered before the first world load, and the only place that
 * is early enough is the construction of the repository. Fabric has no public api for appending a
 * repository source, and Fabric's built-in pack api only accepts packs that live inside a mod jar,
 * which cannot hold runtime generated content.
 *
 * <p>Vanilla stores the sources in an immutable set with no mutator, so the field is replaced by a
 * mutable copy that contains our extra source.
 *
 * <p>The pack is declared required and only serves {@code SERVER_DATA}: the client resource pack
 * repository sees a pack that contributes nothing and never lists anything.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Mutable
    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void merlinlib$addGeneratedPackSource(CallbackInfo callbackInfo) {
        Set<RepositorySource> merged = new LinkedHashSet<>(this.sources);
        merged.add(GeneratedPackSource.INSTANCE);
        this.sources = merged;
    }
}
