// Mob behaviour hooks, reached through MerlinApi#ai().

package com.huziyang520.merlinlib.api;

import com.huziyang520.merlinlib.ai.AiRouter;

public final class AiApi {
    public static final AiApi INSTANCE = new AiApi();

    private AiApi() {
    }

    public void register(AiHook hook) {
        AiRouter.register(hook);
    }
}
