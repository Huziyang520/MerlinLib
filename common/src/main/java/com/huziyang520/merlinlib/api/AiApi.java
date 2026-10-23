package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.ai.AiRouter;

/**
 * Mob behaviour hooks, reached through {@link MerlinApi#ai()}.
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
