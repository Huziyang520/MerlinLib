package com.huziyang520.merlinlib.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The event types the library fires on its own, and the event classes that go with them.
 *
 * <p>A dependent mod reads these and registers callbacks; the library fires them from the triggers it installs
 * on both loaders. A mod that needs a trigger nobody has - an inventory click, a fishing cast - can create its
 * own {@link EnchantmentEventType} and dispatch it itself; nothing here is privileged.
 *
 * <h2>Which entity each event belongs to</h2>
 *
 * <p>An event is delivered to the enchantments of {@link EnchantmentEvent#getEntity()}. That is the attacker
 * for an attack, the victim for a hurt, the breaker for a block. The other side of the interaction is on the
 * event under its own name, so a callback never has to guess which one it got.
 */
public final class BuiltInEvents {

    /** Fired once per attack that landed, after the damage was applied. */
    public static final EnchantmentEventType<PostAttackEvent> POST_ATTACK =
            EnchantmentEventType.create(name("post_attack"), PostAttackEvent.class);

    /** Fired before damage is applied, with a mutable amount. */
    public static final EnchantmentEventType<ModifyDamageEvent> MODIFY_DAMAGE =
            EnchantmentEventType.create(name("modify_damage"), ModifyDamageEvent.class);

    /** Fired on the victim after damage was applied, with the amount that actually went through. */
    public static final EnchantmentEventType<PostHurtEvent> POST_HURT =
            EnchantmentEventType.create(name("post_hurt"), PostHurtEvent.class);

    /** Fired on the killer when a living entity dies. */
    public static final EnchantmentEventType<PostKillEvent> POST_KILL =
            EnchantmentEventType.create(name("post_kill"), PostKillEvent.class);

    /**
     * Fired when a projectile hits, delivered from the weapon snapshot.
     *
     * <p>This is the type whose dispatch must go through
     * {@link EnchantmentEventDispatcher#dispatchStack}: at the moment a thrown trident lands, the weapon is
     * not in the thrower's hand any more, so an equipment scan would find nothing at all.
     */
    public static final EnchantmentEventType<ProjectileHitEvent> PROJECTILE_HIT =
            EnchantmentEventType.create(name("projectile_hit"), ProjectileHitEvent.class);

    /** Fired on every tick of a living entity, delivered to that entity's equipment. */
    public static final EnchantmentEventType<EntityTickEvent> ENTITY_TICK =
            EnchantmentEventType.create(name("entity_tick"), EntityTickEvent.class);

    /** Fired when a block is broken, before its drops are produced, with a mutable drop list. */
    public static final EnchantmentEventType<ModifyBlockDropsEvent> MODIFY_BLOCK_DROPS =
            EnchantmentEventType.create(name("modify_block_drops"), ModifyBlockDropsEvent.class);

    /** Fired after a block was broken and its drops were handed out. */
    public static final EnchantmentEventType<PostBlockBreakEvent> POST_BLOCK_BREAK =
            EnchantmentEventType.create(name("post_block_break"), PostBlockBreakEvent.class);

    private BuiltInEvents() {
    }

    /**
     * @param path the path of the type's id
     * @return an id in the MerlinLib namespace
     */
    private static Identifier name(String path) {
        return Identifier.fromNamespaceAndPath("merlinlib", path);
    }

    /**
     * An attack that landed.
     *
     * @param level    the level
     * @param attacker the entity that attacked; the event is delivered to its equipment
     * @param target   the entity that was hit
     * @param charge   how fully the weapon was charged, 0.0 to 1.0
     * @param critical whether the hit was a critical hit
     * @param sweep    whether the hit was a sweeping attack
     */
    public record PostAttackEvent(ServerLevel level, LivingEntity attacker, LivingEntity target,
                                  float charge, boolean critical, boolean sweep) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.attacker;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * Damage about to be applied.
     *
     * @param level          the level
     * @param attacker       the attacking entity, or {@code null} when the source has none
     * @param target         the entity about to be hurt
     * @param source         the damage source
     * @param originalDamage the amount before any enchantment touched it
     * @param damage         the amount that will be applied; write to it to change the outcome
     */
    public record ModifyDamageEvent(ServerLevel level, LivingEntity attacker, LivingEntity target,
                                    DamageSource source, float originalDamage,
                                    MutableFloat damage) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.attacker;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * Damage that was applied.
     *
     * @param level         the level
     * @param target        the entity that was hurt; the event is delivered to its equipment
     * @param attacker      the attacking entity, or {@code null} when the source has none
     * @param source        the damage source
     * @param amount        the amount that went through
     * @param blockedDamage how much was blocked by a shield
     * @param blocked       whether a shield blocked at all
     */
    public record PostHurtEvent(ServerLevel level, LivingEntity target, LivingEntity attacker,
                                DamageSource source, float amount, float blockedDamage,
                                boolean blocked) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.target;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * A living entity died.
     *
     * @param level  the level
     * @param killer the entity that dealt the killing blow; the event is delivered to its equipment
     * @param victim the entity that died
     * @param source the damage source that killed it
     */
    public record PostKillEvent(ServerLevel level, LivingEntity killer, LivingEntity victim,
                                DamageSource source) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.killer;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * A projectile hit something.
     *
     * @param level       the level
     * @param attacker    the entity that fired it; the event is delivered to the weapon snapshot, not to its
     *                    equipment, because the weapon is in flight
     * @param target      the entity that was hit
     * @param weapon      the weapon as it was when the projectile was fired
     * @param critArrow   whether the shot was a critical one
     * @param drawStrength how far the bow was drawn, 0.0 to 1.0
     */
    public record ProjectileHitEvent(ServerLevel level, LivingEntity attacker, LivingEntity target,
                                     ItemStack weapon, boolean critArrow,
                                     float drawStrength) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.attacker;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * A living entity ticked.
     *
     * @param level     the level
     * @param entity    the entity; the event is delivered to its equipment
     * @param tickCount its age in ticks
     */
    public record EntityTickEvent(ServerLevel level, LivingEntity entity,
                                  int tickCount) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.entity;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }

    /**
     * A block was broken and its drops are about to be produced.
     *
     * @param level      the level
     * @param player     the player that broke it; the event is delivered to their equipment
     * @param pos        where the block was
     * @param blockState the block before it broke
     * @param tool       the tool that broke it
     * @param drops      the drops, mutable so a callback can add, remove or replace them
     * @param bonusXp    extra experience to drop, on top of what the block gives
     */
    public record ModifyBlockDropsEvent(ServerLevel level, ServerPlayer player, BlockPos pos,
                                        BlockState blockState, ItemStack tool, List<ItemStack> drops,
                                        MutableInt bonusXp) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.player;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }

        /**
         * Adds experience to the block's own drop.
         *
         * @param amount the amount to add, ignored when not positive
         */
        public void addBonusXp(int amount) {
            if (amount > 0) {
                this.bonusXp.add(amount);
            }
        }

        /**
         * Replaces every drop through a function.
         *
         * @param operator the function, applied to each drop in order
         */
        public void transformDrops(UnaryOperator<ItemStack> operator) {
            for (int index = 0; index < this.drops.size(); index++) {
                ItemStack replaced = operator.apply(this.drops.get(index));
                if (replaced != null) {
                    this.drops.set(index, replaced);
                }
            }
        }
    }

    /**
     * A block was broken and its drops were already handed out.
     *
     * @param level      the level
     * @param player     the player that broke it; the event is delivered to their equipment
     * @param pos        where the block was
     * @param blockState the block before it broke
     * @param tool       the tool that broke it
     */
    public record PostBlockBreakEvent(ServerLevel level, ServerPlayer player, BlockPos pos,
                                      BlockState blockState, ItemStack tool) implements EnchantmentEvent {

        @Override
        public LivingEntity getEntity() {
            return this.player;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }
    }
}
