# JavaGameCore

A reusable 2D game framework for Java, built on [libGDX](https://libgdx.com/) 1.14.0 (LWJGL3)
and targeting Java 21.

It provides the pieces most 2D games need — an ECS-lite entity/component world, a layered
render pipeline, screens and a shared game context, a ~40-widget UI toolkit, an event bus,
camera pan/zoom, asset and audio caches, grid geometry and A* pathfinding, timers and
coroutine-like sequencing, collision detection, health/movement/projectile components,
dialogue, quest state and localization, plus Tiled (TMX) map parsing and rendering — so a game
project only has to write its own rules, content and balance.

```java
WorldEntity enemy =
    new WorldEntity()
        .with(PositionComponent.class, new PositionComponent(x, y))
        .with(RenderComponent.class, new TextureRenderComponent(region, RenderPass.WORLD))
        .with(HealthComponent.class, new HealthComponent(30))
        .with(Collider.class, new BoxCollider(position, 0, 0, 16, 16));
world.add(enemy);
```

## Modules

```
demo        ──▶ core     demo        Cave Defense — bundled reference/example game
demo        ──▶ tiled    tiled       TMX parsing + rendering   (com.cryptroot.tiled)
tiled       ──▶ core     core        Engine framework          (com.cryptroot.core)
performance ──▶ core     performance Benchmark + visual demo of the parallel CollisionSystem
```

- **[core](core)** — the framework itself: entities/components, render passes, screens, UI,
  events, camera, assets, audio, grid + pathfinding, time/scheduling, physics, worker pool,
  dialogue/story/i18n.
- **[tiled](tiled)** — Tiled map editor support: TMX parsing (CSV / base64 + gzip / zlib),
  orthogonal tile-layer rendering, object-layer → entity spawning, tile ↔ world math.
- **[demo](demo)** — "Cave Defense", a small example game that exercises the framework.
- **[performance](performance)** — a benchmark and visual showcase of `core`'s
  `WorkerPool`-backed parallel collision detection.

Dependencies point inward: `core` never depends on `tiled` or on a game, and `tiled` never
depends on a game.

## Using it in your own game

> **The #1 rule when conusming this project:** don't re-invent the framework.

This repository is designed to be consumed as a library or git submodule. Your game depends on
`core` (and `tiled` if you use Tiled maps) and implements only game-specific logic — unit
stats, placement rules, balance, screen wiring, what happens on impact. Anything an unrelated
game would reuse unchanged belongs in the framework instead.

Start with [CAPABILITIES.md](CAPABILITIES.md): it is a full inventory of what already exists
(including a Unity → Java concept map), and checking it first is the cheapest way to avoid
re-implementing something the framework already provides.

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

Tests are plain JUnit 5 — no headless GL context and no mocking framework required.

## Documentation

- [CAPABILITIES.md](CAPABILITIES.md) — inventory of existing framework capabilities (search here
  before building anything new) and a Unity → Java concept map.
- [CLAUDE.md](CLAUDE.md) — project-wide conventions and golden rules.
- [core/CLAUDE.md](core/CLAUDE.md), [tiled/CLAUDE.md](tiled/CLAUDE.md),
  [demo/CLAUDE.md](demo/CLAUDE.md), [performance/CLAUDE.md](performance/CLAUDE.md) —
  per-module rules.
