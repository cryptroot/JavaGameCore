# Framework-first Java library project

**The `core` and `tiled` libraries are the
framework and the source of truth.** Game code consumes them; it does not re-implement them.

> **"Game" means any consumer of this framework — not specifically the `demo` module.**
> `demo` is only a small bundled reference implementation ("Cave Defense") used to exercise
> `core`/`tiled`. When this repo is embedded as a submodule/library (as it is in this checkout),
> the real game usually lives in a sibling project outside this reactor, and every rule below
> that talks about "game code" applies there exactly as it does to `demo`.

> ⚠️ **The #1 rule — do not re-invent the framework.** Before writing any grid, pathfinding,
> timer, tween, health-bar, entity, render, UI, event, camera, or asset code in your game
> (whether that's the bundled `demo` sample or an external project consuming `core`/`tiled`),
> assume the primitive already exists and go find it (start with
> [CAPABILITIES.md](CAPABILITIES.md)). Only genuinely game-specific logic belongs in game code.
> A stubbed/simplified re-implementation of an existing core feature is a defect, not a shortcut.

## Modules (Maven reactor, Java 21, libGDX 1.14.0 / LWJGL3, Jackson, JUnit 5)
```
demo  ──▶ core            demo    = Cave Defense — a bundled reference/example game
demo  ──▶ tiled           tiled   = TMX parsing + rendering  (com.cryptroot.tiled)
tiled ──▶ core            core    = engine framework          (com.cryptroot.core)
```
Dependencies point **inward**: `core` never imports `tiled` or a game; `tiled` never imports
a game. A new feature's module is the innermost one that could reuse it. `demo` is just one
example consumer — an actual game (e.g. built in a sibling project when this repo is used as a
submodule/library) follows the exact same rule: it depends on `core`/`tiled`, never the reverse.

## Where does new code go? (the decision rule)
> **Would another, unrelated game reuse this unchanged?** → `core` (or `tiled` if it is
> about tile maps). **Does it encode game-specific rules, content, or balance?** → the game
> (i.e. `demo`, or your actual game project if this repo is consumed as a submodule/library).

Worked examples:
- Grid cell↔world math, A* → **core**. Which cells are "placeable"/"walkable", the lane and
  treasure rows, the troop/trap/boulder cost numbers → **the game**.
- A countdown/attack-cadence timer → **core**. "Enemies attack once per second" → **the game**.
- A world-space health bar widget → **core**. "only show it during the Resolve phase" → **the game**.

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
- **Every public constructor/method either fails fast or fails soft — deliberately, never by
  accident.** Default to fail-fast: validate arguments at the API boundary and throw immediately
  (`Objects.requireNonNull` for null references, `IllegalArgumentException`/`IllegalStateException`
  for invalid values — see `core.grid.Grid`, `core.dialogue.DialogueGraph`,
  `core.render.SpriteAnimation`). Fail-soft (clamping, defaulting, silently no-op'ing) is only
  acceptable when it is the documented, intentional contract of that API — e.g. `core.time.Timer`/
  `Cadence` clamp a negative duration, `Localization.getOrDefault` falls back instead of throwing,
  `EventBus.publish` no-ops when there are no subscribers. Document the fail-soft behavior in the
  method's Javadoc so it reads as a choice, not a missing check. Never let bad input silently
  propagate un-validated and un-documented.

## Build & test
Run from `Java/` directory. PowerShell chains with `;`.
```
mvn -pl core test            # core only (no external network needed)
mvn -pl tiled -am test       # tiled + core   (needs jackson-dataformat-xml resolvable)
mvn -pl demo  -am package    # demo fat-jar
mvn test                     # full reactor
mvn spotless:apply           # reformat before finishing any change (see below)
```
If Maven Central is unreachable in your environment, artifacts must already be in `~/.m2`.

**Always run `mvn spotless:apply` after editing Java files and before considering a change
done** (root `pom.xml` wires `com.diffplug.spotless:spotless-maven-plugin` with Google Java
Format, but its `check`/`apply` goals are **not** bound to any lifecycle phase — `mvn test`/
`package`/`verify` will not catch or fix formatting for you). Run it against the whole reactor
(plain `mvn spotless:apply` from the repo root) rather than a single module, since it also fixes
import order/unused imports repo-wide.

## Read next
- [CAPABILITIES.md](Java/CAPABILITIES.md) — what already exists (search here before building).
- `Java/core/CLAUDE.md`, `Java/tiled/CLAUDE.md`, `Java/demo/CLAUDE.md` — per-module rules.
