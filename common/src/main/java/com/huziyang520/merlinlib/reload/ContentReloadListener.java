// Re-reads the MerlinLib configuration on every resource reload, covering server startup and the vanilla /reload command.

package com.huziyang520.merlinlib.reload;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class ContentReloadListener implements PreparableReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "content");
    private static final ContentReloadListener INSTANCE = new ContentReloadListener();

    private ContentReloadListener() {
    }

    public static ContentReloadListener instance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<Void> reload(SharedState sharedState, Executor prepareExecutor,
                                          PreparationBarrier barrier, Executor gameExecutor) {
        return CompletableFuture
                .supplyAsync(() -> {
                    ConfigManager.reload();
                    return ContentManager.refresh();
                }, prepareExecutor)
                .thenCompose(barrier::wait)
                .thenAccept(report -> {
                    if (report.hasErrors()) {
                        Constants.LOG.warn("[MerlinLib] {} content definition(s) could not be loaded, see the report above",
                                report.errors().size());
                    }
                });
    }

    @Override
    public String getName() {
        return Constants.MOD_NAME + " content";
    }

    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
