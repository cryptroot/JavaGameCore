# demo — Cave Defense (`com.cryptroot.demo`)

A bundled reference/example consumer of the framework — not the only place game-specific logic
is allowed to live. Depends on `core` + `tiled`. It is the **thin** layer: rules, content,
balance, and wiring — everything reusable lives in `core`/`tiled`. If this repo is embedded as a
submodule/library (as it is here) and your actual game lives in a sibling project, apply these
same rules there instead of forcing everything into `demo`.

> Before adding a class here, check [../CAPABILITIES.md](../CAPABILITIES.md). If it's a grid,
> path, timer, tween, health/damage, movement, projectile, collision, audio, health bar, entity,
> render, UI, event, camera, or asset primitive, it almost certainly belongs in `core`/`tiled`
> instead. Extend the framework there (with a test), then consume it here.

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
`com.cryptroot.demo` is Cave Defense end to end: `CaveDemoScreen` loads `Cave.tmx` via
`TiledMap.addTo(world)` (drag-pan/scroll-zoom; Space toggles pause and 1/2 set game speed via the
inherited `BaseGameScreen.timeScale`), then `towerdefense.TowerDefenseController` layers a
tower-defense mini-game on top:
- Click to place a tower; `PlacementGrid` rejects a placement that would seal the enemy lane shut.
- `EnemySpawnerComponent` paths enemies along the floor lane (`core.path.Pathfinder`) and gives
  each one a `core.world.component.PathFollowerComponent` (movement), `HealthComponent` (HP), and
  a `core.physics.BoxCollider` matching its sprite.
- `TowerComponent` finds the nearest in-range enemy (`core.world.WorldQueries.nearest`) and fires a
  `core.world.component.HomingProjectileComponent`; impact is confirmed by
  `core.physics.CollisionSystem` shape overlap, not the bullet reaching the enemy's exact position
  — `HomingProjectileComponent`'s own arrival callback is a deliberate no-op (see the note in
  `TowerComponent.fireAt`, and `core/CLAUDE.md`'s `core.physics` entry for why).

Still unbuilt (see "What belongs here" above): the day/phase state machine, currency, roster/rest,
HUD, and lose/win conditions — an enemy that reaches the top just despawns silently for now.

## Build
`mvn -pl demo -am package` builds the fat-jar; run `com.cryptroot.demo.CaveDemoLauncher`.
`mvn -pl demo -am test` runs demo's (GL-free) tests — `junit-jupiter` is already a declared test
dependency in `demo/pom.xml`. See `towerdefense.TowerComponentCollisionTest` for the pattern:
construct real package-private demo components directly, fake a `TextureRegion` via an anonymous
subclass overriding `getRegionWidth`/`getRegionHeight`, and never call `draw()` or otherwise touch
GL, matching `core`'s testing convention.

Follow the same fail-fast/fail-soft rule as `core`/`tiled` (see root `../CLAUDE.md`) for any new
game code, and run `mvn spotless:apply` from the repo root after editing Java files here.
