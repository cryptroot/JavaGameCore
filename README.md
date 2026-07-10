# JavaGameCore

A framework-first Java game project built on [libGDX](https://libgdx.com/). The `core` and
`tiled` modules are a reusable engine framework; `game` is a thin, game-specific consumer of
that framework (currently a "Cave Defense" demo).

> **The #1 rule:** don't re-invent the framework. Before writing grid, pathfinding, timer,
> tween, health-bar, entity, render, UI, event, camera, or asset code in `game`, check
> [CAPABILITIES.md](CAPABILITIES.md) — it almost certainly already exists in `core`/`tiled`.

## Modules

This is a Maven reactor project targeting Java 21, using libGDX 1.14.0 (LWJGL3), Jackson, and
JUnit 5.

```
game  ──▶ core            game    Cave Defense — the ported game (currently a demo)
game  ──▶ tiled           tiled   TMX parsing + rendering  (com.cryptroot.tiled)
tiled ──▶ core            core    Engine framework          (com.cryptroot.core)
```

Dependencies point **inward**: `core` never imports `tiled` or `game`, and `tiled` never
imports `game`. A new feature's code belongs in the innermost module that could reuse it.

- **[core](core)** — the engine framework. ECS-lite entity/component system, render pipeline,
  screens, a ~40-widget UI toolkit, event bus, camera controls, asset management, grid geometry,
  A* pathfinding, timers/cadences/coroutine-like sequencing, dialogue/story/i18n. See
  [core/CLAUDE.md](core/CLAUDE.md).
- **[tiled](tiled)** — TMX (Tiled map editor) parsing and rendering: orthogonal tile layers,
  object-layer → entity spawning, tile-grid math. See [tiled/CLAUDE.md](tiled/CLAUDE.md).
- **[game](game)** — the only module allowed to hold game-specific logic (units, balance,
  placement rules, day/phase state machine, screen wiring). See [game/CLAUDE.md](game/CLAUDE.md).

### Where does new code go?

**Would another, unrelated game reuse this unchanged?** → `core` (or `tiled` for tile-map
concerns). **Does it encode game-specific rules, content, or balance?** → `game`.

See [CAPABILITIES.md](CAPABILITIES.md) for a full inventory of what already exists, and
[CLAUDE.md](CLAUDE.md) for the complete set of project conventions and golden rules (no static
singletons, reuse libGDX math types, one `UpdateComponent.update(delta)` hook, entities as
component bags, GL-free unit-testable algorithmic code).

## Build & test

Run from the repository root (or from `Java/` if working inside a nested checkout).

```bash
mvn -pl core test            # core only (no external network needed)
mvn -pl tiled -am test       # tiled + core (needs jackson-dataformat-xml resolvable)
mvn -pl game  -am package    # game fat-jar
mvn test                     # full reactor
```

If Maven Central is unreachable in your environment, required artifacts must already be present
in `~/.m2`.

Run the demo after packaging:

```bash
mvn -pl game -am package
java -cp game/target/*.jar com.cryptroot.demo.CaveDemoLauncher
```

## Documentation

- [CLAUDE.md](CLAUDE.md) — project-wide conventions and golden rules.
- [CAPABILITIES.md](CAPABILITIES.md) — inventory of existing framework capabilities (search here
  before building anything new) and a Unity → Java concept map.
- [core/CLAUDE.md](core/CLAUDE.md), [tiled/CLAUDE.md](tiled/CLAUDE.md),
  [game/CLAUDE.md](game/CLAUDE.md) — per-module rules.
