# MerlinLib — Usage Manual / 使用手册
[English](#english) | [中文](#中文)
> Developer API: [`DEV.md`](DEV.md) · Player facing overview: [`../README.md`](../README.md)

---

# English

MerlinLib is a **library**: by itself it only ships a testing/editing toolkit and a few general capabilities, and the content mods (such as Practical Enchantments) depend on it. This page is the "I installed it, now what" reference.

## 1. Quick start

| I want to | How |
|---|---|
| Get the testing items | MerlinLib's two creative tabs, or `/give merlinlib:health_editor` / `/give merlinlib:test_sword` (see §3) |
| Edit an item | The hotkey is **off by default**: run `/merlinlib editorhotkey on`, then press it. Needs command permission (`security.editor_requires_permission` in `server.toml`) |
| Edit a mob's health | **Hold the health editor item, sneak and right click** a mob (turn `tools.health_editor_requires_item` off to drop the "must be held" part) |
| Change settings | In game: `ESC → Mods → MerlinLib → Config`. On disk: `config/MerlinLib/*.toml` |
| Check whether enchantments really loaded | `/merlinlib list`, `/merlinlib pack`, `/merlinlib info <id>` |

> In single player the server file takes effect immediately. On a dedicated server, edit the `config/MerlinLib/server.toml` **on that server**; client side options live in the local `client.toml`.

## 2. Configuration files

Everything is under `config/MerlinLib/`:

| File | What it holds |
|---|---|
| `client.toml` | Local only: hotkey, step sizes, damage numbers, crosshair damage, particle limit, interface animation |
| `server.toml` | Server rules: toolkit permissions, level and damage ceilings, editing scope, join notice switches |
| `enchantments.json` | Enchantments injected from configuration (a data pack in one file) |

Common keys (the template written on first launch is fully commented and is the reference):

| Key | Default | Meaning |
|---|---|---|
| `server.toml → tools.enable_testing_toolkit` | on | Master switch for the testing screens |
| `server.toml → security.editor_requires_permission` | on | Only operators may open the item editor and save |
| `server.toml → security.restrict_tools_to_operators` | on | Restrict the whole toolkit to operators |
| `server.toml → tools.allow_editing_all_items` | **off** | Off means weapons, tools and armour only; on opens the editor for every item |
| `server.toml → tools.allow_non_test_item_damage` | off | Also allow editing the base damage of ordinary weapons |
| `server.toml → tools.allow_mismatched_enchantments` | off | Allow enchantments an item does not normally accept |
| `server.toml → tools.allow_levels_above_max` | on | Allow levels past the enchantment's own maximum (bounded by `max_enchantment_level`) |
| `server.toml → content.disable_generated_enchantments` | off | Emergency switch: do not generate the configured enchantments |
| `server.toml → notices.enabled` / `notices.per_mod` | on / empty | Join notices in chat, and the per mod switches |
| `client.toml → floating_text.enabled` | **off** | Damage numbers flying off a hit mob |
| `client.toml → hud.crosshair_damage` | off | Damage number at the crosshair |
| `client.toml → editor.shift_step` / `scroll_step` / `scroll_step_fast` | 10 / 1 / 10 | Step for Shift click, wheel, and Ctrl+wheel |
| `client.toml → editor.hotkey_enabled` | off | Item editor hotkey (same as `/merlinlib editorhotkey on`) |
| `client.toml → particles.limit_enabled` / `particles.limit` | on / 512 | Particle ceiling, applied on both sides |
| `client.toml → gui.animation_enabled` and friends | on | Interface animation: master switch, open/close kind, tab slide, sub screen |

> A file written by an older build is migrated once on startup (the `config_version` marker) so options whose default changed (damage numbers, editing all items) move to the new default. Values you change after that are never touched again.

## 3. Item registry names

| Registry name | What it is |
|---|---|
| `merlinlib:health_editor` | Health editor: sneak and right click a mob |
| `merlinlib:test_sword` | Testing sword (base damage 7) |
| `merlinlib:test_axe` | Testing axe (9) |
| `merlinlib:test_spear` | Testing spear (5) |
| `merlinlib:test_trident` | Testing trident (9) |

To find an id: try `/give`, hover an item with F3+H enabled, or look at the file names under `common/src/main/resources/assets/merlinlib/models/item/`.

## 4. Commands

Root command: `/merlinlib`. Items marked OP need command permission.

| Command | Who | What it does |
|---|---|---|
| `/merlinlib list` | everyone | Lists the enchantments that really reached the registry: first the ones MerlinLib manages (with their source: configured / code api), then every other mod's and data pack's enchantments. A mod missing here did not load |
| `/merlinlib pack` | everyone | Status of the generated pack: files and counts. Empty usually means `config/MerlinLib/enchantments.json` did not parse (check the log) |
| `/merlinlib info <id>` | everyone | One enchantment: source, enabled, whether it is in the registry, max level, weight |
| `/merlinlib book <id> [level]` | OP | Gives you the enchanted book, level 1 by default |
| `/merlinlib editorhotkey on\|off` | OP | Toggles the item editor hotkey (writes the local `client.toml`, takes effect at once) |

```
/merlinlib list
/merlinlib info merlinlib:unbreakable
/merlinlib pack
/merlinlib book practical_enchantments:venom 3
/merlinlib editorhotkey on
```

Where to find enchantment registry names:

| Mod | Where | Form |
|---|---|---|
| MerlinLib | code api: `common/src/main/java/com/huziyang520/merlinlib/content/BuiltInContent.java`; configured: `config/MerlinLib/enchantments.json` | `merlinlib:<id>` |
| Practical Enchantments | `common/src/main/resources/data/practical_enchantments/enchantment/<name>.json` | `practical_enchantments:<name>` |
| Any other mod | `data/<namespace>/enchantment/*.json` inside its jar, or just `/merlinlib list` | `<namespace>:<name>` |

## 5. Screens

| Screen | Where | Notes |
|---|---|---|
| Item editor | the hotkey, once enabled | `-`/`+` step levels and damage, `X` removes a row. **Hovering a row with the wheel** steps it by `scroll_step`, **Ctrl+wheel** by `scroll_step_fast`, **Shift+wheel** scrolls the list, **Shift+click** on `-`/`+` uses the shift step |
| Health editor | health editor item + sneak right click | Current and maximum health of a mob |
| Macros | the "commands" tab of MerlinLib's config | A macro is a name, one command per line and a key combination; a clash is shown in red on the row |
| Config (general) | `Mods → MerlinLib → Config` | Join notices, the per mod notice screen, the animation screen, restore defaults |
| Animation | General → Configure interface animation | Master switch, opening/closing animation, tab slide, sub screens, and the kind (pop / slide up / slide from the side / fade) |

## 6. When something is wrong

1. Enchantments not working ⇒ `/merlinlib list` and `/merlinlib pack`, then the `generated datapack` line in the log.
2. A setting seems ignored ⇒ check you edited the right file (`client.toml` vs `server.toml`); on a server, edit the server's copy.
3. Want the toolkit gone ⇒ `tools.enable_testing_toolkit = false`.
4. Animation or damage numbers unwanted ⇒ turn the switch off for exactly vanilla behaviour.
5. "This world uses experimental settings" on **every** world load ⇒ see [`DEV.md`](DEV.md) and the general notes: a data pack that adds registry entries must declare a `KnownPack`, otherwise the registry counts as experimental. MerlinLib declares its own, so its generated content no longer triggers it.

---

# 中文

MerlinLib 是一个**前置库**：它自己只提供一套测试/编辑工具与若干通用能力，业务模组（如 Practical Enchantments）依赖它。本文是"装了之后怎么用"的速查；开发接口见 [`DEV.md`](DEV.md)，面向玩家的说明见 [`../README.md`](../README.md)。

## 1. 一分钟上手

| 想做什么 | 怎么做 |
|---|---|
| 拿到测试物品 | 创造模式物品栏里的 MerlinLib 两组标签页，或 `/give merlinlib:health_editor`、`/give merlinlib:test_sword` 等（见 §3） |
| 编辑手上的物品 | 热键默认**关闭**：先 `/merlinlib editorhotkey on`，再按热键；需要命令权限（`server.toml` 的 `security.editor_requires_permission`） |
| 编辑生物血量 | **手持血量编辑器物品 + 潜行 + 右键**点生物（`tools.health_editor_requires_item` 可关掉"必须手持"） |
| 改配置 | 游戏内：`ESC → 模组列表 → MerlinLib → 设置`；文件：`config/MerlinLib/*.toml` |
| 检查附魔是否真的加载 | `/merlinlib list`、`/merlinlib pack`、`/merlinlib info <附魔id>` |

> 单人游戏里服务端配置**当场生效**；专用服务器必须改服务器上的 `config/MerlinLib/server.toml`，客户端项写在本机 `client.toml`。

## 2. 配置文件速查

目录 `config/MerlinLib/`：

| 文件 | 管什么 |
|---|---|
| `client.toml` | 只有本机看得到的东西：热键、步长、伤害飘字、准星伤害、粒子上限、界面动画 |
| `server.toml` | 服务端规则：工具权限、附魔等级/伤害上限、编辑范围、进服聊天提示开关 |
| `enchantments.json` | 用配置注入附魔（等价于自带一个数据包） |

常用键（首次启动生成的模板自带完整注释，模板本身就是文档）：

| 键 | 默认 | 说明 |
|---|---|---|
| `server.toml → tools.enable_testing_toolkit` | 开 | 测试界面总开关 |
| `server.toml → security.editor_requires_permission` | 开 | 只有 OP 能开物品编辑器并保存 |
| `server.toml → security.restrict_tools_to_operators` | 开 | 只限 OP 使用整套测试工具 |
| `server.toml → tools.allow_editing_all_items` | **关** | 关着只对武器/工具/装备开放；开启后所有物品都能开 |
| `server.toml → tools.allow_non_test_item_damage` | 关 | 是否允许改普通武器的基础伤害 |
| `server.toml → tools.allow_mismatched_enchantments` | 关 | 允许给物品加它本来不接受的附魔 |
| `server.toml → tools.allow_levels_above_max` | 开 | 允许等级超过附魔自身定义上限 |
| `server.toml → content.disable_generated_enchantments` | 关 | 应急开关：不再生成 `enchantments.json` 里的附魔 |
| `server.toml → notices.enabled` / `notices.per_mod` | 开 / 空 | 进服聊天提示总开关与逐模组开关 |
| `client.toml → floating_text.enabled` | **关** | 伤害飘字 |
| `client.toml → hud.crosshair_damage` | 关 | 准星处显示伤害数字 |
| `client.toml → editor.shift_step` / `scroll_step` / `scroll_step_fast` | 10 / 1 / 10 | Shift 点击、滚轮、Ctrl+滚轮 的步长 |
| `client.toml → editor.hotkey_enabled` | 关 | 物品编辑热键（等同 `/merlinlib editorhotkey on`） |
| `client.toml → particles.limit_enabled` / `particles.limit` | 开 / 512 | 粒子上限，两端同时生效 |
| `client.toml → gui.animation_enabled` 等 | 开 | 界面动画：总开关、打开/关闭种类、切换类别滑动、二级界面 |

> 旧版本写的文件会在启动时**自动迁移一次**（`config_version` 标记），把改了默认值的项（伤害飘字、对所有物品启用编辑）移到新默认；标记之后你手动改的值不会再被动。

## 3. 物品注册名

| 注册名 | 用途 |
|---|---|
| `merlinlib:health_editor` | 血量编辑器：潜行+右键点生物 |
| `merlinlib:test_sword` | 测试剑（基础伤害 7） |
| `merlinlib:test_axe` | 测试斧（9） |
| `merlinlib:test_spear` | 测试矛（5） |
| `merlinlib:test_trident` | 测试三叉戟（9） |

查找方法：`/give` 试出来、开 F3+H 悬停看物品名，或看 `common/src/main/resources/assets/merlinlib/models/item/` 下的文件名（与注册名一一对应）。

## 4. 指令全表

根指令 `/merlinlib`（标注 OP 的需要权限）：

| 指令 | 权限 | 作用 |
|---|---|---|
| `/merlinlib list` | 所有人 | 列出**真正进入注册表**的附魔：先列 MerlinLib 管理的（标注来源：配置生成 / 代码 API），再列注册表里**其它模组与数据包**的附魔。没出现的模组就是没加载成功 |
| `/merlinlib pack` | 所有人 | 内部生成包状态：文件与条数。为空通常意味着 `config/MerlinLib/enchantments.json` 没解析成功（看日志） |
| `/merlinlib info <附魔id>` | 所有人 | 单个附魔：来源、是否启用、是否在注册表里、最大等级、权重 |
| `/merlinlib book <附魔id> [等级]` | OP | 给自己一本对应附魔书，等级默认 1 |
| `/merlinlib editorhotkey on\|off` | OP | 开关物品编辑热键（写本机 `client.toml`，立即生效） |

```
/merlinlib list
/merlinlib info merlinlib:unbreakable
/merlinlib pack
/merlinlib book practical_enchantments:venom 3
/merlinlib editorhotkey on
```

附魔注册名在哪查：

| 模组 | 位置 | 形式 |
|---|---|---|
| MerlinLib | 代码 API：`common/src/main/java/com/huziyang520/merlinlib/content/BuiltInContent.java`；配置注入：`config/MerlinLib/enchantments.json` | `merlinlib:<id>` |
| Practical Enchantments | `common/src/main/resources/data/practical_enchantments/enchantment/<名字>.json` | `practical_enchantments:<名字>` |
| 其它模组 | 它自己 jar 内 `data/<命名空间>/enchantment/*.json`，或直接 `/merlinlib list` | `<命名空间>:<名字>` |

## 5. 界面速查

| 界面 | 入口 | 关键行为 |
|---|---|---|
| 物品编辑 | 开启后的编辑热键 | `-`/`+` 调等级与伤害，`X` 删行；**悬停某行滚轮** ±`scroll_step`，**Ctrl+滚轮** ±`scroll_step_fast`，**Shift+滚轮**滚动列表，**Shift+点击** `-`/`+` 走大步长 |
| 血量编辑器 | 血量编辑器物品 + 潜行右键 | 调生物当前/最大血量 |
| 命令宏 | MerlinLib 设置的「命令宏」标签 | 一个宏 = 名称 + 多行指令 + 组合键；冲突在列表里标红 |
| 设置（通用） | `模组列表 → MerlinLib → 设置` | 进服提示开关、编辑各模组提示、动画设置入口、恢复默认 |
| 动画设置 | 通用 → 配置界面动画 → 编辑 | 总开关、打开/关闭动画、切换类别滑动、二级界面动画、动画种类（跳出 / 上滑 / 侧滑 / 淡入淡出） |

## 6. 出问题时先看什么

1. 附魔不生效 ⇒ `/merlinlib list` 与 `/merlinlib pack`，再看日志里的 `generated datapack` 行。
2. 配置改了没反应 ⇒ 确认改的是**正确那一侧**的文件（`client.toml` vs `server.toml`），服务器上要改服务器那份。
3. 想彻底关掉测试工具 ⇒ `tools.enable_testing_toolkit = false`。
4. 不想看动画/飘字 ⇒ 对应开关关掉即恢复原版行为。
5. 每次进世界都弹「此世界使用实验性设置」⇒ 这是原版对**数据包提供的注册表条目**的判定：提供附魔等条目的数据包必须在 `PackLocationInfo` 里声明 `KnownPack`，否则该注册表会被判「实验性」。MerlinLib 已为自己那个生成包声明，因此不再触发。
