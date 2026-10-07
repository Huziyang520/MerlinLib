package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.ai.AiRouter;

/**
 * Mob behaviour hooks, reached through {@link MerlinApi#ai()}.
 *
 * <h2>What it is for</h2>
 *
 * <p>Content that changes what a mob wants to do about a player - disguise, deterrents, taming effects - all
 * end up in the same few vanilla places. This is the one registration point for them, so a mod states its
 * rule instead of finding those places again:
 *
 * <pre>{@code
 * MerlinApi.ai().register(new AiHook() {
 *     @Override
 *     public boolean allowsTargeting(Mob mob, LivingEntity target) {
 *         return !(target instanceof Player player) || !wearsTheCharm(player, mob);
 *     }
 * });
 * }</pre>
 *
 * <h2>The rules that keep it honest</h2>
 *
 * <ul>
 *   <li>Hooks are asked <b>only when a mob is actually deciding something</b>, and the whole path is skipped
 *       when no mod registered a hook. Nothing is scanned per tick by default.</li>
 *   <li>A hook <b>decides, it does not act</b>: it is running inside the mob's tick or its goal selection, so
 *       it must be quick and must not change the world. The one action the library performs for a hook is the
 *       "keep away" steering, because that is the part nobody wants to write twice.</li>
 *   <li>Refusing a target refuses it for every reason vanilla has, including being hurt. A rule that should
 *       still let a mob fight back has to say so itself - the mob's own record of who hurt it
 *       ({@code getLastHurtByMob}) is the usual way.</li>
 * </ul>
 */
public final class AiApi {

    /** Single instance, handed out by {@link MerlinApi#ai()}. */
    public static final AiApi INSTANCE = new AiApi();

    private AiApi() {
    }

    /**
     * Registers a hook. Safe to call from a mod constructor.
     *
     * @param hook the hook, never {@code null}
     */
    public void register(AiHook hook) {
        AiRouter.register(hook);
    }
}
