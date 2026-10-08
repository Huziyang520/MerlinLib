# MerlinLib — Limits

[English](#english) | [中文](#中文)

> What MerlinLib can and cannot do, so that a modpack description never promises more than the mod
> delivers. For the file formats see [`USAGE.md`](USAGE.md).

---

## English

### What a configuration file can do

| Content | Code API | Add from config | Change parameters from config | Disable from config |
|---|---|---|---|---|
| Enchantments | yes | yes | yes | yes |
| Effects | yes | no | yes (colour and so on) | yes |
| Potions | yes | no | yes | yes |

The reason: only enchantments can be built from a description at runtime. `MobEffect` and `Potion` are
code registries — an instance has to exist before it can be registered — and forcing that from a file
would need a mixin whose compatibility risk is not worth taking, so it is not offered.

Note what "add from config" means on this version: the definitions in `config/MerlinLib/*.json` are
turned into registered enchantments **while the game starts**, not while it runs. Editing the file and
reloading does not change what is registered; a restart does. `/merlinlib content` tells you whether
what is running came from the files on disk.

### Two more known limits

- Disabling an effect cannot remove the entry from the registry. MerlinLib turns it into a **no-op** and
  **stops generating potions** for it.
- Server switches are enforced by the server. In single player the client asks the integrated server's
  own player, so a switch applies at once; on a dedicated server the client only opens the screen and
  the server keeps the last word, answering a refused write in chat.

### Names the newer format uses

`#minecraft:enchantable/*` and the enchantment tags (`in_enchanting_table`, `tradeable`, `treasure`,
`exclusive_set/*`, ...) come from the data driven enchantment registry and do not exist on this
version. MerlinLib translates them onto the 1.20.1 tags and item classes it can, so a definition
written for a newer version keeps working — but a name it cannot translate matches nothing, and says so
once in the log rather than quietly accepting every item. Groups such as `exclusive_set` have to be
given as their **members**, not as a tag name.

---

## 中文

### 配置文件能做什么

| 内容 | 代码 API 注册 | 配置文件新增 | 配置文件改参数 | 配置文件禁用 |
|---|---|---|---|---|
| 附魔 | ✅ | ✅ | ✅ | ✅ |
| 效果 | ✅ | ❌ | ✅ 颜色等 | ✅ |
| 药水 | ✅ | ❌ | ✅ | ✅ |

原因：只有附魔能按一份描述在运行期被构建出来。`MobEffect` 与 `Potion` 是代码注册表——必须先有一个实例
才能注册——从文件强行造出来需要 mixin，兼容性风险不可接受，因此不提供。

**注意本版本"配置文件新增附魔"的含义**：`config/MerlinLib/*.json` 里的定义是在**游戏启动时**变成注册好的
附魔，而不是运行中。改完文件执行重载不会改变已注册的内容，**重启才会**。`/merlinlib content` 会告诉你
正在跑的是不是磁盘上那份。

### 另外两条已知限制

- 禁用效果时，效果条目本身无法从注册表中移除。MerlinLib 的做法是把它变成**空操作**，并且**不再为它生成药水**。
- 服务端开关由服务端权威校验：单人游戏下客户端会问**集成服务端**的玩家对象，因此开关一关就立刻生效；
  专用服务器上客户端只负责开屏，服务端保留最终决定权，被拒绝的写入会在聊天栏告知一次。

### 新版格式里的那些名字

`#minecraft:enchantable/*` 与附魔标签（`in_enchanting_table`、`tradeable`、`treasure`、
`exclusive_set/*` …）都来自数据驱动附魔注册表，本版本并不存在。MerlinLib 会把它们翻译到 1.20.1 能对应的
标签与物品类别上，所以照新版写的定义在这里照样能用——但**翻译不了的名字匹配不到任何物品**，并会在日志里
说一次，而不是悄悄接受所有物品。`exclusive_set` 这类分组必须给**展开后的成员**，不能给标签名。
