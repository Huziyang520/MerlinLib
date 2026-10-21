package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentApi;
import com.huziyang520.merlinlib.api.EnchantmentBuilder;
import com.huziyang520.merlinlib.api.EnchantmentInfo;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds every enchantment declared through the java api and the ids that are disabled.
 *
 * <p>Only registration state lives here; the merged, finally effective content lives in
 * {@link ContentManager} because it also depends on the config files.
 */
public final class EnchantmentRegistry implements EnchantmentApi {

    /** Shared instance used by {@code MerlinApi}. */
    public static final EnchantmentRegistry INSTANCE = new EnchantmentRegistry();

    private final Map<Identifier, EnchantmentDraft> apiEntries = new LinkedHashMap<>();
    private final Set<Identifier> disabled = new LinkedHashSet<>();
    private final AtomicLong revision = new AtomicLong();

    EnchantmentRegistry() {
    }

    @Override
    public EnchantmentBuilder register(Identifier id) {
        Objects.requireNonNull(id, "id");
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("illegal enchantment id '" + id + "', the namespace must match ^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$ and the path only a-z 0-9 _ - . /");
        }
        return new EnchantmentBuilder(this, id);
    }

    @Override
    public boolean disable(Identifier id) {
        Objects.requireNonNull(id, "id");
        synchronized (this.disabled) {
            if (!this.disabled.add(id)) {
                return false;
            }
        }
        this.apiEntries.remove(id);
        this.revision.incrementAndGet();
        return true;
    }

    @Override
    public boolean enable(Identifier id) {
        Objects.requireNonNull(id, "id");
        synchronized (this.disabled) {
            if (!this.disabled.remove(id)) {
                return false;
            }
        }
        this.revision.incrementAndGet();
        return true;
    }

    @Override
    public Set<Identifier> disabledIds() {
        synchronized (this.disabled) {
            return Set.copyOf(this.disabled);
        }
    }

    @Override
    public Set<Identifier> registeredIds() {
        synchronized (this.apiEntries) {
            return Set.copyOf(this.apiEntries.keySet());
        }
    }

    @Override
    public Optional<EnchantmentInfo> describe(Identifier id) {
        Objects.requireNonNull(id, "id");
        EnchantmentDraft draft = ContentManager.current().enchantments().get(id);
        if (draft != null) {
            return Optional.of(new EnchantmentInfo(id, draft.source(), true, draft.maxLevel(), draft.weight()));
        }
        if (this.disabledIds().contains(id)) {
            return Optional.of(new EnchantmentInfo(id, ContentSource.API, false, 0, 0));
        }
        return Optional.empty();
    }

    /**
     * Stores a draft built by {@link EnchantmentBuilder#submit()}.
     *
     * @param draft the validated draft
     * @return the stored draft
     * @throws IllegalArgumentException when a field is out of the range the vanilla codec accepts
     */
    public EnchantmentDraft submit(EnchantmentDraft draft) {
        Objects.requireNonNull(draft, "draft");
        validate(draft);
        synchronized (this.apiEntries) {
            EnchantmentDraft previous = this.apiEntries.put(draft.id(), draft);
            if (previous != null) {
                Constants.LOG.warn("[MerlinLib] {} was registered twice through the api, the later registration wins", draft.id());
            }
        }
        synchronized (this.disabled) {
            this.disabled.remove(draft.id());
        }
        this.revision.incrementAndGet();
        Constants.LOG.debug("[MerlinLib] registered enchantment {} through the api", draft.id());
        return draft;
    }

    /**
     * Rejects values the vanilla codec would refuse at load time, so the error surfaces where the
     * mistake was made instead of during datapack loading.
     */
    public static void validate(EnchantmentDraft draft) {
        if (draft.weight() < 1 || draft.weight() > 1024) {
            throw new IllegalArgumentException(draft.id() + ": weight must be in [1, 1024] but was " + draft.weight());
        }
        if (draft.maxLevel() < 1 || draft.maxLevel() > 255) {
            throw new IllegalArgumentException(draft.id() + ": maxLevel must be in [1, 255] but was " + draft.maxLevel());
        }
        if (draft.anvilCost() < 0) {
            throw new IllegalArgumentException(draft.id() + ": anvilCost must not be negative but was " + draft.anvilCost());
        }
        if (draft.minCostBase() < 0 || draft.minCostPerLevel() < 0 || draft.maxCostBase() < 0 || draft.maxCostPerLevel() < 0) {
            throw new IllegalArgumentException(draft.id() + ": costs must not be negative");
        }
        if (draft.slots().isEmpty()) {
            throw new IllegalArgumentException(draft.id() + ": at least one slot is required, e.g. \"mainhand\" or \"any\"");
        }
        if (draft.supportedItems() == null) {
            throw new IllegalArgumentException(draft.id() + ": supportedItems is required");
        }
        if (draft.description() == null) {
            throw new IllegalArgumentException(draft.id() + ": description is required");
        }
    }

    Map<Identifier, EnchantmentDraft> apiEntries() {
        synchronized (this.apiEntries) {
            return Map.copyOf(this.apiEntries);
        }
    }

    long revision() {
        return this.revision.get();
    }
}
