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
 *
 * <h2>What this listener deliberately does not do</h2>
 *
 * <p>On 26.3 this class re-ran the whole content pipeline: {@code ConfigManager.reload()} was followed
 * by {@code ContentManager.refresh()}, which rebuilt the snapshot and regenerated the data pack that
 * the game then read. Adding, changing or removing an enchantment therefore took effect on
 * {@code /reload}, and that was the feature the listener existed to deliver.
 *
 * <p><b>That is impossible on 1.20.1.</b> An enchantment here is an entry in a static game registry
 * that Forge freezes before mod constructors run and re-opens only for the length of the registry
 * event; {@code ContentManager.refresh()} is what submits those entries through a
 * {@code DeferredRegister}, so it can run exactly once, during mod construction, and never again.
 * Calling it from a reload listener would not update anything - it would try to register a second
 * time, after the freeze.
 *
 * <p>So this listener re-reads the switches and nothing else. The reduction is stated here rather
 * than hidden: <b>editing a content file in {@code config/MerlinLib/} requires a game restart on
 * 1.20.1</b>, while editing {@code client.toml} or {@code server.toml} still takes effect on
 * {@code /reload}, because every consumer reads {@link ConfigManager} at the moment it acts. This is
 * the same boundary the README and the project plan record, and it is the honest form of the 26.3
 * contract rather than a silent loss of it.
 *
 * <h2>Why {@code SimplePreparableReloadListener} and not the raw interface</h2>
 *
 * <p>The 26.3 signature is the 1.20.2+ one ({@code reload(SharedState, Executor, PreparationBarrier,
 * Executor)}); this version's interface is {@code reload(PreparationBarrier, ResourceManager,
 * ProfilerFiller, ProfilerFiller, Executor, Executor)}. The abstract base class implements that for
 * us and splits it into {@link #prepare} and {@link #apply}, which is the same two phase shape the
 * 26.3 code hand rolled - and it preserves the property that matters: the files are read on the
 * <em>prepare</em> executor, never on the game thread, and the log line is written after the barrier
 * has been passed. {@code ResourceManagerReloadListener} would have been shorter, but it runs its
 * body on the game executor, which is exactly the behaviour the 26.3 class was written to avoid.
 *
 * <p>Nothing here reads the {@link ResourceManager}: the MerlinLib configuration lives in the
 * {@code config} directory, not in a resource pack, which is why the parameter goes unused.
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
     *
     * <p>Note the tense, and the difference from 26.3: that report describes the <em>registration</em>
     * performed at mod construction, and this listener does not change it. A reader who wants to know
     * whether the files on disk still match what was registered should compare
     * {@code ContentManager.registeredFingerprint()} against the live
     * {@code ConfigDirectory.fingerprint()}, which is what the command does. The method is kept under
     * its 26.3 name because the command and the diagnostics call it.
     */
    public ContentReport lastReport() {
        return ContentManager.lastReport().orElse(ContentReport.EMPTY);
    }
}
