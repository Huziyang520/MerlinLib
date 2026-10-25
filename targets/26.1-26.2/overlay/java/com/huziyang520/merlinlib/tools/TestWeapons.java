// The four testing weapons, each one a variant of its vanilla counterpart.

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

public final class TestWeapons {
    private static final Identifier DAMAGE_MODIFIER = Item.BASE_ATTACK_DAMAGE_ID;

    private static final Identifier SPEED_MODIFIER = Item.BASE_ATTACK_SPEED_ID;

    private static final Identifier LEGACY_DAMAGE_MODIFIER =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "test_weapon_damage");

    private enum Kind {
        SWORD,
        AXE,
        SPEAR,
        TRIDENT
    }

    private enum ToolProfile {
        SWORD,
        AXE
    }

    private record WeaponSpec(Identifier id, Kind kind, int damage, double attackSpeed) {
    }

    private static final Map<Identifier, WeaponSpec> SPECS = new LinkedHashMap<>();
    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();

    static {
        add("test_sword", Kind.SWORD, 7, -2.4D);
        add("test_axe", Kind.AXE, 9, -3.1D);
        add("test_spear", Kind.SPEAR, 5, 1.0D / 0.95D - 4.0D);
        add("test_trident", Kind.TRIDENT, 9, -2.9D);
    }

    private TestWeapons() {
    }

    private static void add(String path, Kind kind, int damage, double attackSpeed) {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, path);
        SPECS.put(id, new WeaponSpec(id, kind, damage, attackSpeed));
    }

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
                properties.delayedComponent(DataComponents.TOOL, lookup -> buildTool(ToolProfile.AXE, lookup));
                properties.component(DataComponents.WEAPON, new Weapon(2, 5.0F));
            }
            case SPEAR -> {
                properties.component(DataComponents.WEAPON, new Weapon(1));
                properties.delayedHolderComponent(DataComponents.DAMAGE_TYPE, DamageTypes.SPEAR);
                properties.component(DataComponents.KINETIC_WEAPON, spearKineticWeapon());
                properties.component(DataComponents.PIERCING_WEAPON, new PiercingWeapon(true, false,
                        Optional.of(SoundEvents.SPEAR_ATTACK), Optional.of(SoundEvents.SPEAR_HIT)));
                properties.component(DataComponents.ATTACK_RANGE,
                        new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F));
                properties.component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F);
                properties.component(DataComponents.SWING_ANIMATION,
                        new SwingAnimation(SwingAnimationType.STAB, 19));
                properties.component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F));
            }
            case TRIDENT -> properties
                    .rarity(Rarity.RARE)
                    .durability(ToolMaterial.NETHERITE.durability())
                    .enchantable(1)
                    .component(DataComponents.TOOL, TridentItem.createToolProperties())
                    .component(DataComponents.WEAPON, new Weapon(1));
        }

        properties.delayedComponent(DataComponents.REPAIRABLE, lookup ->
                new Repairable(lookup.lookupOrThrow(Registries.ITEM).getOrThrow(ItemTags.IRON_TOOL_MATERIALS)));

        properties.attributes(damageModifiers(spec));

        return spec.kind() == Kind.TRIDENT ? new TridentItem(properties) : new Item(properties);
    }

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

    private static KineticWeapon spearKineticWeapon() {
        return new KineticWeapon(
                10,
                12,
                KineticWeapon.Condition.ofAttackerSpeed(50, 11.0F),
                KineticWeapon.Condition.ofAttackerSpeed(135, 5.1F),
                KineticWeapon.Condition.ofRelativeSpeed(225, 4.6F),
                0.38F,
                0.95F,
                Optional.of(SoundEvents.SPEAR_USE),
                Optional.of(SoundEvents.SPEAR_HIT)
        );
    }

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

    public static List<Identifier> ids() {
        return List.copyOf(SPECS.keySet());
    }

    public static Optional<String> vanillaCounterpart(Identifier id) {
        WeaponSpec spec = SPECS.get(id);
        return spec == null ? Optional.empty() : Optional.of(spec.kind().name().toLowerCase(Locale.ROOT));
    }

    public static Optional<Item> item(Identifier id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    public static Optional<ItemStack> stack(Identifier id) {
        return item(id).map(ItemStack::new);
    }

    public static boolean isTestWeapon(ItemStack stack) {
        return spec(stack) != null;
    }

    private static WeaponSpec spec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return SPECS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static Optional<Integer> nominalDamage(ItemStack stack) {
        return damageModifierAmount(stack).map(amount -> (int) Math.round(amount + 1.0D));
    }

    public static Optional<Integer> weaponDamage(ItemStack stack) {
        return damageModifierAmount(stack).map(amount -> (int) Math.round(amount + 1.0D));
    }

    public static boolean hasEditableDamage(ItemStack stack) {
        return damageModifierAmount(stack).isPresent();
    }

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
                fallback = entry.modifier().amount();
            }
        }
        return Optional.ofNullable(fallback);
    }

    private static boolean isDamageModifier(Identifier id) {
        return DAMAGE_MODIFIER.equals(id) || LEGACY_DAMAGE_MODIFIER.equals(id);
    }

    public static boolean setNominalDamage(ItemStack stack, int damage) {
        if (!isTestWeapon(stack)) {
            return false;
        }
        return writeDamage(stack, damage);
    }

    private static boolean writeDamage(ItemStack stack, int damage) {
        ItemAttributeModifiers current = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        boolean written = false;
        if (current != null) {
            for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
                if (isDamageModifier(entry.modifier().id())) {
                    builder.add(entry.attribute(), new AttributeModifier(
                            DAMAGE_MODIFIER, damage - 1.0D, AttributeModifier.Operation.ADD_VALUE), entry.slot());
                    written = true;
                } else {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
        }
        if (!written) {
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(DAMAGE_MODIFIER, damage - 1.0D, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        return true;
    }
}
