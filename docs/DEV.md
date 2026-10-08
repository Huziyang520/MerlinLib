# MerlinLib — Developer Guide

[English](#english) | [中文](#中文)

> The Java API: three registration lines and the hooks around them.
> For the files and commands see [`USAGE.md`](USAGE.md); for the toolkit see [`TOOLKIT.md`](TOOLKIT.md).

---

## English

### Depending on MerlinLib

Drop the jar into `mods/` on the client and the server, and declare the dependency in
`META-INF/mods.toml`:

```toml
[[dependencies.mymod]]
    modId="merlinlib"
    mandatory=true
    versionRange="[0.1.0,)"
    ordering="BEFORE"
    side="BOTH"
```

`ordering="BEFORE"` matters: MerlinLib's own registry work has to run before content that reads it.
Call your registration from the mod constructor:

```java
public MyMod() {
    MyEnchantments.register();
    MyEffects.register();
    MerlinApi.lifecycle().onServerStarted(server -> MyConfig.load());
}
```

Everything is reached through one entry point, `com.huziyang520.merlinlib.api.MerlinApi`:

| Call | What it is for |
|---|---|
| `MerlinApi.enchantments()` | register enchantments, and inspect what won |
| `MerlinApi.effects()` | register mob effects |
| `MerlinApi.potions()` | register potions, including the normal / long / strong variants of one effect |
| `MerlinApi.events()` | callbacks on an enchantment's own events |
| `MerlinApi.loot()` | inject enchanted books or items into vanilla loot tables |
| `MerlinApi.notices()` | a chat message shown when a player arrives |
| `MerlinApi.lifecycle()` | server starting / started / stopping and player join |
| `MerlinApi.ai()` | let a mod veto a mob's target, or make it keep its distance |

### Registering an enchantment

```java
MerlinApi.enchantments()
        .register(new ResourceLocation("mymod", "frostbite"))
        .maxLevel(3)
        .weight(5)
        .cost(10, 9, 40, 9)          // minBase, minPerLevel, maxBase, maxPerLevel
        .anvilCost(2)
        .slots("mainhand")
        .supportedItems("#minecraft:enchantable/weapon")
        .acquisition(Acquisition.DEFAULT)
        .submit();                    // registers it; build() only constructs
```

`category(EnchantmentCategory)` decides which items the enchanting table may offer it on; leave it out
for an enchantment that should never be offered there. `exclusiveSet(JsonArray)` takes the *members*,
not a tag name — on this version there is no enchantment tag layer, so a group has to be expanded
before it is handed over.

To see what actually won, including where an override came from:

```java
MerlinApi.enchantments().describe(new ResourceLocation("minecraft", "sharpness"))
        .ifPresent(info -> LOG.info("{} {}", info.source(), info.enabled()));
```

### Registering an effect

```java
MerlinApi.effects()
        .register(new ResourceLocation("mymod", "frostbite"))
        .category(MobEffectCategory.HARMFUL)
        .color(MerlinColor.CYAN)            // color("#55FFFF") and color("cyan") work too
        .onTick((level, entity, amplifier) -> {
            entity.hurt(entity.damageSources().freeze(), 1.0F);
            return true;
        })
        .submit();
```

### Registering potions

```java
// normal / long / strong variants of an effect registered above
MerlinApi.potions().potionFor(FROSTBITE).submit();
// => mymod:frostbite / mymod:long_frostbite / mymod:strong_frostbite

// or several effects by hand
MerlinApi.potions()
        .register(new ResourceLocation("mymod", "frostbite_bomb"))
        .effect(FROSTBITE, 1200, 1)
        .effect(new ResourceLocation("minecraft", "slowness"), 600)
        .variants()
        .submit();
```

### Enchantment events

```java
MerlinApi.events().register(venomEnchantment, BuiltInEvents.POST_ATTACK, (event, context) -> {
    event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
});
```

- Eight built-in types: `POST_ATTACK`, `MODIFY_DAMAGE`, `POST_HURT`, `POST_KILL`, `PROJECTILE_HIT`,
  `ENTITY_TICK`, `MODIFY_BLOCK_DROPS` and `POST_BLOCK_BREAK`; each event's record is nested in
  `BuiltInEvents`.
- The equipment is scanned for you (main hand, armour, off hand), and effects that outlive the swing —
  a thrown trident, an arrow in flight — are found through the weapon snapshot the projectile carries.
- The context hands you the live enchantment, the level and the item it was found on.
- A callback that throws is logged and skipped, and an event type nobody listens to costs a single bit
  test: no scan happens at all until something registers.

### Loot injection

```java
MerlinApi.loot().register(LootInjectionBuilder.create()
        .toTables("minecraft:chests/simple_dungeon")
        .asBook()
        .withEnchantments("mymod:frostbite")
        .chance(0.1F)
        .weight(1)
        .quality(1));
```

Each rule becomes one loot pool holding an enchanted book (`asBook`) or an item (`asItem`) behind a
random-chance condition. `LootTables` lists the vanilla table names as constants, so a typo is a
compile error instead of a rule that silently never fires. An enchantment that does not exist is
logged and skipped — a missing entry never stops a table from loading.

### Join notices

```java
MerlinApi.notices().register(
        new ResourceLocation("mymod", "dependency_change"),
        NoticeMode.EVERY_JOIN,
        Component.translatable("mymod.notice.dependency_change"),
        true);                        // enabled by default; the player can switch it off per mod
```

`NoticeMode` is `EVERY_JOIN`, `ONCE_PER_SAVE` or `FIRST_JOIN`. The id's namespace is the owning mod:
the settings screen groups by it and the per mod switch is stored under it. Colours come from the
component itself, so a line may carry as many styles as it likes. A data pack can add one at
`data/<namespace>/merlinlib/notices/<name>.json`.

### Server lifecycle

```java
MerlinApi.lifecycle().onServerStarting(() -> MyConfig.load());
MerlinApi.lifecycle().onServerStarted(server -> MyEnchantments.bind(server));
MerlinApi.lifecycle().onServerStopping(MyCache::clear);
MerlinApi.lifecycle().onPlayerJoin(player -> ...);
```

Registration is safe from a mod constructor: the callbacks are buffered and run when the loader event
fires, in registration order, and one that throws is logged without stopping the others. The two start
moments are not interchangeable — `onServerStarting` is early, so loot rules must be registered there
(while the tables are still being read), and `onServerStarted` is where the registry is ready to be
read from.

### Mob behaviour hooks

```java
MerlinApi.ai().register(new AiHook() {
    @Override
    public boolean allowsTargeting(Mob mob, LivingEntity target) {
        return !(target instanceof Player player) || !wearsTheCharm(player, mob);
    }
});
```

- Three questions, all optional: `allowsTargeting` (may the mob take that entity as its target),
  `wantsToFlee` (should it actively keep away — the library does the steering, throttled to once a
  second), and `allowsAvoiding` (may the mob's own "run away from this" behaviour apply).
- Refusing a target covers every reason vanilla asks, including retaliation; a rule that should still
  let a mob fight back checks `mob.getLastHurtByMob()` itself.
- Brain mobs are covered too, not only the ones that go through `Mob#setTarget`: the piglin family
  records hostility in its brain memories instead, and the library hooks the two places that decide it
  (recording anger, and choosing an attack target) so a rule works the same for a zombie, a piglin and
  a piglin brute.
- Nothing is scanned unless a mod registered a hook, and nothing runs on the client.

### Interface animation

MerlinLib's own screens pop open instead of appearing at once, and any mod can ask for the same, or for
parts of it:

```java
ScreenIntro intro = UiAnimation.intro();          // honours client.toml gui.animation_*
graphics.pose().translate(intro.offsetX(width), intro.offsetY(height));
graphics.pose().scale((float) intro.scale(), (float) intro.scale());
```

- `Easing` is a set of five curves (`LINEAR`, `EASE_OUT_CUBIC`, `EASE_IN_OUT_SINE`, `EASE_OUT_BACK`,
  `EASE_OUT_ELASTIC`) as plain double functions, with nothing Minecraft in them.
- `ScreenIntro` is one opening animation — `SCALE_POP`, `SLIDE_UP`, `SLIDE_SIDE` or `FADE` — with
  `scale()`, `offsetX()`, `offsetY()` and `alpha()` for the caller to apply. It is driven by the clock
  rather than by ticks, so a screen asks for the current value whenever it draws.
- The library draws nothing for the caller and does not touch another mod's screens.

### Colours, counters and lookups

```java
int base = MerlinColor.CYAN;                       // palette constant
int parsed = MerlinColor.parse("#55FFFF").orElseThrow();
int[] tones = MerlinColor.gradient(base);          // [highlight, base, shadow]
int softer = MerlinColor.mix(MerlinColor.CYAN, MerlinColor.WHITE, 0.25F);
```

`EntityCounter` counts what a mod has granted to an entity so that only its own share is taken back —
the pattern behind "a temporary effect that ends when the enchantment is removed". `SmeltingLookup`
answers what an item smelts into.

---

## 中文

### 依赖 MerlinLib

把 jar 放进客户端与服务端的 `mods/`，并在 `META-INF/mods.toml` 里声明前置：

```toml
[[dependencies.mymod]]
    modId="merlinlib"
    mandatory=true
    versionRange="[0.1.0,)"
    ordering="BEFORE"
    side="BOTH"
```

`ordering="BEFORE"` 是必须的：MerlinLib 自己的注册要先跑完，读它的业务代码才有内容可读。
注册调用放在模组构造器里即可。

统一入口是 `com.huziyang520.merlinlib.api.MerlinApi`，八条主线与英文部分那张表一致：
`enchantments()` 附魔、`effects()` 效果、`potions()` 药水、`events()` 附魔事件、`loot()` 战利品注入、
`notices()` 进服提示、`lifecycle()` 服务器生命周期、`ai()` 生物行为挂钩。

### 注册附魔

```java
MerlinApi.enchantments()
        .register(new ResourceLocation("mymod", "frostbite"))
        .maxLevel(3)
        .weight(5)
        .cost(10, 9, 40, 9)          // minBase, minPerLevel, maxBase, maxPerLevel
        .anvilCost(2)
        .slots("mainhand")
        .supportedItems("#minecraft:enchantable/weapon")
        .acquisition(Acquisition.DEFAULT)
        .submit();                    // 真正写入；只构建用 build()
```

`category(EnchantmentCategory)` 决定附魔台会把哪些物品拿出来附它；不希望出现在附魔台的附魔不要写它。
`exclusiveSet(JsonArray)` 要的是**展开后的成员 id**，不是标签名——本版本没有附魔标签层。

查询最终生效状态（含覆盖来源）：`MerlinApi.enchantments().describe(id)`。

### 注册效果 / 药水

```java
MerlinApi.effects()
        .register(new ResourceLocation("mymod", "frostbite"))
        .category(MobEffectCategory.HARMFUL)
        .color(MerlinColor.CYAN)            // 也支持 color("#55FFFF") 或 color("cyan")
        .onTick((level, entity, amplifier) -> {
            entity.hurt(entity.damageSources().freeze(), 1.0F);
            return true;
        })
        .submit();

// 普通 / 长效 / 强力三个药水
MerlinApi.potions().potionFor(FROSTBITE).submit();
```

### 附魔事件

```java
MerlinApi.events().register(venomEnchantment, BuiltInEvents.POST_ATTACK, (event, context) -> {
    event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
});
```

八种内置事件：`POST_ATTACK`、`MODIFY_DAMAGE`、`POST_HURT`、`POST_KILL`、`PROJECTILE_HIT`、
`ENTITY_TICK`、`MODIFY_BLOCK_DROPS`、`POST_BLOCK_BREAK`。装备由库帮你扫（主手、护甲、副手）；挥砍结束后
仍在生效的武器效果——投出去的三叉戟、飞行中的箭——通过弹射物携带的武器快照找到。回调抛异常只记日志并
跳过；没有人监听的事件类型只做一次位测试。

### 战利品注入

```java
MerlinApi.loot().register(LootInjectionBuilder.create()
        .toTables("minecraft:chests/simple_dungeon")
        .asBook()
        .withEnchantments("mymod:frostbite")
        .chance(0.1F)
        .weight(1)
        .quality(1));
```

每条规则变成一个战利品池，池里是按随机概率判定的附魔书（`asBook`）或指定物品（`asItem`）。
`LootTables` 把原版表名写成常量，拼错是编译错误，而不是"规则悄悄不触发"。写错的附魔只记日志跳过，
绝不让一张表加载失败。

### 进服提示

`MerlinApi.notices().register(id, NoticeMode.EVERY_JOIN, Component..., true)`。三种时机：
`EVERY_JOIN` / `ONCE_PER_SAVE` / `FIRST_JOIN`。**id 的命名空间就是归属模组**：设置界面按它分组，
逐模组开关也存在它名下。颜色来自组件本身。数据包也可以加，位置
`data/<命名空间>/merlinlib/notices/<名字>.json`。

### 服务器生命周期

```java
MerlinApi.lifecycle().onServerStarting(() -> MyConfig.load());
MerlinApi.lifecycle().onServerStarted(server -> MyEnchantments.bind(server));
```

在模组构造器里注册是安全的：回调先缓冲、等加载器事件触发时按注册顺序执行，某一条抛异常只记日志、
不影响其余。两个"启动"时机不能互换——战利品规则必须挂在更早的 `onServerStarting` 上，
而"注册表已经可以读了"是 `onServerStarted`。

### 生物行为挂钩

`MerlinApi.ai().register(new AiHook() { ... })`，三个可选问题：`allowsTargeting`（能否把该实体当作目标）、
`wantsToFlee`（是否应主动远离，转向由库负责、每秒最多一次）、`allowsAvoiding`（它自己的躲避行为是否生效）。
否决目标覆盖原版的所有提问理由（含被打还手）；希望"主动惹它照打"的规则自己看 `mob.getLastHurtByMob()`。

**Brain 生物同样覆盖**：猪灵族不把敌意写进 `Mob#target`，而是写进 Brain 的记忆，库挂钩了决定它的两处
（记录愤怒、选择攻击目标），所以同一条规则对僵尸、猪灵与猪灵蛮兵表现一致。没有任何模组登记挂钩时
整条路径直接短路；客户端侧完全不跑。

### 界面动画

```java
ScreenIntro intro = UiAnimation.intro();          // 读 client.toml 的 gui.animation_*
graphics.pose().translate(intro.offsetX(width), intro.offsetY(height));
graphics.pose().scale((float) intro.scale(), (float) intro.scale());
```

`Easing` 是五条缓动（纯 double 函数，与 Minecraft 无关）；`ScreenIntro` 是一个入场动画
（`SCALE_POP` / `SLIDE_UP` / `SLIDE_SIDE` / `FADE`），对外给 `scale()` / `offsetX()` / `offsetY()` /
`alpha()` 由调用方自己施加，**按时间推进**而不是按 tick。库不替调用方画任何东西，也不碰别人的界面。

### 颜色与其它工具

`MerlinColor` 提供调色板常量、`parse`、`gradient`（[高光, 基色, 阴影]）与 `mix`；
`EntityCounter` 记录"谁授予了什么"，用于只收回自己给的那一份；`SmeltingLookup` 查询熔炼产物。
