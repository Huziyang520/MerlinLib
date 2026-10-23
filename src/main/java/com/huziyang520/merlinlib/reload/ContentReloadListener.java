package com.huziyang520.merlinlib.reload;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentError;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.impl.ContentManager;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;

/**
 * Re-reads the MerlinLib <b>switch configuration</b> on every resource reload, covering server startup
 * and the vanilla {@code /reload} command.
 */
public final class ContentReloadListener extends SimplePreparableReloadListener<List<ContentError>> {

    /** Id this listener is registered under, so other reload listeners can order against it. */
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "content");

    private static final ContentReloadListener INSTANCE = new ContentReloadListener();

    private ContentReloadListener() {
    }

    /**
     * @return the single instance, which is what {@link #install()} registers
 */
    public static ContentReloadListener instance() {
        return INSTANCE;
    }

    /**
     * Registers this listener with the loader, so it runs on server startup and on every
     * {@code /reload}.
     *
     * <p>Separate from the instance because registration needs the loader bridge, which is available
     * from mod construction onwards. The bridge itself ignores a repeated registration of the same id,
     * so calling this more than once is harmless.
 */
    public static void install() {
        Services.PACK.registerServerReloadListener(ID, INSTANCE);
        Constants.LOG.debug("[MerlinLib] content reload listener registered as {}", ID);
    }

    /**
     * Re-reads the switch files, off the game thread.
     *
     * @param resourceManager the reload's resource manager, unused - the config is not in a pack
     * @param profiler        the preparation profiler, unused
     * @return the configuration problems found, for {@link #apply} to summarise
 */
    @Override
    protected List<ContentError> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // Switch files only. See the class Javadoc for why re-reading the content files here would be
        // a lie rather than an update on this version.
        ConfigManager.reload();
        return ConfigManager.errors();
    }

    /**
     * Logs the outcome once the reload barrier has been passed, so the log order stays readable.
     *
     * <p>Only a summary is printed: {@code ConfigManager.reload()} already logs each problem in full,
     * and repeating them here would double every line.
     *
     * @param errors the problems reported by {@link #prepare}
     * @param resourceManager the reload's resource manager, unused
     * @param profiler        the application profiler, unused
 */
    @Override
    protected void apply(List<ContentError> errors, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (errors == null || errors.isEmpty()) {
            Constants.LOG.info("[MerlinLib] configuration reloaded, no problems found");
            return;
        }
        Constants.LOG.warn("[MerlinLib] configuration reloaded with {} problem(s), see the lines above",
                errors.size());
    }

    /**
     * @return the name this listener is shown under in reload diagnostics.
 */
    @Override
    public String getName() {
        return Constants.MOD_NAME + " content";
    }

    /**
     * @return the report of the last content build, for {@code /merlinlib content} style diagnostics.
 */
    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
