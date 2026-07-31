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
  `demo.towerdefense.TowerComponent.fireAt`). `CollisionSystem`'s broad-phase detection
  transparently parallelizes across `GameContext.workerPool()` once the collider count reaches
  `CollisionSystem.PARALLEL_THRESHOLD` (wired automatically by `RenderPipeline` — no consumer
  changes needed); below the threshold it always runs inline. Resolution (firing listeners) is
  always sequential regardless.
- `core.concurrent` — `WorkerPool`: a dedicated `ForkJoinPool` wrapper for CPU-bound, read-only
  parallel work over an int range, chunked and load-balanced by the caller. `parallelFor` blocks
  until every chunk completes (it has no results to defer). `mapChunks` does NOT block — it forks
  the chunks and returns a `TaskGate<R>` immediately; call `TaskGate.get()` once the results are
  actually needed (that join is the new deferred "gate", safe to call more than once). One
  `WorkerPool` instance lives on every `GameContext` (`workerPool()`, disposed alongside the other
  services) — do not construct a second one per system; pass the context's pool in. Only ever
  parallelize work that doesn't mutate shared state; keep any mutually-exclusive follow-up (world
  mutation, listener callbacks) single-threaded and run it after `TaskGate.get()` returns, exactly
  like `CollisionSystem` does.
- `core.audio` — `AudioManager`: a `Sound`/`Music` cache mirroring `ResourceManager`'s
  `getOrCreate*`-by-classpath pattern, plus master/sfx/music volume (fail-soft clamped to
  `[0,1]`). Owned by `GameContext.audio()`.

## Conventions
- **Never name a game, in code or in prose.** The compiler already stops `core` from importing game
  code; comments and docs have no such guard, so they are where the leak happens. No game class names,
  no game domain nouns, no game button labels, no "<the game> does X, so do Y" asides — in Javadoc,
  inline comments, this file, `../CAPABILITIES.md`, test names and test comments. Applies to
  `src/test` too: a test helper's example documents the framework, so it must not encode one
  consumer's vocabulary. Describe the pattern and use generic names (`leftPanel`, `MyScreen`,
  `Confirm`, `Item 1`). See the golden rule in [../CLAUDE.md](../CLAUDE.md) for why.
- Reuse libGDX math (`Vector2`, `GridPoint2`, `Color`, `MathUtils`, `Interpolation`).
- No static singletons — services hang off `GameContext`.
- **`ResourceManager`/`AudioManager` own every texture, atlas and sound — exclusively.** Nothing
  outside them may construct or dispose one. If a caller needs a shape the manager doesn't offer
  (derived, sliced or synthesised textures), add a cached `getOrCreate*`-style method here instead
  of letting the caller own the resource; `getOrCreateTexture(key, factory)` is the extension point
  for anything built from a `Pixmap`. A caller-owned `Texture` is a defect (leak + duplicate load)
  even when it is created only once.
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
- A container that overrides `hit(x,y)` must offer the point to children through
  `CompositeWidget.hitChildren(x,y)` rather than looping over `children()` itself. `UiLayer` asks only
  the **top-level** widget for the `Focusable` to focus, and `hitChildren` is what records the child
  that consumed the hit so `hitFocusable()` can find it — a container that loops by hand makes every
  nested `Focusable` (e.g. an `InputField` inside a `VStack` inside a `Panel`) silently unfocusable and
  therefore deaf to the keyboard. Covered by `CompositeWidgetFocusTest`.
- Run `mvn spotless:apply` from the repo root after editing any Java file — it is not bound to
  `test`/`package`, so nothing else formats or checks import order for you.
- `mvn -pl core test` needs no network.

## UI interaction tests (`src/test/java/com/cryptroot/core/uitest`)

Static screenshots (`debug.ScreenCapture` + `debug.CaptureRequest`) verify layout; this package
verifies *behaviour* — it clicks, drags, scrolls and types against a real screen in a real window and
captures a PNG per stage. `WidgetPlaygroundUiTest` is the reference example; a game consumes the
package from its own tests by depending on `core`'s `test-jar`
(`<type>test-jar</type><scope>test</scope>`).

```
UiTestApp.run(UiTestConfig.defaults("My UI"), testCase)   // boots one window, blocks, rethrows failures
UiScenario.begin().waitFrames(3).click(...).waitUntil(...).check(...).capture("x.png").build()
```

Rules worth knowing before writing one:
- **No OS-level input, ever.** No `java.awt.Robot`, no `glfwSetCursorPos`, no screen scraping. Events go
  straight to the `InputProcessor` the screen installed, and `SyntheticInput` (a `Gdx.input` decorator)
  answers the *polled* cursor position that `UiLayer.update` reads each frame. That is what makes the
  same test pass on Windows, X11, XWayland and native Wayland, where pointer warping does not exist and
  synthetic X11 input into a native surface is refused.
- **Resolve widgets at click time** with `WidgetQuery` (`requireButton`, `requireButtonContaining`), not
  from a stored reference: a screen that rebuilds its rows when the data changes (the usual way to
  refresh a list) replaces the widget a reference points at, and clicking a detached one silently hits
  nothing.
- **Await effects, don't count frames.** `Button.onClick` fires after a feedback delay, on a frame that
  `UiLayer.update` *consumes* (so nothing is drawn). Use `waitUntil(...)`; `capture(...)` settles first.
- One `Lwjgl3Application` per JVM, so **one UI test class per module** unless Surefire is set to
  `reuseForks=false`. Tests skip when no display is available; `-Dui.tests.require=true` makes that a
  failure instead (for CI under `xvfb-run -a`, with `LIBGL_ALWAYS_SOFTWARE=1` for llvmpipe), and
  `-Dui.tests.hidden=true` runs without showing the window.
- Assert on widget text/geometry and game state. Do **not** assert exact pixels: rasterisation differs
  between a GPU driver and llvmpipe. `CaptureAssertions.assertNotBlank` is the only image check.
- **Keep the harness game-free**, including its examples and its example screen. `WidgetPlaygroundScreen`
  is a counter, a slider, a list and a field for that reason — one widget per input path, no domain. When
  documenting a rule that a real game motivated, state the pattern ("a screen that rebuilds its rows on
  refresh…") and use neutral names (`Confirm`, `Item 1`, `MyScreen`); a game's own test module is where
  its vocabulary belongs. See the Conventions rule above.
- `mvn -pl core test` runs this package (it needs a display, unlike the rest of the module).
