package com.huziyang520.merlinlib.tools;

import com.huziyang520.merlinlib.Constants;
import com.huziyang520.merlinlib.platform.Services;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The four testing weapons, each one a <b>variant of its vanilla counterpart</b>.
 *
 * <p>26.3 has no {@code SwordItem} / {@code AxeItem} / {@code SpearItem}: a sword, an axe and a spear
 * are plain {@link Item}s whose behaviour comes from item components. A trident is still a real class,
 * {@link TridentItem}.
 *
 * <h2>What two failed attempts taught us (this is not obvious, keep it documented)</h2>
 *
 * <p><b>1. You cannot read a vanilla item's components during mod init.</b> The first attempt copied
 * {@code Items.IRON_SWORD}'s component map; it crashed with
 * {@code NullPointerException: Components not bound yet}. {@code Item.components()} goes through the
 * item's {@code Holder.Reference}, and item components are only bound during registry binding, long
 * after mod construction.
 *
 * <p><b>2. You cannot call vanilla's own {@code sword()} / {@code axe()} / {@code spear()} helpers
 * either.</b> The second attempt called them verbatim; it crashed with
 * {@code IllegalStateException: Registry is already frozen}, because they begin with
 * {@code BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)} - a handle that
 * only exists inside vanilla's own bootstrap window. Mod constructors run after the registries freeze,
 * so that call can never succeed from a mod. The same applies to {@code repairable(TagKey)}.
 *
 * <p>So the components are written out explicitly, with vanilla's own values, and everything that needs
 * a registry or tag lookup goes through {@link Item.Properties#delayedComponent} - the very mechanism
 * vanilla uses for the axe's block transformer. Its initializer runs when the registries are usable.
 *
 * <p>The nominal damage is written last so it wins over the vanilla baseline: it is the single source of
 * truth the editor edits and the tooltip displays.
 *
 * <p>Known deliberate gaps versus vanilla, to be closed later: the axe's {@code BLOCK_TRANSFORMER}
 * (log stripping) and the spear's {@code KINETIC_WEAPON} (thrust and dismount parameters) are not set
 * yet, because both need values this class does not reproduce faithfully at the time of writing.
 */
public final class TestWeapons {

    /** Namespace of the attack damage modifier; identifies a MerlinLib weapon. */
    private static final Identifier DAMAGE_MODIFIER = Item.BASE_ATTACK_DAMAGE_ID;
    /** Namespace of the attack speed modifier. */
    private static final Identifier SPEED_MODIFIER = Item.BASE_ATTACK_SPEED_ID;
    /**
     * The damage modifier id older builds wrote.
     *
     * <p>Kept so a testing weapon created by one of those builds is still recognised and stays editable: the
     * id lives inside the item's component, so changing it in code never reaches an existing world.
     */
    private static final Identifier LEGACY_DAMAGE_MODIFIER =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "test_weapon_damage");

    /** Which vanilla weapon a testing weapon mirrors. */
    private enum Kind {
        SWORD,
        AXE,
        SPEAR,
        TRIDENT
    }

    /** The two vanilla mining profiles a testing weapon can mirror. */
    private enum ToolProfile {
        SWORD,
        AXE
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
    private record WeaponSpec(Identifier id, Kind kind, int damage, double attackSpeed) {
    }

    private static final Map<Identifier, WeaponSpec> SPECS = new LinkedHashMap<>();
    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();

    static {
        // Declared in the order the plan lists them, which is also the creative tab order.
        add("test_sword", Kind.SWORD, 7, -2.4D);
        add("test_axe", Kind.AXE, 9, -3.1D);
        add("test_spear", Kind.SPEAR, 5, 1.0D / 0.95D - 4.0D);
        add("test_trident", Kind.TRIDENT, 8, -2.9D);
    }

    private TestWeapons() {
    }

    private static void add(String path, Kind kind, int damage, double attackSpeed) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
        SPECS.put(id, new WeaponSpec(id, kind, damage, attackSpeed));
    }

    /**
     * Registers all four weapons.
     *
     * <p>Called from the shared bootstrap; on NeoForge the actual registry write is queued until the
     * loader opens the registry.
     */
    public static void register() {
        for (WeaponSpec spec : SPECS.values()) {
            // The item must NOT be built here. Constructing an Item calls
            // BuiltInRegistries.ITEM.createIntrusiveHolder(this), which throws
            // "Registry is already frozen" on NeoForge because mod constructors run after the
            // registries freeze. Building it inside the supplier defers construction to the
            // loader's RegisterEvent, where the registry is still open.
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
     * Builds one weapon out of vanilla's component values.
     *
     * @param spec the weapon to build
     * @return the item, ready to be registered
     */
    private static Item create(WeaponSpec spec) {
        Item.Properties properties = new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, spec.id()))
                .durability(ToolMaterial.NETHERITE.durability())
                .enchantable(ToolMaterial.IRON.enchantmentValue());

        switch (spec.kind()) {
            case SWORD -> {
                properties.delayedComponent(DataComponents.TOOL, lookup -> buildTool(ToolProfile.SWORD, lookup));
                properties.component(DataComponents.WEAPON, new Weapon(1));
            }
            case AXE -> {
                // the vanilla axe disables shields for 5 seconds and strips logs
                properties.delayedComponent(DataComponents.TOOL, lookup -> buildTool(ToolProfile.AXE, lookup));
                properties.delayedComponent(DataComponents.BLOCK_TRANSFORMER,
                        lookup -> lookup.getOrThrow(BlockTransformers.AXE));
                properties.component(DataComponents.WEAPON, new Weapon(2, 5.0F));
            }
            case SPEAR -> {
                // A vanilla spear thrusts instead of mining. Every component below is vanilla's own
                // spear recipe. Omitting any one breaks it in a subtle way: without USE_EFFECTS
                // charging walks instead of lunging, without ATTACK_RANGE/ATTACK_ANIMATION there is
                // no stab, without PIERCING_WEAPON no thrust damage. The first attempt missed all of
                // those, which is why the spear charged slowly and fought like a reskinned sword.
                properties.component(DataComponents.WEAPON, new Weapon(1));
                properties.delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR);
                properties.component(DataComponents.KINETIC_WEAPON, spearKineticWeapon());
                properties.component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                        Optional.of(SoundEvents.SPEAR_ATTACK), Optional.of(SoundEvents.SPEAR_HIT)));
                properties.component(DataComponents.ATTACK_RANGE,
                        new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F));
                properties.component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F);
                properties.component(DataComponents.ATTACK_ANIMATION,
                        new SwingAnimation(SwingAnimationType.STAB, 19)); // attackDuration 0.95s * 20
                properties.component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F));
            }
            case TRIDENT -> properties
                    .rarity(Rarity.RARE)
                    .durability(ToolMaterial.NETHERITE.durability())
                    .enchantable(1)
                    .component(DataComponents.TOOL, TridentItem.createToolProperties())
                    .component(DataComponents.WEAPON, new Weapon(1));
        }

        // the repair material is a tag, so it needs the same deferred treatment
        properties.delayedComponent(DataComponents.REPAIRABLE, lookup ->
                new Repairable(lookup.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.IRON_TOOL_MATERIALS)));

        // written last on purpose: the editable nominal damage must win over the vanilla baseline
        properties.attributes(damageModifiers(spec));

        return spec.kind() == Kind.TRIDENT ? new TridentItem(properties) : new Item(properties);
    }

    /**
     * Rebuilds vanilla's tool rules for one profile.
     *
     * <p>Values are copied from vanilla's {@code ToolMaterial.applySwordProperties} /
     * {@code applyToolProperties}. Runs inside a delayed initializer, which is the only place a tag
     * lookup is available.
     *
     * @param profile which profile to mirror
     * @param lookup  registry lookup provided by the loader
     * @return the tool component value
     */
    private static Tool buildTool(ToolProfile profile, HolderLookup.Provider lookup) {
        HolderGetter<Block> blocks = lookup.lookupOrThrow(Registries.BLOCK);
        return switch (profile) {
            case SWORD -> new Tool(List.of(
                    Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_IRON_TOOL)),
                    Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F),
                    Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE),
                    Tool.Rule.overrideSpeed(blocks.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F)
            ), 1.0F, 2, false);
            case AXE -> new Tool(List.of(
                    Tool.Rule.deniesDrops(blocks.getOrThrow(BlockTags.INCORRECT_FOR_IRON_TOOL)),
                    Tool.Rule.minesAndDrops(blocks.getOrThrow(BlockTags.MINEABLE_WITH_AXE), ToolMaterial.IRON.speed())
            ), 1.0F, 1, true);
        };
    }

    /**
     * Rebuilds the vanilla iron spear's kinetic weapon component, which is what makes a spear thrust:
     * it dismounts, knocks back and deals multiplied damage while the attacker is moving fast enough.
     *
     * <p>Values are vanilla's {@code IRON_SPEAR} arguments converted the same way vanilla converts them
     * ({@code (int)(seconds * 20.0F)}), with the material damage multiplier.
     *
     * @return the kinetic weapon component value
     */
    private static KineticWeapon spearKineticWeapon() {
        return new KineticWeapon(
                10,
                12,                                             // delay 0.60s
                KineticWeapon.Condition.ofAttackerSpeed(50, 11.0F),   // dismount 2.50s
                KineticWeapon.Condition.ofAttackerSpeed(135, 5.1F),   // knockback 6.75s
                KineticWeapon.Condition.ofRelativeSpeed(225, 4.6F),   // damage 11.25s
                0.38F,
                0.95F,                                          // damage multiplier
                Optional.of(SoundEvents.SPEAR_USE),
                Optional.of(SoundEvents.SPEAR_HIT)
        );
    }

    /**
     * The vanilla item tags a testing weapon has to join to be treated as its counterpart.
     *
     * <p>This is the fix for "why can't I put any enchantment on them?": enchantability is granted
     * through tags of tags. Vanilla's own {@code swords} / {@code axes} / {@code spears} tags are
     * referenced by {@code enchantable/durability}, {@code enchantable/melee_weapon},
     * {@code enchantable/sharp_weapon}, {@code enchantable/sweeping}, {@code enchantable/lunge},
     * {@code enchantable/fire_aspect} and {@code breaks_decorated_pots}, so joining the base tag grants
     * the whole family at once. A trident is not in any of those, so it joins its own tags instead.
     *
     * <p>Without this the weapons are unknown item categories to the enchanting table and the anvil, and
     * every enchantment is refused no matter which components they carry.
     *
     * @param id the weapon id
     * @return the tag ids to join, empty when the id is not a testing weapon
     */
    public static Optional<List<Identifier>> tagMembership(Identifier id) {
        WeaponSpec spec = SPECS.get(id);
        if (spec == null) {
            return Optional.empty();
        }
        List<String> tags = switch (spec.kind()) {
            case SWORD -> List.of("minecraft:swords");
            case AXE -> List.of("minecraft:axes");
            case SPEAR -> List.of("minecraft:spears");
            case TRIDENT -> List.of(
                    "minecraft:enchantable/trident",
                    "minecraft:enchantable/durability",
                    "minecraft:breaks_decorated_pots");
        };
        return Optional.of(tags.stream().map(Identifier::parse).toList());
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
    public static List<Identifier> ids() {
        return List.copyOf(SPECS.keySet());
    }

    /** @return the vanilla counterpart name of a testing weapon, empty when the id is unknown. */
    public static Optional<String> vanillaCounterpart(Identifier id) {
        WeaponSpec spec = SPECS.get(id);
        return spec == null ? Optional.empty() : Optional.of(spec.kind().name().toLowerCase(Locale.ROOT));
    }

    /** @return the registered item, empty when the id is not a testing weapon. */
    public static Optional<Item> item(Identifier id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    /** @return the stack for a weapon id, empty when unknown. */
    public static Optional<ItemStack> stack(Identifier id) {
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
     * <p>Recognised by the item itself, not by what the stack happens to carry: an id on the item is
     * permanent, while a component can be missing, stripped or written by an older build with a different
     * modifier id. Looking at the component is what once made a testing weapon in an existing world look
     * like an ordinary item, and its damage row disappear from the editor.
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
     * <p>Same reading as {@link #nominalDamage(ItemStack)}: the amount of the attack damage modifier plus the
     * player's own base of one, which is exactly the number the tooltip shows as "attack damage".
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
     * <p>The general form of {@link #setNominalDamage(ItemStack, int)}, for ordinary weapons: the value is
     * written under the vanilla modifier id, so the tooltip keeps the vanilla "attack damage" wording.
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
                // A weapon from another mod may use its own modifier id; the first attack damage entry is then
                // the best answer available, and it is what the tooltip shows as the weapon's damage.
                fallback = entry.modifier().amount();
            }
        }
        return Optional.ofNullable(fallback);
    }

    /**
     * Whether a modifier id is one this mod has used for the nominal damage.
     *
     * <p>The id is part of the component, so an item created by an older build keeps the old one forever:
     * renaming it in code does not reach into a world. Both ids are therefore still recognised, and the
     * value is rewritten under the vanilla id when it is next edited.
     *
     * @param id the modifier id
     * @return {@code true} when the id belongs to this mod's damage modifier
     */
    private static boolean isDamageModifier(Identifier id) {
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
