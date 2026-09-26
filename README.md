# MerlinLib

MerlinLib 是一款 Minecraft **前置库模组**：为模组开发者提供**附魔 / 效果 / 药水**三条注册主线，并内置一套开箱即用的测试工具集。

- 目标版本：**Minecraft 26.3**，双加载器原生实现：**NeoForge 26.3** 与 **Fabric 26.3**
- 部署属性：**客户端 + 服务端模组，两端都必须安装**
- 作者：Huziyang520 · 许可证：**MIT**
- 仓库：<https://github.com/Huziyang520/MerlinLib> · 问题反馈：<https://github.com/Huziyang520/MerlinLib/issues>
- 当前版本：**0.3.x（内部测试版）**
- 开发者文档：`模块指南-附魔-效果-药水.md`（含三条主线的完整调用链示例）

> 依赖方式：把本模组放进 `mods/` 即可；开发时按加载器声明前置依赖（见下文「给下游模组作者」）。

---

## 目录

- [快速开始](#快速开始)
- [配置文件](#配置文件)
  - [client.toml](#clienttoml)
  - [server.toml](#servertoml)
  - [enchantments.json](#enchantmentsjson)
  - [effects.json](#effectsjson)
- [Java API](#java-api)
  - [注册附魔](#注册附魔)
  - [注册效果](#注册效果)
  - [注册药水（含单效果药水快速生成）](#注册药水含单效果药水快速生成)
  - [颜色快速选择与渐变适配](#颜色快速选择与渐变适配)
- [内置测试工具集](#内置测试工具集)
- [重要边界](#重要边界)
- [给下游模组作者](#给下游模组作者)
- [构建](#构建)
- [工程结构](#工程结构)
- [当前实现状态](#当前实现状态)
- [许可证](#许可证)

---

## 快速开始

1. 把 `merlinlib-<版本>-neoforge-26.3.jar` 或 `merlinlib-<版本>-fabric-26.3.jar` 放进 `mods/`（客户端与服务端都要放）。
2. 启动一次游戏，MerlinLib 会在 `config/MerlinLib/` 下生成四个文件：

   | 文件 | 作用 | 生效方式 |
   |---|---|---|
   | `client.toml` | 客户端开关与显示参数 | 保存即可生效 |
   | `server.toml` | 服务端权威开关与上限 | 服务端 `/reload` |
   | `enchantments.json` | 附魔内容定义 | 服务端 `/reload` |
   | `effects.json` | 效果改色 / 禁用 | 服务端 `/reload` |

3. 改完内容定义后在游戏里执行 `/reload`，日志会打印一份**变更清单**：

```
[MerlinLib] content reloaded, 2 enchantment(s) active
added=1 updated=0 removed=0 failed=0
  + merlinlib:example_frostbite
  ~ (none)
  - (none)
```

任何解析失败都会给出**文件名 + JSON 路径 + 期望值 + 实际值**，例如：

```
[MerlinLib] configuration problem: [enchantments.json] enchantments[0].max_level -> merlinlib:example_frostbite (expected an integer in [1, 255]): found 999
```

---

## 配置文件

所有配置集中在 `config/MerlinLib/`，可自由新建更多 `.json` 文件（按文件名排序合并）。

### client.toml

```toml
[hud]
# 在准星附近短暂显示本次武器的标称伤害（武器标称值，不是实际扣除的血量）
damage_display = true
damage_display_ticks = 20

[floating_text]
# 生物受伤飘字：持续时间（游戏刻，20 刻 = 1 秒）
duration_ticks = 20
# 基础字号
base_scale = 1.0
# ARGB 颜色：普通命中 / 暴击
normal_color = 0xFFFFFFFF
critical_color = 0xFFFFAA00
# 同时存在的飘字上限，超出时丢弃最早的
max_concurrent = 32

[editor]
# 按住 Shift 点击 - / + 时的步长
shift_step = 10
# 数值框上滚轮每格的变化量
scroll_step = 1
# 按住 Ctrl 时滚轮每格的变化量
scroll_step_fast = 10
```

### server.toml

```toml
[security]
# 开启后仅 OP 可使用测试工具集
restrict_tools_to_operators = false

[tools]
# 测试工具集总开关
enable_testing_toolkit = true
# 测试武器伤害上限
max_test_weapon_damage = 100000
# 编辑界面可设置的附魔等级上限
max_enchantment_level = 255
# 血量编辑上限（默认 Java int 上限）
max_health_value = 2147483647

[content]
# 应急开关：为 true 时不生成 config/MerlinLib/*.json 里的附魔（代码 API 注册不受影响）
disable_generated_enchantments = false
```

### enchantments.json

用**配置文件新增附魔**是 MerlinLib 的核心能力。字段与原生附魔数据包格式一一对应，MerlinLib 会把它们**在服务端运行期拼成数据包注入**，因此 `/reload` 即可生效。

```json
{
  "enchantments": [
    {
      "id": "frostbite",
      "translation_key": "enchantment.mymod.frostbite",
      "weight": 5,
      "max_level": 3,
      "min_cost": { "base": 10, "per_level_above_first": 9 },
      "max_cost": { "base": 40, "per_level_above_first": 9 },
      "anvil_cost": 2,
      "slots": ["mainhand"],
      "supported_items": "#minecraft:enchantable/weapon",
      "primary_items": "#minecraft:enchantable/melee_weapon",
      "exclusive_set": "#minecraft:exclusive_set/damage",
      "acquisition": {
        "enchanting_table": true,
        "villager_trade": true,
        "fishing": false,
        "loot_chest": true,
        "treasure_only": false,
        "curse": false
      },
      "effects": {
        "minecraft:damage": [
          {
            "effect": {
              "type": "minecraft:add",
              "value": { "type": "minecraft:linear", "base": 1.0, "per_level_above_first": 0.5 }
            }
          }
        ]
      }
    }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `id` | 必填。可写 `frostbite`（自动补 `merlinlib:`）或 `mymod:frostbite` |
| `enabled` | `false` 表示禁用。**禁用优先级最高**，可用来关掉本模组连带的附魔或其它来源的同名附魔 |
| `translation_key` / `description` | 二者取其一；都不写时由 `id` 推导 `enchantment.<命名空间>.<路径>` |
| `weight` | 权重，`1..1024` |
| `max_level` | 最大等级，`1..255` |
| `min_cost` / `max_cost` | 可写整数，也可写 `{base, per_level_above_first}` |
| `anvil_cost` | 铁砧消耗，非负 |
| `slots` | `mainhand` / `offhand` / `hand` / `armor` / `head` / `chest` / `legs` / `feet` / `body` / `any` |
| `supported_items` | 物品 id、`#标签` 或 JSON 数组 |
| `primary_items` | 可选，同上 |
| `exclusive_set` | 互斥：`#标签`、`id` 或数组 |
| `effects` | 原版效果组件对象，原样透传到生成的数据包（结构照抄原版附魔即可） |
| `acquisition` | 获取途径。**原生附魔没有这个字段**，MerlinLib 把它翻译成 `minecraft:tags/enchantment/*` 标签 |

`acquisition` 与原生标签的对应关系：

| 字段 | 生成的标签 |
|---|---|
| `enchanting_table` | `minecraft:in_enchanting_table` |
| `villager_trade` | `minecraft:tradeable` + `minecraft:on_traded_equipment` |
| `fishing` / `loot_chest` | `minecraft:on_random_loot` |
| `treasure_only` | `minecraft:treasure`（否则 `minecraft:non_treasure`） |
| `curse` | `minecraft:curse` |

### effects.json

```json
{
  "effects": [
    { "id": "minecraft:slowness", "enabled": true, "color": "0x5A6ACF" },
    { "id": "minecraft:poison", "enabled": false }
  ]
}
```

- `color` 支持 `"0xRRGGBB"`、`"#RRGGBB"`、`"RRGGBB"`、十进制数字或调色板名（`red`、`cyan`、`pink` …）。
- `enabled: false` 会让该效果**停止产生任何效果**并且不再为它生成药水。

---

## Java API

统一入口是 `com.huziyang520.merlinlib.api.MerlinApi`。

### 注册附魔

```java
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.content.Acquisition;
import net.minecraft.resources.Identifier;

public final class MyEnchantments {

    public static void register() {
        MerlinApi.enchantments()
                .register(Identifier.fromNamespaceAndPath("mymod", "frostbite"))
                .maxLevel(3)
                .weight(5)
                .cost(10, 9, 40, 9)          // minBase, minPerLevel, maxBase, maxPerLevel
                .anvilCost(2)
                .slots("mainhand")
                .supportedItems("#minecraft:enchantable/weapon")
                .acquisition(Acquisition.DEFAULT)
                .submit();                    // 真正写入
    }
}
```

只构建不注册用 `build()`；注册用 `submit()`。

查询最终生效状态（含覆盖来源）：

```java
MerlinApi.enchantments().describe(Identifier.parse("minecraft:sharpness"))
        .ifPresent(info -> System.out.println(info.source() + " " + info.enabled()));
```

### 注册效果

```java
import com.huziyang520.merlinlib.api.MerlinApi;
import com.huziyang520.merlinlib.util.MerlinColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;

public static final Identifier FROSTBITE = Identifier.fromNamespaceAndPath("mymod", "frostbite");

public static void registerEffects() {
    MerlinApi.effects()
            .register(FROSTBITE)
            .category(MobEffectCategory.HARMFUL)
            .color(MerlinColor.CYAN)            // 也支持 color("#55FFFF") 或 color("cyan")
            .onTick((level, entity, amplifier) -> {
                entity.hurtServer(level, level.damageSources().freeze(), 1.0F);
                return true;
            })
            .submit();
}
```

### 注册药水（含单效果药水快速生成）

```java
// 接在上面的效果注册之后，几行就能得到 普通 / 长效 / 强力 三个药水
MerlinApi.potions().potionFor(FROSTBITE).submit();
// => mymod:frostbite / mymod:long_frostbite / mymod:strong_frostbite

// 手动组合多效果药水
MerlinApi.potions()
        .register(Identifier.fromNamespaceAndPath("mymod", "frostbite_bomb"))
        .effect(FROSTBITE, 1200, 1)
        .effect(Identifier.parse("minecraft:slowness"), 600)
        .variants()
        .submit();
```

### 颜色快速选择与渐变适配

```java
int base = MerlinColor.CYAN;                       // 调色板常量
int parsed = MerlinColor.parse("#55FFFF").orElseThrow();
int[] tones = MerlinColor.gradient(base);          // [高光, 基色, 阴影]，用于避免纯色块观感
int softer = MerlinColor.mix(MerlinColor.CYAN, MerlinColor.WHITE, 0.25F);
```

---

## 内置测试工具集

模组内置一组测试工具（可在 `server.toml` 整体禁用，也可设为仅 OP 可用）：

- **四件测试武器**：测试之剑 7 / 测试之斧 9 / 测试之矛 5 / 测试之三叉戟 8。它们是**原版对应武器的变体**——
  完整继承原版物品组件（横扫、破盾、突刺、投掷/激流、采矿、修复材料、附魔能力），只把伤害与攻速换成可编辑值。
  创造栏「工具与实用物品」页可取。
- **武器编辑界面**（默认按键 `G`，可在按键设置改绑）：改名、调伤害、逐条增删附魔（二级选择界面支持实时搜索）、
  一键清空附魔（带二次确认）。
- **生物血量编辑器**：潜行 + 右键实体打开；当前值/最大值双输入框，只接受正整数，非法输入标红且确认置灰；
  写入在服务端校验并钳制到 `server.toml` 的上限。
- **指令宏**（默认按键 `O`）：本地宏列表，每条宏绑定一个按键、包含多行指令，以玩家身份原样执行；
  存储在 `config/MerlinLib/macros.json`，换服务器沿用；同键冲突在列表中标红。
- **HUD 伤害数字**：准星正上方显示**这一击的理论伤害**（服务端解析后下发，含附魔与模组武器加成，
  不随目标剩余血量封顶）。
- **受伤飘字**：被击中生物头顶飞出**实际扣血**数字，始终正对玩家；普通白字、暴击黄字，
  大小随伤害增大；实体删除后停在最后位置淡出。

以上界面全部使用原版控件与 18 像素栅格布局，公共部分沉淀在
`com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi`，其它模组可直接复用。

## 重要边界

MerlinLib 明确区分「能配置新增」与「不能配置新增」，不要在文档或整合包里给出做不到的承诺：

| 内容 | 代码 API 注册 | 配置文件新增 | 配置文件改参数 | 配置文件禁用 |
|---|---|---|---|---|
| 附魔 | ✅ | ✅（运行期数据包注入） | ✅ | ✅ |
| 效果 | ✅ | ❌ | ✅ 颜色等 | ✅ |
| 药水 | ✅ | ❌ | ✅ | ✅ |

原因：26.3 中只有附魔是**数据包驱动的动态注册表**，`MobEffect` 与 `Potion` 仍是**代码注册表**，运行期无法凭空注册。强行改注册表需要 mixin，兼容性风险不可接受，因此不提供。

另外两条已知限制：

- 禁用效果时，效果条目本身无法从注册表中移除，MerlinLib 的做法是把它变成**空操作**并**不再生成对应药水**。
- 服务端开关目前由服务端权威校验；客户端不做主动同步（客户端只负责展示与拦截前的预校验）。

---

## 给下游模组作者

`fabric.mod.json`：

```json
"depends": { "merlinlib": ">=0.1.0" }
```

`neoforge.mods.toml`：

```toml
[[dependencies.mymod]]
modId = "merlinlib"
type = "required"
versionRange = "[0.1.0,)"
ordering = "NONE"
side = "BOTH"
```

在加载器入口里调用一次注册即可（MerlinLib 会把内容在数据包层落地，与注册顺序无关）：

```java
// Fabric
@Override
public void onInitialize() {
    MyEnchantments.register();
    MyEffects.register();
}

// NeoForge：放在你的 @Mod 构造器里
public MyMod(IEventBus modBus) {
    MyEnchantments.register();
    MyEffects.register();
}
```

---

## 构建

```powershell
$env:JAVA_HOME="C:\Users\HP\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon"
cd "d:\浏览器\Mymods\共创附魔\MerlinLib\26.3"
.\gradlew.bat :fabric:jar :neoforge:jar --console=plain --no-daemon
```

产物：

```
fabric/build/libs/merlinlib-0.1.0-fabric-26.3.jar
neoforge/build/libs/merlinlib-0.1.0-neoforge-26.3.jar
```

> 改了 `gradle.properties` 或元数据后请加 `--rerun-tasks`，否则 Gradle 会判 `UP-TO-DATE`，jar 里仍是旧值。

---

## 工程结构

```
common/src/main/java/com/huziyang520/merlinlib/
├─ api/           对外 API：MerlinApi / EnchantmentApi / EffectApi / PotionApi + Builder
├─ content/       内容模型：附魔草稿、获取途径、变更报告、内置内容
├─ impl/          注册表实现、覆盖仲裁、内容快照管理、权限门、ID 校验
├─ config/        TOML 读写（client/server）、JSON 内容解析、配置诊断
├─ datapack/      运行期内存数据包构建与注入
├─ reload/        服务端重载监听（启动与 /reload 共用一条路径）
├─ effect/        效果实现（运行期改色 / 禁用）
├─ util/          颜色与渐变工具
└─ platform/      SPI：Services + IPlatformHelper / IPackBridge / IRegistrationBridge
```

采用 MultiLoader 结构：**业务逻辑在 `common`，平台差异在 `fabric` / `neoforge`**，两者通过 `META-INF/services` 声明 SPI 实现。

---

## 当前实现状态

已完成并编译通过（双端 jar 均可产出）：

- ✅ 工程骨架、元数据、MIT 协议、图标接线
- ✅ 附魔库：API + 校验 + 运行期数据包注入 + `/reload` 变更清单
- ✅ 效果库 / 药水库：API + 颜色与渐变 + 单效果药水变体生成 + 运行期改色与禁用
- ✅ 配置：`client.toml` / `server.toml` / `enchantments.json` / `effects.json`，首启自动生成带注释的默认文件
- ✅ 权限：OP 限制开关（基于 26.3 新权限系统）
- ✅ 内置附魔 `merlinlib:unbreakable`

进行中 / 待完成：

- ⏳ 测试武器（4 件）与武器编辑界面、附魔二级选择界面、删除确认弹窗
- ⏳ 生物血量编辑器、指令宏界面
- ⏳ HUD 标称伤害显示、生物受伤飘字
- ⏳ 4 件武器与界面贴图（由美术智能体 + Blockbench 产出，原版像素风）
- ⏳ 服务端 → 客户端的开关同步

---

## 许可证

本项目以 **MIT 许可证**开源，协议全文见 [LICENSE](LICENSE)。

```
Copyright (c) 2026 Huziyang520
```

问题反馈：<https://issue.mengcai.online/>
