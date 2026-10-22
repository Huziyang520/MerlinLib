package com.huziyang520.merlinlib.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Picks the HUD mixin that matches the running Minecraft build.
 *
 * <h2>Why a plugin is needed at all</h2>
 *
 * <p>This branch covers 26.1 and 26.2 with one jar, and Mojang moved the HUD class between them:
 *
 * <ul>
 *   <li><b>26.1.2</b>: the HUD lives in {@code net.minecraft.client.gui.Gui}, and there is no
 *       {@code net.minecraft.client.gui.Hud} class at all;</li>
 *   <li><b>26.2</b>: the HUD moved back to {@code net.minecraft.client.gui.Hud}, and {@code Gui} became the
 *       screen manager.</li>
 * </ul>
 *
 * <p>A single {@code @Mixin(Gui.class)} therefore applies on 26.1.2 and fails on 26.2 with
 * {@code @Redirect ... could not find any targets matching 'extractPlayerHealth' or 'extractHealthLevel' in
 * net/minecraft/client/gui/Gui}, which aborts client startup - the "26.2 cannot enter the game" report. Two
 * mixins with both targets is not enough either: a declared target class that does not exist is a hard error,
 * so on 26.1.2 the {@code Hud} mixin would crash instead.
 *
 * <p>This plugin answers the question before Mixin resolves a target: {@code MixinInfo.readDeclaredTargets}
 * filters the declared targets through {@link #shouldApplyMixin} and drops the ones this method rejects, so a
 * mixin aimed at a class that does not exist is never loaded. The check itself is a classpath lookup rather
 * than {@code Class.forName}, because loading a Minecraft class from a mixin plugin would transform it too
 * early and trip Mixin's own "target was loaded too early" guard.
 */
public final class MerlinMixinPlugin implements IMixinConfigPlugin {

    /** The HUD class name as it appears on the classpath, used as the version probe. */
    private static final String HUD_RESOURCE = "net/minecraft/client/gui/Hud.class";

    /** The mixin for 26.1 and earlier, where {@code Gui} owns the HUD. */
    private static final String MIXIN_HUD_ON_GUI = "com.huziyang520.merlinlib.mixin.MixinHud";

    /** The mixin for 26.2 and later, where {@code Hud} owns the HUD. */
    private static final String MIXIN_HUD_ON_HUD = "com.huziyang520.merlinlib.mixin.MixinHudModern";

    /** Cached once: the answer cannot change while the game runs. */
    private static Boolean hudClassPresent;

    @Override
    public void onLoad(String mixinPackage) {
        // Nothing to prepare: the only input is the classpath, and it is read on demand.
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean separateHud = hasSeparateHudClass();
        if (MIXIN_HUD_ON_GUI.equals(mixinClassName)) {
            return !separateHud;
        }
        if (MIXIN_HUD_ON_HUD.equals(mixinClassName)) {
            return separateHud;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        // No cross-config coordination.
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
        // No per-class preparation.
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
        // No per-class cleanup.
    }

    /**
     * @return {@code true} when the running build has the HUD in its own class, which is 26.2 and later
     */
    private static boolean hasSeparateHudClass() {
        if (hudClassPresent == null) {
            hudClassPresent = MerlinMixinPlugin.class.getClassLoader().getResource(HUD_RESOURCE) != null;
        }
        return hudClassPresent;
    }
}
