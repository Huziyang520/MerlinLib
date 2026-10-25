// Collects the mods' AI hooks and answers the three questions for the mixins.

package com.huziyang520.merlinlib.ai;

import com.huziyang520.merlinlib.api.AiHook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AiRouter {
    private static final List<AiHook> HOOKS = new CopyOnWriteArrayList<>();
    private static volatile boolean fleeHooks;

    private AiRouter() {
    }

    public static void register(AiHook hook) {
        HOOKS.add(hook);
        fleeHooks = true;
    }

    public static boolean hasHooks() {
        return !HOOKS.isEmpty();
    }

    public static boolean hasFleeHooks() {
        return fleeHooks;
    }

    public static boolean vetoesTargeting(Mob mob, LivingEntity target) {
        for (AiHook hook : HOOKS) {
            if (!hook.allowsTargeting(mob, target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean vetoesAvoiding(Mob mob, LivingEntity entity) {
        for (AiHook hook : HOOKS) {
            if (!hook.allowsAvoiding(mob, entity)) {
                return true;
            }
        }
        return false;
    }

    public static void steerAway(Mob mob) {
        for (Player player : mob.level().players()) {
            if (player.isSpectator() || !player.isAlive() || mob.distanceToSqr(player) > 256.0D) {
                continue;
            }
            if (!wantsToFlee(mob, player)) {
                continue;
            }
            if (!mob.getNavigation().isDone()) {
                return;
            }
            Vec3 away = mob.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4D) {
                return;
            }
            Vec3 goal = mob.position().add(away.normalize().scale(6.0D));
            mob.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.0D);
            return;
        }
    }

    private static boolean wantsToFlee(Mob mob, LivingEntity entity) {
        for (AiHook hook : HOOKS) {
            if (hook.wantsToFlee(mob, entity)) {
                return true;
            }
        }
        return false;
    }
}
