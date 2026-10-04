# MerlinLib

[English](#english) | [中文](#中文)

---

## English

MerlinLib is a Minecraft **library mod**: it gives mod authors three registration lines - **enchantments, effects and potions** - and ships a ready-to-use testing toolkit on top of them.

- Target: **Minecraft 26.3**, implemented natively for both loaders: **NeoForge 26.3** and **Fabric 26.3**
- Sides: **client and server mod - it must be installed on both**
- Author: Huziyang520 · Licence: **MIT**
- Repository: <https://github.com/Huziyang520/MerlinLib> · Issues: <https://github.com/Huziyang520/MerlinLib/issues>
- Version: **0.8.5**
- Usage manual: [`docs/USAGE.md`](docs/USAGE.md) — every feature and every command, English first, Chinese after
- Developer guide: [`docs/DEV.md`](docs/DEV.md) — the API, the three registration lines, English and Chinese

**Added recently (features, newest first)**

- **Join notices**: a mod registers a chat message shown when a player arrives. Three timings - every join, once per save, first join - colours straight from the component, and a data pack form for packs that prefer files. Players and operators control it from the settings screen: a master switch, then one row per mod, filled from what mods registered at runtime and never from a list kept in the library.
- **Interface animation**: the library's own screens can pop, slide up, slide in from the side, or fade (the fade is a veil over the finished frame - the interface has no global alpha to fade with). Each place has its own switch, and the animation kind and the slide on a tab switch are two separate settings.
- **A particle ceiling that applies on both sides**: one number caps how many particles this client draws per 1/20 second and how many one server send may carry, so a hit dealing hundreds of millions of damage cannot freeze the client.
- `/merlinlib list` now lists every non-vanilla enchantment in the registry, not only the ones this library manages, so a mod whose enchantments failed to load is visible at a glance.
- Item editor: the base damage row answers the wheel as well as the enchantment rows do, and a value typed above the ceiling is clamped to the ceiling instead of being thrown away.

> To depend on it, drop the jar into `mods/` and declare the dependency for your loader (see "For downstream mod authors").

### Contents

- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Java API](#java-api)
- [Built-in testing toolkit](#built-in-testing-toolkit)
- [Limits](#limits)
- [For downstream mod authors](#for-downstream-mod-authors)
- [Building](#building)
- [Project layout](#project-layout)
- [Status](#status)
- [Licence](#licence)

### Quick start

1. Put `merlinlib-<version>-neoforge-26.3.jar` or `merlinlib-<version>-fabric-26.3.jar` into `mods/` (both client and server).
2. Start the game once. MerlinLib creates four files under `config/MerlinLib/`:

   | File | Purpose | How it applies |
   |---|---|---|
   | `client.toml` | client switches and display settings | saving is enough |
   | `server.toml` | authoritative server switches and ceilings | server `/reload` |
   | `enchantments.json` | enchantment definitions | server `/reload` |
   | `effects.json` | effect recolouring / disabling | server `/reload` |

3. After editing the content files run `/reload`; the log prints a **change report**:

```
[MerlinLib] content reloaded, 2 enchantment(s) active
added=1 updated=0 removed=0 failed=0
  + merlinlib:example_frostbite
  ~ (none)
  - (none)
```

Anything that fails to parse is reported as **file + JSON path + expected + found**:

```
[MerlinLib] configuration problem: [enchantments.json] enchantments[0].max_level -> merlinlib:example_frostbite (expected an integer in [1, 255]): found 999
```

The settings screen (Mod Menu on Fabric, the config button on NeoForge) covers every switch, grouped by module, and writes only what changed while keeping the comments in the files.

### Configuration

Everything lives in `config/MerlinLib/`; you may add more `.json` files (they are merged in file name order).

#### client.toml

```toml
[hud]
# Show the nominal damage of the held weapon near the crosshair (the listed value, not the health removed).
crosshair_damage = false
# How long it stays on screen, in ticks.
crosshair_damage_ticks = 20

[floating_text]
# Damage numbers that fly off a hurt creature.
enabled = true
# Duration in ticks (20 ticks = 1 second).
duration_ticks = 20
# Base size.
base_scale = 1.0
# ARGB colours: normal hit / critical hit.
normal_color = 0xFFFFFFFF
critical_color = 0xFFFFAA00
# At most this many numbers at once; the oldest is dropped.
max_concurrent = 32

[editor]
# The hotkey opens the item editor only when this is on. /merlinlib editorhotkey on switches it on.
hotkey_enabled = false
# How much a shift click on - / + changes a value.
shift_step = 10
# How much one wheel notch over a row changes its level.
scroll_step = 1
# The same, while control is held.
scroll_step_fast = 10

[macros]
# Enable the macro toolkit (the macro screen and running macros from their bound keys).
enabled = true

[particles]
# Cap how many particles this client draws per 1/20 second, and how many particles one server send may
# carry. Some paths of the game ask for one particle per point of damage, so a hit with a very large damage
# number asked for a count in the hundreds of millions. The cap cuts the count down at both ends: the server
# clamps what it sends, and the client drops whatever still arrives above the limit.
limit_enabled = true
limit = 512
```

#### server.toml

```toml
[security]
# Only operators may use the testing toolkit.
restrict_tools_to_operators = false
# Require command permission to open the item editor.
editor_requires_permission = true
# Require command permission to open the health editor.
health_editor_requires_permission = true

[tools]
# Master switch of the whole toolkit. The items and every toolkit screen disappear when it is off.
enable_testing_toolkit = true
# Ceiling for the damage of a testing weapon. The integer limit by default.
max_test_weapon_damage = 2147483647
# Ceiling for an enchantment level set through the item editor. The integer limit by default.
max_enchantment_level = 2147483647
# Allow a level above the maximum the enchantment's own definition declares.
allow_levels_above_max = true
# Allow any enchantment on any item (efficiency on a sword, for instance).
allow_mismatched_enchantments = false
# Let the item editor open for every item, not only weapons, tools and armour.
allow_editing_all_items = true
# The health editor only opens while the health editor item is held.
health_editor_requires_item = true
# File layout version, written by the mod; only used for one-off migrations. Leave it alone.
config_version = 3

[content]
# Emergency switch: when true, none of the enchantments in config/MerlinLib/*.json are generated.
disable_generated_enchantments = false

[notices]
# Show the join notices business mods registered through MerlinLib (MerlinApi.notices()). On by default.
enabled = true
# Per mod switches, written by the settings screen as "<modid>=true;<modid>=false>". A mod that is not
# listed uses the default it declared when it registered.
per_mod = ""
```

`server.toml` is authoritative: the client only shows what the server allows, and every write is validated on the server. In single player the integrated server shares the process, so a change applies at once.

#### enchantments.json

Adding enchantments from a configuration file is the core of MerlinLib. The fields match the vanilla enchantment data pack format one to one; MerlinLib assembles them into a data pack **at runtime on the server**, which is why `/reload` is enough.

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
| `id` | Required. `frostbite` is completed to `merlinlib:frostbite`; a full `mymod:frostbite` is used as is |
| `enabled` | `false` disables it. **Disabling wins over everything**, so it can switch off same-named enchantments from other sources |
| `translation_key` / `description` | Either one; when both are missing the key is derived from `id` as `enchantment.<namespace>.<path>` |
| `weight` | Weight, `1..1024` |
| `max_level` | Maximum level, `1..255` |
| `min_cost` / `max_cost` | An integer, or `{base, per_level_above_first}` |
| `anvil_cost` | Anvil cost, non-negative |
| `slots` | `mainhand` / `offhand` / `hand` / `armor` / `head` / `chest` / `legs` / `feet` / `body` / `any` |
| `supported_items` | An item id, a `#tag` or a JSON array |
| `primary_items` | Optional, same forms |
| `exclusive_set` | Mutual exclusion: a `#tag`, an id or an array |
| `effects` | The vanilla effect components, passed through unchanged |
| `acquisition` | How it is obtained. **Vanilla has no such field**; MerlinLib turns it into the `minecraft:tags/enchantment/*` tags |

`acquisition` and the vanilla tags:

| Field | Tags generated |
|---|---|
| `enchanting_table` | `minecraft:in_enchanting_table` |
| `villager_trade` | `minecraft:tradeable` + `minecraft:on_traded_equipment` |
| `fishing` / `loot_chest` | `minecraft:on_random_loot` |
| `treasure_only` | `minecraft:treasure` (otherwise `minecraft:non_treasure`) |
| `curse` | `minecraft:curse` |

#### effects.json

```json
{
  "effects": [
    { "id": "minecraft:slowness", "enabled": true, "color": "0x5A6ACF" },
    { "id": "minecraft:poison", "enabled": false }
  ]
}
```

- `color` accepts `"0xRRGGBB"`, `"#RRGGBB"`, `"RRGGBB"`, a decimal number or a palette name (`red`, `cyan`, `pink`, ...).
- `enabled: false` makes the effect **do nothing at all** and stops potions from being generated for it.

### Java API

The single entry point is `com.huziyang520.merlinlib.api.MerlinApi`.

#### Join notices

A message a mod wants shown in the chat when a player arrives:

```java
MerlinApi.notices().register(
        Identifier.fromNamespaceAndPath("mymod", "dependency_change"),
        NoticeMode.EVERY_JOIN,
        Component.translatable("mymod.notice.dependency_change"));
```

- The id's namespace is the owning mod: the settings screen groups by it, and the per mod switch is stored
  under it.
- `NoticeMode.EVERY_JOIN` (every arrival), `ONCE_PER_SAVE` (once per player per world) and `FIRST_JOIN`
  (once per world, for whoever arrives first).
- Colours come from the component, so a line may carry as many colours as it has styles.
- Register from `onServerStarted`, after your own configuration has been read, to pass
  `enabledByDefault` from it. Registering the same id again replaces the earlier entry.

A data pack can add one at `data/<namespace>/merlinlib/notices/<name>.json`:

```json
{
  "mode": "once_per_save",
  "enabled_by_default": true,
  "lines": [
    [ {"text": "My Mod: ", "color": "#55FF55"}, {"text": "thanks for installing", "color": "gold"} ],
    [ "a second line" ]
  ]
}
```

`mode` is `every_join`, `once_per_save` or `first_join`. A line is either a plain string or a list of
segments, each with `text` plus optional `color` (`#RRGGBB`, `0xRRGGBB`, a decimal number or a colour name),
`bold`, `italic` and `underlined`. Pack notices are read when a player joins, so a reloaded pack is picked
up on the next arrival. A file that does not parse is logged and skipped rather than allowed to stop the
join.

Players and operators control all of it from the settings screen: *General → Join notices in chat* is the
master switch, and *Edit each mod's join notices* lists MerlinLib's own notices first, then one row per
business mod that registered one. Only mods that opted in appear; if none has, the screen says so. The
switches are stored in `server.toml` under `[notices]`, because the server is what sends the message.

#### Server lifecycle

```java
MerlinApi.lifecycle().onServerStarting(() -> MyConfig.load());
MerlinApi.lifecycle().onServerStarted(server -> MyEnchantments.bind(server.registryAccess()));
MerlinApi.lifecycle().onServerStopping(MyCache::clear);
MerlinApi.lifecycle().onPlayerJoin(player -> player.sendSystemMessage(...));
```

Registration is safe from a mod constructor: the callbacks are buffered and run when the loader event fires,
in registration order, and one that throws is logged without stopping the others. The two start moments are
not interchangeable - `onServerStarting` is early, so it is where loot rules must be registered (the packs
have not been read yet), while `onServerStarted` is the first moment the dynamic registries hold anything, so
it is where an enchantment `Holder` can be resolved.

#### Enchantment events

```java
MerlinApi.events().register(venomHolder, BuiltInEvents.POST_ATTACK, (event, context) -> {
    event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
});
```

- Eight built-in types: `POST_ATTACK`, `MODIFY_DAMAGE`, `POST_HURT`, `POST_KILL`, `PROJECTILE_HIT`,
  `ENTITY_TICK`, `MODIFY_BLOCK_DROPS` and `POST_BLOCK_BREAK`. Each event's record is nested in `BuiltInEvents`.
- The equipment is scanned for you (main hand, armour, off hand), and effects that outlive the swing - a
  thrown trident, an arrow in flight - are found through the weapon snapshot the projectile carries.
- Callbacks are matched by enchantment **id**, so they survive a data pack reload, and the context hands you
  the live holder, the level of the enchantment and the item it was found on.
- A callback that throws is logged and skipped, and an event type nobody listens to costs a single bit test:
  no scan happens at all until something registers.

#### Loot injection

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
random-chance condition. `LootTables` lists the vanilla table names as constants so a typo is a compile error
instead of a rule that silently never fires. Rules are applied on both loaders while the tables load, and an
enchantment that does not exist is logged and skipped - a missing entry never stops a table from loading.

#### Mob behaviour hooks

```java
MerlinApi.ai().register(new AiHook() {
    @Override
    public boolean allowsTargeting(Mob mob, LivingEntity target) {
        return !(target instanceof Player player) || !wearsTheCharm(player, mob);
    }
});
```

- Three questions, all optional: `allowsTargeting` (may the mob take that entity as its target),
  `wantsToFlee` (should it actively keep away - the library does the steering, throttled to once a second),
  and `allowsAvoiding` (may the mob's own "run away from this" behaviour apply).
- Refusing a target covers every reason vanilla asks, including retaliation; a rule that should still let a
  mob fight back checks `mob.getLastHurtByMob()` itself.
- Nothing is scanned unless a mod registered a hook, and nothing runs on the client.

#### Interface animation

MerlinLib's own screens pop open instead of appearing at once, and any mod can ask for the same, or for parts of
it:

```java
ScreenIntro intro = UiAnimation.intro();          // honours client.toml gui.animation_enabled / _kind
graphics.pose().translate(intro.offsetX(width), intro.offsetY(height));
graphics.pose().scale((float) intro.scale(), (float) intro.scale());
```

- `Easing` is a set of five curves (`LINEAR`, `EASE_OUT_CUBIC`, `EASE_IN_OUT_SINE`, `EASE_OUT_BACK`,
  `EASE_OUT_ELASTIC`) as plain double functions. They know nothing about Minecraft, so a mod can use them for
  anything.
- `ScreenIntro` is one opening animation: `SCALE_POP`, `SLIDE_UP`, `SLIDE_SIDE` or `FADE`, with `scale()`,
  `offsetX()`, `offsetY()` and `alpha()` for the caller to apply. It is driven by the clock rather than by ticks,
  so a screen asks for the current value whenever it draws and nothing has to pump it.
- The library draws nothing for the caller and does not touch another mod's screens. `UiAnimation.intro()` reads
  MerlinLib's switch; `UiAnimation.intro(int)` takes a mod's own choice; the two classes may be used directly as
  well. The switch only ever affects MerlinLib's own interface.

#### Registering an enchantment

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
                .submit();                    // actually writes it
    }
}
```

Use `build()` to only construct, `submit()` to register. To inspect what really won, including the source of an override:

```java
MerlinApi.enchantments().describe(Identifier.parse("minecraft:sharpness"))
        .ifPresent(info -> System.out.println(info.source() + " " + info.enabled()));
```

#### Registering an effect

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
            .color(MerlinColor.CYAN)            // color("#55FFFF") and color("cyan") work too
            .onTick((level, entity, amplifier) -> {
                entity.hurtServer(level, level.damageSources().freeze(), 1.0F);
                return true;
            })
            .submit();
}
```

#### Registering potions (including one-effect variants)

```java
// After the effect above, this is all it takes to get normal / long / strong variants
MerlinApi.potions().potionFor(FROSTBITE).submit();
// => mymod:frostbite / mymod:long_frostbite / mymod:strong_frostbite

// Or combine several effects by hand
MerlinApi.potions()
        .register(Identifier.fromNamespaceAndPath("mymod", "frostbite_bomb"))
        .effect(FROSTBITE, 1200, 1)
        .effect(Identifier.parse("minecraft:slowness"), 600)
        .variants()
        .submit();
```

#### Colours and gradients

```java
int base = MerlinColor.CYAN;                       // palette constant
int parsed = MerlinColor.parse("#55FFFF").orElseThrow();
int[] tones = MerlinColor.gradient(base);          // [highlight, base, shadow], avoids a flat block of colour
int softer = MerlinColor.mix(MerlinColor.CYAN, MerlinColor.WHITE, 0.25F);
```

### Built-in testing toolkit

The toolkit can be switched off entirely in `server.toml` and can be limited to operators:

- **Four testing weapons**: test sword 7 / test axe 9 / test spear 5 / test trident 8. They are **variants of the vanilla counterparts**, inheriting every vanilla component (sweeping, shield breaking, thrusting, throwing, mining, repair material, enchantability) and only replacing damage and attack speed with editable values. Durability matches netherite. Found in the creative "Tools and Utilities" tab.
- **Item editor** (default key: left arrow; the hotkey ships **off** and is switched on in the settings or with `/merlinlib editorhotkey on`): rename any item, edit its enchantments row by row (level field, `-`, `+`, `X`), add enchantments through a second screen with live search, or remove them all behind a confirmation. The level of a row can be typed, stepped (`-` / `+`, shift for the configured step) or scrolled (the wheel over a row, control for the fast step; shift and wheel scrolls the list). Editing any item is allowed by default; an enchantment the item cannot carry is only offered when `allow_mismatched_enchantments` is on. A testing weapon additionally has a base damage row.
- **Health editor**: sneak right click an entity to edit it, or sneak right click **thin air** to edit yourself. Two fields, current and maximum, whole numbers from 0 to the integer limit; invalid input is outlined in red, explained underneath, and the confirm button is switched off. The write happens on the server, which re-validates permission and clamps the values. Opens with the health editor item held when `health_editor_requires_item` is on (the item is always in the creative "Tools and Utilities" tab).
- **Command macros** (default key: right arrow): a local macro list; every macro has a name, a key combination that may contain several keys held together (mouse buttons included) and any number of command lines, run as the player exactly as written. Stored in `config/MerlinLib/macros.json`, so it follows you between servers; clashing keys are shown in red and hovering them lists all the commands involved.
- **HUD damage numbers**: above the crosshair, the **theoretical damage of this swing** (resolved on the server, including enchantments and modded weapon bonuses, not capped by the target's remaining health). Off by default.
- **Floating damage numbers**: the **health actually removed** flies off the hurt creature, always facing the player; white normally, yellow on a critical hit, growing with the damage, and fading where the entity disappeared.

Every screen uses vanilla widgets and is laid out from the vanilla sprite sheet: panels are the vanilla popup sprite (rounded corners, border and shading included), lists have the vanilla scrollbar and can be dragged, and text is clipped with an ellipsis to the room it really has. The shared part lives in `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi` and can be reused by other mods.

Commands: `/merlinlib list`, `/merlinlib book <id> [level]`, `/merlinlib pack`, `/merlinlib editorhotkey on|off`.

### Limits

MerlinLib is explicit about what can and cannot be added from a configuration file - do not promise more in a modpack:

| Content | Code API | Add from config | Change parameters from config | Disable from config |
|---|---|---|---|---|
| Enchantments | yes | yes (runtime data pack injection) | yes | yes |
| Effects | yes | no | yes (colour and so on) | yes |
| Potions | yes | no | yes | yes |

The reason: in 26.3 only enchantments are a **data pack driven dynamic registry**; `MobEffect` and `Potion` are still **code registries** and cannot be created out of thin air at runtime. Forcing that would need a mixin and the compatibility risk is not acceptable, so it is not offered.

Two more known limits:

- Disabling an effect cannot remove the entry from the registry; MerlinLib turns it into a **no-op** and **stops generating potions** for it.
- Server switches are enforced by the server. In single player the client asks the integrated server's own player, so a switch takes effect immediately; on a dedicated server the client simply opens the screen and the server keeps the last word, answering a refused write in chat.

One display limit worth knowing: the health bar draws at most twenty hearts, so an absurd maximum health is shown as a full row of hearts rather than a billion of them. The attribute itself is untouched - the ceiling is the integer limit.

### For downstream mod authors

`fabric.mod.json`:

```json
"depends": { "merlinlib": ">=0.1.0" }
```

`neoforge.mods.toml`:

```toml
[[dependencies.mymod]]
modId = "merlinlib"
type = "required"
versionRange = "[0.1.0,)"
ordering = "NONE"
side = "BOTH"
```

Call your registration once from the loader entry point (MerlinLib lands content at the data pack layer, so registration order does not matter):

```java
// Fabric
@Override
public void onInitialize() {
    MyEnchantments.register();
    MyEffects.register();
}

// NeoForge: inside your @Mod constructor
public MyMod(IEventBus modBus) {
    MyEnchantments.register();
    MyEffects.register();
}
```

### Building

```powershell
$env:JAVA_HOME="C:\Users\HP\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon"
cd "d:\浏览器\Mymods\共创附魔\MerlinLib\26.3"
.\gradlew.bat :fabric:jar :neoforge:jar --console=plain --no-daemon
```

Output:

```
fabric/build/libs/merlinlib-0.7.3-fabric-26.3.jar
neoforge/build/libs/merlinlib-0.7.3-neoforge-26.3.jar
```

> After changing `gradle.properties` or any metadata, add `--rerun-tasks`, otherwise Gradle reports `UP-TO-DATE` and the jar keeps the old values.

### Project layout

```
common/src/main/java/com/huziyang520/merlinlib/
├─ api/           public API: MerlinApi / EnchantmentApi / EffectApi / PotionApi + builders
├─ content/       content model: enchantment drafts, acquisition, change reports, built-in content
├─ impl/          registries, override arbitration, content snapshots, permission gate, id validation
├─ config/        TOML reading and writing, JSON content parsing, diagnostics
├─ datapack/      runtime in-memory data pack construction and injection
├─ reload/        server reload listener (startup and /reload share one path)
├─ effect/        effect implementation (runtime recolouring / disabling)
├─ tools/         the testing toolkit: items, screens, HUD, macros
├─ mixin/         the accessor and injection mixins (attribute ceilings, save format, HUD, use key)
├─ util/          colour and gradient helpers
└─ platform/      SPI: Services + IPlatformHelper / IPackBridge / IRegistrationBridge
```

MultiLoader layout: **the logic lives in `common`, the platform differences in `fabric` / `neoforge`**, wired through `META-INF/services`.

### Status

Done and compiling (both jars are produced):

- Project skeleton, metadata, MIT licence, icon wiring
- Enchantment library: API + validation + runtime data pack injection + `/reload` change report
- Effect and potion libraries: API + colours and gradients + one-effect variant generation + runtime recolouring and disabling
- Configuration: `client.toml` / `server.toml` / `enchantments.json` / `effects.json`, generated with comments on first launch, plus a settings screen grouped by module that keeps the file comments
- Permissions: operator-only switch and per-editor command permission switches, based on the 26.3 permission system
- Built-in enchantment `merlinlib:unbreakable`
- Testing toolkit: four weapons, item editor with a scrolling enchantment list, second level picker, delete confirmation, health editor (entities and yourself), command macros with key capture, crosshair damage readout and floating damage numbers
- Ceilings widened to the integer limit: maximum health, attack damage and enchantment levels (including the 255 wall in the save format)

Planned:

- Textures for the four weapons and the screens (vanilla pixel style, produced with Blockbench)
- Server to client switch synchronisation

### Licence

Released under the **MIT licence**, see [LICENSE](LICENSE).

```
Copyright (c) 2026 Huziyang520
```

Issues: <https://issue.mengcai.online/>

---

## 中文

MerlinLib 是一款 Minecraft **前置库模组**：为模组开发者提供**附魔 / 效果 / 药水**三条注册主线，并内置一套开箱即用的测试工具集。

- 目标版本：**Minecraft 26.3**，双加载器原生实现：**NeoForge 26.3** 与 **Fabric 26.3**
- 部署属性：**客户端 + 服务端模组，两端都必须安装**
- 作者：Huziyang520 · 许可证：**MIT**
- 仓库：<https://github.com/Huziyang520/MerlinLib> · 问题反馈：<https://github.com/Huziyang520/MerlinLib/issues>
- 当前版本：**0.8.5**
- 使用手册：[`docs/USAGE.md`](docs/USAGE.md)——全部功能与全部指令一页速查，英文在前、中文在后
- 开发者文档：[`docs/DEV.md`](docs/DEV.md)——API 与三条注册主线，中英双语

**近期新增（只列功能，不含修复；由新到旧）**

- **进服聊天提示**：业务模组注册一条玩家进入世界时显示的聊天消息。三种时机（每次进入 / 每存档一次 / 首次进入），颜色直接来自组件，也可以写成数据包文件。玩家与管理员在设置界面里控制：一个总开关，下面每个"主动适配过本功能"的模组一行——列表只来自运行时注册，库内不存任何模组清单。
- **界面动画**：库自己的界面可以跳出、上滑、侧滑、淡入淡出（淡入是整屏暗幕渐隐——这套界面没有全局透明度可用）。每个使用位置各有开关，且"动画种类"与"切换类别时滑动"是两个互不影响的设置。
- **两端同时生效的粒子上限**：一个数值同时限制客户端每 1/20 秒的绘制量与服务端单次发送量，一击打出上亿伤害也不会再卡死客户端。
- `/merlinlib list` 现在会列出注册表里**所有非原版附魔**（不只本库管理的），哪个模组的附魔没加载成功一眼可见。
- 物品编辑：基础伤害那一行也支持滚轮（原先只有附魔行支持）；输入的数值超过上限时按上限处理，而不是被丢弃。

> 依赖方式：把本模组放进 `mods/` 即可；开发时按加载器声明前置依赖（见下文「给下游模组作者」）。

### 目录

- [快速开始](#快速开始)
- [配置文件](#配置文件)
- [Java API](#java-api)
- [内置测试工具集](#内置测试工具集)
- [重要边界](#重要边界)
- [给下游模组作者](#给下游模组作者)
- [构建](#构建)
- [工程结构](#工程结构)
- [当前实现状态](#当前实现状态)
- [许可证](#许可证)

### 快速开始

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

设置界面（Fabric 用 Mod Menu，NeoForge 用模组列表里的配置按钮）按模块分页收录了全部开关，**只写改动过的项，并且保留文件里的注释**。

### 配置文件

所有配置集中在 `config/MerlinLib/`，可自由新建更多 `.json` 文件（按文件名排序合并）。

#### client.toml

```toml
[hud]
# 在准星附近短暂显示手持武器的标称伤害（标称值，不是实际扣除的血量）
crosshair_damage = false
# 显示时长（游戏刻）
crosshair_damage_ticks = 20

[floating_text]
# 生物受伤飘字总开关
enabled = true
# 持续时间（游戏刻，20 刻 = 1 秒）
duration_ticks = 20
# 基础字号
base_scale = 1.0
# ARGB 颜色：普通命中 / 暴击
normal_color = 0xFFFFFFFF
critical_color = 0xFFFFAA00
# 同时存在的飘字上限，超出时丢弃最早的
max_concurrent = 32

[editor]
# 快捷键开启物品编辑；默认关闭，可用 /merlinlib editorhotkey on 开启
hotkey_enabled = false
# 按住 Shift 点击 - / + 时的步长
shift_step = 10
# 鼠标停在某条附魔上滚轮每格的变化量
scroll_step = 1
# 按住 Ctrl 时滚轮每格的变化量
scroll_step_fast = 10

[macros]
# 命令宏总开关（宏界面与按键触发）
enabled = true

[particles]
# 粒子数量上限：既是客户端每 1/20 秒最多绘制的数量，也是服务端单次发送的上限。原版有些路径按
# 伤害点数逐点要粒子，一击打出极大伤害时会要到上亿的计数。上限在两端同时夹住：服务端少发、客户端
# 丢弃仍然超标的那些。
limit_enabled = true
limit = 512
```

#### server.toml

```toml
[security]
# 开启后仅 OP 可使用测试工具集
restrict_tools_to_operators = false
# 物品编辑需要命令权限
editor_requires_permission = true
# 血量编辑器需要命令权限
health_editor_requires_permission = true

[tools]
# 测试工具集总开关：关闭后测试物品与全部测试界面都不可用
enable_testing_toolkit = true
# 测试武器伤害上限（默认 Java int 上限）
max_test_weapon_damage = 2147483647
# 编辑界面可设置的附魔等级上限（默认 Java int 上限）
max_enchantment_level = 2147483647
# 允许附魔突破自身定义的最大等级
allow_levels_above_max = true
# 允许把任意附魔加到任意物品上（例如剑上效率）
allow_mismatched_enchantments = false
# 允许对所有物品启用编辑（不止武器/工具/装备）
allow_editing_all_items = true
# 血量编辑器需要手持对应物品
health_editor_requires_item = true
# 文件布局版本，由模组写入，仅用于一次性迁移；不要手改
config_version = 3

[content]
# 应急开关：为 true 时不生成 config/MerlinLib/*.json 里的附魔（代码 API 注册不受影响）
disable_generated_enchantments = false

[notices]
# 是否显示业务模组通过 MerlinLib 注册的进服聊天提示（默认开启）
enabled = true
# 各模组的开关，由设置界面写成 "<模组id>=true;<模组id>=false>"；未列出的模组用其注册时声明的默认值
per_mod = ""
```

`server.toml` 是权威：客户端只展示服务端允许的范围，每次写入都由服务端校验。单人游戏下集成服务端与本进程共享配置，改动立即生效。

#### enchantments.json

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

#### effects.json

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

### Java API

统一入口是 `com.huziyang520.merlinlib.api.MerlinApi`。

#### 进服聊天提示

业务模组想让玩家进入世界时在聊天栏看到一条消息，交给库来发：

```java
MerlinApi.notices().register(
        Identifier.fromNamespaceAndPath("mymod", "dependency_change"),
        NoticeMode.EVERY_JOIN,
        Component.translatable("mymod.notice.dependency_change"));
```

- **id 的命名空间就是归属模组**：设置界面按它分组，逐模组开关也存在它名下。
- 三种时机：`EVERY_JOIN`（每次进入世界）、`ONCE_PER_SAVE`（每个存档每名玩家一次）、
  `FIRST_JOIN`（每个存档只发一次，发给先进来的那位）。
- 颜色来自组件本身，所以一行里可以有多种颜色（组件有多少样式就有多少颜色）。
- 在 `onServerStarted` 里注册、且排在自己的配置读取之后，就能把配置值当作 `enabledByDefault` 传进来；
  同一 id 重复注册是覆盖，换世界再次启动服务器不会重复提示。

数据包也可以加，位置 `data/<命名空间>/merlinlib/notices/<名字>.json`：

```json
{
  "mode": "once_per_save",
  "enabled_by_default": true,
  "lines": [
    [ {"text": "我的模组：", "color": "#55FF55"}, {"text": "感谢安装", "color": "gold"} ],
    [ "第二行" ]
  ]
}
```

`mode` 取 `every_join` / `once_per_save` / `first_join`。一行要么是普通字符串，要么是片段数组，每个片段
有 `text`，可选 `color`（`#RRGGBB`、`0xRRGGBB`、十进制数字或颜色名）、`bold`、`italic`、`underlined`。
数据包内容在玩家进服时读取，所以改完数据包下次进服即生效；解析失败的文件只记日志跳过，不会阻断进服。

玩家与管理员在设置界面里控制这一切：**通用 → 进入世界聊天栏提示**是总开关，**编辑各模组聊天栏提示**
里先列 MerlinLib 自己的通知，然后每个"主动适配了本功能"的业务模组一行；没有模组适配时会明确写出这一点。
开关存在 `server.toml` 的 `[notices]` 段——消息是服务端发的，判断就得在服务端做。

#### 服务器生命周期

```java
MerlinApi.lifecycle().onServerStarting(() -> MyConfig.load());
MerlinApi.lifecycle().onServerStarted(server -> MyEnchantments.bind(server.registryAccess()));
MerlinApi.lifecycle().onServerStopping(MyCache::clear);
MerlinApi.lifecycle().onPlayerJoin(player -> player.sendSystemMessage(...));
```

在模组构造器里注册是安全的：回调先缓冲、等加载器事件触发时按注册顺序执行，某一条抛异常只记日志、
不影响其余。两个"启动"时机不能互换——`onServerStarting` 更早，数据包还没读，所以战利品规则必须挂在
它上面；`onServerStarted` 是动态注册表第一次有内容的时刻，解析附魔 `Holder` 要等它。

#### 附魔事件

```java
MerlinApi.events().register(venomHolder, BuiltInEvents.POST_ATTACK, (event, context) -> {
    event.target().addEffect(new MobEffectInstance(MobEffects.POISON, 100, context.level() - 1));
});
```

- 八种内置事件：`POST_ATTACK`、`MODIFY_DAMAGE`、`POST_HURT`、`POST_KILL`、`PROJECTILE_HIT`、
  `ENTITY_TICK`、`MODIFY_BLOCK_DROPS`、`POST_BLOCK_BREAK`；各事件的记录类型嵌在 `BuiltInEvents` 里。
- 装备由库帮你扫（主手、护甲、副手）；挥砍结束后仍在生效的武器效果——投出去的三叉戟、飞行中的箭——
  通过弹射物携带的武器快照找到。
- 回调按附魔 **id** 匹配，所以数据包重载后依然有效；上下文回传活 `Holder`、附魔等级与命中的物品。
- 回调抛异常只记日志并跳过；没有任何回调的事件类型只做一次位测试，**一次扫描都不会发生**。

#### 战利品注入

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
`LootTables` 把原版表名写成常量，拼错是编译错误，而不是"规则悄悄不触发"。两端都在战利品表加载时应用；
写错/不存在的附魔只记日志跳过，绝不让一张表加载失败。

#### 生物行为挂钩

```java
MerlinApi.ai().register(new AiHook() {
    @Override
    public boolean allowsTargeting(Mob mob, LivingEntity target) {
        return !(target instanceof Player player) || !wearsTheCharm(player, mob);
    }
});
```

- 三个可选问题：`allowsTargeting`（能否把该实体当作攻击目标）、`wantsToFlee`（是否应主动远离——**由库负责转向**，
  每个生物每秒最多一次）、`allowsAvoiding`（它自己的"躲避"行为是否生效）。
- 否决目标覆盖原版的**所有**提问理由（含被攻击后的还手）；希望"主动惹它照打"的规则自己看
  `mob.getLastHurtByMob()` 判断。
- 没有任何模组登记挂钩时，整条路径直接短路；客户端侧完全不跑。

#### 界面动画

MerlinLib 自己的界面是"跳出"而不是直接出现；任何模组都能要到同一套动画，或者只要其中一部分：

```java
ScreenIntro intro = UiAnimation.intro();          // 读 client.toml 的 gui.animation_enabled / _kind
graphics.pose().translate(intro.offsetX(width), intro.offsetY(height));
graphics.pose().scale((float) intro.scale(), (float) intro.scale());
```

- `Easing` 是五条缓动（`LINEAR` / `EASE_OUT_CUBIC` / `EASE_IN_OUT_SINE` / `EASE_OUT_BACK` / `EASE_OUT_ELASTIC`），
  纯 double 函数、与 Minecraft 无关，可以拿去用在任何地方。
- `ScreenIntro` 是一个入场动画：`SCALE_POP` / `SLIDE_UP` / `SLIDE_SIDE` / `FADE`，对外给
  `scale()` / `offsetX()` / `offsetY()` / `alpha()` 四个量由调用方自己施加。它**按时间推进**而不是按 tick，
  界面画的时候随时问就行，不需要谁来"泵"它。
- 库不替调用方画任何东西，也不碰别人的界面：`UiAnimation.intro()` 读的是 MerlinLib 自己的开关，
  `UiAnimation.intro(int)` 接受业务模组自己的选择，两个类也可以直接用。开关**只影响 MerlinLib 自己的界面**。

#### 注册附魔

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

#### 注册效果

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

#### 注册药水（含单效果药水快速生成）

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

#### 颜色快速选择与渐变适配

```java
int base = MerlinColor.CYAN;                       // 调色板常量
int parsed = MerlinColor.parse("#55FFFF").orElseThrow();
int[] tones = MerlinColor.gradient(base);          // [高光, 基色, 阴影]，用于避免纯色块观感
int softer = MerlinColor.mix(MerlinColor.CYAN, MerlinColor.WHITE, 0.25F);
```

### 内置测试工具集

模组内置一组测试工具（可在 `server.toml` 整体禁用，也可设为仅 OP 可用）：

- **四件测试武器**：测试之剑 7 / 测试之斧 9 / 测试之矛 5 / 测试之三叉戟 8。它们是**原版对应武器的变体**——
  完整继承原版物品组件（横扫、破盾、突刺、投掷/激流、采矿、修复材料、附魔能力），只把伤害与攻速换成可编辑值，耐久与下界合金一致。
  创造栏「工具与实用物品」页可取。
- **物品编辑界面**（默认按键：左方向键；快捷键**默认关闭**，可在设置界面或 `/merlinlib editorhotkey on` 开启）：
  改名、逐条增删附魔（等级输入框 + `-` / `+` / `X`，二级选择界面支持实时搜索）、一键清空附魔（带二次确认）。
  等级有四种改法：输入框直接输入、点 `-` / `+`（按住 Shift 用配置的步长）、**鼠标停在某行附魔上滚轮**（按住 Ctrl 用快速步长）、
  Shift + 滚轮滚动列表。默认对所有物品开放；物品本就不能附的附魔只有在 `allow_mismatched_enchantments` 开启时才会出现在选择界面。
  测试武器额外多一行「基础伤害」。
- **生物血量编辑器**：潜行 + 右键实体编辑该实体；潜行 + 右键**空气**编辑自己。当前值/最大值双输入框，
  只接受 0 到 int 上限的整数，非法输入标红并在下方说明、确认按钮置灰；写入在服务端二次校验权限并钳制。
  `health_editor_requires_item` 开启时必须手持血量编辑器（该物品始终在创造栏「工具与实用物品」页）。
- **指令宏**（默认按键：右方向键）：本地宏列表，每条宏有名称、可由多个按键同时按的快捷键（**支持鼠标键**）与任意行指令，
  以玩家身份原样执行；存储在 `config/MerlinLib/macros.json`，换服务器沿用；同键冲突在列表中标红，悬停可看冲突的全部指令。
- **HUD 准星伤害**：准星正上方显示**这一击的理论伤害**（服务端解析后下发，含附魔与模组武器加成，不随目标剩余血量封顶）。**默认关闭**。
- **受伤飘字**：被击中生物头顶飞出**实际扣血**数字，始终正对玩家；普通白字、暴击黄字，大小随伤害增大；实体删除后停在最后位置淡出。**默认开启**。

界面全部使用原版控件与**原版贴图**：面板是原版弹窗精灵（自带圆角、描边、倒角），列表用原版滚动条且可拖动，
文字按实际可用宽度裁剪（超长显示省略号）；面板尺寸由内容测量得出，并会随窗口高度收缩，大 GUI 尺寸下不会超出屏幕。
公共部分沉淀在 `com.huziyang520.merlinlib.tools.ui.vanilla.VanillaUi`，其它模组可直接复用。

指令：`/merlinlib list`、`/merlinlib book <id> [level]`、`/merlinlib pack`、`/merlinlib editorhotkey on|off`。

### 重要边界

MerlinLib 明确区分「能配置新增」与「不能配置新增」，不要在文档或整合包里给出做不到的承诺：

| 内容 | 代码 API 注册 | 配置文件新增 | 配置文件改参数 | 配置文件禁用 |
|---|---|---|---|---|
| 附魔 | ✅ | ✅（运行期数据包注入） | ✅ | ✅ |
| 效果 | ✅ | ❌ | ✅ 颜色等 | ✅ |
| 药水 | ✅ | ❌ | ✅ | ✅ |

原因：26.3 中只有附魔是**数据包驱动的动态注册表**，`MobEffect` 与 `Potion` 仍是**代码注册表**，运行期无法凭空注册。强行改注册表需要 mixin，兼容性风险不可接受，因此不提供。

另外两条已知限制：

- 禁用效果时，效果条目本身无法从注册表中移除，MerlinLib 的做法是把它变成**空操作**并**不再生成对应药水**。
- 服务端开关由服务端权威校验：单人游戏下客户端会问**集成服务端**的玩家对象，因此开关一关就立刻生效；专用服务器上客户端只负责开屏，服务端保留最终决定权，被拒绝的写入会在聊天栏告知一次。

还有一条显示上的限制值得知道：血量条最多绘制 20 颗心，因此把最大生命值调到天文数字时只会显示**一整行心**，而不是十几亿颗。属性本身不受影响——上限就是 int 上限。

### 给下游模组作者

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

### 构建

```powershell
$env:JAVA_HOME="C:\Users\HP\AppData\Roaming\.minecraft\runtime\java-runtime-epsilon"
cd "d:\浏览器\Mymods\共创附魔\MerlinLib\26.3"
.\gradlew.bat :fabric:jar :neoforge:jar --console=plain --no-daemon
```

产物：

```
fabric/build/libs/merlinlib-0.7.3-fabric-26.3.jar
neoforge/build/libs/merlinlib-0.7.3-neoforge-26.3.jar
```

> 改了 `gradle.properties` 或元数据后请加 `--rerun-tasks`，否则 Gradle 会判 `UP-TO-DATE`，jar 里仍是旧值。

### 工程结构

```
common/src/main/java/com/huziyang520/merlinlib/
├─ api/           对外 API：MerlinApi / EnchantmentApi / EffectApi / PotionApi + Builder
├─ content/       内容模型：附魔草稿、获取途径、变更报告、内置内容
├─ impl/          注册表实现、覆盖仲裁、内容快照管理、权限门、ID 校验
├─ config/        TOML 读写（client/server）、JSON 内容解析、配置诊断
├─ datapack/      运行期内存数据包构建与注入
├─ reload/        服务端重载监听（启动与 /reload 共用一条路径）
├─ effect/        效果实现（运行期改色 / 禁用）
├─ tools/         测试工具集：物品、界面、HUD、宏
├─ mixin/         访问器与注入 mixin（属性上限、存档格式、HUD、使用键）
├─ util/          颜色与渐变工具
└─ platform/      SPI：Services + IPlatformHelper / IPackBridge / IRegistrationBridge
```

采用 MultiLoader 结构：**业务逻辑在 `common`，平台差异在 `fabric` / `neoforge`**，两者通过 `META-INF/services` 声明 SPI 实现。

### 当前实现状态

已完成并编译通过（双端 jar 均可产出）：

- ✅ 工程骨架、元数据、MIT 协议、图标接线
- ✅ 附魔库：API + 校验 + 运行期数据包注入 + `/reload` 变更清单
- ✅ 效果库 / 药水库：API + 颜色与渐变 + 单效果药水变体生成 + 运行期改色与禁用
- ✅ 配置：`client.toml` / `server.toml` / `enchantments.json` / `effects.json`，首启自动生成带注释的默认文件，
  并带一个按模块分页、只写改动项且保留注释的设置界面
- ✅ 权限：OP 限制开关，以及「物品编辑 / 血量编辑器需要命令权限」两个独立开关（基于 26.3 新权限系统）
- ✅ 内置附魔 `merlinlib:unbreakable`
- ✅ 测试工具集：四件测试武器、物品编辑界面（可滚动附魔列表）、附魔二级选择、删除确认、血量编辑器（实体与自己）、
  指令宏（含按键捕获与鼠标键）、准星伤害显示与生物受伤飘字
- ✅ 上限放宽到 int 上限：最大生命值、攻击伤害、附魔等级（含存档格式里 255 的那道墙）

进行中 / 待完成：

- ⏳ 4 件武器与界面贴图（由美术智能体 + Blockbench 产出，原版像素风）
- ⏳ 服务端 → 客户端的开关同步

### 许可证

本项目以 **MIT 许可证**开源，协议全文见 [LICENSE](LICENSE)。

```
Copyright (c) 2026 Huziyang520
```

问题反馈：<https://issue.mengcai.online/>
