# Framework-first Java library project

**The `core` and `tiled` libraries are the
framework and the source of truth.** Game code consumes them; it does not re-implement them.

> ⚠️ **The #1 rule — do not re-invent the framework.** Before writing any grid, pathfinding,
> timer, tween, health-bar, entity, render, UI, event, camera, or asset code in the `game`
> module, assume the primitive already exists in `core`/`tiled` and go find it (start with
> [CAPABILITIES.md](CAPABILITIES.md)). Only genuinely game-specific logic belongs in `game`.
> A stubbed/simplified re-implementation of an existing core feature is a defect, not a shortcut.

## Modules (Maven reactor, Java 21, libGDX 1.14.0 / LWJGL3, Jackson, JUnit 5)
```
game  ──▶ core            game    = Cave Defense (the ported game; currently a demo)
game  ──▶ tiled           tiled   = TMX parsing + rendering  (com.cryptroot.tiled)
tiled ──▶ core            core    = engine framework          (com.cryptroot.core)
```
Dependencies point **inward**: `core` never imports `tiled` or `game`; `tiled` never imports
`game`. A new feature's module is the innermost one that could reuse it.

## Where does new code go? (the decision rule)
> **Would another, unrelated game reuse this unchanged?** → `core` (or `tiled` if it is
> about tile maps). **Does it encode game-specific rules, content, or balance?** → `game`.

Worked examples:
- Grid cell↔world math, A* → **core**. Which cells are "placeable"/"walkable", the lane and
  treasure rows, the troop/trap/boulder cost numbers → **game**.
- A countdown/attack-cadence timer → **core**. "Enemies attack once per second" → **game**.
- A world-space health bar widget → **core**. "only show it during the Resolve phase" → **game**.

## Golden rules
- **No static singletons.** There is no `GameManager.Instance`. Shared services live on a
  `GameContext` subclass and are passed into screens (see `core.GameContext`,
  `core.screen.BaseGameScreen`). Port Unity singletons to `GameContext` fields.
- **Reuse libGDX math.** Use `Vector2`, `GridPoint2`, `Rectangle`, `Color`, `MathUtils`,
  `Interpolation`. Do not write a custom vector/point/color/easing class.
- **One update hook.** Per-frame logic is a `UpdateComponent.update(delta)` ticked by
  `UpdateSystem` — the equivalent of Unity `MonoBehaviour.Update`. There is no
  `Awake/Start/FixedUpdate`.
- **Entities are components.** A `WorldEntity` is a bag of components (`.with(Class, comp)`),
  one per interface type. No transform hierarchy, no parenting; position is `PositionComponent`.
- **Tests are plain JUnit 5** (no headless-GL, no Mockito). Keep algorithmic code GL-free and
  unit-test it; leave `draw()` bodies uncovered as the existing render components do.

## Build & test
Run from `Java/` directory. PowerShell chains with `;`.
```
mvn -pl core test            # core only (no external network needed)
mvn -pl tiled -am test       # tiled + core   (needs jackson-dataformat-xml resolvable)
mvn -pl game  -am package    # game fat-jar
mvn test                     # full reactor
```
If Maven Central is unreachable in your environment, artifacts must already be in `~/.m2`.

## Read next
- [CAPABILITIES.md](Java/CAPABILITIES.md) — what already exists (search here before building).
- `Java/core/CLAUDE.md`, `Java/tiled/CLAUDE.md`, `Java/game/CLAUDE.md` — per-module rules.
