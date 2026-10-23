package com.huziyang520.merlinlib.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
    private static ResourceLocation name(String path) {
        return new ResourceLocation("merlinlib", path);
    }

/**
 * An attack that landed.
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
