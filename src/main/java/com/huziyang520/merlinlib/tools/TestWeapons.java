// The three testing weapons, each one a subclass of its vanilla counterpart.

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

public final class TestWeapons {
    private static final UUID DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    @SuppressWarnings("unused")
    private static final ResourceLocation LEGACY_DAMAGE_MODIFIER =
            new ResourceLocation(Constants.MOD_ID, "test_weapon_damage");

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
        ResourceLocation id = new ResourceLocation(Constants.MOD_ID, path);
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
                .durability(Tiers.NETHERITE.getUses());

        float modifierDamage = spec.damage() - 1.0F;
        float modifierSpeed = (float) spec.attackSpeed();

        return switch (spec.kind()) {
            case SWORD -> new SwordItem(Tiers.IRON, (int) modifierDamage, modifierSpeed, properties);
            case AXE -> new AxeItem(Tiers.IRON, modifierDamage, modifierSpeed, properties);
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
            case TRIDENT -> List.of("minecraft:breaks_decorated_pots");
        };
        return Optional.of(tags.stream().map(ResourceLocation::new).toList());
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
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? null : SPECS.get(id);
    }

    public static Optional<Integer> nominalDamage(ItemStack stack) {
        return weaponDamage(stack);
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

    public static boolean setNominalDamage(ItemStack stack, int damage) {
        if (!isTestWeapon(stack)) {
            return false;
        }
        return writeDamage(stack, damage);
    }

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

    private static boolean writeDamage(ItemStack stack, int damage) {
        removeStaleDamageModifier(stack);
        stack.addAttributeModifier(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(DAMAGE_MODIFIER, "Weapon modifier", damage - 1.0D,
                        AttributeModifier.Operation.ADDITION),
                EquipmentSlot.MAINHAND);
        return true;
    }

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

    private static final String TAG_ATTRIBUTE_MODIFIERS = "AttributeModifiers";
}
