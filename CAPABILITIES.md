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
| Frame pipeline | `core.render.RenderPipeline`, `core.render.system.*` | `update → processHover → render`; passes `BACKGROUND→WORLD→NORMAL_MAPPED→FOREGROUND_WORLD→UI`; WORLD is Y-sorted by `sortKey()` |
| Screens / app loop | `core.screen.BaseScreen`, `BaseGameScreen`, `core.GameContext` | libGDX `Screen`; context owns camera/viewport/batch/eventBus/assets |
| UI toolkit (~40 widgets, screen-space) | `core.ui.*` | `Button`, `TextLabel`, `Panel`, `ProgressBar`, `Slider`, `Dropdown`, `ScrollList`, `TabbedPanel`, `UiLayer` (z-order + focus), … |
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
| 2D collider abstraction | `core.physics.Collider` | `bounds(Rectangle)` (AABB) + default `overlaps(Collider)`; extends `EntityComponent` so it auto-registers on a `WorldEntity` |
| Box (AABB) collider | `core.physics.BoxCollider` | anchors to a live `PositionComponent` + offset/size — never owns/duplicates position (mirrors `WorldHealthBarComponent`'s anchor pattern) |
| Collider vs. grid/tile-map blocking | `core.physics.GridCollisions.isBlocked(Collider,Grid,Board)` | reuses `core.path.Board` (the same abstraction pathfinding uses) for "which cells are blocked"; out-of-grid always counts as blocked |
| Tile layer → `Board` bridge | `tiled.render.TiledBoards.fromLayer(TmxMap,TileLayer,IntPredicate)` | decodes gids once, flips Tiled's top-down row into `core.grid`'s bottom-up convention, strips flip-flags via `GlobalTileId.id`; the blocked-gid predicate stays game-specific |
| Tinted / toggleable texture quad | `core.world.component.TextureRenderComponent` | `setTint(Color)` (default opaque white) + `setVisible(boolean)` (default true) — a translucent placement ghost or a hide-when-off-grid overlay needs no decorator |
| Shape textures (circle/ring, filled or outline) | `core.render.ShapeTextureFactory` | rasterises a solid-colour `ShapeMask` to a cached `Texture` via `ResourceManager.getOrCreateTexture`; `ring()`/`filledCircle()` helpers + generic `shape(key,w,h,color,mask)`; use the 1×1 pixel for rectangles |
| Hoverable sprite at an explicit draw size | `core.world.component.HoverableSpriteComponent` | new `(region,x,y,w,h,renderPass[,hoverTint])` ctors for when the draw size must differ from the region's native pixel size (the original native-size ctors are unchanged) |

## Engine-parity backlog (with Unity) — deliberately NOT built
These are not needed by the current game (it uses tile-occupancy + distance checks, static
sprites, and no audio). Build the reusable core primitive **only when a game first needs it** —
still in `core`, not stubbed in your game code (`demo` or an external consumer).
- **Per-frame collision system / trigger volumes** (auto-checking overlaps between entities every
  frame with enter/exit signals, the collision equivalent of `HoverSystem`). `core.physics.Collider`
  / `BoxCollider` / `GridCollisions` now provide the shape and grid-blocking primitives; games still
  call these manually (e.g. from an `UpdateComponent`). Build the automatic system only when a game
  first needs entity-vs-entity collision. → future `core.physics` addition, not a new package.
- **Audio manager** (`Sound`/`Music` wrapper). None in core. → future `core.audio`.
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
| `Placeable : ScriptableObject` | **game** — a plain record/POJO (no ScriptableObject) |
| `GameManager`/`GameSession`/HUD/day-phase FSM/placement rules | **game** — all of it |
| TMX map (SuperTiled2Unity backdrop) | `tiled.render.TiledMapLoader` → `TiledMap.addTo(world)` |
| Tiled object layer → spawn points/triggers | `tiled.render.TiledMap.spawnObjects(world, factory)` (+ `TmxObjectFactory`) |
