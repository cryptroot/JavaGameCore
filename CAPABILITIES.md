# Framework capabilities — search here before building anything

If a capability below is **present** or **new**, use it. Do **not** re-implement it in your
game code — whether that's the bundled `demo` sample or an external project that consumes this
framework as a submodule/library.
Package prefixes: `com.cryptroot.core` (core), `com.cryptroot.tiled` (tiled).

## Present (pre-existing)
| Capability | Where | Notes |
|---|---|---|
| Entity container / ECS-lite | `core.world.World`, `WorldEntity`, `EntityComponent` | `with(Class,comp)`/`get`/`has`; one component per interface; auto-registers under sub-interfaces |
| Components | `core.world.*` + `core.world.component.*` | Position, Render, Bounds, Clickable, Update, Triggerable, Tag, DialogueSpeaker, LightSource; defaults incl. `TextureRenderComponent`, `HoverableSpriteComponent` |
| Frame pipeline | `core.render.RenderPipeline`, `core.render.system.*` | `update → processHover → processCollisions → render`; passes `BACKGROUND→WORLD→NORMAL_MAPPED→FOREGROUND_WORLD`. **Screen-space UI is not part of this** — `BaseScreen` draws its `UiLayer` after `onRender` returns |
| Screens / app loop | `core.screen.BaseScreen`, `BaseGameScreen`, `core.GameContext` | libGDX `Screen`; context owns camera/viewport/batch/eventBus/assets/audio; `BaseGameScreen` also owns a per-screen `timeScale`. `BaseScreen` owns the UI pass, seals `show()` (override `onShow()` / `inputProcessor()`), and `onRender` is optional — a UI-only screen implements no render hook |
| UI toolkit (~22 widgets, screen-space) | `core.ui.*` | `Button`, `TextLabel`, `Panel`, `ProgressBar`, `Slider`, `Dropdown`, `ScrollList`, `TabbedPanel`, `WorldViewport`, `UiLayer` (z-order + focus), … |
| **UI layout** | `core.ui.layout.*` | `VStack`, `HStack`, `GridStack`, `Overlay`, `Spacer`, `Insets`, `LayoutElement`. **Never position widgets with hand-computed coordinates** — build a tree and `uiLayer.setRoot(root)`; the root is sized from the viewport so it reflows on resize. Grow weights size children from the weight alone, so equal weights give equal columns |
| UI geometry contract | `core.ui.layout.LayoutElement`, `core.ui.BoundedWidget` | `preferredSize(Vector2)` to measure, `setBounds(x,y,w,h)` to place — always the widget's **outer** rect, bottom-left origin. `BoundedWidget` splits `frame` (assigned input) from `bounds` (hit-test output) |
| UI theme / metrics | `core.ui.UiTheme` via `UiSkin.theme()`, `core.ui.UiHelper` | spacing ramp + control/panel padding in one place; `UiHelper.baselineIn`/`alignIn`/`barHeight` are GL-free statics — use these instead of inlining cap-height arithmetic. `AssetRegistry.skin(FontSize)` for a per-widget font size |
| UI clipping | `core.ui.ScissorRegion`, `core.ui.layout.Clippable` | `ScissorStack`-based so regions **nest**; `LayoutContainer.clipChildren(true)` to clip content. `UiLayer` supplies the viewport/camera automatically |
| Debug screenshots | `core.debug.ScreenCapture`, `core.debug.CaptureRequest` | `BaseScreen.requestCapture(path, afterFrames, exitAfter)`; parse `--capture/--frames/--exit` in a launcher to verify rendering headlessly |
| **UI interaction tests** (clicks, drags, scroll, keys + per-stage screenshots) | `core.uitest.*` — **test sources**, published as `core`'s `test-jar` | `UiTestApp.run(UiTestConfig, UiTestCase)` boots a real window and runs a `UiScenario` one step per frame; `UiRobot` clicks/hovers/drags/scrolls/types, `WidgetQuery` finds widgets by label text, `DisplayAvailability` skips where there is no display. Depend on it from a game's tests with `<type>test-jar</type><scope>test</scope>`. **Never use `java.awt.Robot` or cursor warping** — input is injected in-process so the same test passes on Win32, X11, XWayland and native Wayland. See `core/CLAUDE.md` |
| Events | `core.event.EventBus`, `Signal<T>`, `Signal0` | typed pub/sub + multicast delegates |
| Camera pan/zoom + unproject | `core.world.WorldCameraController` | `unproject(x,y)`, drag/scroll adapters |
| Assets / textures | `core.AssetRegistry`, `core.resources.ResourceManager` | fonts, skin, texture/atlas cache, 1×1 `getPixelTexture()`; `getOrCreateTexture(key,factory)` caches any caller-synthesised texture (creation stays out of the manager) |
| Dialogue / story / i18n | `core.dialogue.*`, `core.story.*`, `core.i18n.*` | branching dialogue, quest state, localized strings |
| TMX parse + render (orthogonal) | `tiled.io.TmxParser`, `tiled.render.TiledMapLoader`, `TiledMap` | CSV/base64+gzip/zlib; layers render in BACKGROUND via `TiledMap.addTo(world)` |
| Tile grid→world math | `tiled.io.TileGeometry` | `worldX/worldY/index` |

## New (added in the framework-mirror pass — use these, don't rebuild)
| Capability | Where | Notes |
|---|---|---|
| Standalone grid geometry | `core.grid.Grid` | origin/cellW/cellH/cols/rows; `cellToWorld` (center), `worldToCell` (Optional + out), `inBounds`; square or non-square |
| A* pathfinding + reachability | `core.path.Pathfinder` | `findPath(grid,start,goal,Board,PathCostStrategy)` (4-conn, deterministic), `pathExists(...)` BFS for seal-off checks |
| Passability / cost hooks | `core.path.Board`, `core.path.PathCostStrategy` | `Board.isBlocked`; strategy `tileCost`+`minTileCost` (+`uniform()`) |
| One-shot countdown | `core.time.Timer` | `update(delta)` fires once on expiry |
| Repeating rate limiter | `core.time.Cadence` | `consumeReady()` once per interval; catch-up safe; fires immediately when fresh |
| MoveTowards | `core.time.Motion.moveTowards` | Unity `Vector3.MoveTowards` for `Vector2` |
| Coroutine-like sequencer | `core.time.Sequence` (+`Scheduler`, `SequenceComponent`) | `waitSeconds`/`waitUntil`/`run`; `Scheduler` for world-scoped, `SequenceComponent` for entity-scoped |
| Deferred delay component | `core.time.TimerComponent` | concrete `TriggerableComponent`; `trigger(delaySec)`, `onExpire()` |
| World-space health/progress bar | `core.world.component.WorldHealthBarComponent` | entity-anchored, `FOREGROUND_WORLD`, green→red; distinct from screen-space `ui.ProgressBar` |
| Hit-flash tint | `core.world.component.TintFlashRenderComponent` | decorator over any `RenderComponent`; `flash(color,dur)` |
| Entity removal / despawn | `core.world.World` | `remove` (immediate), `queueRemove`+`flushRemovals` (deferred, safe mid-update), `onRemoved()` signal; pipeline flushes each frame |
| Entity spawn during `update()` | `core.world.World` | `add` (immediate — outside the system loop only), `queueAdd`+`flushAdditions` (deferred, safe mid-update — e.g. a tower firing a bullet or a spawner creating an enemy from its own `update()`); pipeline flushes each frame |
| Tile world→cell inverse | `tiled.io.TileGeometry` | `columnAt`, `rowAt`, `cellAt` |
| Grid from a map | `tiled.render.TiledGrids.fromMap` | derive a `core.grid.Grid` from a `TmxMap` |
| Object-layer → entity spawn | `tiled.render.TiledMap.spawnObjects` + `TmxObjectFactory` | factory turns each `TmxObject` into a `WorldEntity` |
| Sprite-sheet / flipbook animation | `core.render.SpriteAnimation`, `core.world.component.AnimatedSpriteRenderComponent` | wraps `Animation<TextureRegion>`; `play()`/`idle()`/`advance(delta)`; idle = static first frame |
| Per-frame flipbook loader (async) | `core.resources.FrameSequenceLoader` | loads a directory of frame PNGs into an ordered `TextureRegion[]`; forks the decode across `WorkerPool` (returns a `Pending`, non-blocking) and uploads on the render thread in `resolve()`; lists frames via the resource manifest below, falling back to probing `1.png`,`2.png`,… |
| Classpath directory listing ("resource reflection") | `core.resources.ResourceManifest` (+ `ResourceManifestGenerator`, `ResourceManager.manifest()`) | the classpath cannot be enumerated, so a build step writes a manifest of packaged resource paths that is ingested at runtime for in-memory `list(directory)`/`contains(path)`; the parent POM's managed `exec-maven-plugin` execution `generate-resource-manifest` builds it at `process-classes` (folder/output set by `resourceManifest.sourceDir`/`resourceManifest.output`), and modules opt in just by declaring `exec-maven-plugin`; frame/variant loaders prefer it and fall back to numbered probing when it is absent (e.g. an IDE build) |
| 2D collider abstraction | `core.physics.Collider` | `bounds(Rectangle)` (AABB) + default `overlaps(Collider)`; extends `EntityComponent` so it auto-registers on a `WorldEntity` |
| Box (AABB) collider | `core.physics.BoxCollider` | anchors to a live `PositionComponent` + offset/size — never owns/duplicates position (mirrors `WorldHealthBarComponent`'s anchor pattern) |
| Collider vs. grid/tile-map blocking | `core.physics.GridCollisions.isBlocked(Collider,Grid,Board)` | reuses `core.path.Board` (the same abstraction pathfinding uses) for "which cells are blocked"; out-of-grid always counts as blocked |
| Tile layer → `Board` bridge | `tiled.render.TiledBoards.fromLayer(TmxMap,TileLayer,IntPredicate)` | decodes gids once, flips Tiled's top-down row into `core.grid`'s bottom-up convention, strips flip-flags via `GlobalTileId.id`; the blocked-gid predicate stays game-specific |
| Tinted / toggleable texture quad | `core.world.component.TextureRenderComponent` | `setTint(Color)` (default opaque white) + `setVisible(boolean)` (default true) — a translucent placement ghost or a hide-when-off-grid overlay needs no decorator |
| Shape textures (circle/ring, filled or outline) | `core.render.ShapeTextureFactory` | rasterises a solid-colour `ShapeMask` to a cached `Texture` via `ResourceManager.getOrCreateTexture`; `ring()`/`filledCircle()` helpers + generic `shape(key,w,h,color,mask)`; use the 1×1 pixel for rectangles |
| Hoverable sprite at an explicit draw size | `core.world.component.HoverableSpriteComponent` | new `(region,x,y,w,h,renderPass[,hoverTint])` ctors for when the draw size must differ from the region's native pixel size (the original native-size ctors are unchanged) |
| Generic hit-point tracker | `core.world.component.HealthComponent` | `damage(int)`/`heal(int)` (clamped to `[0,maxHp]`, negative amounts rejected), `isAlive()`, `fraction()` (feeds `WorldHealthBarComponent.setFraction`), `Signal<HealthComponent> onChanged()`, `Signal0 onDeath()` (fires exactly once) — pure data/logic, not a `RenderComponent`/`UpdateComponent` |
| Nearest-entity-in-range query | `core.world.WorldQueries.nearest(world,x,y,range,componentType,filter)` | the generic form of a tower/spell/AI "find nearest target" linear scan; ties resolve to entity iteration order |
| Waypoint path-following movement | `core.world.component.PathFollowerComponent` | `UpdateComponent`; follows a fixed `List<Vector2>` via `core.time.Motion.moveTowards`, `Signal0 onCompleted()` fires once at the final waypoint; does not replan — construct a new instance if the route changes mid-flight |
| Homing projectile | `core.world.component.HomingProjectileComponent` | `UpdateComponent`; homes to a target `WorldEntity`'s `PositionComponent`, takes a `Predicate<WorldEntity> isTargetValid` + `Consumer<WorldEntity> onImpact`, self-despawns via `bind(WorldEntity)` (same pattern as other self-despawning game components) |
| Pause / speed-up | `core.time.TimeScale` | owned per-screen by `core.screen.BaseGameScreen` (`protected final timeScale` field); `apply(rawDelta)` is applied to the world pipeline's delta each frame; `setScale`/`setPaused`/`togglePause`; defaults to a 1x/unpaused no-op |
| Automatic entity-vs-entity collision system | `core.physics.CollisionSystem` + `core.physics.CollisionListener` | the collision equivalent of `HoverSystem`: re-scans every `Collider`-carrying entity each frame, fires `onCollisionEnter`/`onCollisionExit` (`Signal<WorldEntity>`) on transitions; O(n²) pairwise, wired unconditionally into `RenderPipeline.processCollisions`/`BaseGameScreen` (negligible cost with zero `Collider`s present — opt in just by attaching one); demo's tower-defense bullets/enemies use it for shape-accurate impact (see `demo.towerdefense.TowerComponent`); transparently parallelizes broad-phase detection across `GameContext.workerPool()` once the collider count is large (see `core.concurrent.WorkerPool`) — automatic via `RenderPipeline`, no consumer changes needed |
| CPU-bound parallel range work | `core.concurrent.WorkerPool`, `TaskGate` | dedicated `ForkJoinPool` wrapper; `parallelFor`/`mapChunks` split `[from,to)` into chunks and submit one task per chunk — `parallelFor` blocks until all finish (no results to defer); `mapChunks` does NOT block, it returns a `TaskGate<R>` immediately, and `TaskGate.get()` is the deferred "gate" that joins all chunks (safe to call more than once) — for read-only/embarrassingly-parallel work only, one instance per `GameContext` (`workerPool()`) |
| Sound/music manager | `core.audio.AudioManager` | mirrors `ResourceManager`'s `getOrCreate*`-by-classpath cache/dispose pattern for `Sound`/`Music`; `loadSound`/`playSound`/`loadMusic`/`playMusic`/`stopMusic`; master/sfx/music volume (`setMasterVolume` etc., fail-soft clamped to `[0,1]` via `clampVolume`/`combinedVolume`); owned by `GameContext.audio()`, disposed alongside `assets` |
| **Scene inside a UI rectangle (pan/zoom/clip/pick)** | `core.ui.WorldViewport` | hosts a real `core.world.World` in a widget: pan (drag), zoom-towards-cursor (wheel), clamp to scene bounds, scissor clip, hover tooltips, hover outlines, and click dispatch through the entity's own `ClickableComponent`. **This is the only "game in a box" mechanism** — do not invent a UI-side sprite/record type for viewport contents; build `WorldEntity`s exactly as you would for a full-screen scene and everything (animation, Y-sorting, collision, tinting, `UpdateComponent`) works unchanged. Several viewports may show the same or different worlds, and viewports nest |
| Scene→surface pan/zoom transform | `core.render.SceneTransform` | the camera maths behind `WorldViewport`, GL-free and unit-tested: `setView`/`setSceneBounds`/`setZoomRange`, `centreOn`/`panByView`/`zoomAt(factor,anchorX,anchorY)`, `sceneToView`/`viewToScene`, `visibleSceneRect`, `applyTo(out, base)`. Produces a **transform** matrix composed into the batch, never a projection swap — that is what lets a scene nest inside a UI layer without breaking `UiLayer`'s projection contract. **Zoom is inverted vs. `OrthographicCamera.zoom`: larger = magnified** |
| Coordinate-space-agnostic world draw | `core.render.RenderPipeline.drawSceneInto(world, batch)` | all four visual passes into an already-open batch, owning no `begin()`/`end()`, projection or outline blit — the only pass method safe to call more than once per frame (one call per viewport). `renderScene`/`render` remain the screen-filling, camera-owning paths |
| Which entity was clicked | `core.render.RenderPipeline.clickAt(world,x,y)` | `Optional<WorldEntity>` variant of `handleClick`, for callers that must react to the identity of the hit rather than merely that one landed |
| Entity tooltip text | `core.world.TooltipComponent` | `tooltip()` (`null` suppresses); `of(String)` / `of(Supplier<String>)`. Presentation belongs to the host — `WorldViewport` draws it in **UI** space near the cursor so it stays legible at any zoom |
| World-space text / captions | `core.world.component.TextRenderComponent` | a line of text aligned inside a world-space box, in any `RenderPass`, with an optional drop shadow; reuses `ui.TextLabel`'s `GlyphLayout` caching. Scales with scene zoom (unlike a tooltip), which is what a caption painted onto the world should do |
| Pointer-release hook | `core.ui.UiWidget.released(x,y)` | closing bracket of the `hit`/`dragged` gesture, delivered once even if the pointer strayed outside; routed by `UiLayer`'s `touchUp`. **Use this instead of polling `Gdx.input.isTouched()`** from `update()`, which sees the global pointer rather than this widget's own gesture |

## Engine-parity backlog (with Unity) — deliberately NOT built
These are not needed by the current game (it uses tile-occupancy + distance checks). Build the
reusable core primitive **only when a game first needs it** — still in `core`, not stubbed in your
game code (`demo` or an external consumer).
- **Action-map / multi-button / gamepad input.** Only left-click, hover, drag-pan, scroll-zoom and
  UI focus keys exist; raw input is libGDX `InputProcessor`/`InputMultiplexer`. → future `core.input`.
- **Save/load persistence.** In-memory only.


## Unity → Java concept map (hypothetical)
| Unity | Java framework |
|---|---|
| `MonoBehaviour.Update(dt)` | `core.world.UpdateComponent.update(delta)` (ticked by `UpdateSystem`) |
| `GameObject` + components | `core.world.WorldEntity` via `.with(Class, component)` |
| `Transform.position` | `core.world.PositionComponent` (`x/y/moveTo`) — no hierarchy, no rotation/scale |
| `SpriteRenderer` | `core.world.component.TextureRenderComponent` |
| `SpriteRenderer.color` hit flash | `core.world.component.TintFlashRenderComponent` (`flash(color, dur)`) |
| `Instantiate` / `Destroy` | `world.add(entity)` / `world.queueRemove(entity)` (+ `onRemoved()` signal); spawning from inside a system's `update()` (e.g. a projectile) uses `world.queueAdd(entity)` instead |
| `GameManager.Instance` / `FindObjectOfType` | a field on your `GameContext` subclass, injected into screens |
| `Camera.ScreenToWorldPoint` | `WorldCameraController.unproject(screenX, screenY)` |
| `Input` (mouse/keys) | libGDX `InputProcessor`/`InputMultiplexer` (see `CaveDemoScreen`) |
| `GridManager` geometry (`CellToWorld`/`WorldToCell`/`InBounds`) | `core.grid.Grid` |
| `GridManager` `IsPlaceable`/`IsWalkable`/lane/spawn/treasure/trigger rows | **game** — a wrapper around a `Grid` |
| `Pathfinder.FindPath` / `PathExists` | `core.path.Pathfinder.findPath` / `pathExists` |
| `IPathCostStrategy` | `core.path.PathCostStrategy` (`tileCost`+`minTileCost`) |
| `BoardSnapshot` (enemy/trap/boulder occupancy) | **game** — implement `core.path.Board` (fold walkable-zone + occupancy into `isBlocked`) |
| `DefaultCostStrategy` / `InvertedEnemyCostStrategy` | **game** — implement `PathCostStrategy` |
| `Time.deltaTime` countdown; attack cadence | `core.time.Timer`; `core.time.Cadence` |
| Coroutines (`WaitForSeconds`, `while(!done) yield`, `HitFlashRoutine`, `ResolveRoutine`) | `core.time.Sequence` + `Scheduler` (world-scoped) or `SequenceComponent` (entity-scoped) |
| `Vector3.MoveTowards` | `core.time.Motion.moveTowards` |
| `UnitHealthBar` (world-space) | `core.world.component.WorldHealthBarComponent` |
| Screen-space bars / HUD text / buttons (uGUI `Canvas`/`Text`/`Button`) | `core.ui.*` (`ProgressBar`, `TextLabel`, `Button`, `Panel`, …) in a `UiLayer` |
| `Camera.rect` sub-rect rendering / `RenderTexture` + `RawImage` (minimap, inventory preview) | `core.ui.WorldViewport` — a real `World` drawn into a widget rectangle via `core.render.SceneTransform` |
| `Placeable : ScriptableObject` | **game** — a plain record/POJO (no ScriptableObject) |
| `GameManager`/`GameSession`/HUD/day-phase FSM/placement rules | **game** — all of it |
| TMX map (SuperTiled2Unity backdrop) | `tiled.render.TiledMapLoader` → `TiledMap.addTo(world)` |
| Tiled object layer → spawn points/triggers | `tiled.render.TiledMap.spawnObjects(world, factory)` (+ `TmxObjectFactory`) |
