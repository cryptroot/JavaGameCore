# JavaGameCore

A framework-first Java game project built on [libGDX](https://libgdx.com/). The `core` and
`tiled` modules are a reusable engine framework; `demo` is a thin, game-specific consumer of
that framework, bundled as a reference example (currently a "Cave Defense" demo). **"Game" in
this document means any consumer of the framework** — `demo`, or (when this repo is embedded as
a submodule/library, as it is in this checkout) a sibling project that depends on `core`/`tiled`.

> **The #1 rule:** don't re-invent the framework. Before writing grid, pathfinding, timer,
> tween, health-bar, entity, render, UI, event, camera, or asset code in your game, check
> [CAPABILITIES.md](CAPABILITIES.md) — it almost certainly already exists in `core`/`tiled`.

## Modules

This is a Maven reactor project targeting Java 21, using libGDX 1.14.0 (LWJGL3), Jackson, and
JUnit 5.

```
demo  ──▶ core            demo    Cave Defense — a bundled reference/example game
demo  ──▶ tiled           tiled   TMX parsing + rendering  (com.cryptroot.tiled)
tiled ──▶ core            core    Engine framework          (com.cryptroot.core)
```

Dependencies point **inward**: `core` never imports `tiled` or a game, and `tiled` never
imports a game. A new feature's code belongs in the innermost module that could reuse it.
`demo` is only one example consumer — a real game (e.g. a sibling project when this repo is
embedded as a submodule/library) follows the same inward-only dependency direction.

- **[core](core)** — the engine framework. ECS-lite entity/component system, render pipeline,
  screens, a ~40-widget UI toolkit, event bus, camera controls, asset management, grid geometry,
  A* pathfinding, timers/cadences/coroutine-like sequencing, dialogue/story/i18n. See
  [core/CLAUDE.md](core/CLAUDE.md).
- **[tiled](tiled)** — TMX (Tiled map editor) parsing and rendering: orthogonal tile layers,
  object-layer → entity spawning, tile-grid math. See [tiled/CLAUDE.md](tiled/CLAUDE.md).
- **[demo](demo)** — a bundled reference/example consumer of the framework (units, balance,
  placement rules, day/phase state machine, screen wiring). It is *not* the only place
  game-specific logic is allowed to live: if this repo is used as a submodule/library (as in
  this checkout), your actual game project is where that logic goes — `demo` just shows the
  pattern. See [demo/CLAUDE.md](demo/CLAUDE.md).

### Where does new code go?

**Would another, unrelated game reuse this unchanged?** → `core` (or `tiled` for tile-map
concerns). **Does it encode game-specific rules, content, or balance?** → the game consuming
the framework (`demo`, or your real game project if this repo is embedded as a submodule).

See [CAPABILITIES.md](CAPABILITIES.md) for a full inventory of what already exists, and
[CLAUDE.md](CLAUDE.md) for the complete set of project conventions and golden rules (no static
singletons, reuse libGDX math types, one `UpdateComponent.update(delta)` hook, entities as
component bags, GL-free unit-testable algorithmic code).

## Build & test

Run from the repository root (or from `Java/` if working inside a nested checkout).

```bash
mvn -pl core test            # core only (no external network needed)
mvn -pl tiled -am test       # tiled + core (needs jackson-dataformat-xml resolvable)
mvn -pl demo  -am package    # demo fat-jar
mvn test                     # full reactor
```

If Maven Central is unreachable in your environment, required artifacts must already be present
in `~/.m2`.

Run the demo after packaging:

```bash
mvn -pl demo -am package
java -cp demo/target/*.jar com.cryptroot.demo.CaveDemoLauncher
```

## Documentation

- [CLAUDE.md](CLAUDE.md) — project-wide conventions and golden rules.
- [CAPABILITIES.md](CAPABILITIES.md) — inventory of existing framework capabilities (search here
  before building anything new) and a Unity → Java concept map.
- [core/CLAUDE.md](core/CLAUDE.md), [tiled/CLAUDE.md](tiled/CLAUDE.md),
  [demo/CLAUDE.md](demo/CLAUDE.md) — per-module rules.
