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
