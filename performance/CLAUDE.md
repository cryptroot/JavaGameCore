# performance — parallel collision showcase (`com.cryptroot.performance`)

A benchmark + visual proof-of-concept sibling to `demo`: not a game. Depends on `core` only (no
`tiled`).

**The parallelization itself now lives in `core`**, not here — `core.concurrent.WorkerPool` and
`core.physics.CollisionSystem`'s `WorkerPool`-backed constructor were promoted from this module
once the approach was proven out (see git history on this module for the original prototype:
`concurrent.WorkerPool`, `physics.ParallelBroadPhase`, `physics.ParallelCollisionSystem`,
`physics.EntityPair`, all since deleted). This module's job now is purely to **showcase and
benchmark** that core capability at scale; it contains no parallel-detection logic of its own.

## Why this exists
`core.physics.CollisionSystem.update` does an O(n²) broad-phase overlap loop over every
`Collider`-carrying entity, then resolves the result by firing `CollisionListener` enter/exit
signals. The overlap tests are read-only (`Collider.overlaps` allocates and reads bounds, never
mutates), so they can run concurrently; resolving must stay single-threaded since listener
callbacks are arbitrary game code that can mutate the world. `CollisionSystem` now does exactly
this split internally — see `core/CLAUDE.md`'s `core.physics`/`core.concurrent` entries.

Measured on this machine: the visual demo (`BoxFieldScreen`) shows ~30-35ms/frame sequential vs.
~1-2ms/frame parallel at its default box count; the headless `CollisionBenchmark` shows the same
story scaling from 1k to 16k boxes (see its output for exact numbers, which are
environment-dependent).

## What's here
- `physics.RandomColliderField` — GL-free entity builder (`PositionComponent` + `Collider` only)
  used by the headless benchmark.
- `CollisionBenchmark` — headless `main()`; builds a `core.physics.CollisionSystem` with its
  no-arg (sequential) constructor and one with its `WorkerPool`-backed constructor, and times
  `update()` for both across a range of box counts (including one count below `CollisionSystem`'s
  internal parallel threshold, to demonstrate there's no regression at small scale). Run with:
  ```
  mvn -pl performance -am exec:java -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark
  ```
- `BoxFieldLauncher`/`BoxFieldGame`/`BoxFieldScreen`/`BoxField`/`MovingBoxComponent`/
  `FlashOnCollision`/`PerfDemoContext` — a visual libGDX showcase: a field of bouncing, colliding
  boxes that flash red on collision. Press `P` to toggle between `BoxFieldScreen`'s two
  `core.physics.CollisionSystem` instances (sequential vs. `WorkerPool`-backed — same class, two
  constructors), `+`/`-` to rescale the box count; the HUD shows the active mode and the
  collision step's wall-clock time. Run with `mvn -pl performance -am exec:java` (default main
  class).

## Golden rules (same as the rest of the repo)
- No static singletons — `PerfDemoContext` is a plain `GameContext` subclass; the `WorkerPool` it
  uses is `GameContext.workerPool()`, inherited for free.
- `BoxFieldScreen` extends `core.screen.BaseScreen` directly, not `BaseGameScreen`, because
  `BaseGameScreen.onRender` is hardwired to a single `CollisionSystem` instance and this screen
  needs to swap between two instances at runtime.
- `WorldEntity` allows only one component registration per interface — a box's motion and its
  hit-flash decay live in one `UpdateComponent` (`MovingBoxComponent`), not two.
- Fail-fast validation at API boundaries (`RandomColliderField`, `BoxField`, etc.), same
  convention as `core`/`tiled`/`demo`.

## Build & test
```
mvn -pl performance -am test        # this module's own tests (BoxField/RandomColliderField, etc.)
mvn -pl performance -am exec:java -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark
mvn -pl performance -am exec:java   # visual demo (GL, cannot be run headless/CI)
```
Correctness of the parallel path itself is proven in `core` (`core.physics.CollisionSystemTest`,
`core.concurrent.WorkerPoolTest`), not here. Run `mvn spotless:apply` from the repo root after
editing Java files here.

