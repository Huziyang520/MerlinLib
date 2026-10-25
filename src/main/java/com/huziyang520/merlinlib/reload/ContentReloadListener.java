// Re-reads the MerlinLib configuration and rebuilds the generated content on every resource reload, covering server ...

package com.huziyang520.merlinlib.reload;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.config.ConfigManager;
import com.huziyang520.merlinlib.content.ContentReport;
import com.huziyang520.merlinlib.impl.ContentManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class ContentReloadListener extends SimplePreparableReloadListener<ContentReport> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "content");
    private static final ContentReloadListener INSTANCE = new ContentReloadListener();

    private ContentReloadListener() {
    }

    public static ContentReloadListener instance() {
        return INSTANCE;
    }

    @Override
    protected ContentReport prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        ConfigManager.reload();
        return ContentManager.refresh();
    }

    @Override
    protected void apply(ContentReport report, ResourceManager resourceManager, ProfilerFiller profiler) {
        if (report != null && report.hasErrors()) {
            Constants.LOG.warn("[MerlinLib] {} content definition(s) could not be loaded, see the report above",
                    report.errors().size());
        }
    }

    @Override
    public String getName() {
        return Constants.MOD_NAME + " content";
    }

    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
