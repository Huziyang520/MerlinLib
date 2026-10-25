// The event types the library fires on its own, and the event classes that go with them.

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

public final class BuiltInEvents {
    public static final EnchantmentEventType<PostAttackEvent> POST_ATTACK =
            EnchantmentEventType.create(name("post_attack"), PostAttackEvent.class);

    public static final EnchantmentEventType<ModifyDamageEvent> MODIFY_DAMAGE =
            EnchantmentEventType.create(name("modify_damage"), ModifyDamageEvent.class);

    public static final EnchantmentEventType<PostHurtEvent> POST_HURT =
            EnchantmentEventType.create(name("post_hurt"), PostHurtEvent.class);

    public static final EnchantmentEventType<PostKillEvent> POST_KILL =
            EnchantmentEventType.create(name("post_kill"), PostKillEvent.class);

    public static final EnchantmentEventType<ProjectileHitEvent> PROJECTILE_HIT =
            EnchantmentEventType.create(name("projectile_hit"), ProjectileHitEvent.class);

    public static final EnchantmentEventType<EntityTickEvent> ENTITY_TICK =
            EnchantmentEventType.create(name("entity_tick"), EntityTickEvent.class);

    public static final EnchantmentEventType<ModifyBlockDropsEvent> MODIFY_BLOCK_DROPS =
            EnchantmentEventType.create(name("modify_block_drops"), ModifyBlockDropsEvent.class);

    public static final EnchantmentEventType<PostBlockBreakEvent> POST_BLOCK_BREAK =
            EnchantmentEventType.create(name("post_block_break"), PostBlockBreakEvent.class);

    private BuiltInEvents() {
    }

    private static Identifier name(String path) {
        return Identifier.fromNamespaceAndPath("merlinlib", path);
    }

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

        public void addBonusXp(int amount) {
            if (amount > 0) {
                this.bonusXp.add(amount);
            }
        }

        public void transformDrops(UnaryOperator<ItemStack> operator) {
            for (int index = 0; index < this.drops.size(); index++) {
                ItemStack replaced = operator.apply(this.drops.get(index));
                if (replaced != null) {
                    this.drops.set(index, replaced);
                }
            }
        }
    }

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
