// Holds every enchantment declared through the java api and the ids that are disabled, and turns each draft into the ...

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

public final class EnchantmentRegistry implements EnchantmentApi {
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

    public static Enchantment implement(EnchantmentDraft draft) {
        return new MerlinEnchantment(draft);
    }

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
            this.supportedReferences = new ArrayList<>(EnchantmentJson.references(draft.supportedItems()));
            this.discoverable = draft.acquisition().discoverable();
            this.tradeable = draft.acquisition().villagerTrade() && !draft.acquisition().treasureOnly();
            this.treasureOnly = draft.acquisition().treasureOnly();
            this.curse = draft.acquisition().curse();
        }

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

        public int anvilCost() {
            return this.draft.anvilCost();
        }

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

        @Override
        public boolean isDiscoverable() {
            return this.discoverable;
        }

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

        private static boolean matchesTag(ItemStack stack, ResourceLocation reference) {
            if (!"minecraft".equals(reference.getNamespace())) {
                return stack.is(ItemTags.create(reference));
            }
            net.minecraft.world.item.Item item = stack.getItem();
            return switch (reference.getPath()) {
                case "enchantable/weapon", "enchantable/melee_weapon" -> stack.is(ItemTags.SWORDS)
                        || stack.is(ItemTags.AXES);
                case "enchantable/sword" -> stack.is(ItemTags.SWORDS);
                case "enchantable/sharp_weapon" -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES);
                case "enchantable/axe" -> stack.is(ItemTags.AXES);
                case "enchantable/fire_aspect", "enchantable/sweeping" ->
                        item instanceof net.minecraft.world.item.SwordItem;

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

                case "enchantable/mining" -> item instanceof net.minecraft.world.item.DiggerItem;
                case "enchantable/mining_loot" -> stack.is(ItemTags.PICKAXES)
                        || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.AXES);
                case "enchantable/fishing" -> stack.is(net.minecraft.world.item.Items.FISHING_ROD);
                case "enchantable/trident" -> item instanceof net.minecraft.world.item.TridentItem;
                case "enchantable/bow" -> stack.is(net.minecraft.world.item.Items.BOW);
                case "enchantable/crossbow" -> stack.is(net.minecraft.world.item.Items.CROSSBOW);
                case "enchantable/durability" -> item.canBeDepleted();
                case "enchantable/equippable", "enchantable/vanishing" -> true;
                default -> stack.is(ItemTags.create(reference));
            };
        }
    }

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

    public static EnchantmentDraft submitUnbreakable(EnchantmentBuilder builder) {
        return INSTANCE.submit(builder.build());
    }

    public static void submitRegistration(EnchantmentDraft draft) {
        Enchantment implemented = implement(draft);

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
