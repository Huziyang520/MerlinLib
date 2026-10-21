package com.huziyang520.merlinlib.reload;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Re-reads the MerlinLib configuration on every resource reload, covering server startup and the
 * vanilla {@code /reload} command.
 *
 * <p>The files are read on the prepare executor (never on the game thread) and the change report is
 * logged once the reload barrier has been passed, so the log order stays readable.
 */
public final class ContentReloadListener implements PreparableReloadListener {

    /** Id this listener is registered under, so other reload listeners can order against it. */
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
                    // Switch files are read first so the content build below already sees new limits.
                    ConfigManager.reload();
                    return ContentManager.refresh();
                }, prepareExecutor)
                .thenCompose(barrier::wait)
                .thenAccept(report -> {
                    // Nothing to push to the client here: clients receive the generated content through
                    // the normal registry synchronisation, and the switch files are synced separately.
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

    /**
     * @return the report of the last finished reload, for {@code /merlinlib content} style diagnostics.
     */
    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
