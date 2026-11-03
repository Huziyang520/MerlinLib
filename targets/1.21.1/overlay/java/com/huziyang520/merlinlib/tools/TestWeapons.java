// The three testing weapons.

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

public final class TestWeapons {
    private static final ResourceLocation DAMAGE_MODIFIER = Item.BASE_ATTACK_DAMAGE_ID;
    private static final ResourceLocation SPEED_MODIFIER = Item.BASE_ATTACK_SPEED_ID;

    private static final ResourceLocation LEGACY_DAMAGE_MODIFIER =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "test_weapon_damage");

    private enum Kind {
        SWORD,
        AXE,
        TRIDENT
    }

    private record WeaponSpec(ResourceLocation id, Kind kind, int damage, double attackSpeed) {
    }

    private static final Map<ResourceLocation, WeaponSpec> SPECS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Item> ITEMS = new LinkedHashMap<>();

    static {
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
                .durability(Tiers.NETHERITE.getUses())
                .attributes(damageModifiers(spec));

        return switch (spec.kind()) {
            case SWORD -> new SwordItem(Tiers.IRON, properties);
            case AXE -> new AxeItem(Tiers.IRON, properties);
            case TRIDENT -> new TestTridentItem(properties.rarity(Rarity.RARE));
        };
    }

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

    public static List<ResourceLocation> ids() {
        return List.copyOf(SPECS.keySet());
    }

    public static Optional<String> vanillaCounterpart(ResourceLocation id) {
        WeaponSpec spec = SPECS.get(id);
        return spec == null ? Optional.empty() : Optional.of(spec.kind().name().toLowerCase(Locale.ROOT));
    }

    public static Optional<Item> item(ResourceLocation id) {
        return Optional.ofNullable(ITEMS.get(id));
    }

    public static Optional<ItemStack> stack(ResourceLocation id) {
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

    private static boolean isDamageModifier(ResourceLocation id) {
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
