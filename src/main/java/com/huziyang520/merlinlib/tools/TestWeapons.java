package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The three testing weapons, each one a <b>subclass of its vanilla counterpart</b>.
 *
 * <h2>Why this class looks nothing like its 26.3 counterpart, and is far simpler</h2>
 *
 * <p>On 26.3 a sword, an axe and a spear were plain {@code Item}s whose entire behaviour came from item
 * components, because the classes had been replaced by data. Building one there meant writing out
 * vanilla's own component values by hand, and two attempts failed before that worked:
 *
 * <ol>
 *   <li>copying {@code Items.IRON_SWORD}'s component map crashed with
 *       {@code NullPointerException: Components not bound yet} - components are bound at registry
 *       binding, long after mod construction;</li>
 *   <li>calling vanilla's {@code sword()} / {@code axe()} helpers crashed with
 *       {@code IllegalStateException: Registry is already frozen}, because they begin by acquiring a
 *       bootstrap registry lookup that only exists inside vanilla's own bootstrap window.</li>
 * </ol>
 *
 * <p><b>None of that exists on 1.20.1.</b> {@link SwordItem}, {@link AxeItem}, {@link TridentItem} and
 * {@link Tiers} are all real classes here, they take their damage and attack speed as plain numbers in
 * the constructor, and no registry lookup is involved at all. The three weapons are therefore three
 * constructor calls, and the delayed-component mechanism, the frozen-registry crash chain and the
 * "which component did vanilla set" archaeology all simply do not apply. This is the simplification the
 * migration plan predicted, and it is worth stating plainly here so nobody re-derives the 26.3
 * workarounds on a version that does not need them.
 *
 * <h2>The spear is not here</h2>
 *
 * <p>26.3 had a fourth weapon, {@code test_spear}. The spear is a 1.21.11 weapon; 1.20.1 has no
 * spear item, no spear behaviour and no {@code minecraft:spears} tag to join. It is deliberately not
 * ported - not omitted by oversight - and the migration plan calls this out as a hard constraint.
 */
public final class TestWeapons {

    /**
     * UUID of the attack damage modifier, which is the one the editor writes back.
     *
     * <p>Vanilla's own constant, {@code Item.BASE_ATTACK_DAMAGE_UUID} ({@code CB3F55D3-645C-4F38-A497-9C13A33DB5CF}),
     * is {@code protected} on 1.20.1 and cannot be read from another package - it only became public
     * later. The value is therefore written out here, and it has to be this exact UUID rather than one
     * of our own: the tooltip shows a modifier under vanilla's id as the familiar "Attack Damage" row,
     * and reusing the id also means a stack written by vanilla and a stack written by the editor are
     * the same shape.
     */
    private static final UUID DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");

    /**
     * UUID of the attack speed modifier, vanilla's own, written out for the same reason.
     */
    private static final UUID SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * The damage modifier id older builds wrote.
     *
     * <p>The 26.3 line wrote its own modifier id ({@code merlinlib:test_weapon_damage}) into the item's
     * attribute component on some builds, which is why an id has to be recognised at all. On 1.20.1 the
     * modifiers live in NBT keyed by UUID rather than by name, and the id a stack carries is always
     * {@link #DAMAGE_MODIFIER} - a named id from the component era cannot appear in a 1.20.1 world. The
     * constant is kept as the record of what the old builds meant, not as a second accepted spelling.
     */
    @SuppressWarnings("unused")
    private static final ResourceLocation LEGACY_DAMAGE_MODIFIER =
            new ResourceLocation(Constants.MOD_ID, "test_weapon_damage");

    /** Which vanilla weapon a testing weapon mirrors. */
    private enum Kind {
        SWORD,
        AXE,
        TRIDENT
    }

    /**
     * One weapon definition.
     *
     * @param id          item id
     * @param kind        the vanilla counterpart whose behaviour is mirrored
     * @param damage      nominal attack damage shown in the tooltip; the constructor takes this minus the
     *                    player base damage of 1
     * @param attackSpeed attack speed modifier, vanilla style: the player base is 4.0, so -2.4 shows 1.6
     */
    private record WeaponSpec(ResourceLocation id, Kind kind, int damage, double attackSpeed) {
    }

    private static final Map<ResourceLocation, WeaponSpec> SPECS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    static {
        // Declared in the order the plan lists them, which is also the creative tab order.
        add("test_sword", Kind.SWORD, 7, -2.4D);
        add("test_axe", Kind.AXE, 9, -3.1D);
        add("test_trident", Kind.TRIDENT, 9, -2.9D);
    }

    private TestWeapons() {
    }

    private static void add(String path, Kind kind, int damage, double attackSpeed) {
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, path);
        SPECS.put(id, new WeaponSpec(id, kind, damage, attackSpeed));
    }

    /**
     * Registers all three weapons.
     *
     * <p>Called from the bootstrap. The item is deliberately built inside the supplier rather than
     * before it: Forge freezes its registries before mod constructors run, and constructing an
     * {@code Item} registers it into a creative-tab bookkeeping map, so the supplier is the only safe
     * place - the same rule the 26.3 line followed, for the same reason.
     */
    public static void register() {
        for (WeaponSpec spec : SPECS.values()) {
            Services.REGISTRATIONS.register(BuiltInRegistries.ITEM, spec.id(), () -> {
                Item item = create(spec);
                ITEMS.put(spec.id(), item);
                Constants.LOG.debug("[MerlinLib] registered test weapon {} (vanilla {}, damage {})",
                        spec.id(), spec.kind().name().toLowerCase(Locale.ROOT), spec.damage());
                return item;
            });
        }
    }

    /**
     * Builds one weapon.
     *
     * <p>The damage and attack speed passed to the constructor are the <em>modifier</em> amounts, not
     * the nominal ones: vanilla adds the player's own base of 1 damage and 4.0 attack speed, so the
     * nominal damage is reduced by one and the attack speed is used as given. The tooltip arithmetic
     * the editor reads back is the inverse of this, in {@link #nominalDamage(ItemStack)}.
     *
     * @param spec the weapon to build
     * @return the item, ready to be registered
     */
    private static Item create(WeaponSpec spec) {
        Item.Properties properties = new Item.Properties()
                // Netherite durability and iron enchantability, matching what 26.3 chose so the
                // weapons behave the same in the editor's ceiling tests on both lines.
                //
                // NO stacksTo(1) HERE, and it is not an omission. Item.Properties#stacksTo throws
                // "Unable to have damage AND stack." when maxDamage is already set, and durability()
                // sets maxStackSize to 1 itself - so the call is both redundant and fatal. It threw
                // during the common_setup registry event and took the whole game down:
                //   java.lang.RuntimeException: Unable to have damage AND stack.
                //     at net.minecraft.world.item.Item$Properties.stacksTo(Item.java)
                //     at com.huziyang520.merlinlib.tools.TestWeapons.create(TestWeapons.java:163)
                //     ... MerlinLib (merlinlib) encountered an error during the common_setup event phase
                // Anything damageable is unstackable by construction, so the correct code is to say
                // nothing about the stack size at all.
                .durability(Tiers.NETHERITE.getUses());

        float modifierDamage = spec.damage() - 1.0F;
        float modifierSpeed = (float) spec.attackSpeed();

        return switch (spec.kind()) {
            case SWORD -> new SwordItem(Tiers.IRON, (int) modifierDamage, modifierSpeed, properties);
            case AXE -> new AxeItem(Tiers.IRON, modifierDamage, modifierSpeed, properties);
            // TridentItem takes no tier: throwing, riptide and its melee damage are all vanilla's own,
            // which is precisely what a testing trident should reproduce. The subclass exists for one
            // reason only - the vanilla in-hand look is a hard-coded renderer branch keyed on
            // Items.TRIDENT, so a custom trident has to supply its own renderer to not fall back to the
            // flat sprite; see TestTridentItem.
            case TRIDENT -> new TestTridentItem(properties.rarity(Rarity.RARE));
        };
    }

    /**
     * The vanilla item tags a testing weapon has to join to be treated as its counterpart.
     *
     * <p>This is the fix for "why can't I put any enchantment on them?": on 1.20.1 the enchanting table
     * and the anvil decide whether an enchantment may go on an item by asking the item's
     * {@code EnchantmentCategory}, and for a modded item that category comes from the tags the item
     * joins. Vanilla's {@code swords} / {@code axes} tags are what a {@code SwordItem} / {@code AxeItem}
     * would normally be in; without them the weapon is an unknown category to both blocks and every
     * enchantment is refused.
     *
     * <p>The tags are written as <b>static resources</b> -
     * {@code data/minecraft/tags/items/*.json} in this project - because 1.20.1 has no runtime data pack
     * generation here, which is decision Q5 of the migration plan.
     *
     * <p>Note the plural {@code items} directory name: 1.20.1 uses {@code tags/items/}, and
     * {@code tags/item/} (the 1.21 spelling) is silently ignored - a mistake that produces exactly the
     * "enchantments are refused" symptom this method exists to prevent.
     *
     * @param id the weapon id
     * @return the tag ids to join, empty when the id is not a testing weapon
     */
    public static Optional<List<ResourceLocation>> tagMembership(ResourceLocation id) {
        WeaponSpec spec = SPECS.get(id);
        if (spec == null) {
            return Optional.empty();
        }
        List<String> tags = switch (spec.kind()) {
            case SWORD -> List.of("minecraft:swords");
            case AXE -> List.of("minecraft:axes");
            // A trident is in neither the sword nor the axe tag, and 1.20.1 has no `enchantable/*`
            // tag family at all - that arrives with the data driven enchantment registry in 1.21.
            // The one vanilla tag a trident does belong to here is `breaks_decorated_pots`, and it
            // is the only one worth joining: the repair material is not a tag on this version,
            // `TridentItem#isValidRepairItem` hard-codes it.
            case TRIDENT -> List.of("minecraft:breaks_decorated_pots");
        };
        return Optional.of(tags.stream().map(ResourceLocation::new).toList());
    }

    /** @return ids of every testing weapon, in declaration order. */
    public static List<ResourceLocation> ids() {
        return List.copyOf(SPECS.keySet());
    }

    /** @return the vanilla counterpart name of a testing weapon, empty when the id is unknown. */
    public static Optional<String> vanillaCounterpart(ResourceLocation id) {
        WeaponSpec spec = SPECS.get(id);
        return spec == null ? Optional.empty() : Optional.of(spec.kind().name().toLowerCase(Locale.ROOT));
    }

    /** @return the registered item, empty when the id is not a testing weapon. */
    public static Optional<Item> item(ResourceLocation id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    /** @return the stack for a weapon id, empty when unknown. */
    public static Optional<ItemStack> stack(ResourceLocation id) {
        return item(id).map(ItemStack::new);
    }

    /**
     * @param stack the item stack to inspect
     * @return {@code true} when this stack is one of the testing weapons
     */
    public static boolean isTestWeapon(ItemStack stack) {
        return spec(stack) != null;
    }

    /**
     * The weapon spec behind a stack.
     *
     * <p>Recognised by the item itself, not by what the stack happens to carry. The 26.3 line learned
     * this the hard way: recognising a testing weapon by its attribute modifier made a weapon in an
     * existing world look like an ordinary item and its damage row disappear from the editor. An item
     * identity is permanent; a modifier is not.
     *
     * @param stack the stack to inspect, may be {@code null}
     * @return the spec, or {@code null} when the stack is not one of the testing weapons
     */
    private static WeaponSpec spec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? null : SPECS.get(id);
    }

    /**
     * Reads the nominal damage of an item.
     *
     * <p>On 1.20.1 the attribute modifiers live in the stack's NBT under {@code AttributeModifiers},
     * keyed by the modifier's UUID, so the read is a lookup of vanilla's own base attack damage id.
     * The nominal value is the modifier amount plus the player's base of one, which is the number the
     * tooltip shows.
     *
     * @param stack the stack to read
     * @return the damage, empty when the item carries no attack damage modifier
     */
    public static Optional<Integer> nominalDamage(ItemStack stack) {
        return weaponDamage(stack);
    }

    /**
     * The base attack damage of any item that declares one, not only of a testing weapon.
     *
     * @param stack the stack to read
     * @return the damage, empty when the item carries no attack damage modifier
     */
    public static Optional<Integer> weaponDamage(ItemStack stack) {
        return damageModifierAmount(stack).map(amount -> (int) Math.round(amount + 1.0D));
    }

    /**
     * @param stack the stack to inspect
     * @return {@code true} when the stack's attack damage can be read and written
     */
    public static boolean hasEditableDamage(ItemStack stack) {
        return damageModifierAmount(stack).isPresent();
    }

    /**
     * Writes a new base attack damage onto any item that declares one.
     *
     * @param stack  the stack to modify
     * @param damage the new damage
     * @return {@code true} when the value was applied
     */
    public static boolean setWeaponDamage(ItemStack stack, int damage) {
        if (stack == null || stack.isEmpty() || damageModifierAmount(stack).isEmpty()) {
            return false;
        }
        return writeDamage(stack, damage);
    }

    /**
     * Writes a new nominal damage onto a testing weapon.
     *
     * @param stack  the stack to modify, must be a testing weapon
     * @param damage the new nominal damage
     * @return {@code true} when the value was applied
     */
    public static boolean setNominalDamage(ItemStack stack, int damage) {
        if (!isTestWeapon(stack)) {
            return false;
        }
        return writeDamage(stack, damage);
    }

    /**
     * Reads the attack damage modifier out of a stack.
     *
     * <p>1.20.1 has no {@code ItemAttributeModifiers} component, so
     * {@link ItemStack#getAttributeModifiers(EquipmentSlot)} is the reader - and it is the right one
     * here rather than a raw NBT walk: it merges what the item declares with what the stack stores,
     * which is exactly the number the tooltip prints as "Attack Damage". The 26.3 line read a
     * component because that was the only storage it had; on this version the merged view is both
     * simpler and closer to what the player sees.
     *
     * <p>The vanilla base modifier is preferred when it is present, because that is the one the
     * editor writes back. Any other attack damage entry is used as a fallback, so a weapon from
     * another mod that uses its own modifier id is still editable rather than reported as having no
     * damage at all.
     *
     * @param stack the stack
     * @return the modifier amount, empty when the stack has no attack damage entry
     */
    private static Optional<Double> damageModifierAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        com.google.common.collect.Multimap<Attribute, AttributeModifier> modifiers =
                stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        Double fallback = null;
        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entries()) {
            if (!Attributes.ATTACK_DAMAGE.equals(entry.getKey())) {
                continue;
            }
            if (DAMAGE_MODIFIER.equals(entry.getValue().getId())) {
                return Optional.of(entry.getValue().getAmount());
            }
            if (fallback == null) {
                fallback = entry.getValue().getAmount();
            }
        }
        return Optional.ofNullable(fallback);
    }

    /**
     * Rewrites the attack damage modifier of a stack, keeping every other modifier untouched.
     *
     * <p>Written with {@link ItemStack#addAttributeModifier(Attribute, AttributeModifier,
     * EquipmentSlot)}, which is this version's public door for the vanilla {@code AttributeModifiers}
     * NBT list.
     *
     * <h2>Why the stale entry is removed by hand first</h2>
     *
     * <p>1.20.1's {@code addAttributeModifier} only ever <b>appends</b> to that list - its bytecode ends
     * in {@code ListTag.add} and there is no remover on this version, neither on {@code ItemStack} nor on
     * Forge's item stack extension. A second edit therefore left two entries with the same UUID, and the
     * readers - {@code ItemStack#getAttributeModifiers} and {@link #damageModifierAmount} - answer with
     * the first of them, which is the stale one. The stack's real attribute did change on every write,
     * but the editor read the old value back, which is the "damage can only be edited once" report.
     *
     * <p>The entry is matched by {@code UUID}: it is the key vanilla's own {@code AttributeModifier#save}
     * writes (together with {@code Name}, {@code Amount} and {@code Operation}) and the only one that
     * identifies a modifier. The list key is spelled out because 1.20.1 exposes no constant for it.
     *
     * @param stack  the stack to modify
     * @param damage the new damage
     * @return {@code true} when the modifier was written
     */
    private static boolean writeDamage(ItemStack stack, int damage) {
        removeStaleDamageModifier(stack);
        stack.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(DAMAGE_MODIFIER, "Weapon modifier", damage - 1.0D,
                        AttributeModifier.Operation.ADDITION),
                EquipmentSlot.MAINHAND);
        return true;
    }

    /**
     * Drops every attribute modifier entry that carries the base attack damage UUID.
     *
     * @param stack the stack to clean
     */
    private static void removeStaleDamageModifier(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ATTRIBUTE_MODIFIERS, Tag.TAG_LIST)) {
            return;
        }
        ListTag modifiers = tag.getList(TAG_ATTRIBUTE_MODIFIERS, Tag.TAG_COMPOUND);
        modifiers.removeIf(entry -> entry instanceof CompoundTag compound
                && compound.hasUUID("UUID")
                && DAMAGE_MODIFIER.equals(compound.getUUID("UUID")));
        if (modifiers.isEmpty()) {
            tag.remove(TAG_ATTRIBUTE_MODIFIERS);
        }
    }

    /** The NBT list holding a stack's attribute modifiers; 1.20.1 exposes no constant for it. */
    private static final String TAG_ATTRIBUTE_MODIFIERS = "AttributeModifiers";
}
