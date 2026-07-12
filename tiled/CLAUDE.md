# tiled — TMX parsing + rendering (`com.cryptroot.tiled`)

Depends on `core` (+ Jackson XML). Never imports `game`. Tiled-map concerns live here; generic
geometry/pathfinding belongs in `core` (`core.grid`, `core.path`), not here.

## Layout
- `io/` — GL-free parsing & math: `TmxParser` (orthogonal, non-infinite only), `TmxMapDeserializer`,
  `TileDataCodec` (CSV/base64 + gzip/zlib), `GlobalTileId`, `ResourceLocator`, `TileGeometry`.
- `model/` — immutable parsed data: `TmxMap`, sealed `TmxLayer` (`TileLayer`/`ObjectGroup`),
  `TmxObject`, `TmxTileset`, `TmxTile`, `Properties`, `Orientation`, …
- `render/` — libGDX: `TiledMapLoader`, `TiledMap`, `TileAtlas`, `TileLayerRenderComponent`,
  `TiledGrids`, `TmxObjectFactory`.

## Conventions & invariants
- **Coordinates:** Tiled is top-left / y-down (row 0 = top); the world is bottom-left / y-up.
  `TileGeometry` flips this: forward `worldX/worldY`, inverse `columnAt/rowAt/cellAt`. A derived
  `core.grid.Grid` (via `TiledGrids.fromMap`) has **grid row 0 = world bottom = Tiled's bottom row**.
- **Rendering:** only orthogonal maps render; tile layers draw in `RenderPass.BACKGROUND` via
  `TiledMap.addTo(world)`. Isometric/hex/infinite are parse-only or rejected.
- **Object layers are parsed but not auto-spawned.** Turn them into entities with
  `TiledMap.spawnObjects(world, factory)` — the factory (game-supplied) reads each `TmxObject`'s
  type/name/properties and returns a `WorldEntity` (or empty to skip).
- Textures are owned by the `core` `ResourceManager` that loaded the map — nothing to dispose here.
- Tests parse fixtures under `src/test/resources/assets/test/` (e.g. `Embedded.tmx`); follow
  `EmbeddedMapTest`. Running them needs `jackson-dataformat-xml` resolvable in `~/.m2`.
- **Fail fast by default**, matching `core`: null-check stored/dereferenced references with
  `Objects.requireNonNull`, reject invalid values with `IllegalArgumentException` (see
  `TmxColors`, `TileSliceMath`, `TileDataCodec`). Fail-soft only where already documented as
  intentional (e.g. a missing table/resource returning empty instead of throwing) — otherwise
  validate at the public API boundary rather than letting bad TMX data fail obscurely deep in
  parsing/rendering.
- Run `mvn spotless:apply` from the repo root after editing any Java file here.
