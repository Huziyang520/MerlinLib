package com.huziyang520.merlinlib.ai;

import com.huziyang520.merlinlib.api.AiHook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collects the mods' AI hooks and answers the three questions for the mixins.
 *
 * <h2>Cost when nobody is listening</h2>
 *
 * <p>{@link #hasHooks()} is a plain boolean read and short circuits the whole path, so a server whose mods do
 * not use the feature never walks a mob list, and a mob that is not considering a player never asks anything.
 * The two questions that can be answered without a scan - targeting and avoiding - are asked directly; only
 * the "actively keep away" question needs to look at nearby players, and that is throttled to once a second
 * per mob so it cannot turn into a pathfinding cost of its own.
 *
 * <h2>Why a copy-on-write list</h2>
 *
 * <p>Registration happens while mods load, but a data pack or a late loader event can register later, and the
 * list is read from mob ticks on the server thread. Copy-on-write keeps a registration from ever being seen
 * half done, and removes nothing.
 *
 * <p>1.20.1 note: direct port. The members this class calls all exist unchanged on this version -
 * {@code Entity#level()}, {@code Level#players()} through {@code EntityGetter}, {@code Entity#distanceToSqr},
 * {@code PathNavigation#isDone()} and {@code PathNavigation#moveTo(double, double, double, double)} - so
 * nothing here is a rename and no mixin behaviour has to differ. The 26.3 line's mixins reach the same three
 * vanilla places on 1.20.1 ({@code Mob#setTarget}, {@code AvoidEntityGoal} and
 * {@code Cat$CatAvoidEntityGoal}), with the cat class at {@code net.minecraft.world.entity.animal.Cat}
 * instead of the {@code .animal.feline} package it moved to later.
 */
public final class AiRouter {

    /** The hooks registered by mods, in registration order. */
    private static final List<AiHook> HOOKS = new CopyOnWriteArrayList<>();

    /** Whether any hook cares about the "keep away" question, so its tick path can be skipped entirely. */
    private static volatile boolean fleeHooks;

    private AiRouter() {
    }

    /**
     * Registers a hook.
     *
     * @param hook the hook, never {@code null}
     */
    public static void register(AiHook hook) {
        HOOKS.add(hook);
        fleeHooks = true;
    }

    /** @return whether any hook is registered at all */
    public static boolean hasHooks() {
        return !HOOKS.isEmpty();
    }

    /** @return whether any hook answers the "keep away" question */
    public static boolean hasFleeHooks() {
        return fleeHooks;
    }

    /**
     * @param mob    the mob choosing a target
     * @param target the entity it is considering
     * @return true when some hook refuses the target
     */
    public static boolean vetoesTargeting(Mob mob, LivingEntity target) {
        for (AiHook hook : HOOKS) {
            if (!hook.allowsTargeting(mob, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param mob    the mob
     * @param entity the entity it considers avoiding
     * @return true when some hook refuses to let the mob flee
     */
    public static boolean vetoesAvoiding(Mob mob, LivingEntity entity) {
        for (AiHook hook : HOOKS) {
            if (!hook.allowsAvoiding(mob, entity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Steers a mob away from the nearest player some hook wants it to avoid.
     *
     * <p>Called from the mob's tick; the caller has already checked {@link #hasFleeHooks()}. The mob is only
     * moved when it currently has no path, so fleeing never fights with a mob's own goals (a creeper that is
     * already charging keeps charging).
     *
     * @param mob the mob
     */
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
