# MerlinLib

[English](#english) | [中文](#中文)

---

## English

MerlinLib is a Minecraft **library mod**: it gives mod authors three registration lines — **enchantments,
effects and potions** — and ships a ready-to-use testing toolkit on top of them.

- Sides: **client and server** — it must be installed on both
- Author: Huziyang520 · Licence: **MIT**
- Repository: <https://github.com/Huziyang520/MerlinLib> · Issues: <https://issue.mengcai.online/>

### What it gives a developer

Everything is reached through one entry point, `com.huziyang520.merlinlib.api.MerlinApi`:

| Capability | What it is for |
|---|---|
| **Enchantments** | register one from code, or describe it in `config/MerlinLib/*.json` and let the library register it |
| **Effects** | register a mob effect, recolour or disable an existing one from the config |
| **Potions** | register potions, including the normal / long / strong variants of one effect |
| **Enchantment events** | eight event types with the equipment scan done for you, matched by enchantment id |
| **Loot injection** | put enchanted books or items into vanilla chests and mob drops |
| **Join notices** | a chat message shown when a player arrives, with the per-mod switch handled for you |
| **Server lifecycle** | starting / started / stopping and player join, with the two start moments kept distinct |
| **Mob behaviour hooks** | veto a mob's target, make it keep its distance — ordinary mobs **and** brain mobs such as the piglin family |
| **Interface animation** | the library's own screens animate; any mod can ask for the same curves and intro |
| **Colours and helpers** | colour parsing and gradients, grant counters, smelting lookups |

The library is **client side too**: the settings screen, the toolkit and every display feature live in
the same jar, so a mod that only wants the registration API still installs one file.

### What it ships as content

- **Testing toolkit** — four testing weapons (sword, axe, spear, trident) that are editable variants of
  the vanilla ones, an item editor, a health editor, command macros, crosshair damage and floating
  damage numbers. Every part of it can be switched off, and it can be limited to operators.
- **A settings screen** — reachable from the mods list, covering every switch, grouped by module, and
  writing only what changed while keeping the comments in the files.
- **A vanilla-UI base** — `tools.ui.vanilla.VanillaUi` and the screens built on it use the vanilla
  widget sprites only, and other mods may reuse them.

### Documentation

| Document | What is in it |
|---|---|
| [`docs/USAGE.md`](docs/USAGE.md) | the config files, the commands, the settings screen |
| [`docs/DEV.md`](docs/DEV.md) | the Java API, and how to depend on MerlinLib |
| [`docs/TOOLKIT.md`](docs/TOOLKIT.md) | the testing toolkit in detail |
| [`docs/LIMITS.md`](docs/LIMITS.md) | what a configuration file can and cannot do |

### Recent additions

- **Mob behaviour hooks now cover brain mobs**: a veto answers for the piglin family as well, both when
  anger is recorded and when an attack target is chosen, so a "do not attack this player" rule works the
  same for a piglin, a piglin brute and a zombie.
- **A very large health bar stays readable**: above one hundred health the row becomes a single heart
  followed by the numbers, instead of one heart per two points.
- **Join notices**: a mod registers a chat message and the switch, the screen and the per-mod storage
  are the library's business.
- **Interface animation**: pop, slide up, slide in from the side or fade, each place with its own switch.
- **A particle ceiling on both sides**: one number caps how many particles a client draws per 1/20
  second and how many one server send may carry.
- `/merlinlib list` lists every non-vanilla enchantment in the registry, not only the ones this library
  manages, so a mod whose enchantments failed to register is visible at a glance.

### Licence

Released under the **MIT licence**, see [LICENSE](LICENSE).

```
Copyright (c) 2026 Huziyang520
```

---

## 中文

MerlinLib 是一款 Minecraft **前置库模组**：为模组开发者提供**附魔 / 效果 / 药水**三条注册主线，
并内置一套开箱即用的测试工具集。

- 部署属性：**客户端 + 服务端模组**，两端都必须安装
- 作者：Huziyang520 · 许可证：**MIT**
- 仓库：<https://github.com/Huziyang520/MerlinLib> · 问题反馈：<https://issue.mengcai.online/>

### 给开发者提供什么

统一入口是 `com.huziyang520.merlinlib.api.MerlinApi`：

| 能力 | 用途 |
|---|---|
| **附魔** | 代码注册，或在 `config/MerlinLib/*.json` 里写一份定义、由库完成注册 |
| **效果** | 注册状态效果；已有效果可以改色或禁用 |
| **药水** | 注册药水，含单一效果的 普通 / 长效 / 强力 三连 |
| **附魔事件** | 八种事件类型，装备扫描由库完成，按附魔 id 匹配 |
| **战利品注入** | 往原版箱子与生物掉落里塞附魔书或物品 |
| **进服提示** | 玩家进入世界的聊天消息，逐模组开关由库处理 |
| **服务器生命周期** | 启动中 / 已启动 / 停止与玩家进入，两个"启动"时机严格区分 |
| **生物行为挂钩** | 否决生物的寻敌目标、让它主动远离——普通生物**与** Brain 生物（如猪灵族）都覆盖 |
| **界面动画** | 库自己的界面带动画，任何模组都能拿到同一套曲线与入场动画 |
| **颜色与工具** | 颜色解析与渐变、授予记账、熔炼查询 |

本库**同时也是客户端模组**：设置界面、测试工具与全部显示功能都在同一个 jar 里，所以只想要注册 API 的
模组也只装一个文件。

### 内置内容模块

- **测试工具集**——四件测试武器（剑 / 斧 / 矛 / 三叉戟，原版武器的可编辑变体）、物品编辑界面、血量编辑器、
  命令宏、准心伤害与受伤飘字。整套可以关闭，也可以限制为仅 OP 可用。
- **设置界面**——模组列表里可进，按模块收录全部开关，只写改动过的项并保留文件注释。
- **原版界面基座**——`tools.ui.vanilla.VanillaUi` 及其上的全部界面只用原版控件贴图，其它模组可直接复用。

### 文档

| 文档 | 内容 |
|---|---|
| [`docs/USAGE.md`](docs/USAGE.md) | 配置文件、指令、设置界面 |
| [`docs/DEV.md`](docs/DEV.md) | Java API 与依赖方式 |
| [`docs/TOOLKIT.md`](docs/TOOLKIT.md) | 测试工具集详解 |
| [`docs/LIMITS.md`](docs/LIMITS.md) | 配置文件能做什么、不能做什么 |

### 近期新增

- **生物行为挂钩覆盖 Brain 生物**：否决规则对猪灵族同样生效——记录愤怒与选择攻击目标两处都挂钩，
  所以"别攻击这个玩家"这条规则对猪灵、猪灵蛮兵与僵尸表现一致。
- **极大的血量条仍然可读**：血量超过一百后血条变成一颗心 + 数值，而不是按"每 2 点一颗心"去画。
- **进服聊天提示**：业务模组只注册一条消息，开关、界面与逐模组存储都由库负责。
- **界面动画**：跳出、上滑、侧滑、淡入，每个使用位置各有开关。
- **两端同时生效的粒子上限**：一个数值同时限制客户端每 1/20 秒的绘制量与服务端单次发送量。
- `/merlinlib list` 会列出注册表里**所有非原版附魔**（不只本库管理的），哪个模组的附魔没注册成功一眼可见。

### 许可证

本项目以 **MIT 许可证**开源，协议全文见 [LICENSE](LICENSE)。

```
Copyright (c) 2026 Huziyang520
```
