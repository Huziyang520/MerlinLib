# MerlinLib — Usage

[English](#english) | [中文](#中文)

> Configuration files, commands and the settings screen.
> For the Java API see [`DEV.md`](DEV.md); for the built-in toolkit see [`TOOLKIT.md`](TOOLKIT.md);
> for what can and cannot be changed from a file see [`LIMITS.md`](LIMITS.md).

---

## English

### Where everything lives

MerlinLib keeps everything under `config/MerlinLib/`:

| File | Purpose | How a change applies |
|---|---|---|
| `client.toml` | client switches and display settings | saving is enough |
| `server.toml` | authoritative server switches and ceilings | saving is enough |
| `enchantments.json` | enchantment definitions | **at the next start** |
| `effects.json` | effect recolouring / disabling | **at the next start** |
| `macros.json` | the command macro list, written by the macro screen | saving is enough |

You may add more `.json` files next to `enchantments.json`; they are merged in file name order.

**Content is read once, at startup.** Enchantments are a code registry here — there is no data driven
enchantment registry to inject into — so editing `enchantments.json` or `effects.json` and running
`/reload` changes nothing about what is registered. Restart the game. `/reload` is still worth having:
it reloads loot tables and re-reads the switch files.

`/merlinlib content` prints what is registered and whether it came from the files on disk, so a
mismatch between what you edited and what is running is one command away.

The values in `client.toml` and `server.toml` are read as they are used, so a switch takes effect as
soon as the file is saved.

### `client.toml`

| Key | Default | Meaning |
|---|---|---|
| `hud.crosshair_damage` | `false` | show the nominal damage of the held weapon near the crosshair |
| `hud.crosshair_damage_ticks` | `20` | how long it stays on screen, in ticks |
| `floating_text.enabled` | `false` | damage numbers that fly off a hurt creature |
| `floating_text.duration_ticks` | `20` | their lifetime, in ticks |
| `floating_text.base_scale` | `1.0` | their base size |
| `floating_text.normal_color` / `critical_color` | white / orange | ARGB colours for a normal and a critical hit |
| `floating_text.max_concurrent` | `32` | at most this many at once; the oldest is dropped |
| `editor.hotkey_enabled` | `false` | whether the hotkey may open the item editor (`/merlinlib editorhotkey on` switches it on) |
| `editor.shift_step` | `10` | how much a shift click on `-` / `+` changes a value |
| `editor.scroll_step` / `scroll_step_fast` | `1` / `10` | how much one wheel notch over a row changes its level, and the same with control held |
| `macros.enabled` | `true` | the macro toolkit (its screen and running macros from their keys) |
| `particles.limit_enabled` / `particles.limit` | `true` / `512` | cap on how many particles this client draws per 1/20 second and how many one server send may carry |
| `gui.animation_enabled` | `true` | whether MerlinLib's own screens animate when they open |
| `gui.animation_kind` | `0` | which opening animation: pop / slide up / slide side / fade |
| `gui.animation_screen` / `_tab` / `_sub` | `true` | which places use it: the screen itself, switching tabs, opening a sub-screen |

### `server.toml`

| Key | Default | Meaning |
|---|---|---|
| `security.restrict_tools_to_operators` | `false` | only operators may use the testing toolkit |
| `security.editor_requires_permission` | `true` | the item editor needs command permission |
| `security.health_editor_requires_permission` | `true` | the health editor needs command permission |
| `tools.enable_testing_toolkit` | `true` | master switch: with it off the items and every toolkit screen are gone |
| `tools.max_test_weapon_damage` | integer limit | ceiling for the damage of a testing weapon |
| `tools.max_enchantment_level` | integer limit | ceiling for a level set through the item editor |
| `tools.allow_levels_above_max` | `true` | allow a level above the one the enchantment's own definition declares |
| `tools.allow_mismatched_enchantments` | `false` | allow any enchantment on any item |
| `tools.allow_editing_all_items` | `false` | let the item editor open for every item, not only weapons, tools and armour |
| `tools.allow_non_test_item_damage` | `false` | allow the damage row on items that are not testing weapons |
| `tools.health_editor_requires_item` | `true` | the health editor opens only while its own item is held |
| `content.disable_generated_enchantments` | `false` | emergency switch: none of the enchantments in `config/MerlinLib/*.json` are registered |
| `notices.enabled` | `true` | show the join notices business mods registered through MerlinLib |
| `notices.per_mod` | `""` | per mod switches, written by the settings screen as `<modid>=true;<modid>=false>` |

`server.toml` is authoritative: the client only shows what the server allows and every write is
validated on the server. In single player the integrated server shares the process, so a change applies
at once.

### `enchantments.json`

Adding enchantments from a file is the core of MerlinLib. The field names match the vanilla
enchantment data format one to one, and MerlinLib turns them into registered enchantments at startup.

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

| Field | Meaning |
|---|---|
| `id` | required; `frostbite` is completed to `merlinlib:frostbite`, a full `mymod:frostbite` is used as is |
| `enabled` | `false` disables it. Disabling wins over everything, so it can switch off a same-named enchantment from another source |
| `translation_key` / `description` | either one; when both are missing the key is derived from `id` as `enchantment.<namespace>.<path>` |
| `weight` | `1..1024` |
| `max_level` | `1..255` |
| `min_cost` / `max_cost` | an integer, or `{base, per_level_above_first}` |
| `anvil_cost` | non-negative |
| `slots` | `mainhand` / `offhand` / `hand` / `armor` / `head` / `chest` / `legs` / `feet` / `body` / `any` |
| `supported_items` | an item id, a `#tag` or an array of either |
| `primary_items` | optional, same forms |
| `exclusive_set` | mutual exclusion: a `#tag`, an id or an array |
| `effects` | the vanilla effect components, passed through unchanged |
| `acquisition` | how it is obtained — see below |

Anything that fails to parse is reported as **file + JSON path + expected + found** and the entry is
skipped; the rest of the file still loads.

#### `acquisition` (MerlinLib's own field)

Vanilla has no such field. MerlinLib uses the six switches to drive the 1.21 enchantment tags
(`in_enchanting_table`, `tradeable`, `on_random_loot`, `treasure`, `curse`) *by name*, and on this
version — where those tags do not exist — translates them onto the equivalent 1.20.1 tags and item
classes. A definition written for a newer version therefore keeps working here.

| Field | Effect |
|---|---|
| `enchanting_table` | the enchanting table may offer it |
| `villager_trade` | librarian trades may sell it |
| `fishing` / `loot_chest` | it may appear in random loot |
| `treasure_only` | it is treasure, so it never appears on the enchanting table |
| `curse` | it is a curse |

#### `#minecraft:enchantable/*` names

Those tags arrive with the data driven enchantment registry and do not exist here. MerlinLib
translates the family onto the 1.20.1 tags and item classes — `enchantable/sharp_weapon` becomes
"swords and axes", `enchantable/head_armor` becomes "armour worn on the head", and so on. A name that
does not resolve matches nothing and says so once in the log rather than quietly accepting every item.

### `effects.json`

```json
{
  "effects": [
    { "id": "minecraft:slowness", "enabled": true, "color": "0x5A6ACF" },
    { "id": "minecraft:poison", "enabled": false }
  ]
}
```

- `color` accepts `"0xRRGGBB"`, `"#RRGGBB"`, `"RRGGBB"`, a decimal number or a palette name
  (`red`, `cyan`, `pink`, ...).
- `enabled: false` makes the effect do nothing at all and stops potions from being generated for it.

### Commands

| Command | What it does |
|---|---|
| `/merlinlib list` | every non-vanilla enchantment in the registry, not only the ones this library manages |
| `/merlinlib content` | what is registered and whether it matches the files on disk |
| `/merlinlib info <id>` | one enchantment: whether it is in the registry, its maximum level and weight |
| `/merlinlib book <id> [level]` | hands out the enchanted book (operators) |
| `/merlinlib editorhotkey on\|off` | switches the item editor hotkey, writing `editor.hotkey_enabled` |

### The settings screen

On this loader the entry point is the **Config** button in the mods list. It covers every switch,
grouped by module, and writes only what changed while keeping the comments in the files. The join
notice list lives under *General → Edit each mod's join notices*, and lists MerlinLib's own notices
first, then one row per business mod that registered one; a mod that did not opt in never appears.

---

## 中文

### 文件都在哪

MerlinLib 的一切都在 `config/MerlinLib/` 下：

| 文件 | 作用 | 生效方式 |
|---|---|---|
| `client.toml` | 客户端开关与显示参数 | 保存即可 |
| `server.toml` | 服务端权威开关与上限 | 保存即可 |
| `enchantments.json` | 附魔定义 | **下次启动** |
| `effects.json` | 效果改色 / 禁用 | **下次启动** |
| `macros.json` | 指令宏列表，由宏界面写入 | 保存即可 |

你可以在 `enchantments.json` 旁边新建更多 `.json` 文件（按文件名排序合并）。

**内容只在启动时读一次。** 本版本的附魔是代码注册表，没有数据驱动注册表可注入，因此改完
`enchantments.json` 或 `effects.json` 再执行 `/reload` 什么都不会变——**要重启游戏**。
`/reload` 仍然有用：它会重读战利品表与两个开关文件。

`/merlinlib content` 会打印当前注册了什么、以及是否与磁盘上的文件一致，改完文件想确认"跑的到底是哪份"
只需一条指令。

`client.toml` 与 `server.toml` 的值是**用的时候才读**，所以开关一保存就生效。

### `client.toml`

| 键 | 默认 | 说明 |
|---|---|---|
| `hud.crosshair_damage` | `false` | 在准星附近显示手持武器的标称伤害 |
| `hud.crosshair_damage_ticks` | `20` | 显示时长（游戏刻） |
| `floating_text.enabled` | `false` | 生物受伤时头顶飞出的伤害数字 |
| `floating_text.duration_ticks` | `20` | 持续时间（游戏刻） |
| `floating_text.base_scale` | `1.0` | 基础字号 |
| `floating_text.normal_color` / `critical_color` | 白 / 橙 | 普通命中与暴击的 ARGB 颜色 |
| `floating_text.max_concurrent` | `32` | 同时存在的上限，超出时丢弃最早的 |
| `editor.hotkey_enabled` | `false` | 快捷键是否可打开物品编辑（`/merlinlib editorhotkey on` 可开） |
| `editor.shift_step` | `10` | 按住 Shift 点 `-` / `+` 时的步长 |
| `editor.scroll_step` / `scroll_step_fast` | `1` / `10` | 鼠标停在某行上滚轮每格的变化量，以及按住 Ctrl 时的 |
| `macros.enabled` | `true` | 命令宏总开关（宏界面与按键触发） |
| `particles.limit_enabled` / `particles.limit` | `true` / `512` | 粒子上限：既是客户端每 1/20 秒的绘制上限，也是服务端单次发送上限 |
| `gui.animation_enabled` | `true` | 库自己的界面是否带入场动画 |
| `gui.animation_kind` | `0` | 入场动画种类：跳出 / 上滑 / 侧滑 / 淡入 |
| `gui.animation_screen` / `_tab` / `_sub` | `true` | 哪些位置使用它：界面本身、切换标签、打开二级界面 |

### `server.toml`

| 键 | 默认 | 说明 |
|---|---|---|
| `security.restrict_tools_to_operators` | `false` | 仅 OP 可使用测试工具集 |
| `security.editor_requires_permission` | `true` | 物品编辑需要命令权限 |
| `security.health_editor_requires_permission` | `true` | 血量编辑器需要命令权限 |
| `tools.enable_testing_toolkit` | `true` | 总开关：关闭后测试物品与全部测试界面都不可用 |
| `tools.max_test_weapon_damage` | int 上限 | 测试武器伤害上限 |
| `tools.max_enchantment_level` | int 上限 | 编辑界面可设置的附魔等级上限 |
| `tools.allow_levels_above_max` | `true` | 允许突破附魔自身定义的最大等级 |
| `tools.allow_mismatched_enchantments` | `false` | 允许把任意附魔加到任意物品上 |
| `tools.allow_editing_all_items` | `false` | 允许对所有物品启用编辑（不止武器/工具/装备） |
| `tools.allow_non_test_item_damage` | `false` | 允许非测试武器也出现伤害行 |
| `tools.health_editor_requires_item` | `true` | 血量编辑器需要手持对应物品 |
| `content.disable_generated_enchantments` | `false` | 应急开关：不再注册 `config/MerlinLib/*.json` 里的附魔 |
| `notices.enabled` | `true` | 是否显示业务模组注册的进服聊天提示 |
| `notices.per_mod` | `""` | 各模组开关，由设置界面写成 `<模组id>=true;<模组id>=false>` |

`server.toml` 是权威：客户端只展示服务端允许的范围，每次写入都由服务端校验。单人游戏下集成服务端
与本进程共享，改动立即生效。

### `enchantments.json`

用文件新增附魔是 MerlinLib 的核心能力。字段名与原生附魔数据格式一一对应，MerlinLib 在启动时把它们
变成注册好的附魔。

字段含义见英文部分的两张表（`id` / `enabled` / `translation_key` / `weight` / `max_level` /
`min_cost` / `max_cost` / `anvil_cost` / `slots` / `supported_items` / `primary_items` /
`exclusive_set` / `effects` / `acquisition`）。

解析失败会给出**文件名 + JSON 路径 + 期望值 + 实际值**并跳过该条，文件其余部分照常加载。

#### `acquisition`（MerlinLib 自有字段）

原生附魔没有这个字段。六个开关对应 1.21 的附魔标签（`in_enchanting_table`、`tradeable`、
`on_random_loot`、`treasure`、`curse`），而本版本没有这些标签——MerlinLib 会按名字把它们翻译到
1.20.1 的等价标签与物品类别上，所以照新版写的定义在这里照样能用。

#### `#minecraft:enchantable/*` 这类名字

它们随数据驱动附魔注册表一起出现，本版本并不存在。MerlinLib 会把这一族翻译到 1.20.1 的标签与物品类上
——`enchantable/sharp_weapon` 变成"剑与斧"，`enchantable/head_armor` 变成"戴在头上的护甲"，以此类推。
解析不出来的名字匹配不到任何物品，并会在日志里说一次，而不是悄悄接受所有物品。

### `effects.json`

- `color` 支持 `"0xRRGGBB"`、`"#RRGGBB"`、`"RRGGBB"`、十进制数字或调色板名（`red`、`cyan`、`pink` …）。
- `enabled: false` 会让该效果**停止产生任何效果**，并且不再为它生成药水。

### 指令

| 指令 | 作用 |
|---|---|
| `/merlinlib list` | 列出注册表里所有非原版附魔（不只本库管理的） |
| `/merlinlib content` | 当前注册了什么，以及是否与磁盘上的文件一致 |
| `/merlinlib info <id>` | 单个附魔：是否在注册表、最大等级、权重 |
| `/merlinlib book <id> [等级]` | 发放对应附魔书（需要 OP） |
| `/merlinlib editorhotkey on\|off` | 切换物品编辑快捷键，写入 `editor.hotkey_enabled` |

### 设置界面

本加载器上的入口是模组列表里的 **配置（Config）** 按钮。它按模块收录全部开关，**只写改动过的项，
并且保留文件里的注释**。进服提示的列表在**通用 → 编辑各模组聊天栏提示**：先列 MerlinLib 自己的通知，
再每个"主动适配过本功能"的业务模组一行；没有适配的模组不会出现。
