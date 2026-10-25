// Picks the HUD mixin that matches the running Minecraft build.

package com.huziyang520.merlinlib.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public final class MerlinMixinPlugin implements IMixinConfigPlugin {
    private static final String HUD_RESOURCE = "net/minecraft/client/gui/Hud.class";

    private static final String MIXIN_HUD_ON_GUI = "com.huziyang520.merlinlib.mixin.MixinHud";

    private static final String MIXIN_HUD_ON_HUD = "com.huziyang520.merlinlib.mixin.MixinHudModern";

    private static Boolean hudClassPresent;

    @Override
    public void onLoad(String mixinPackage) {
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
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }

    private static boolean hasSeparateHudClass() {
        if (hudClassPresent == null) {
            hudClassPresent = MerlinMixinPlugin.class.getClassLoader().getResource(HUD_RESOURCE) != null;
        }
        return hudClassPresent;
    }
}
