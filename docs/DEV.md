# MerlinLib developer guide: enchantments / effects / potions

[English](#english) | [中文](#中文)

---

## English

This guide is for developers who use MerlinLib from another mod. Every signature is taken from the current source (the `26.3` branch).

Single entry point:

```java
import com.huziyang520.merlinlib.api.MerlinApi;

MerlinApi.enchantments();  // enchantments
MerlinApi.effects();       // effects
MerlinApi.potions();       // potions
```

Identifiers are `net.minecraft.resources.Identifier` everywhere (26.3 renamed it from `ResourceLocation`).

### 1. Enchantments (data driven: config can add, change and disable)

Enchantments are a data pack driven dynamic registry in 26.3. Entries registered through the code API go into a
generated in-memory pack, are merged with the entries declared in `config/MerlinLib/*.json`, and take effect after
`/reload`.

#### Minimal example

```java
Identifier id = Identifier.fromNamespaceAndPath("yourmod", "frostbite");

MerlinApi.enchantments().register(id)
        .maxLevel(3)
        .weight(5)
        .anvilCost(2)
        .cost(10, 10, 40, 10)                       // min/max cost
        .slots("any")
        .supportedItems("#minecraft:enchantable/weapon")
        .acquisition(Acquisition.TREASURE_ONLY)     // treasure only
        .submit();                                  // throws IllegalArgumentException naming the field when invalid
```

Every chained method of `EnchantmentBuilder`: `description(JsonElement)`, `defaultDescription()`, `weight(int)`,
`maxLevel(int)`, `cost(minBase, minPerLevel, maxBase, maxPerLevel)`, `anvilCost(int)`, `slots(String...)`,
`supportedItems(String|JsonElement)`, `primaryItems(String)`, `exclusiveSet(JsonElement)`,
`effects(JsonObject)`, `acquisition(Acquisition)`, `enabled(boolean)`.

Querying and disabling:

```java
MerlinApi.enchantments().registeredIds();
MerlinApi.enchantments().describe(id);   // Optional<EnchantmentInfo>: source / enabled / level / weight
MerlinApi.enchantments().disable(id);    // wins over everything, including the config file
MerlinApi.enchantments().enable(id);
```

#### Declaring it in a configuration file (no code needed)

`config/MerlinLib/enchantments.json`; the root may be an array or `{"enchantments": [...]}`. The fields match
`EnchantmentBuilder` one to one, plus `enabled: false` to disable it outright; `min_cost` / `max_cost` accept an
`int` or `{"base": n, "per_level_above_first": n}`. An entry that fails to parse is reported with
**file + field path + expected type** and is skipped, so it can never break startup or `/reload`.

Priority for the same id: `data pack < code API < this mod's config JSON`. A higher priority overwrites and says so
in the log; nothing is silent.

### 2. Effects (code registry: config changes parameters, it cannot add)

`MobEffect` is a code-registered static registry, so **a new effect can only be written in code**;
`config/MerlinLib/effects.json` can only recolour or disable an effect that exists (see the boundary note below).

```java
Identifier slowId = Identifier.fromNamespaceAndPath("yourmod", "chill");

MerlinApi.effects().register(slowId)
        .category(MobEffectCategory.BENEFICIAL)
        .color("#7FD4E8")                 // an int (ARGB) or a palette name works too
        .submit();
```

An effect with a per-tick behaviour:

```java
MerlinApi.effects().register(slowId)
        .color("#7FD4E8")
        .onTick((ServerLevel level, LivingEntity entity, int amplifier) -> {
            // returning true means this tick really did something (particles and so on are shown)
            return false;
        })
        .submit();
```

Overriding an existing effect (this can also be written into `effects.json`):

```java
MerlinApi.effects().overrideColor(Identifier.withDefaultNamespace("slowness"), 0xFF5A6ACF);
MerlinApi.effects().disable(Identifier.withDefaultNamespace("slowness"));
```

> **Boundary**: `effects.json` accepts only `id`, `enabled` and `color`, and can never create an effect; a new
> effect has to go through `EffectBuilder`.

### 3. Potions (a few lines once the effect is registered)

Requirement: the effect is registered (the order does not matter, ids are resolved late).

```java
MerlinApi.potions().potionFor(slowId)      // a PotionBuilder pre-filled with the effect
        .normalDuration(1200)
        .longDuration(3600)
        .strongDuration(300)
        .variants()                        // also registers the long_ and strong_ variants
        .submit();
```

Several effects in one potion:

```java
MerlinApi.potions().register(Identifier.fromNamespaceAndPath("yourmod", "trance"))
        .effect(slowId, 1200, 1)
        .effect(nightVisionId, 1200)
        .submit();
```

### 4. When content is assembled

1. Mod initialisation: a code API `submit()` only records the draft in the registry.
2. `ContentManager.refresh()` (at startup and on every `/reload`) reads `config/MerlinLib/*.json`, arbitrates it
   against the API entries by priority, and produces a snapshot.
3. The runtime data pack is rebuilt from the snapshot and the game registries reload.

After `/reload` a change report lists what was added, updated, removed and failed; a parse failure is isolated per
entry and reported with its location.

### 5. Other library entry points

- `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi`: the vanilla-styled screen kit (vanilla popup panel,
  scrollbar with dragging, header, labelled rows, number fields, `-` / `+` step pairs, text clipped to a width,
  roman numerals).
- `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen`: base class that measures its panel from content,
  draws the panel at the right moment, pauses the world, and shrinks its content to the window height.
- `com.huziyang520.merlinlib.tools.ui.MerlinScreens`: the client side entry points for the toolkit screens
  (`openItemEditor()`, `openItemEditorFromHotkey()`, `openMacroScreen(parent)`,
  `openHealthEditor(parent, target, holdingEditorItem)`).
- `com.huziyang520.merlinlib.tools.TestWeapons`: the four testing weapons, their damage modifiers and the
  readers and writers of the editable nominal damage.
- `com.huziyang520.merlinlib.tools.hud.CombatFeedback`: the client state behind the crosshair readout and the
  floating damage numbers (the numbers are resolved on the server).
- `com.huziyang520.merlinlib.network.DamageFeedbackPayload`: the damage feedback payload
  (`merlinlib:damage_feedback`).

Counter-examples and known boundaries (effects and potions cannot be added from config, `EnchantmentHelper` damage
modification needs a `ServerLevel`, and so on) are recorded in the maintainer's decision log.

---

## 中文

本文面向在其它模组中使用 MerlinLib 的开发者，所有签名均摘自当前源码（26.3 分支）。

统一入口：

```java
import com.huziyang520.merlinlib.api.MerlinApi;

MerlinApi.enchantments();  // 附魔主线
MerlinApi.effects();       // 效果主线
MerlinApi.potions();       // 药水主线
```

标识符统一用 `net.minecraft.resources.Identifier`（26.3 已从 ResourceLocation 改名）。

### 1. 附魔（数据驱动，配置可增改禁）

附魔在 26.3 是数据包驱动的动态注册表。代码 API 注册的条目进入运行期生成的内存数据包，与
`config/MerlinLib/*.json` 中声明的条目合并，`/reload` 后生效。

#### 最小示例

```java
Identifier id = Identifier.fromNamespaceAndPath("yourmod", "frostbite");

MerlinApi.enchantments().register(id)
        .maxLevel(3)
        .weight(5)
        .anvilCost(2)
        .cost(10, 10, 40, 10)                       // min/max 成本
        .slots("any")
        .supportedItems("#minecraft:enchantable/weapon")
        .acquisition(Acquisition.TREASURE_ONLY)     // 只出现在宝藏
        .submit();                                  // 校验失败抛 IllegalArgumentException，消息含字段名
```

`EnchantmentBuilder` 全部链式方法：`description(JsonElement)`、`defaultDescription()`、`weight(int)`、
`maxLevel(int)`、`cost(minBase, minPerLevel, maxBase, maxPerLevel)`、`anvilCost(int)`、`slots(String...)`、
`supportedItems(String|JsonElement)`、`primaryItems(String)`、`exclusiveSet(JsonElement)`、
`effects(JsonObject)`、`acquisition(Acquisition)`、`enabled(boolean)`。

查询与禁用：

```java
MerlinApi.enchantments().registeredIds();
MerlinApi.enchantments().describe(id);   // Optional<EnchantmentInfo>：来源/启用/等级/权重
MerlinApi.enchantments().disable(id);    // 优先级最高，覆盖数据包与配置
MerlinApi.enchantments().enable(id);
```

#### 配置文件声明（无需写代码）

`config/MerlinLib/enchantments.json`，根可以是数组或 `{"enchantments": [...]}`。字段与
`EnchantmentBuilder` 一一对应，另支持 `enabled: false` 直接禁用；`min_cost`/`max_cost` 接受
`int` 或 `{"base": n, "per_level_above_first": n}`。被解析失败的条目会带「文件名 + 字段路径 +
期望类型」记入日志，不会拖垮启动或 `/reload`。

优先级（同名同 ID）：`数据包 < 代码 API < 本模组配置 JSON`；高优先级覆盖并写日志，不静默。

### 2. 效果（代码注册；配置只调参，不能新增）

`MobEffect` 是代码注册的静态注册表，**新效果只能写代码**；`config/MerlinLib/effects.json`
只能改颜色或禁用既有效果（边界说明见下）。

```java
Identifier slowId = Identifier.fromNamespaceAndPath("yourmod", "chill");

MerlinApi.effects().register(slowId)
        .category(MobEffectCategory.BENEFICIAL)
        .color("#7FD4E8")                 // 也接受 int（ARGB）或调色板名
        .submit();
```

带每刻行为的效果：

```java
MerlinApi.effects().register(slowId)
        .color("#7FD4E8")
        .onTick((ServerLevel level, LivingEntity entity, int amplifier) -> {
            // 返回 true 表示这一刻确实产生了效果（会显示粒子等）
            return false;
        })
        .submit();
```

对既有效果做覆盖（这些也可以写进 `effects.json`）：

```java
MerlinApi.effects().overrideColor(Identifier.withDefaultNamespace("slowness"), 0xFF5A6ACF);
MerlinApi.effects().disable(Identifier.withDefaultNamespace("slowness"));
```

> **边界**：`effects.json` 只接受 `id`、`enabled`、`color` 三个字段，无法凭空新增效果；
> 新效果必须走 `EffectBuilder`。

### 3. 药水（效果注册后几行代码）

前提：效果已注册（顺序无关，ID 延迟解析）。

```java
MerlinApi.potions().potionFor(slowId)      // 预填好效果的 PotionBuilder
        .normalDuration(1200)
        .longDuration(3600)
        .strongDuration(300)
        .variants()                        // 同时注册 long_/strong_ 变体
        .submit();
```

多效果药水：

```java
MerlinApi.potions().register(Identifier.fromNamespaceAndPath("yourmod", "trance"))
        .effect(slowId, 1200, 1)
        .effect(nightVisionId, 1200)
        .submit();
```

### 4. 内容装配时序

1. 模组初始化：代码 API 的 `submit()` 只是把草稿登记进注册器；
2. `ContentManager.refresh()`（启动与每次 `/reload`）读取 `config/MerlinLib/*.json`，
   与 API 条目按优先级仲裁，产出快照；
3. 运行期数据包按快照重建，游戏注册表重载。

`/reload` 后会输出「新增 / 更新 / 删除 / 失效」变更清单；解析失败逐条隔离并给出定位。

### 5. 其它库级入口速查

- `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi`：原版风格界面套件（原版弹窗面板、可拖动滚动条、
  表头、标签行、数字输入框、`-`/`+` 步进对、按宽度裁剪文本、罗马数字）。
- `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaScreen`：界面基类，按内容测量面板、在正确时机画面板、
  暂停世界，并会随窗口高度收缩内容。
- `com.huziyang520.merlinlib.tools.ui.MerlinScreens`：工具集界面的客户端开屏入口
  （`openItemEditor()`、`openItemEditorFromHotkey()`、`openMacroScreen(parent)`、
  `openHealthEditor(parent, target, holdingEditorItem)`）。
- `com.huziyang520.merlinlib.tools.TestWeapons`：四件测试武器、它们的伤害修饰符，以及可编辑标称伤害的读写。
- `com.huziyang520.merlinlib.tools.hud.CombatFeedback`：准星伤害显示与飘字背后的客户端状态（数值由服务端下发）。
- `com.huziyang520.merlinlib.network.DamageFeedbackPayload`：伤害反馈载荷（`merlinlib:damage_feedback`）。

反例与已知边界：效果与药水不能由配置新增，而 `EnchantmentHelper` 的伤害修改需要 `ServerLevel`，
所以相关事件只在服务端触发。
