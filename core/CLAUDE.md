# core — the engine framework (`com.cryptroot.core`)

The innermost module. Depends only on libGDX + Jackson. **Never** imports `tiled` or `game`.
Put reusable, game-agnostic engine primitives here. See root [../CAPABILITIES.md](../CAPABILITIES.md).

## ECS-lite contract
- `WorldEntity` is a `Class → component` map. `with(Type, comp)` registers under `Type` **and**
  every `EntityComponent` sub-interface `comp` implements (`putIfAbsent`, so explicit wins).
  One component per interface type. No transform hierarchy.
- `World` holds entities; `entities()` is a **live, unmodifiable view** iterated directly by
  systems. **Invariant: never structurally mutate the entity list while a system iterates it.**
  - Remove between frames (input handlers): `world.remove(e)`.
  - Remove during a system's `update()`: `world.queueRemove(e)`, applied by `flushRemovals()`.
  - `RenderPipeline.update()` calls `flushRemovals()` at the **start** of each frame; if you drive
    systems manually, you must call it yourself. `onRemoved()` fires per removed entity.

## Frame / render
- One update hook: `UpdateComponent.update(delta)`, ticked by `UpdateSystem`. No Unity-style
  `Awake/Start/FixedUpdate`.
- Passes render in order `BACKGROUND → WORLD → NORMAL_MAPPED → FOREGROUND_WORLD → UI`. Only
  `WORLD` is Y-sorted (ascending `sortKey()`, typically world Y). Overlays (health bars) use
  `FOREGROUND_WORLD` with `sortKey()==0`.
- `RenderComponent` in `NORMAL_MAPPED` must throw from `draw()` (drawn by its own system).

## New packages added in the mirror pass
- `core.grid` — `Grid` geometry only (no gameplay zones). Dependency-free.
- `core.path` — `Pathfinder` (A* + `pathExists`), `Board`, `PathCostStrategy`. Depends on `core.grid`.
- `core.time` — `Timer`, `Cadence`, `Motion`, `Sequence`, `Scheduler`, `SequenceComponent`,
  `TimerComponent`. Pure timing; the components ride `UpdateSystem`.

## Conventions
- Reuse libGDX math (`Vector2`, `GridPoint2`, `Color`, `MathUtils`, `Interpolation`).
- No static singletons — services hang off `GameContext`.
- Extract render/color math into `static` pure methods (e.g. `WorldHealthBarComponent.barColor`,
  `TintFlashRenderComponent.tintAt`) so it is unit-testable without GL. Tests are plain JUnit 5
  with anonymous no-op `RenderComponent`/`TextureRegion` fakes; do not call `draw()` in tests.
- `mvn -pl core test` needs no network.
