# Phase 0 Research: Workout Planning, Execution & History Tracking (v1)

**Branch**: `001-workout-tracker` | **Date**: 2026-09-02

All open questions from the spec (OQ-1..OQ-3) and the technology decisions deferred by the
spec's technology-neutrality clause are resolved below. Every entry follows
Decision / Rationale / Alternatives considered.

---

## R-01 — Technology stack

- **Decision**: Native Android: Kotlin 2.0, Jetpack Compose + Material 3, Room (SQLite),
  Hilt, Kotlin Coroutines + Flow/StateFlow, AndroidX ViewModel, Navigation Compose.
  minSdk 26 (Android 8.0), latest stable compile/target SDK.
- **Rationale**: The spec defers technology decisions to the plan and records the PRD's
  preferred direction (PRD-§29–§32). This stack is the PRD-directed stack, is fully
  offline-capable (BR-18), is actively maintained (Constitution XII), and is the standard
  modern Android baseline. minSdk 26 covers ~99% of active devices while keeping the
  codebase free of legacy compatibility paths.
- **Alternatives considered**:
  - *Kotlin Multiplatform / Flutter / React Native*: rejected — no cross-platform
    requirement exists in v1 or stated future scope; adds complexity without benefit
    (Constitution II).
  - *Classic Views + XML*: rejected — Compose is the PRD-directed UI layer and reduces
    UI boilerplate.
  - *SQLDelight / plain SQLiteOpenHelper*: rejected — Room provides compile-time query
    verification, Flow observation, and a structured migration framework required by
    BR-19/FR-057.

## R-02 — Architecture layering

- **Decision**: Single `app` module, three layers: `ui` (Compose screens + ViewModels) →
  `domain` (pure Kotlin: models, repository interfaces, use cases, calculators) → `data`
  (Room entities/DAOs/database, repository implementations, seed data). Dependencies point
  inward only; `domain` has zero Android imports.
- **Rationale**: Constitution III (modular architecture, dependencies inward) and PRD-§29.
  Pure-Kotlin domain makes core rules (volume, duration, 1RM, validation, history
  protection) unit-testable on the JVM without a device (Constitution VII). A single
  module is the simplest structure that satisfies this; multi-module Gradle splits add
  build complexity with no v1 payoff (Constitution II).
- **Alternatives considered**:
  - *Multi-module Gradle project (feature modules)*: rejected — one team, one app, no
    current need for independent compilation boundaries.
  - *MVVM with business logic in ViewModels*: rejected — PRD-§29 explicitly forbids
    business logic in UI layer; untestable ViewModels violate Constitution VII.
  - *Clean Architecture with extra ceremony (entities/interactors/DTO mappers everywhere)*:
    rejected — unnecessary abstraction (Constitution II, PRD-§49). One mapper boundary at
    the repository implementation is sufficient.

## R-03 — History immutability mechanism (BR-02/03/04, FR-043, FR-056; resolves OQ-2)

- **Decision**: Snapshot + soft-delete model:
  1. Historical entities (`workout_sessions`, `workout_exercises`, `workout_sets`) store
     **name snapshots** (`routine_name_snapshot`, `exercise_name_snapshot`) and only
     *optional* foreign keys to templates/library. History never joins to templates for
     display names.
  2. Routine deletion removes the routine and its template rows only; historical sessions
     keep their `routine_id` nullable (SET NULL) plus the snapshot name.
  3. Exercise deletion is a **soft delete** (`deleted_at` timestamp): the exercise row
     remains (preserving referential integrity of history), is filtered out of library
     browsing/search/routine-editing pickers, and — with explicit user confirmation — its
     references are removed from routine *templates* (spec Assumption). History is
     untouched in all cases.
- **Rationale**: Directly satisfies FR-043/FR-056 (names as they were at workout time),
  BR-04 (deletion never corrupts history), and Constitution V (referential integrity
  enforced at the persistence layer). Soft delete keeps foreign keys valid without
  cascading destruction; snapshots make history display independent of mutable entities.
- **Alternatives considered**:
  - *Hard delete with ON DELETE SET NULL only*: rejected for exercises — loses the ability
    to show exercise metadata (muscle group) for historical sets and risks orphaned
    references in routine templates; soft delete is safer and reversible.
  - *Full deep-copy of exercise rows into history*: rejected — unnecessary duplication
    (Constitution II); name snapshots plus optional FK cover every stated requirement.
  - *Rename-propagation into history*: rejected — violates BR-11/FR-043 outright.

## R-04 — Weight unit storage & conversion (FR-048, FR-049, FR-052)

- **Decision**: Store each set's weight exactly as entered together with the unit it was
  entered in (`weight REAL`, `weight_unit TEXT` — `KG` or `LB`). Display converts to the
  currently selected unit using the exact factor 1 lb = 0.45359237 kg. The global setting
  only controls entry defaults and display; it never rewrites stored data.
- **Rationale**: Storing the entered value + its unit is truly unit-agnostic and makes
  conversion lossless and reversible (FR-049): the originally typed value (e.g. 45 lb) is
  preserved byte-for-byte and always re-displayable, satisfying "the values the user
  originally typed are preserved". No stored value is ever transformed, satisfying
  Constitution V. Volume and 1RM calculations normalize to the display unit at calculation
  time only.
- **Alternatives considered**:
  - *Store everything normalized to kg, convert on entry*: rejected — floating-point
    round-trips (kg→lb→kg) can drift from the typed value, violating the "originally
    typed values preserved" guarantee; also transforms user data on write (Constitution V).
  - *Store an integer in grams*: rejected — decimal weights (22.5) are required (FR-024)
    and REAL with unit context is simpler and sufficient at this scale.

## R-05 — Immediate persistence & crash recovery (FR-027, FR-038, FR-039, BR-05/06)

- **Decision**: Write-through persistence: every meaningful change (weight/reps edit, set
  completion, set add/delete, exercise add/remove, notes) issues a DAO write immediately
  (suspend function, awaited before UI confirms). The workout session row exists with
  status `ACTIVE` from the moment START is tapped. On app launch, the repository queries
  for a session with status `ACTIVE`; if found, the recovery prompt is shown (FR-038).
  No in-memory-only workout state, no batching, no deferred flush.
- **Rationale**: Guarantees SC-003 (zero data loss on interruption) with the simplest
  possible mechanism — Room + SQLite gives durable writes per operation. Deriving elapsed
  time from `started_at` (BR-07) means recovery needs no timer state at all.
- **Alternatives considered**:
  - *In-memory session + periodic flush*: rejected — violates FR-027/FR-054 and risks data
    loss between flushes.
  - *WorkManager/background service for persistence*: rejected — unnecessary complexity
    (Constitution II); direct DAO writes are sufficient for single-user local data.

## R-06 — Timers (workout duration & rest timer) (FR-021, FR-031, FR-032, BR-07)

- **Decision**: Workout duration = `now − started_at`, rendered by a UI ticker
  (1-second `Flow`/`LaunchedEffect` tick) that recomputes from timestamps; nothing is
  counted in memory. The rest timer is ephemeral UI state (countdown end-instant in
  ViewModel memory), default from Settings (FR-050), not restored after restart (spec
  Assumption), and fully independent of the duration ticker.
- **Rationale**: BR-07 mandates timestamp-derived duration; computing from an instant makes
  navigation/background/screen-off accuracy automatic (SC-004). Keeping the rest timer
  ephemeral matches the recorded assumption and avoids persisting throwaway state.
- **Alternatives considered**:
  - *Handler/CountDownTimer-based counting*: rejected — drifts and breaks across process
    death; violates BR-07.
  - *Persisting rest-timer state*: rejected — not required by any FR; adds schema surface
    (Constitution II).

## R-07 — Large-history performance & incremental loading (FR-058, SC-006, PRD-§51)

- **Decision**: Incremental loading via Room queries with `LIMIT`/`OFFSET` windows
  (page size ~30) driven by Compose `LazyColumn` scroll position, ordered by indexed
  columns (`workout_sessions.started_at DESC`, `workout_exercises.position`,
  `workout_sets.set_number`). Exercise history queries filter by indexed `exercise_id`.
  Detail screens load one session graph on demand.
- **Rationale**: Meets SC-006/FR-058 with plain Room queries. The AndroidX Paging library
  was considered but rejected: for a single-user local dataset, LIMIT/OFFSET with lazy
  lists is simpler, has fewer moving parts, and satisfies "never load the complete
  lifetime history at once" (Constitution II, XII — avoid extra dependencies when
  existing capabilities suffice).
- **Alternatives considered**:
  - *AndroidX Paging 3*: rejected — extra dependency and abstraction not justified at
    single-user scale; can be introduced later behind the repository interface if needed
    (BR-21).
  - *Loading all history and filtering in memory*: rejected — explicitly forbidden by
    FR-058.

## R-08 — Exercise library seeding (FR-005, FR-010, BR-20, SC-008)

- **Decision**: Ship seed data as a Kotlin data structure (~30+ common exercises across
  Chest, Back, Shoulders, Arms, Legs, sourced from PRD-§6) inserted transactionally on
  first launch via a Room `onCreate` callback that invokes a seeder repository. Routines
  are never seeded. Seeding is idempotent (guarded by the callback firing once per DB
  creation).
- **Rationale**: Code-based seed data is testable and needs no asset parsing; the Room
  `onCreate` callback is the standard hook that runs exactly once per database creation,
  matching "populated on first install".
- **Alternatives considered**:
  - *`createFromAsset` prebuilt database file*: rejected — harder to evolve with
    migrations and to test; binary asset adds maintenance burden.
  - *Network fetch of exercises*: rejected — offline-first (BR-18) forbids any network
    dependency.

## R-09 — Personal records & estimated 1RM (FR-047; resolves OQ-1)

- **Decision**: Records are computed on demand by querying completed-session data through
  the repository (heaviest weight, highest reps, best estimated 1RM, largest session
  volume), each with enough context to identify exercise/date (FR-047 scenario 1).
  Estimated 1RM uses the **Epley formula**: `1RM = weight × (1 + reps/30)` for reps ≥ 1
  (1RM = weight when reps = 1; REPS_ONLY sets and zero-weight sets are excluded from 1RM).
  The calculator lives in `domain` and is unit-tested; the formula is isolated behind a
  single function so it can be swapped later.
- **Rationale**: Epley is the proposed default recorded in the spec (Assumptions/OQ-1) and
  is a widely used standard estimation. Computing on demand avoids a derived-records table
    and its synchronization hazards (Constitution II); at single-user scale with indexed
    queries this is fast enough (Constitution XIII — no premature optimization).
- **Alternatives considered**:
  - *Brzycki, Lombardi, or other formulas*: viable, but no requirement prefers them; the
    spec proposed Epley, so Epley is adopted (Constitution I — don't invent beyond spec).
  - *Materialized records table updated on finish*: rejected — extra write path and
    consistency risk for no v1 requirement.

## R-10 — Routine reordering & duplication (FR-014, FR-015; spec Assumptions)

- **Decision**: Routines carry an integer `position` for home-screen order; drag-to-reorder
  rewrites positions transactionally and persists (FR-015). Duplication deep-copies the
  routine, its routine-exercises, and set templates with a derived name `"<name> Copy"`
  (FR-014). Names are not unique (spec Assumption).
- **Rationale**: Integer positions are the simplest persistent ordering. Deep copy makes
  the duplicate independently editable as required. No uniqueness constraint avoids
  blocking legitimate user input (spec explicitly says names need not be unique).
- **Alternatives considered**:
  - *Linked-list ordering (prev/next pointers)*: rejected — more complex updates for no
    benefit at this scale.
  - *Sparse/double positions (1.5 between 1 and 2)*: rejected — single-user local data;
    transactional renumbering is trivial and avoids float degradation.

## R-11 — Measurement types & extensibility (FR-030, BR-17)

- **Decision**: `workout_sets` and set templates carry a `measurement_type` column with
  v1 values `WEIGHT_AND_REPS` and `REPS_ONLY`. The column is a plain TEXT enum-like value
  validated at the application layer, so `DURATION` and `DISTANCE` can be added later as
  new values plus optional nullable columns — an additive, non-destructive migration.
- **Rationale**: Satisfies BR-17's extensibility requirement with the minimum schema
  surface; adding enum values later is a non-breaking change (Constitution V).
- **Alternatives considered**:
  - *Separate tables per measurement type*: rejected — speculative complexity for types
    not in v1 scope (Constitution II).
  - *JSON blob per set*: rejected — defeats typed queries, indexing, and validation
    (Constitution IV/V).

## R-12 — One-active-workout enforcement & discard semantics (FR-020, FR-037, BR-14/15)

- **Decision**: Starting a workout first checks for an existing `ACTIVE` session; if one
  exists the UI surfaces resume/discard instead of creating a second (FR-020). Discard
  requires explicit confirmation (FR-037) and deletes the session and its rows permanently
  (BR-15); discarded sessions never appear in history (history lists `COMPLETED` only —
  resolving OQ-3's proposed default).
- **Rationale**: Matches the spec's recorded assumptions exactly. Enforcing the invariant
  in the start-workout use case keeps it in one place (Constitution IX — one implementation
  per business rule).
- **Alternatives considered**:
  - *Keep discarded sessions with a `DISCARDED` status in the table*: rejected — BR-15
    says removed permanently; retaining rows would require filtering everywhere and
    contradicts the recorded assumption. (The `status` column still distinguishes
    ACTIVE/COMPLETED for lifecycle handling.)

## R-13 — Historical workout correction (FR-042, US11)

- **Decision**: Editing a completed workout reuses the same historical entities
  (`workout_sessions`, `workout_exercises`, `workout_sets`) behind an explicit edit action;
  edits write only to that session's rows. Adding an exercise to a historical workout
  inserts a `workout_exercises` row (with name snapshot from the library entry at edit
  time); removing deletes that session's rows only. Routine templates are never touched.
- **Rationale**: Historical rows are already the canonical record, so correcting them in
  place is the minimal change satisfying FR-042 while the "only that workout" guarantee is
  enforced structurally (writes are scoped by session id).
- **Alternatives considered**:
  - *Correction-as-new-version (event sourcing / versioned copies)*: rejected — no audit
    requirement exists; adds substantial complexity (Constitution II).

## R-14 — Testing strategy (Constitution VII, spec quality constraint)

- **Decision**:
  - *JVM unit tests* (`test/`): domain calculators (volume BR-12, duration BR-13, 1RM
    Epley, unit conversion), input validation (BR-16), use-case logic with fake
    repositories.
  - *Robolectric/JVM integration tests* (`test/` source set, run on the JVM): DAO behavior, repository flows, **history-protection regression tests**
    (routine deletion ≠ history deletion; exercise soft-delete ≠ history corruption;
    rename ≠ history rename), seeding.
  - *Room migration tests*: every schema change ships with a tested, non-destructive
    migration (BR-19, Constitution V).
  - *Compose UI tests*: critical journeys — start/log/finish workout, recovery prompt,
    set completion marking not color-only.
  - The spec's mandatory test list (quality constraint) maps 1:1 onto these suites.
- **Rationale**: Covers Constitution VII's unit/integration/e2e requirement while keeping
  most tests fast on the JVM.
- **Alternatives considered**:
  - *Instrumented-only tests*: rejected — slow feedback loop for core domain logic.
  - *Espresso instead of Compose test*: rejected — UI is Compose; compose-test is the
    native tooling.

## R-15 — Error handling & empty states (FR-059, FR-060)

- **Decision**: Repository/use-case boundaries return typed results (sealed result or
  thrown domain exceptions caught at the ViewModel edge); ViewModels map failures to
  user-friendly message resources — no stack traces surface (FR-060). Empty states are
  first-class composables with the exact spec copy ("No workout routines yet." + create
  action; "Your completed workouts will appear here.") and a clear no-search-results state.
- **Rationale**: Constitution VIII (actionable errors, no silent loss) and FR-059/FR-060.
- **Alternatives considered**: *Global exception handler only*: rejected — coarse; typed
  results make expected failure paths (e.g., invalid input, active-workout conflict)
  explicit at contracts level.

---

## Resolution status

| Open item | Status | Resolution |
|-----------|--------|------------|
| OQ-1 (1RM formula) | RESOLVED | Epley: weight × (1 + reps/30) (R-09) |
| OQ-2 (delete exercise referenced by routines) | RESOLVED | Soft delete + remove from templates with confirmation; history untouched (R-03) |
| OQ-3 (history includes non-completed sessions?) | RESOLVED | No — only completed sessions appear in history (R-12) |
| Technology stack (spec deferred) | RESOLVED | R-01 |
| Architecture (spec deferred) | RESOLVED | R-02 |
| Unit storage semantics (FR-049 detail) | RESOLVED | R-04 |

No NEEDS CLARIFICATION items remain.
