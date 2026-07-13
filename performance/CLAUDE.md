# performance — parallel collision showcase (`com.cryptroot.performance`)

An experimental sibling to `demo`: not a game, a benchmark + visual proof-of-concept for
parallelizing an existing `core` system. Depends on `core` only (no `tiled`). Everything here is
**deliberately experimental** — nothing in `com.cryptroot.performance.concurrent` or
`com.cryptroot.performance.physics` is promoted into `core` yet. If `WorkerPool`/
`ParallelCollisionSystem` prove out, the promotion path is: move `WorkerPool` +
`IntRangeTask`/`IntRangeFunction` into `core.concurrent`, fold `ParallelBroadPhase`'s striped
detection into `core.physics.CollisionSystem` itself (e.g. behind a constructor flag or an
injected executor), and delete the duplicated classes here.

## Why this exists
`core.physics.CollisionSystem.update` does an O(n²) broad-phase overlap loop over every
`Collider`-carrying entity, then resolves the result by firing `CollisionListener` enter/exit
signals. The overlap tests are read-only (`Collider.overlaps` allocates and reads bounds, never
mutates), so they can run concurrently; resolving must stay single-threaded since listener
callbacks are arbitrary game code that can mutate the world. This module explores exactly that
split.

## What's here
- `concurrent.WorkerPool` — a small `ForkJoinPool`-backed "thread manager": `parallelFor`/
  `mapChunks` split an int range into near-even chunks, submit one task per chunk, then block
  (`.join()`) until all chunks finish before returning — the "gate" the caller waits on.
- `physics.ParallelBroadPhase` — striped/round-robin parallel re-implementation of the collision
  system's triangular double loop, built on `WorkerPool`. Striping (not a contiguous split) is
  needed because the loop's per-row work is wildly uneven (row 0 scans almost everything, the
  last row scans nothing).
- `physics.ParallelCollisionSystem` — drop-in alternative to `core.physics.CollisionSystem` that
  parallelizes detection via `ParallelBroadPhase` but resolves (fires listeners) sequentially,
  exactly like the original.
- `physics.RandomColliderField` — GL-free entity builder (`PositionComponent` + `Collider` only)
  shared by the correctness test and the headless benchmark.
- `CollisionBenchmark` — headless `main()`, prints sequential vs. parallel ms/frame and speedup
  for a range of box counts. Run with:
  ```
  mvn -pl performance -am exec:java -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark
  ```
- `BoxFieldLauncher`/`BoxFieldGame`/`BoxFieldScreen`/`BoxField`/`MovingBoxComponent`/
  `FlashOnCollision`/`PerfDemoContext` — a visual libGDX showcase: a field of bouncing, colliding
  boxes that flash red on collision. Press `P` to toggle sequential/parallel collision at
  runtime, `+`/`-` to rescale the box count; the HUD shows the active mode and the collision
  step's wall-clock time. Run with `mvn -pl performance -am exec:java` (default main class).

## Golden rules (same as the rest of the repo)
- No static singletons — `PerfDemoContext` (a `GameContext` subclass) owns the `WorkerPool`.
- `BoxFieldScreen` extends `core.screen.BaseScreen` directly, not `BaseGameScreen`, because
  `BaseGameScreen.onRender` is hardwired to the sequential `CollisionSystem` and this screen needs
  to swap collision systems at runtime.
- `WorldEntity` allows only one component registration per interface — a box's motion and its
  hit-flash decay live in one `UpdateComponent` (`MovingBoxComponent`), not two.
- Fail-fast validation at API boundaries (`WorkerPool`, `RandomColliderField`, `BoxField`, etc.),
  same convention as `core`/`tiled`/`demo`.

## Build & test
```
mvn -pl performance -am test        # correctness tests (parity with core.physics.CollisionSystem)
mvn -pl performance -am exec:java -Dexec.mainClass=com.cryptroot.performance.CollisionBenchmark
mvn -pl performance -am exec:java   # visual demo (GL, cannot be run headless/CI)
```
Run `mvn spotless:apply` from the repo root after editing Java files here.
