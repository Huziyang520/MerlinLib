package com.huziyang520.merlinlib.impl;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.api.EnchantmentApi;
import com.huziyang520.merlinlib.api.EnchantmentBuilder;
import com.huziyang520.merlinlib.api.EnchantmentInfo;
import com.huziyang520.merlinlib.content.ContentSource;
import com.huziyang520.merlinlib.content.EnchantmentDraft;
import com.huziyang520.merlinlib.content.EnchantmentJson;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Holds every enchantment declared through the java api and the ids that are disabled, and turns each
 * draft into the {@link Enchantment} the game registers.
 *
 * <h2>Why this class looks nothing like its 26.3 counterpart</h2>
 *
 * <p>On 26.3 an enchantment was a data pack entry: this class only kept the declarations, and a
 * generated data pack was what made them exist. 1.20.1 has no data driven enchantment registry, so
 * the draft has to become a real {@code Enchantment} object, and the fields a data pack would have
 * carried have to be expressed as overridden methods. That translation is
 * {@link #implement(EnchantmentDraft)} below, and it is the single place where the two models meet.
 *
 * <p>Only registration state lives here; the merged, finally effective content lives in
 * {@link ContentManager} because it also depends on the config files.
 */
public final class EnchantmentRegistry implements EnchantmentApi {

    /** Shared instance used by {@code MerlinApi}. */
    public static final EnchantmentRegistry INSTANCE = new EnchantmentRegistry();

    private final Map<ResourceLocation, EnchantmentDraft> apiEntries = new LinkedHashMap<>();
    private final Set<ResourceLocation> disabled = new LinkedHashSet<>();
    private final AtomicLong revision = new AtomicLong();

    EnchantmentRegistry() {
    }

    @Override
    public EnchantmentBuilder register(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!IdValidator.isValid(id)) {
            throw new IllegalArgumentException("illegal enchantment id '" + id + "', the namespace must match ^(?=.{2,64}$)[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$ and the path only a-z 0-9 _ - . /");
        }
        return new EnchantmentBuilder(this, id);
    }

    @Override
    public boolean disable(ResourceLocation id) {
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
    public boolean enable(ResourceLocation id) {
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
    public Set<ResourceLocation> disabledIds() {
        synchronized (this.disabled) {
            return Set.copyOf(this.disabled);
        }
    }

    @Override
    public Set<ResourceLocation> registeredIds() {
        synchronized (this.apiEntries) {
            return Set.copyOf(this.apiEntries.keySet());
        }
    }

    @Override
    public Optional<EnchantmentInfo> describe(ResourceLocation id) {
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
     * @throws IllegalArgumentException when a field is out of the range the game accepts
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
     * Rejects values the game would refuse at registration time, so the error surfaces where the
     * mistake was made instead of during the registry event.
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
        if (draft.translationKey() == null) {
            throw new IllegalArgumentException(draft.id() + ": translationKey is required");
        }
    }

    /**
     * Turns a draft into the enchantment object the game will register.
     *
     * <p>Every field of the draft is carried over by overriding the method the 1.20.1 game actually
     * calls for it. The mapping is:
     *
     * <table>
     *   <tr><th>draft</th><th>1.20.1 method</th></tr>
     *   <tr><td>{@code weight}</td><td>{@code getRarity()} - this version takes a four band enum, so the number is mapped onto the nearest band by {@link EnchantmentDraft#rarity()}</td></tr>
     *   <tr><td>{@code maxLevel}</td><td>{@code getMaxLevel()}</td></tr>
     *   <tr><td>{@code minCost*}</td><td>{@code getMinCost(int)}</td></tr>
     *   <tr><td>{@code maxCost*}</td><td>{@code getMaxCost(int)}</td></tr>
     *   <tr><td>{@code anvilCost}</td><td>{@code getAnvilCost()}</td></tr>
     *   <tr><td>{@code slots}</td><td>the constructor's {@code EquipmentSlot[]}</td></tr>
     *   <tr><td>{@code supportedItems}</td><td>{@code canEnchant(ItemStack)}</td></tr>
     *   <tr><td>{@code primaryItems}</td><td>{@code isDiscoverable()} together with the acquisition flags</td></tr>
     *   <tr><td>{@code exclusiveSet}</td><td>{@code checkCompatibility(Enchantment)}</td></tr>
     *   <tr><td>{@code acquisition}</td><td>{@code isTreasureOnly()} / {@code isCurse()} / {@code isDiscoverable()} / {@code isTradeable()}</td></tr>
     * </table>
     *
     * @param draft the draft, already validated
     * @return a new enchantment instance, not yet in any registry
     */
    public static Enchantment implement(EnchantmentDraft draft) {
        return new MerlinEnchantment(draft);
    }

    /**
     * The concrete enchantment type.
     *
     * <p>A named class rather than an anonymous one so a crash report, a debugger and
     * {@code /merlinlib list} all name the same thing, and so the mixin that widens the level ceiling
     * has one type to recognise.
     */
    public static final class MerlinEnchantment extends Enchantment {

        private final EnchantmentDraft draft;
        private final Set<ResourceLocation> exclusive;
        private final List<String> supportedReferences;
        private final boolean discoverable;
        private final boolean tradeable;
        private final boolean treasureOnly;
        private final boolean curse;

        MerlinEnchantment(EnchantmentDraft draft) {
            super(draft.rarity(), draft.category(), draft.equipmentSlots());
            this.draft = draft;
            this.exclusive = readReferences(draft.exclusiveSet());
            // Every declared reference is kept, not just the first: the vanilla format allows a list
            // and a real definition uses it (Practical Enchantments' disguise names seven head
            // items). Keeping only one would make the enchantment apply to a single item type while
            // looking perfectly healthy everywhere else.
            this.supportedReferences = new ArrayList<>(EnchantmentJson.references(draft.supportedItems()));
            this.discoverable = draft.acquisition().discoverable();
            this.tradeable = draft.acquisition().villagerTrade() && !draft.acquisition().treasureOnly();
            this.treasureOnly = draft.acquisition().treasureOnly();
            this.curse = draft.acquisition().curse();
        }

        /** @return the draft this enchantment was created from, for diagnostics and the item editor */
        public EnchantmentDraft draft() {
            return this.draft;
        }

        @Override
        public String getDescriptionId() {
            return this.draft.translationKey();
        }

        @Override
        public int getMaxLevel() {
            return this.draft.maxLevel();
        }

        @Override
        public int getMinCost(int level) {
            return Math.max(0, this.draft.minCostBase() + this.draft.minCostPerLevel() * Math.max(0, level - 1));
        }

        @Override
        public int getMaxCost(int level) {
            return Math.max(0, this.draft.maxCostBase() + this.draft.maxCostPerLevel() * Math.max(0, level - 1));
        }

        /**
         * @return the level cost the anvil adds, computed from the declared {@code anvil_cost}.
         *
         * <p>1.20.1's {@code Enchantment} has <b>no</b> {@code getAnvilCost()} - that method arrives
         * with the data driven enchantment registry in 1.21, where the field has somewhere to live.
         * The value is not lost: {@code AnvilMenu} asks the enchantment for a cost through its own
         * path on this version, and the honest place to expose the declared number is a method of our
         * own (below) rather than an override of something that does not exist.
         */
        public int anvilCost() {
            return this.draft.anvilCost();
        }

        /**
         * @return whether the item can carry this enchantment, evaluated against the declared
         *         {@code supported_items} reference.
         *
         * <p>An item reference ("minecraft:diamond_sword") and a tag reference
         * ("#minecraft:enchantable/weapon") both come through here. A reference that cannot be parsed
         * or that names something the registry does not have matches nothing, and says so once in the
         * log rather than silently accepting every item.
         */
        @Override
        public boolean canEnchant(ItemStack stack) {
            if (this.supportedReferences.isEmpty()) {
                return false;
            }
            for (String reference : this.supportedReferences) {
                if (matches(stack, reference)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return whether this enchantment may be offered by the enchanting table.
         *
         * <p>{@code EnchantmentTableMenu} asks exactly this before offering an entry, so it is where
         * the {@code in_enchanting_table} channel of the acquisition record is honoured on 1.20.1.
         */
        @Override
        public boolean isDiscoverable() {
            return this.discoverable;
        }

        /**
         * @return whether librarian trades may offer this enchantment.
         *
         * <p>Vanilla reads this from the {@code tradeable} enchantment tag, which a
         * {@code DeferredRegister} entry cannot carry. Overriding it is the closest equivalent: it is
         * the same question asked of the same object, and any code that consults the enchantment
         * rather than the tag gets the declared answer.
         */
        @Override
        public boolean isTradeable() {
            return this.tradeable;
        }

        @Override
        public boolean isTreasureOnly() {
            return this.treasureOnly;
        }

        @Override
        public boolean isCurse() {
            return this.curse;
        }

        /**
         * @return whether this enchantment may sit on the same item as {@code other}.
         *
         * <p>This is the 1.20.1 answer to {@code exclusive_set}: vanilla's own method, called by the
         * anvil, the enchanting table and {@code ItemStack#enchant}. Both directions are checked,
         * because a declaration only lists one side of the pair and vanilla asks each enchantment
         * about the other.
         */
        @Override
        protected boolean checkCompatibility(Enchantment other) {
            if (!super.checkCompatibility(other)) {
                return false;
            }
            if (!this.exclusive.isEmpty() && this.exclusive.contains(idOf(other))) {
                return false;
            }
            if (other instanceof MerlinEnchantment merlin && merlin.excludes(this)) {
                return false;
            }
            return true;
        }

        /** @return whether this enchantment's own exclusive set names {@code other} */
        private boolean excludes(Enchantment other) {
            return !this.exclusive.isEmpty() && this.exclusive.contains(idOf(other));
        }

        private static ResourceLocation idOf(Enchantment enchantment) {
            return net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
        }

        private static boolean matches(ItemStack stack, String reference) {
            Optional<ResourceLocation> id = EnchantmentJson.idOf(reference);
            if (id.isEmpty()) {
                Constants.LOG.warn("[MerlinLib] supported_items reference '{}' is not a legal id and matches nothing", reference);
                return false;
            }
            if (reference.startsWith("#")) {
                return matchesTag(stack, id.get());
            }
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id.get());
            return item != null && stack.is(item);
        }

        /**
         * Resolves a {@code #namespace:tag} reference, translating the 1.21 {@code enchantable/*} family
         * when the tag does not exist on 1.20.1.
         *
         * <h2>Why a translation is needed at all</h2>
         *
         * <p>The 1.21 enchantment format describes what an enchantment may go on with tag references,
         * and the whole {@code minecraft:enchantable/*} family -
         * {@code enchantable/weapon}, {@code enchantable/sharp_weapon}, {@code enchantable/head_armor},
         * {@code enchantable/mining} and the rest - was introduced with it. <b>None of those tags exist
         * on 1.20.1.</b> A datapack or a ported config file that names one would match nothing at all,
         * which is the worst possible outcome: the enchantment registers, appears in every list, and
         * can never be applied to anything.
         *
         * <p>So each of those names is answered from the tag or the item class that carried the same
         * meaning on this version, which is what vanilla itself used before the family existed. The
         * translation is by name and is deliberately explicit rather than a pattern, so an unknown tag
         * still falls through to a real tag lookup instead of being guessed at.
         *
         * @param stack     the item
         * @param reference the tag id, without the leading {@code #}
         * @return whether the item belongs to that tag, or to the 1.20.1 equivalent of it
         */
        private static boolean matchesTag(ItemStack stack, ResourceLocation reference) {
            if (!"minecraft".equals(reference.getNamespace())) {
                return stack.is(ItemTags.create(reference));
            }
            net.minecraft.world.item.Item item = stack.getItem();
            return switch (reference.getPath()) {
                // The melee family.
                case "enchantable/weapon", "enchantable/melee_weapon" -> stack.is(ItemTags.SWORDS)
                        || stack.is(ItemTags.AXES);
                case "enchantable/sword" -> stack.is(ItemTags.SWORDS);
                // sharp_weapon is the sword-AND-axe family, not the sword-only one. The 1.21.11
                // definition, read out of that version's client jar, is
                //   {"values": ["#minecraft:enchantable/melee_weapon", "#minecraft:axes"]},
                // so falling back to swords alone silently kept axes out of every enchantment declared
                // with this family - seven of them in PE, "execute" among them, which is exactly the
                // "execute does not apply to axes" report. Axes are back in.
                case "enchantable/sharp_weapon" -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
                case "enchantable/axe" -> stack.is(ItemTags.AXES);
                case "enchantable/fire_aspect", "enchantable/sweeping" ->
                        item instanceof net.minecraft.world.item.SwordItem;
                // Armour, by the slot the piece is worn in. 1.20.1 has no "head armour" tag; the
                // armour class plus the equipment slot is what vanilla's own head-slot enchantments
                // are filtered by.
                case "enchantable/armor" -> item instanceof net.minecraft.world.item.ArmorItem;
                case "enchantable/head_armor", "enchantable/helmet" ->
                        item instanceof net.minecraft.world.item.ArmorItem armor
                                && armor.getEquipmentSlot() == EquipmentSlot.HEAD;
                case "enchantable/chest_armor", "enchantable/chestplate" ->
                        item instanceof net.minecraft.world.item.ArmorItem armor
                                && armor.getEquipmentSlot() == EquipmentSlot.CHEST;
                case "enchantable/leg_armor", "enchantable/leggings" ->
                        item instanceof net.minecraft.world.item.ArmorItem armor
                                && armor.getEquipmentSlot() == EquipmentSlot.LEGS;
                case "enchantable/foot_armor", "enchantable/boots" ->
                        item instanceof net.minecraft.world.item.ArmorItem armor
                                && armor.getEquipmentSlot() == EquipmentSlot.FEET;
                // Tools and the rest.
                case "enchantable/mining" -> item instanceof net.minecraft.world.item.DiggerItem;
                case "enchantable/mining_loot" -> stack.is(ItemTags.PICKAXES)
                        || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.AXES);
                case "enchantable/fishing" -> stack.is(net.minecraft.world.item.Items.FISHING_ROD);
                case "enchantable/trident" -> item instanceof net.minecraft.world.item.TridentItem;
                case "enchantable/bow" -> stack.is(net.minecraft.world.item.Items.BOW);
                case "enchantable/crossbow" -> stack.is(net.minecraft.world.item.Items.CROSSBOW);
                case "enchantable/durability" -> item.canBeDepleted();
                case "enchantable/equippable", "enchantable/vanishing" -> true;
                // Not part of the 1.21 family: a genuine 1.20.1 tag, resolved normally.
                default -> stack.is(ItemTags.create(reference));
            };
        }
    }

    /**
     * Reads an {@code exclusive_set} value, which may be a single reference or an array.
     *
     * @param element the json value, may be {@code null}
     * @return the ids named, empty when there is nothing to read
     */
    private static Set<ResourceLocation> readReferences(com.google.gson.JsonElement element) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (element == null || element.isJsonNull()) {
            return ids;
        }
        if (element.isJsonArray()) {
            for (com.google.gson.JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    EnchantmentJson.idOf(item.getAsString()).ifPresent(ids::add);
                }
            }
            return ids;
        }
        if (element.isJsonPrimitive()) {
            EnchantmentJson.idOf(element.getAsString()).ifPresent(ids::add);
        }
        return ids;
    }

    /**
     * @return the built-in {@code merlinlib:unbreakable} enchantment, or {@code null} when it is not in
     *         the registry.
     *
     * <p>Asked by {@code MixinItemStackUnbreakable} on every durability event, so the answer is
     * cached: the lookup is a map read, but it is a map read on the hottest path the library has.
     * The cache is filled on first use, which is after the registry event has run - the mixin only
     * runs once a game is being played.
     *
     * <p>{@code null} is a real answer and not an error: the enchantment can be disabled by a config
     * file, and the registry has not been populated during the very early phases of startup. Callers
     * must treat it as "no rule to enforce" rather than as a failure.
     */
    public static Enchantment unbreakable() {
        Enchantment cached = unbreakable;
        if (cached == null) {
            cached = net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT.get(
                    com.huziyang520.merlinlib.content.BuiltInContent.UNBREAKABLE);
            unbreakable = cached;
        }
        return cached;
    }

    private static volatile Enchantment unbreakable;

    /**
     * Registers {@code merlinlib:unbreakable}.
     *
     * <p>Separate from {@link #submit} only so the intent is named at the call site: this is the one
     * built-in enchantment, and the reason its behaviour lives in
     * {@code MixinItemStackUnbreakable} rather than in the declaration is written down in
     * {@code BuiltInContent}. Everything else goes through the ordinary path, so a config file can
     * still disable or override it like any other entry.
     *
     * @param builder the builder {@code BuiltInContent} filled in
     * @return the stored draft
     */
    public static EnchantmentDraft submitUnbreakable(EnchantmentBuilder builder) {
        return INSTANCE.submit(builder.build());
    }

    /**
     * Writes one effective draft into the game's enchantment registry.
     *
     * <p>This is the step that has no counterpart on the 26.3 line, and the reason the whole content
     * pipeline had to be reworked: there, a draft became a file in a generated data pack and the game
     * read it. Here it becomes a {@code DeferredRegister} entry, which is the only way to add to a
     * Forge registry - the registry is frozen before mod constructors run and re-opened only for the
     * length of the registry event.
     *
     * <p>Called from {@link ContentManager#refresh()} with the finally effective content, so a draft
     * that a config file overrode or disabled never reaches this method.
     *
     * @param draft the effective draft
     */
    public static void submitRegistration(EnchantmentDraft draft) {
        Enchantment implemented = implement(draft);
        // BuiltInRegistries.ENCHANTMENT, not Registries.ENCHANTMENT: the latter is the ResourceKey
        // that names the registry, and the bridge needs the registry itself.
        Services.REGISTRATIONS.register(net.minecraft.core.registries.BuiltInRegistries.ENCHANTMENT,
                draft.id(), () -> implemented);
        Constants.LOG.debug("[MerlinLib] queued enchantment {} for registration", draft.id());
    }

    Map<ResourceLocation, EnchantmentDraft> apiEntries() {
        synchronized (this.apiEntries) {
            return Map.copyOf(this.apiEntries);
        }
    }

    long revision() {
        return this.revision.get();
    }
}
