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
  - Add between frames (input handlers): `world.add(e)`.
  - Spawn during a system's `update()` (e.g. a tower firing a bullet, a spawner creating an enemy):
    `world.queueAdd(e)`, applied by `flushAdditions()`. Both `add`/`queueAdd` return the entity for
    fluent post-wiring (e.g. binding it back onto a component it carries).
  - `RenderPipeline.update()` calls `flushAdditions()` then `flushRemovals()` at the **start** of
    each frame; if you drive systems manually, you must call both yourself. `onRemoved()` fires per
    removed entity.

## Frame / render
- One update hook: `UpdateComponent.update(delta)`, ticked by `UpdateSystem`. No Unity-style
  `Awake/Start/FixedUpdate`.
- Passes render in order `BACKGROUND → WORLD → NORMAL_MAPPED → FOREGROUND_WORLD → UI`. Only
  `WORLD` is Y-sorted (ascending `sortKey()`, typically world Y). Overlays (health bars) use
  `FOREGROUND_WORLD` with `sortKey()==0`.
- `RenderComponent` in `NORMAL_MAPPED` must throw from `draw()` (drawn by its own system).
- `BaseGameScreen.onRender` is sealed and fixed: `timeScale.apply(delta)` → `pipeline.update` →
  `pipeline.processHover` → `pipeline.processCollisions` → `pipeline.render`. A screen cannot
  reorder this — it only attaches/detaches components (`Collider`+`CollisionListener`,
  `HoverableSpriteComponent`, …) and drives `timeScale`'s setters from input handling.

## Newer packages (added after the original core/tiled split)
- `core.grid` — `Grid` geometry only (no gameplay zones). Dependency-free.
- `core.path` — `Pathfinder` (A* + `pathExists`), `Board`, `PathCostStrategy`. Depends on `core.grid`.
- `core.time` — `Timer`, `Cadence`, `Motion`, `Sequence`, `Scheduler`, `SequenceComponent`,
  `TimerComponent`, `TimeScale` (per-screen pause/speed-up multiplier, owned by `BaseGameScreen`).
  Pure timing; the components ride `UpdateSystem`.
- `core.physics` — `Collider`/`BoxCollider` (shape + anchor), `GridCollisions` (collider vs.
  `Grid`/`Board`), `CollisionSystem`/`CollisionListener` (automatic entity-vs-entity overlap
  enter/exit — the collision equivalent of `HoverSystem`). If a game also uses a self-contained
  arrival/impact component on the same entity (e.g. `HomingProjectileComponent`), only one of the
  two may apply a side-effect (damage, a signal, …) for a given event — make the other's callback a
  documented no-op, or the effect double-fires on the frame both happen to trigger together (see
  `demo.towerdefense.TowerComponent.fireAt`).
- `core.audio` — `AudioManager`: a `Sound`/`Music` cache mirroring `ResourceManager`'s
  `getOrCreate*`-by-classpath pattern, plus master/sfx/music volume (fail-soft clamped to
  `[0,1]`). Owned by `GameContext.audio()`.

## Conventions
- Reuse libGDX math (`Vector2`, `GridPoint2`, `Color`, `MathUtils`, `Interpolation`).
- No static singletons — services hang off `GameContext`.
- Extract render/color math into `static` pure methods (e.g. `WorldHealthBarComponent.barColor`,
  `TintFlashRenderComponent.tintAt`) so it is unit-testable without GL. Tests are plain JUnit 5
  with anonymous no-op `RenderComponent`/`TextureRegion` fakes; do not call `draw()` in tests.
- Reusable components take `Predicate`/`Consumer`/`Supplier` callbacks rather than depending on a
  concrete game type, so `core` never references game code — see `HomingProjectileComponent`'s
  `Predicate<WorldEntity> isTargetValid` + `Consumer<WorldEntity> onImpact`, or
  `WorldQueries.nearest`'s `Predicate<T> filter`. The game supplies the closures (e.g. `e ->
  e.get(HealthComponent.class)...`); `core` only calls them.
- A stateful per-frame system that remembers the previous frame's state (one instance per scene —
  e.g. `HoverSystem`, `CollisionSystem`) should re-scan `World#entities()` fresh on every call
  rather than subscribing to `World#onRemoved()`. A despawned entity simply stops appearing in the
  next scan, so enter/exit-style transitions resolve for free with no special-cased cleanup.
- **Fail fast by default.** Every public constructor/method validates its arguments and throws
  immediately at the API boundary: `Objects.requireNonNull(x, "x must not be null")` for stored/
  dereferenced references, `IllegalArgumentException` for out-of-range or malformed values (see
  `Grid`, `DialogueGraph`, `SpriteAnimation`). Skip validation only for per-frame hot-path methods
  (`draw(Batch,...)`, `update(delta)`) unless the check is cheap and clearly warranted. Fail-soft
  (clamp/default/no-op) is fine where it's the class's documented contract (`Timer`/`Cadence`
  clamping a negative duration, `SequenceComponent` treating a null sequence as "done") — but say
  so in the Javadoc.
- Run `mvn spotless:apply` from the repo root after editing any Java file — it is not bound to
  `test`/`package`, so nothing else formats or checks import order for you.
- `mvn -pl core test` needs no network.
