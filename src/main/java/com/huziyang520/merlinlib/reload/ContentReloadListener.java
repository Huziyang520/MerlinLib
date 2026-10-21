package com.huziyang520.merlinlib.reload;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Re-reads the MerlinLib configuration and rebuilds the generated content on every resource reload,
 * covering server startup and the vanilla {@code /reload} command.
 *
 * <p>On 1.21.1 this is a real content reload, exactly as on 26.3: enchantments are data driven
 * ({@code data/<namespace>/enchantment/<path>.json}), the generated pack is rebuilt from
 * {@link ContentManager}, and the registry asks the pack again after every reload. That is why the
 * listener calls {@code ContentManager.refresh()} and not merely {@code ConfigManager.reload()}.
 *
 * <h2>1.21.1: {@code SimplePreparableReloadListener}, not the 26.x {@code SharedState} signature</h2>
 *
 * <p>The 26.3 interface is {@code reload(SharedState, Executor, PreparationBarrier, Executor)}; this
 * line's is
 * {@code reload(PreparationBarrier, ResourceManager, ProfilerFiller, ProfilerFiller, Executor, Executor)}
 * (verified with {@code javap -p net.minecraft.server.packs.resources.PreparableReloadListener}). The
 * abstract base class implements that and splits it into {@link #prepare} and {@link #apply}, which is
 * the same two phase shape the 26.3 code hand rolled - and it keeps the property that matters: the
 * files are read on the <em>prepare</em> executor, never on the game thread, and the log line is
 * written after the barrier has been passed.
 */
public final class ContentReloadListener extends SimplePreparableReloadListener<ContentReport> {

    /** Id this listener is registered under, so other reload listeners can order against it. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "content");

    private static final ContentReloadListener INSTANCE = new ContentReloadListener();

    private ContentReloadListener() {
    }

    public static ContentReloadListener instance() {
        return INSTANCE;
    }

    /**
     * Re-reads the switch files and rebuilds the content, off the game thread.
     *
     * @param resourceManager the reload's resource manager, unused - the config is not in a pack
     * @param profiler        the preparation profiler, unused
     * @return what changed since the previous build
     */
    @Override
    protected ContentReport prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // Switch files are read first so the content build below already sees the new limits.
        ConfigManager.reload();
        return ContentManager.refresh();
    }

    /**
     * Logs the outcome once the reload barrier has been passed, so the log order stays readable.
     *
     * @param report          what changed, already logged in full by {@link ContentManager#refresh()}
     * @param resourceManager the reload's resource manager, unused
     * @param profiler        the application profiler, unused
     */
    @Override
    protected void apply(ContentReport report, ResourceManager resourceManager, ProfilerFiller profiler) {
        // Nothing to push to the client here: clients receive the generated content through the normal
        // registry synchronisation, and the switch files are synced separately.
        if (report != null && report.hasErrors()) {
            Constants.LOG.warn("[MerlinLib] {} content definition(s) could not be loaded, see the report above",
                    report.errors().size());
        }
    }

    /**
     * @return the name this listener is shown under in reload diagnostics.
     */
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
