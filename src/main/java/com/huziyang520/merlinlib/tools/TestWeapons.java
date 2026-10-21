package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The three testing weapons.
 *
 * <h2>1.21.1 sits between the two earlier lines</h2>
 *
 * <p>Unlike 26.3, 1.21.1 still has the real item classes - {@link SwordItem}, {@link AxeItem} and
 * {@link TridentItem} - so building one is a constructor call rather than a hand written set of
 * components, and none of the 26.3 workarounds (delayed components, "Components not bound yet") apply.
 * Unlike 1.20.1, the damage and attack speed are no longer constructor arguments: they live in the
 * {@code ATTRIBUTE_MODIFIERS} component, so the numbers are passed through
 * {@code Item.Properties#attributes(...)} exactly the way vanilla itself does it.
 *
 * <p>There is no spear: the spear item arrives in 1.21.11, so this line carries the sword, the axe and
 * the trident, the same three the 1.20.1 line carries.
 *
 * <h2>Why the attributes are written twice</h2>
 *
 * <p>{@link #damageModifiers} is what the item is born with; {@link #writeDamage} is what the editor
 * writes afterwards. Both use vanilla's own modifier ids ({@code minecraft:base_attack_damage} /
 * {@code minecraft:base_attack_speed}), so the tooltip shows the familiar "Attack Damage" row and a
 * stack edited by the toolkit is indistinguishable from one that shipped that way.
 */
public final class TestWeapons {

    /** The vanilla attack damage modifier id. */
    private static final ResourceLocation DAMAGE_MODIFIER = Item.BASE_ATTACK_DAMAGE_ID;

    /** The vanilla attack speed modifier id. */
    private static final ResourceLocation SPEED_MODIFIER = Item.BASE_ATTACK_SPEED_ID;

    /**
     * The damage modifier id older builds wrote.
     *
     * <p>Kept so a testing weapon created by one of those builds is still recognised and stays editable:
     * the id lives inside the item's component, so changing it in code never reaches an existing world.
     */
    private static final ResourceLocation LEGACY_DAMAGE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "test_weapon_damage");

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
     * @param damage      nominal attack damage shown in the tooltip, so the modifier is this minus the
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
        SPECS.put(id, new WeaponSpec(id, kind, damage, attackSpeed));
    }

    /**
     * Registers all three weapons.
     *
     * <p>The item must NOT be built here: constructing an {@code Item} touches the item registry, and
     * registries are frozen while mod constructors run. Building it inside the supplier defers
     * construction to the loader's registry callback, where the registry is still open.
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
     * @param spec the weapon to build
     * @return the item, ready to be registered
     */
    private static Item create(WeaponSpec spec) {
        // Netherite durability and iron tool tier, matching what the other two lines chose so the weapons
        // behave the same in the editor's ceiling tests.
        //
        // NO stacksTo(1) HERE, and it is not an omission: Item.Properties#stacksTo throws
        // "Unable to have damage AND stack." once maxDamage is set, and durability() sets it.
        Item.Properties properties = new Item.Properties()
                .durability(Tiers.NETHERITE.getUses())
                .attributes(damageModifiers(spec));

        return switch (spec.kind()) {
            case SWORD -> new SwordItem(Tiers.IRON, properties);
            case AXE -> new AxeItem(Tiers.IRON, properties);
            // TridentItem takes no tier: throwing, riptide and its melee behaviour are all vanilla's own,
            // which is what a testing trident should reproduce. TestTridentItem exists for one reason only -
            // the vanilla in-hand look is a hard-coded branch keyed on Items.TRIDENT, so a custom trident
            // has to supply its own renderer or it falls back to the flat sprite.
            case TRIDENT -> new TestTridentItem(properties.rarity(Rarity.RARE));
        };
    }

    /**
     * The vanilla item tags a testing weapon has to join to be treated as its counterpart.
     *
     * <p>1.21 grants enchantability through tags of tags: {@code enchantable/durability},
     * {@code enchantable/melee_weapon}, {@code enchantable/sharp_weapon} and friends all reference
     * {@code minecraft:swords} / {@code minecraft:axes}, so joining the base tag grants the whole family
     * at once. A trident is in none of those, so it joins its own tags instead.
     *
     * <p>Without this the weapons are unknown item categories to the enchanting table and the anvil, and
     * every enchantment is refused no matter which components they carry.
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
            case TRIDENT -> List.of(
                    "minecraft:enchantable/trident",
                    "minecraft:enchantable/durability",
                    "minecraft:breaks_decorated_pots");
        };
        return Optional.of(tags.stream().map(ResourceLocation::parse).toList());
    }

    private static ItemAttributeModifiers damageModifiers(WeaponSpec spec) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(DAMAGE_MODIFIER, spec.damage() - 1.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(SPEED_MODIFIER, spec.attackSpeed(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
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
     * <p>Recognised by the item itself, not by what the stack happens to carry: an item id is permanent,
     * while a component can be missing, stripped or written by an older build with a different modifier
     * id. Looking at the component is what once made a testing weapon in an existing world look like an
     * ordinary item, and its damage row disappear from the editor.
     *
     * @param stack the stack to inspect, may be {@code null}
     * @return the spec, or {@code null} when the stack is not one of the testing weapons
     */
    private static WeaponSpec spec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return SPECS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /**
     * Reads the nominal damage of a testing weapon.
     *
     * @param stack the stack to read
     * @return the damage shown in the tooltip, empty when the stack is not a testing weapon
     */
    public static Optional<Integer> nominalDamage(ItemStack stack) {
        return damageModifierAmount(stack).map(amount -> (int) Math.round(amount + 1.0D));
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

    private static Optional<Double> damageModifierAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return Optional.empty();
        }
        Double fallback = null;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                continue;
            }
            if (isDamageModifier(entry.modifier().id())) {
                return Optional.of(entry.modifier().amount());
            }
            if (fallback == null) {
                // A weapon from another mod may use its own modifier id; the first attack damage entry is
                // then the best answer available, and it is what the tooltip shows as the weapon's damage.
                fallback = entry.modifier().amount();
            }
        }
        return Optional.ofNullable(fallback);
    }

    /**
     * Whether a modifier id is one this mod has used for the nominal damage.
     *
     * @param id the modifier id
     * @return {@code true} when the id belongs to this mod's damage modifier
     */
    private static boolean isDamageModifier(ResourceLocation id) {
        return DAMAGE_MODIFIER.equals(id) || LEGACY_DAMAGE_MODIFIER.equals(id);
    }

    /**
     * Writes a new nominal damage onto a testing weapon, keeping every other modifier untouched.
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
     * Rewrites the attack damage modifier of a stack, keeping every other modifier untouched.
     *
     * @param stack  the stack to modify
     * @param damage the new damage
     * @return {@code true} when the component was written
     */
    private static boolean writeDamage(ItemStack stack, int damage) {
        ItemAttributeModifiers current = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        boolean written = false;
        if (current != null) {
            for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                if (isDamageModifier(entry.modifier().id())) {
                    // Rewritten under the current id, so editing an old weapon also brings it up to date.
                    builder.add(entry.attribute(), new AttributeModifier(
                            DAMAGE_MODIFIER, damage - 1.0D, AttributeModifier.Operation.ADD_VALUE), entry.slot());
                    written = true;
                } else {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
        }
        if (!written) {
            // A component that lost its damage entry would leave the editor with nothing to write, so one is
            // added instead of silently doing nothing.
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(DAMAGE_MODIFIER, damage - 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        return true;
    }
}
