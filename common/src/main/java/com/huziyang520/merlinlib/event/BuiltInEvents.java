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

    public static final class PostAttackEvent implements EnchantmentEvent {
        private final ServerLevel level;
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final float charge;
        private final boolean critical;
        private final boolean sweep;
        private boolean canceled;

        public PostAttackEvent(ServerLevel level, LivingEntity attacker, LivingEntity target,
                               float charge, boolean critical, boolean sweep) {
            this.level = level;
            this.attacker = attacker;
            this.target = target;
            this.charge = charge;
            this.critical = critical;
            this.sweep = sweep;
        }

        public ServerLevel level() {
            return this.level;
        }

        public LivingEntity attacker() {
            return this.attacker;
        }

        public LivingEntity target() {
            return this.target;
        }

        public float charge() {
            return this.charge;
        }

        public boolean critical() {
            return this.critical;
        }

        public boolean sweep() {
            return this.sweep;
        }

        @Override
        public LivingEntity getEntity() {
            return this.attacker;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void cancel() {
            this.canceled = true;
        }
    }

    public static final class ModifyDamageEvent implements EnchantmentEvent {
        private final ServerLevel level;
        private final LivingEntity attacker;
        private final LivingEntity target;
        private final DamageSource source;
        private final float originalDamage;
        private final MutableFloat damage;
        private boolean canceled;

        public ModifyDamageEvent(ServerLevel level, LivingEntity attacker, LivingEntity target,
                                 DamageSource source, float originalDamage, MutableFloat damage) {
            this.level = level;
            this.attacker = attacker;
            this.target = target;
            this.source = source;
            this.originalDamage = originalDamage;
            this.damage = damage;
        }

        public ServerLevel level() {
            return this.level;
        }

        public LivingEntity attacker() {
            return this.attacker;
        }

        public LivingEntity target() {
            return this.target;
        }

        public DamageSource source() {
            return this.source;
        }

        public float originalDamage() {
            return this.originalDamage;
        }

        public MutableFloat damage() {
            return this.damage;
        }

        @Override
        public LivingEntity getEntity() {
            return this.attacker;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void cancel() {
            this.canceled = true;
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

    public static final class ModifyBlockDropsEvent implements EnchantmentEvent {
        private final ServerLevel level;
        private final ServerPlayer player;
        private final BlockPos pos;
        private final BlockState blockState;
        private final ItemStack tool;
        private final List<ItemStack> drops;
        private final MutableInt bonusXp;
        private boolean canceled;

        public ModifyBlockDropsEvent(ServerLevel level, ServerPlayer player, BlockPos pos,
                                     BlockState blockState, ItemStack tool, List<ItemStack> drops,
                                     MutableInt bonusXp) {
            this.level = level;
            this.player = player;
            this.pos = pos;
            this.blockState = blockState;
            this.tool = tool;
            this.drops = drops;
            this.bonusXp = bonusXp;
        }

        public ServerLevel level() {
            return this.level;
        }

        public ServerPlayer player() {
            return this.player;
        }

        public BlockPos pos() {
            return this.pos;
        }

        public BlockState blockState() {
            return this.blockState;
        }

        public ItemStack tool() {
            return this.tool;
        }

        public List<ItemStack> drops() {
            return this.drops;
        }

        public MutableInt bonusXp() {
            return this.bonusXp;
        }

        @Override
        public LivingEntity getEntity() {
            return this.player;
        }

        @Override
        public ServerLevel getLevel() {
            return this.level;
        }

        @Override
        public boolean isCanceled() {
            return this.canceled;
        }

        @Override
        public void cancel() {
            this.canceled = true;
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
