# MerlinLib

[English](#english) | [中文](#中文)

---

## English

MerlinLib is a Minecraft **library mod**. It gives mod authors three registration lines — **enchantments,
effects and potions** — plus join notices, loot injection, mob behaviour hooks, a data-driven content file
and a built-in testing toolkit.

- Target: **Minecraft 1.21.1**, **NeoForge 21.1**
- Sides: **client and server** — install it on both
- Author: Huziyang520 · Licence: MIT
- Repository: <https://github.com/Huziyang520/MerlinLib> · Issues: <https://github.com/Huziyang520/MerlinLib/issues>
- Downloads: <https://www.curseforge.com/minecraft/mc-mods/merlinlib>

### Install

1. Put `merlinlib-<version>-neoforge-1.21.1.jar` into `mods/`, on the client and on the server.
2. Start the game once. The library writes `config/MerlinLib/client.toml` and
   `config/MerlinLib/server.toml`.

### In game

| Command | What it does |
|---|---|
| `/merlinlib list` | lists every non-vanilla enchantment in the registry |
| `/merlinlib book <enchantment>` | gives an enchanted book of that enchantment |
| `/reload` | re-reads `config/MerlinLib/enchantments.json` and `effects.json`, then prints a change report |

The settings screen opens from the mod list's **Config** button. Two key bindings are registered under
their own controls category: **left arrow** opens the item editor, **right arrow** opens the macros; both
can be rebound like any vanilla key.

### For mod authors

Declare MerlinLib as a required dependency and use the public API in
`com.huziyang520.merlinlib.api`:

```java
MerlinApi.enchantments().register(...);
MerlinApi.events().register(...);
MerlinApi.loot().add(...);
MerlinApi.notices().register(...);
```

`MerlinApi` is the only entry point that is stable across versions; the enchantment, effect and potion
builders behind it are the supported way to add content.

### Licence

MIT. See `LICENSE`.

---

## 中文

MerlinLib 是一个 Minecraft **前置库模组**：为模组作者提供**附魔、状态效果、药水**三条注册线，以及进服提示、
战利品注入、生物行为挂钩、数据驱动的内容文件和一套测试工具。

- 目标版本：**Minecraft 1.21.1**，**NeoForge 21.1**
- 安装位置：**客户端与服务端都要装**
- 作者：Huziyang520 · 许可：MIT
- 仓库：<https://github.com/Huziyang520/MerlinLib> · 问题反馈：<https://github.com/Huziyang520/MerlinLib/issues>
- 下载：<https://www.curseforge.com/minecraft/mc-mods/merlinlib>

### 安装

1. 把 `merlinlib-<版本>-neoforge-1.21.1.jar` 放进 `mods/`（客户端与服务端都要放）。
2. 启动一次游戏，库会生成 `config/MerlinLib/client.toml` 与 `config/MerlinLib/server.toml`。

### 游戏内

| 指令 | 作用 |
|---|---|
| `/merlinlib list` | 列出注册表中所有非原版附魔 |
| `/merlinlib book <附魔>` | 给予该附魔的附魔书 |
| `/reload` | 重新读取 `config/MerlinLib/enchantments.json` 与 `effects.json`，并打印变更报告 |

设置界面从模组列表的 **Config** 按钮进入。两个按键绑定注册在独立的按键分类下：**左方向键**打开物品编辑器，
**右方向键**打开宏；与其它原版按键一样可以改绑。

### 给模组作者

把 MerlinLib 声明为必需前置，使用 `com.huziyang520.merlinlib.api` 下的公开 API：

```java
MerlinApi.enchantments().register(...);
MerlinApi.events().register(...);
MerlinApi.loot().add(...);
MerlinApi.notices().register(...);
```

`MerlinApi` 是跨版本唯一稳定的入口；其后的附魔、状态效果与药水 Builder 是受支持的内容添加方式。

### 许可

MIT，见 `LICENSE`。
