# Implementation Plan: Gymora — Workout Planning, Execution & History Tracking (v1)

**Branch**: `001-workout-tracker` | **Date**: 2026-09-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-workout-tracker/spec.md`

## Summary

Gymora v1 is an offline-first workout tracker. Users plan routines (templates) from a
seeded + custom exercise library, execute workouts with immediate per-set persistence and
crash-safe recovery, and review permanent, immutable history. The core promise is a fast,
reliable plan → do → save → review loop that never loses or mutates recorded data.

Technical approach (decided here per the spec's deferral): a single native Android app in
Kotlin with Jetpack Compose + Material 3, a layered UI → domain → data architecture, Room
(SQLite) for local persistence using snapshot-based history immutability and non-destructive
migrations, Hilt for dependency injection, and Coroutines/Flow for async. Delivery is split
into 11 independently testable, prioritized vertical slices (user stories US1–US11).

## Technical Context

**Language/Version**: Kotlin 2.0 (JVM target 17)

**Primary Dependencies**: Jetpack Compose (Material 3), AndroidX Navigation Compose, Room,
Hilt, Kotlin Coroutines + Flow/StateFlow, AndroidX ViewModel. No third-party libraries
beyond the standard AndroidX/Jetpack stack (per Constitution XII).

**Storage**: Room backed by SQLite. Local-only, offline-first (FR-053, BR-18). The Room
database is the offline source of truth that a future sync layer could sit above (BR-21).

**Testing**: JUnit (JVM unit tests for domain logic/calculations), Kotlin Coroutines Test,
Robolectric (JVM integration for repositories/DAOs), Compose UI Test, and Room migration
tests (Constitution VII; spec quality constraint).

**Target Platform**: Android — minSdk 26 (Android 8.0), compile/target SDK latest stable.
Single user, single device; no accounts or network in v1.

**Project Type**: mobile-app (single Android application module).

**Performance Goals**: History, routines, and exercise-history screens open and scroll
smoothly with 1,000+ stored workouts (SC-006); data loaded incrementally, never the full
lifetime history at once (FR-058). Set logging persists within the same interaction frame.

**Constraints**: Offline-first (zero network, SC-007); immediate persistence — no important
data in transient memory only (FR-027, FR-054); timers derived from timestamps, never an
in-memory counter (BR-07); non-destructive schema migrations (BR-19, FR-057); historical
records immutable to later template/library edits (BR-02/03/04, FR-043).

**Scale/Scope**: Single user, single device; years of accumulated history (1,000+ workouts);
~15 primary screens/composables across Home, Routines, Exercise Library, Active Workout,
History, Records, and Settings.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Requirements First (NON-NEGOTIABLE) | PASS | All plan content traces to `spec.md` (FR-xxx, BR-xx, SC-xxx) and `docs/prod_requirements.md`. No functionality invented. OQ-1/OQ-2/OQ-3 resolved explicitly in research.md with recorded defaults, not silently assumed. |
| II. Simplicity Over Complexity | PASS | Single Android app module, single local database. No microservices, no backend, no queues, no caching layer, no speculative abstractions. Stack limited to the PRD-directed Jetpack set. |
| III. Modular Architecture | PASS | Layered UI → domain → data with dependencies pointing inward (Constitution III, PRD-§29). Domain layer has no Android dependencies and is independently testable. |
| IV. API and Contract Stability | PASS | Repository/use-case interfaces define explicit inputs, outputs, and error behavior (see contracts/). Input validated at boundaries (weight/reps validation, unit conversion). |
| V. Data Integrity (NON-NEGOTIABLE) | PASS | Template vs. historical entities are strictly separated (FR-055). History uses name snapshots (FR-056). Exercise deletion is soft-delete (BR-04). Room migrations are explicit, additive, tested; destructive migration is never the default (BR-19). |
| VI. Security and Privacy | PASS | Fully offline/local; no network, no telemetry, no credentials in v1. No sensitive data in logs or error messages (FR-060). |
| VII. Testing | PASS | Test strategy covers unit (domain), integration (Room/repositories), UI (Compose), and migration tests; the spec's mandatory test list is captured in the quality constraint mapping. |
| VIII. Observability and Reliability | PASS | Explicit failure handling, user-friendly errors, no silent data loss (FR-060); immediate persistence and crash recovery (FR-027, FR-038). |
| IX. Code Quality | PASS | Small focused classes, single implementation per business rule (volume, duration, 1RM in domain layer only). |
| X. AI Agent Development Rules | PASS | Greenfield repo inspected (only spec/PRD/constitution exist); no unverified APIs referenced. |
| XI. Incremental Delivery | PASS | Feature decomposed into 11 independently deliverable, prioritized user stories (US1–US11) as vertical slices. |
| XII. Dependency Management | PASS | Only standard AndroidX/Jetpack libraries; no third-party dependencies added. |
| XIII. Performance | PASS | Incremental loading, indexed columns, lazy lists (FR-058, SC-006); no speculative optimization. |
| XIV. Documentation | PASS | Plan artifacts (research.md, data-model.md, contracts/, quickstart.md) document decisions and contracts. |
| XV. Definition of Done | PASS | Enforced at task level via tasks.md acceptance criteria. |

**Gate result**: PASS — no violations; Complexity Tracking not required.

**Post-design re-check (after Phase 0/1)**: PASS. The design artifacts introduce no new
violations: research.md resolves all open questions within the spec's recorded assumptions
(Principle I); data-model.md enforces template/history separation, snapshots, soft delete,
and a partial-unique index on the single-active-session invariant at the persistence layer
(Principles III, IV, V); contracts/ define explicit inputs/outputs/error behavior and keep
history-protection structural (Principles IV, VII); quickstart.md maps validation to the
spec's mandatory test list (Principle VII). Still no third-party dependencies beyond the
Jetpack set (Principle XII) and no speculative infrastructure (Principle II).

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

Single Android application module (`app/`) with a layered package structure
(PRD-§30, adapted). Package root: `com.gymora`.

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/gymora/
    │       ├── GymoraApplication.kt
    │       ├── ui/                          # Presentation layer (Compose)
    │       │   ├── navigation/             # Nav graph, destinations
    │       │   ├── theme/                  # Material 3 theme, dark mode
    │       │   ├── home/                   # Home screen (routine cards)
    │       │   ├── routines/               # Routine list, editor
    │       │   ├── library/                # Exercise library, search, editor
    │       │   ├── workout/                # Active workout, summary, recovery prompt
    │       │   ├── history/                # Workout history, workout detail, exercise history
    │       │   ├── records/                # Personal records
    │       │   ├── settings/               # Settings screen
    │       │   └── components/             # Shared composables (empty states, dialogs)
    │       ├── domain/                     # Pure Kotlin; no Android deps
    │       │   ├── model/                  # Exercise, Routine, WorkoutSession, Set, Settings…
    │       │   ├── repository/             # Repository interfaces
    │       │   ├── usecase/                # StartWorkout, LogSet, FinishWorkout, …
    │       │   └── calculator/             # Volume, duration, 1RM, unit conversion
    │       └── data/
    │           ├── local/
    │           │   ├── db/                 # Room database, migrations
    │           │   ├── entity/             # Room entities (template + historical)
    │           │   ├── dao/                # DAOs
    │           │   └── seed/               # Built-in exercise library seed data
    │           └── repository/             # Repository implementations, mappers
    ├── test/                               # JVM unit tests (domain, calculators)
    └── androidTest/                        # Instrumented/Robolectric: Room, DAOs,
                                            # repositories, migrations, Compose UI tests
```

**Structure Decision**: Single Android app module with three layers inside one package
root — `ui` (presentation) → `domain` (pure Kotlin models, repository interfaces, use
cases, calculators) → `data` (Room entities/DAOs/database, repository implementations,
seed data). Dependencies point inward only; `domain` has no Android imports. This is the
simplest structure satisfying Constitution III and PRD-§29/§30 without multi-module
overhead (Constitution II). Tests live in `test/` (JVM) and `androidTest/` (device/
Robolectric) per Android convention.

## Complexity Tracking

> Not applicable — Constitution Check passed with no violations.
