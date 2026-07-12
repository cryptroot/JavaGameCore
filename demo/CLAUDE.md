# demo — Cave Defense (`com.cryptroot.demo`)

A bundled reference/example consumer of the framework — not the only place game-specific logic
is allowed to live. Depends on `core` + `tiled`. It is the **thin** layer: rules, content,
balance, and wiring — everything reusable lives in `core`/`tiled`. If this repo is embedded as a
submodule/library (as it is here) and your actual game lives in a sibling project, apply these
same rules there instead of forcing everything into `demo`.

> Before adding a class here, check [../CAPABILITIES.md](../CAPABILITIES.md). If it's a grid,
> path, timer, tween, health bar, entity, render, UI, event, camera, or asset primitive, it
> almost certainly belongs in `core`/`tiled` instead. Extend the framework there (with a test),
> then consume it here.

## What belongs here
- Units & combat semantics (HP, damage, attack cadence values, engage range, death handling).
- `Placeable` definitions (plain records/POJOs — **not** ScriptableObjects) and the enemy/trap/boulder types.
- The pathfinding **inputs**: a `core.path.Board` impl (occupancy + walkable-zone folded into
  `isBlocked`) and `PathCostStrategy` impls (the empty/trap/enemy cost numbers, the inverted "tank" override).
- The arena config: which cells are placeable/walkable, lane/spawn/treasure/trigger rows — a
  wrapper around a `core.grid.Grid`.
- The day/phase state machine (Management→Prep→Resolve→Aftermath), currency, roster/rest, HUD,
  placement input, win/lose — all driven off a `GameContext` subclass (no singletons).
- Screen wiring: subclass `core.screen.BaseGameScreen`, populate `world` in `show()`.

## Current state
`com.cryptroot.demo` is a TMX-render demo (`CaveDemoScreen` loads `Cave.tmx` via
`TiledMap.addTo(world)`, with drag-pan/scroll-zoom).

## Build
`mvn -pl demo -am package` builds the fat-jar; run `com.cryptroot.demo.CaveDemoLauncher`.

Follow the same fail-fast/fail-soft rule as `core`/`tiled` (see root `../CLAUDE.md`) for any new
game code, and run `mvn spotless:apply` from the repo root after editing Java files here.
