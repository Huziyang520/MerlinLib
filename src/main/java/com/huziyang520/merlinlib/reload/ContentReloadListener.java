// Re-reads the MerlinLib switch configuration on every resource reload, covering server startup and the vanilla /reload ...

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

public final class ContentReloadListener extends SimplePreparableReloadListener<List<ContentError>> {
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "content");
    private static final ContentReloadListener INSTANCE = new ContentReloadListener();

    private ContentReloadListener() {
    }

    public static ContentReloadListener instance() {
        return INSTANCE;
    }

    public static void install() {
        Services.PACK.registerServerReloadListener(ID, INSTANCE);
        Constants.LOG.debug("[MerlinLib] content reload listener registered as {}", ID);
    }

    @Override
    protected List<ContentError> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        ConfigManager.reload();
        return ConfigManager.errors();
    }

    @Override
    protected void apply(List<ContentError> errors, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (errors == null || errors.isEmpty()) {
            Constants.LOG.info("[MerlinLib] configuration reloaded, no problems found");
            return;
        }
        Constants.LOG.warn("[MerlinLib] configuration reloaded with {} problem(s), see the lines above",
                errors.size());
    }

    @Override
    public String getName() {
        return Constants.MOD_NAME + " content";
    }

    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
