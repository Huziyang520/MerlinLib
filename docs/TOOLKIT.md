# MerlinLib — Built-in Testing Toolkit

[English](#english) | [中文](#中文)

> The toolkit can be switched off entirely in `server.toml` and can be limited to operators.
> For the switches themselves see [`USAGE.md`](USAGE.md).

---

## English

### Testing weapons

Four weapons — test sword 7, test axe 9, test spear 5, test trident 8. They are **variants of the
vanilla counterparts**: sweeping, shield breaking, thrusting, throwing, mining, repair material and
enchantability are inherited unchanged, and only damage and attack speed are replaced with editable
values. Durability matches netherite. They live in the creative *Tools and Utilities* tab.

### Item editor

Opens with a hotkey — left arrow by default, and the hotkey ships **off**; switch it on in the settings
or with `/merlinlib editorhotkey on`.

- Rename any item.
- Edit its enchantments row by row: a level field, `-`, `+` and `X` per row, and a second screen with
  live search for adding one.
- A level can be typed, stepped (`-` / `+`, shift for the configured step) or scrolled (the wheel over a
  row, control for the fast step); shift and the wheel scrolls the list.
- Removing every enchantment is behind a confirmation.
- Editing any item is allowed; whether an enchantment the item cannot normally carry is offered at all
  depends on `tools.allow_mismatched_enchantments`, and whether non-weapons open at all depends on
  `tools.allow_editing_all_items`.
- A testing weapon additionally has a **base damage** row, which answers the wheel too and clamps a
  value typed above the ceiling instead of throwing it away.

### Health editor

- Sneak right click an entity to edit that entity, or sneak right click **thin air** to edit yourself.
- Two fields, current and maximum, whole numbers from 0 to the integer limit. Invalid input is outlined
  in red, explained underneath, and the confirm button is switched off; a value above the limit is
  clamped rather than refused.
- The write happens on the server, which re-validates permission and clamps the values again.
- With `tools.health_editor_requires_item` on, the item must be held first; it is always in the
  creative *Tools and Utilities* tab.

A maximum health far above the vanilla twenty is handled on the HUD: above one hundred health the row
becomes a **single heart followed by the numbers** (`20/102`), instead of asking vanilla to draw one
heart per two points — which is what a ceiling near the integer limit used to do, and what used to
freeze the client. The attribute itself is untouched.

### Command macros

Right arrow by default. A local macro list; every macro has a name, a key combination that may contain
several keys held together (mouse buttons included) and any number of command lines, run as the player
exactly as written. Stored in `config/MerlinLib/macros.json`, so it follows you between servers.
Clashing keys are shown in red, and hovering them lists every command involved.

### Damage numbers

Two separate things, with two separate switches:

- **Crosshair damage** — above the crosshair, the **theoretical damage of this swing**: resolved on the
  server, including enchantments and modded weapon bonuses, and *not* capped by the target's remaining
  health. Off by default (`hud.crosshair_damage`).
- **Floating damage numbers** — the **health actually removed** flies off the hurt creature, always
  facing the player; white normally, orange on a critical hit, growing with the damage, and fading where
  the entity disappeared. Its size, colours, lifetime and concurrency are all configurable.

### The interface itself

Every screen uses vanilla widgets and is laid out from the vanilla sprite sheet: panels are the vanilla
popup sprite (rounded corners, border and shading included), lists have the vanilla scrollbar and can be
dragged, and text is clipped with an ellipsis to the room it really has. The shared part lives in
`com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi` and can be reused by other mods.

Commands that go with the toolkit are listed in [`USAGE.md`](USAGE.md).

---

## 中文

### 测试武器

四件：测试之剑 7 / 测试之斧 9 / 测试之矛 5 / 测试之三叉戟 8。它们是**原版对应武器的变体**——
横扫、破盾、突刺、投掷、采矿、修复材料与附魔能力都原样继承，只把伤害与攻速换成可编辑值，耐久与下界合金
一致。创造栏「工具与实用物品」页可取。

### 物品编辑界面

用快捷键打开——默认左方向键，且快捷键**默认关闭**，可在设置界面或 `/merlinlib editorhotkey on` 开启。

- 给任意物品改名。
- 逐条编辑附魔：每行有等级输入框与 `-` / `+` / `X`，另有二级选择界面支持实时搜索。
- 等级有四种改法：输入框直接输入、点 `-` / `+`（按住 Shift 用配置的步长）、**鼠标停在某行上滚轮**
  （按住 Ctrl 用快速步长）；Shift + 滚轮滚动列表。
- 一键清空附魔带二次确认。
- 默认允许编辑任意物品；物品本就不能附的附魔是否出现在选择界面取决于
  `tools.allow_mismatched_enchantments`，非武器/工具/装备是否可开取决于 `tools.allow_editing_all_items`。
- 测试武器额外多一行**基础伤害**，它同样支持滚轮，且输入超过上限时按上限处理而不是被丢弃。

### 血量编辑器

- 潜行 + 右键实体编辑该实体；潜行 + 右键**空气**编辑自己。
- 当前值 / 最大值双输入框，只接受 0 到 int 上限的整数。非法输入标红、在下方说明、确认按钮置灰；
  超过上限的值按上限处理，而不是拒绝。
- 写入在服务端进行，服务端会再次校验权限并钳制数值。
- 开启 `tools.health_editor_requires_item` 时必须手持血量编辑器（该物品始终在创造栏「工具与实用物品」页）。

最大值远超原版 20 时 HUD 会这样呈现：血量超过一百后，血条变成**一颗心 + 数值**（如 `20/102`），
而不是让原版按"每 2 点画一颗心"去画——后者在上限接近 int 上限时就是卡死客户端的原因。属性本身不受影响。

### 命令宏

默认右方向键。本地宏列表，每条宏有名称、可由多个按键同时按的快捷键（**支持鼠标键**）与任意行指令，
以玩家身份原样执行；存储在 `config/MerlinLib/macros.json`，换服务器沿用。同键冲突在列表中标红，
悬停可看冲突的全部指令。

### 伤害数字

两件事、两个开关：

- **准心伤害**——准星正上方显示**这一击的理论伤害**：服务端解析后下发，含附魔与模组武器加成，
  **不随目标剩余血量封顶**。默认关闭（`hud.crosshair_damage`）。
- **受伤飘字**——被击中生物头顶飞出**实际扣血**数字，始终正对玩家；普通白字、暴击橙字，大小随伤害增大，
  实体删除后停在最后位置淡出。字号、颜色、时长与同时存在数量都可配置。

### 界面本身

界面全部使用原版控件与**原版贴图**：面板是原版弹窗精灵（自带圆角、描边、倒角），列表用原版滚动条且可拖动，
文字按实际可用宽度裁剪（超长显示省略号）。公共部分沉淀在
`com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi`，其它模组可直接复用。

配套指令见 [`USAGE.md`](USAGE.md)。
