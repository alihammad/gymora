# Tasks: Gymora — Workout Planning, Execution & History Tracking (v1)

**Input**: Design documents from `/specs/001-workout-tracker/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: INCLUDED — the feature specification explicitly mandates automated tests (spec
quality constraint; PRD-§43 mandatory test list; Constitution VII). Tests are written
before implementation within each story phase.

**Organization**: Tasks are grouped by user story (US1–US11 from spec.md) to enable
independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1..US11)
- All paths are relative to the repository root; Android source root is
  `app/src/main/java/com/gymora/` (abbreviated `com/gymora/` below). JVM tests —
  domain unit tests AND Robolectric Room/repository integration tests — live in
  `app/src/test/java/com/gymora/`; instrumented Compose UI tests live in
  `app/src/androidTest/java/com/gymora/`.

## Path Conventions

Single Android app module per plan.md Structure Decision:

- App module: `app/`
- Presentation: `com/gymora/ui/`
- Domain (pure Kotlin): `com/gymora/domain/`
- Data: `com/gymora/data/`
- DI: `com/gymora/di/`
- Design docs: `specs/001-workout-tracker/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Android project initialization per plan.md Technical Context (Kotlin 2.0,
Compose + Material 3, Room, Hilt, Coroutines/Flow, Navigation Compose, minSdk 26).

- [X] T001 Create the Android project scaffold: Gradle Kotlin DSL with version catalog `gradle/libs.versions.toml` (Kotlin 2.0, Compose compiler plugin, KSP, Hilt, Room, Navigation Compose, JUnit, Robolectric, Compose UI test deps), root `settings.gradle.kts`, `app/build.gradle.kts` (minSdk 26, compile/target SDK latest stable, JVM target 17), `app/src/main/AndroidManifest.xml` (single activity, offline — no network permission), `app/proguard-rules.pro`, and `gradle.properties`
- [X] T002 [P] Configure static analysis: `config/detekt/detekt.yml` and wire detekt + Android Lint into `app/build.gradle.kts` (Constitution X — never disable checks to pass)
- [X] T003 [P] Create Material 3 theme foundation (System/Light/Dark plumbing for FR-051): `com/gymora/ui/theme/Color.kt`, `com/gymora/ui/theme/Type.kt`, `com/gymora/ui/theme/Theme.kt` with gym-comfortable dark palette (FR-051 dark mode, PRD-§40)
- [X] T004 [P] Create application entry points: `com/gymora/GymoraApplication.kt` (@HiltAndroidApp) and `com/gymora/MainActivity.kt` (single activity hosting the Compose nav graph)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story — domain
model vocabulary, Room database shell, DI wiring, navigation shell, error model, and
shared UI components.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [P] Create domain enums and value types in `com/gymora/domain/model/Enums.kt`: `WeightUnit { KG, LB }`, `Theme { SYSTEM, LIGHT, DARK }`, `MeasurementType { WEIGHT_AND_REPS, REPS_ONLY }`, `MuscleGroup { CHEST, BACK, SHOULDERS, ARMS, LEGS }`, `SessionStatus { ACTIVE, COMPLETED }` (data-model.md)
- [X] T006 [P] Create domain error types in `com/gymora/domain/model/Errors.kt`: `ValidationException(field, message)`, `ActiveWorkoutConflictException(activeSessionId)`, `EntityNotFoundException(id)` (contracts/repositories.md error model)
- [X] T007 [P] Create shared UI components: empty-state composable with exact spec copy ("No workout routines yet." + create action; "Your completed workouts will appear here."; no-search-results state) in `com/gymora/ui/components/EmptyStates.kt` and confirm-dialog composable in `com/gymora/ui/components/ConfirmDialogs.kt` (FR-059, FR-012)
- [X] T008 Create Room database shell `com/gymora/data/local/db/GymoraDatabase.kt` (version 1, @Database declaring ONLY `SettingsEntity` at this point — created here as the single-row entity per data-model.md with defaults KG/90s/SYSTEM; the remaining entities are registered into the @Database entity list by their story tasks T014/T025/T035 as they land, and the schema stays frozen at version 1 until release so no migrations are required; `fallbackToDestructiveMigration` NOT configured per BR-19) and Hilt module `com/gymora/di/DatabaseModule.kt` providing database + DAOs + `onCreate` seeding callback hook (R-08)
- [X] T009 Create navigation shell `com/gymora/ui/navigation/GymoraNavHost.kt` and `com/gymora/ui/navigation/Destinations.kt`: bottom nav with Home, History, Exercises, Settings (FR-004, PRD-§28) plus destinations for routine list (My Routines), routine detail/editor, exercise editor, active workout (dedicated experience replacing bottom nav — FR-004), workout summary, workout detail, exercise history, records; placeholder screens where needed
- [X] T010 Create settings persistence foundation (needed by theme plumbing and many stories): Room entity `SettingsEntity` is created in T008; here add DAO `com/gymora/data/local/dao/SettingsDao.kt`, domain model `Settings` in `com/gymora/domain/model/Settings.kt`, `SettingsRepository` interface in `com/gymora/domain/repository/SettingsRepository.kt`, implementation `com/gymora/data/repository/SettingsRepositoryImpl.kt` (FR-048..FR-051; settings in Room, not SharedPreferences — FR-054), Hilt binding in `com/gymora/di/RepositoryModule.kt`, and theme application wiring in `MainActivity.kt`

**Checkpoint**: Foundation ready — app builds, navigation shell runs, database creates,
settings persist. User story implementation can now begin.

---

## Phase 3: User Story 1 — Exercise Library & Custom Exercises (Priority: P1) 🎯 MVP

**Goal**: Browsable, searchable seeded exercise library; create/edit custom exercises;
deletion never destroys history (soft delete).

**Independent Test**: Fresh install → library shows 30+ seeded exercises grouped by muscle
group and zero routines; search "bench" returns matches; create a custom exercise; edit it;
confirm it appears in search — no routine or workout needed (spec US1).

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T011 [P] [US1] Unit test for exercise input validation (blank name rejected, muscle group restricted) in `app/src/test/java/com/gymora/domain/ExerciseValidationTest.kt`
- [X] T012 [P] [US1] Robolectric/Room test: seeding inserts 30+ built-in exercises across 5 muscle groups, is idempotent, and seeds zero routines in `app/src/test/java/com/gymora/data/LibrarySeedingTest.kt` (FR-005, FR-010, SC-008)
- [X] T013 [P] [US1] Robolectric/Room test: ExerciseRepository — create custom, edit, search includes custom exercises, soft delete hides from library/search but keeps row in `app/src/test/java/com/gymora/data/ExerciseRepositoryTest.kt` (FR-006..FR-009)

### Implementation for User Story 1

- [X] T014 [P] [US1] Create Room entity `com/gymora/data/local/entity/ExerciseEntity.kt` per data-model.md (name, muscle_group, description, notes, is_custom, deleted_at, timestamps; partial index for active-name lookup) and register it in the `GymoraDatabase` @Database entity list (per T008 incremental registration)
- [X] T015 [P] [US1] Create domain model `com/gymora/domain/model/Exercise.kt`
- [X] T016 [US1] Create `com/gymora/data/local/dao/ExerciseDao.kt` (observe active library, case-insensitive name search excluding soft-deleted, insert/update, soft-delete timestamp update, get-by-id)
- [X] T017 [US1] Create seed data `com/gymora/data/local/seed/ExerciseSeedData.kt` — 30+ exercises from PRD-§6 lists across Chest/Back/Shoulders/Arms/Legs (SC-008) — and seeder `com/gymora/data/local/seed/LibrarySeeder.kt` invoked transactionally from the Room onCreate callback (R-08)
- [X] T018 [US1] Implement `ExerciseRepository` interface in `com/gymora/domain/repository/ExerciseRepository.kt` and implementation with entity↔domain mappers in `com/gymora/data/repository/ExerciseRepositoryImpl.kt`; bind in `com/gymora/di/RepositoryModule.kt` (contracts/repositories.md)
- [X] T019 [US1] Implement library browse/search UI: `com/gymora/ui/library/ExerciseLibraryScreen.kt`, `ExerciseLibraryViewModel.kt`, `ExerciseLibraryUiState.kt` — grouped by muscle group, search field with empty-result state (FR-008)
- [X] T020 [US1] Implement custom exercise create/edit UI: `com/gymora/ui/library/ExerciseEditorScreen.kt`, `ExerciseEditorViewModel.kt` — name required, optional muscle group/description/notes (FR-006, FR-007)
- [X] T021 [US1] Implement exercise delete flow with confirmation dialog in `ExerciseLibraryScreen.kt` calling soft delete; wire `DeleteExerciseUseCase` stub behavior (template-reference removal lands in US2's routine tables) (FR-009, R-03)

**Checkpoint**: User Story 1 fully functional and independently testable (MVP).

---

## Phase 4: User Story 2 — Routine Creation & Management (Priority: P1)

**Goal**: Create/rename/edit/duplicate/delete routines; add/reorder exercises; configure
planned sets; deletion never touches history.

**Independent Test**: Create a routine with 3 exercises × 3 sets, reorder exercises,
duplicate, rename, delete (with confirmation), verify persistence across app restart — no
workout execution required (spec US2).

### Tests for User Story 2 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T022 [P] [US2] Unit tests for routine validation and duplicate naming ("<name> Copy") in `app/src/test/java/com/gymora/domain/RoutineRulesTest.kt` (FR-011, FR-014)
- [X] T023 [P] [US2] Robolectric/Room test: RoutineRepository — create/rename/delete/duplicate/reorder, add/remove/reorder exercises, set template CRUD, positions persist in `app/src/test/java/com/gymora/data/RoutineRepositoryTest.kt` (FR-011..FR-018)
- [X] T024 [P] [US2] Robolectric/Room regression test: deleting a routine never deletes or alters workout history rows; deleting an exercise referenced by templates removes template references but never touches history (BR-03, BR-04, SC-005) in `app/src/test/java/com/gymora/data/TemplateDeletionHistoryProtectionTest.kt`

### Implementation for User Story 2

- [X] T025 [P] [US2] Create Room entities `com/gymora/data/local/entity/RoutineEntity.kt` (position column for home order), `RoutineExerciseEntity.kt` (UNIQUE(routine_id, position)), `SetTemplateEntity.kt` (UNIQUE(routine_exercise_id, set_number), measurement_type, target_weight_unit) per data-model.md, and register all three in the `GymoraDatabase` @Database entity list (per T008 incremental registration)
- [X] T026 [P] [US2] Create domain models `com/gymora/domain/model/Routine.kt` (`RoutineSummary`, `RoutineDetail`, `RoutineExerciseDetail`, `SetTemplate`, `SetTemplateInput`)
- [X] T027 [US2] Create DAOs `com/gymora/data/local/dao/RoutineDao.kt`, `RoutineExerciseDao.kt`, `SetTemplateDao.kt` (ordered reads by position, cascade-safe deletes, transactional reorder)
- [X] T028 [US2] Implement `RoutineRepository` interface in `com/gymora/domain/repository/RoutineRepository.kt` and implementation in `com/gymora/data/repository/RoutineRepositoryImpl.kt` (create/rename/updateDescription/delete/duplicate/reorder + exercise membership + set templates); bind in `com/gymora/di/RepositoryModule.kt` (contracts/repositories.md)
- [X] T029 [US2] Implement use cases `com/gymora/domain/usecase/DeleteRoutineUseCase.kt` (template-only deletion, FR-013), `DuplicateRoutineUseCase.kt` (deep copy, R-10), and complete `DeleteExerciseUseCase.kt` in `com/gymora/domain/usecase/DeleteExerciseUseCase.kt` (soft delete + remove from routine templates with confirmation, R-03/OQ-2)
- [X] T030 [US2] Implement home screen `com/gymora/ui/home/HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt`: routine cards (name, exercise count, last-performed date, START), drag-to-reorder persisting positions, Create Routine action, navigation links to My Routines / Recent Workouts / History, empty state (FR-001, FR-002, FR-015, FR-059)
- [X] T031 [US2] Implement routine detail/editor `com/gymora/ui/routines/RoutineEditorScreen.kt`, `RoutineEditorViewModel.kt`, `RoutineEditorUiState.kt`: rename, description, add exercises (library picker excluding soft-deleted), remove/reorder exercises, per-exercise notes, set template add/edit/delete (target reps × target weight), duplicate, delete-with-confirmation (FR-011..FR-018, PRD-§36)
- [X] T031a [US2] Implement My Routines list destination `com/gymora/ui/routines/RoutineListScreen.kt` + `RoutineListViewModel.kt`: all routines ordered by position (RoutineRepository.observeAll), tap opens routine detail/editor, reachable from the Home "My Routines" link wired in T030 (FR-002)

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 — Workout Execution & Logging (Priority: P1)

**Goal**: START a routine → active session with timestamp-derived timer, immediate per-set
persistence, add sets/exercises, finish with confirmation and summary, cancel with
confirmation. This is the product's core value story.

**Independent Test**: Start a workout from a routine, log sets (incl. decimal weights), add
a set, add/remove an exercise, force-stop mid-workout and verify data survives, finish and
verify summary figures (duration, sets, reps, volume) (spec US3).

### Tests for User Story 3 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T032 [P] [US3] Unit tests for calculators in `app/src/test/java/com/gymora/domain/WorkoutCalculatorsTest.kt`: volume = Σ(weight×reps) over completed weighted sets (BR-12), duration/elapsed from timestamps (BR-13), input validation (negative weight/reps rejected, zero weight allowed, decimals accepted — BR-16)
- [X] T033 [P] [US3] Robolectric/Room test: start workout copies routine into session rows with status ACTIVE and start timestamp; at-most-one-active enforced; set logging persists immediately; add/delete sets; add exercise (both "add to routine" and "this workout only"); remove exercise leaves routine untouched in `app/src/test/java/com/gymora/data/WorkoutExecutionTest.kt` (FR-019, FR-020, FR-023..FR-029)
- [X] T034 [P] [US3] Robolectric/Room test: finish records end timestamp, sets COMPLETED, returns correct summary; discard deletes session permanently; zero-completed-set finish allowed in `app/src/test/java/com/gymora/data/WorkoutFinishDiscardTest.kt` (FR-033..FR-037, BR-15)

### Implementation for User Story 3

- [X] T035 [P] [US3] Create Room entities `com/gymora/data/local/entity/WorkoutSessionEntity.kt` (routine_name_snapshot NOT NULL, status, partial UNIQUE index on status='ACTIVE' enforcing BR-14 at persistence layer), `WorkoutExerciseEntity.kt` (exercise_name_snapshot, position), `WorkoutSetEntity.kt` (weight+weight_unit, reps, measurement_type, is_completed, completed_at) per data-model.md, and register all three in the `GymoraDatabase` @Database entity list (per T008 incremental registration; with T014/T025 this completes the full 8-entity schema from data-model.md)
- [X] T036 [P] [US3] Create domain models `com/gymora/domain/model/Workout.kt`: `WorkoutSession`, `ActiveWorkout`, `ActiveExercise`, `SetValue`, `WorkoutSummary`, `WorkoutDetail`
- [X] T037 [US3] Create DAOs `com/gymora/data/local/dao/WorkoutSessionDao.kt` (observe ACTIVE, insert, finish update, delete), `WorkoutExerciseDao.kt`, `WorkoutSetDao.kt` (immediate single-row writes) with indexes per data-model.md
- [X] T038 [US3] Implement calculators `com/gymora/domain/calculator/WorkoutCalculators.kt`: totalVolume (unit-normalized), duration/elapsed from Instants, convertWeight (1 lb = 0.45359237 kg) (BR-12/13, R-04)
- [X] T039 [US3] Implement `WorkoutSessionRepository` interface in `com/gymora/domain/repository/WorkoutSessionRepository.kt` and implementation in `com/gymora/data/repository/WorkoutSessionRepositoryImpl.kt`: startFromRoutine (deep-copy templates→historical rows, snapshot names, throw ActiveWorkoutConflictException), write-through set logging, add/delete sets, add exercise with addToRoutine flag, remove exercise from session only, finish, discard (contracts/repositories.md, R-05)
- [X] T040 [US3] Implement use cases in `com/gymora/domain/usecase/`: `StartWorkoutUseCase.kt`, `LogSetUseCase.kt` (BR-16 validation), `ModifySessionStructureUseCase.kt`, `FinishWorkoutUseCase.kt`, `DiscardWorkoutUseCase.kt`
- [X] T041 [US3] Implement active workout UI `com/gymora/ui/workout/ActiveWorkoutScreen.kt`, `ActiveWorkoutViewModel.kt`, `ActiveWorkoutUiState.kt`: timestamp-derived ticking timer (00:00:00, BR-07/FR-021), per-exercise set rows with weight (decimal)/reps inputs and complete toggle marked not by color alone (FR-022, FR-024, FR-025, FR-061, FR-062), add set, add exercise with "Add to routine / This workout only" dialog (FR-028), skip/remove exercise for this workout only (FR-029), FINISH WORKOUT confirmation showing duration/exercises/completed sets/volume (FR-033), cancel confirmation with "Keep working out"/"Discard workout" (FR-037)
- [X] T042 [US3] Implement workout summary screen `com/gymora/ui/workout/WorkoutSummaryScreen.kt` (routine name, date, duration, exercise count, set count, total reps, total volume, per-exercise breakdown — FR-035) and wire START actions on `HomeScreen.kt`/routine detail into `StartWorkoutUseCase` with dedicated-workout navigation switch (FR-003, FR-004)

**Checkpoint**: Core plan→do→save loop works end-to-end; Stories 1–3 independently testable.

---

## Phase 6: User Story 4 — Workout Recovery After Interruption (Priority: P1)

**Goal**: Detect an unfinished workout on launch; RESUME restores all logged sets with
correct elapsed duration; discard requires confirmation.

**Independent Test**: Start a workout, log two sets, force-stop the app, relaunch, verify
the recovery prompt with correct elapsed time, resume, verify all sets and timer intact
(spec US4).

### Tests for User Story 4 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T043 [P] [US4] Robolectric test: on launch with an ACTIVE session the recovery state exposes workout name + start time; resume returns full session graph with all logged sets; discard path requires explicit confirmation before deletion in `app/src/test/java/com/gymora/data/WorkoutRecoveryTest.kt` (FR-038, FR-039, SC-003)

### Implementation for User Story 4

- [X] T044 [US4] Implement `ResumeWorkoutUseCase.kt` in `com/gymora/domain/usecase/ResumeWorkoutUseCase.kt` (detect ACTIVE session, expose name + startedAt, restore graph)
- [X] T045 [US4] Implement recovery prompt UI `com/gymora/ui/workout/RecoveryPromptScreen.kt` ("Workout in progress — <name>, started X ago" + RESUME, FR-038) shown on launch from `MainActivity.kt`/nav startup logic, with discard-confirmation reusing `ConfirmDialogs.kt`

**Checkpoint**: Stories 1–4 independently functional; crash-safe core loop complete.

---

## Phase 7: User Story 5 — Workout History Review (Priority: P1)

**Goal**: Completed-workout history, newest first, paged; read-only detail with full
fidelity; snapshots immune to renames/deletions.

**Independent Test**: Complete a workout, then rename the routine, remove an exercise from
it, delete an exercise from the library — the history entry still shows the original name
and complete original data (spec US5).

### Tests for User Story 5 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T046 [P] [US5] Robolectric/Room regression test (mandatory per spec quality constraint): renaming/deleting routines or exercises changes 0 historical records; history detail displays snapshot names in `app/src/test/java/com/gymora/data/HistoryProtectionTest.kt` (FR-043, BR-11, SC-005)
- [X] T047 [P] [US5] Robolectric/Room test: history lists only COMPLETED sessions newest-first with LIMIT/OFFSET paging; empty history returns empty list in `app/src/test/java/com/gymora/data/HistoryRepositoryTest.kt` (FR-040, FR-058, R-12)

### Implementation for User Story 5

- [X] T048 [US5] Implement `HistoryRepository` interface in `com/gymora/domain/repository/HistoryRepository.kt` and read-side implementation in `com/gymora/data/repository/HistoryRepositoryImpl.kt` (listCompleted paged by started_at DESC, getWorkoutDetail full graph from snapshot rows — no joins to templates for display names) (contracts/repositories.md, R-07)
- [X] T049 [US5] Implement history list UI `com/gymora/ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`, `HistoryUiState.kt`: newest-first entries (date, workout name, duration), incremental loading in LazyColumn (FR-040, FR-058), empty state "Your completed workouts will appear here." (FR-059)
- [X] T050 [US5] Implement read-only workout detail UI `com/gymora/ui/history/WorkoutDetailScreen.kt` showing every exercise and set as performed (FR-041)
- [X] T050a [US5] Implement the Recent Workouts section on `HomeScreen.kt`/`HomeViewModel.kt`: the 3 most recent completed workouts (HistoryRepository.listCompleted(limit=3) per contracts/usecases.md Home mapping; spec Assumption "Recent Workouts on home"), tap opens workout detail, wired to the Home "Recent Workouts" link from T030 (FR-002)

**Checkpoint**: All five P1 stories independently functional — full core product complete.

---

## Phase 8: User Story 6 — Previous Performance Display & Pre-Fill (Priority: P2)

**Goal**: Show most recent performance per exercise beside today's sets and pre-fill
today's values, reducing gym typing.

**Independent Test**: Complete a routine once, start it again, verify previous values shown
and fields pre-filled, modify one value, verify the saved set reflects the edit (spec US6).

### Tests for User Story 6 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T051 [P] [US6] Robolectric/Room test: previous-performance lookup returns most recent completed sets per exercise; returns null for never-performed exercises; pre-filled values are overridable on save in `app/src/test/java/com/gymora/data/PreviousPerformanceTest.kt` (FR-045, FR-046, SC-002)

### Implementation for User Story 6

- [ ] T052 [US6] Implement previous-performance query in `WorkoutSessionDao.kt` / `WorkoutSessionRepositoryImpl.kt` (latest COMPLETED session's completed sets for an exercise) and `PreviousPerformanceUseCase.kt` in `com/gymora/domain/usecase/PreviousPerformanceUseCase.kt` (contracts/repositories.md)
- [ ] T053 [US6] Extend `ActiveWorkoutScreen.kt` and `ActiveWorkoutViewModel.kt`: previous-performance column beside today's sets, pre-fill weight/reps from previous values keeping fields editable, empty for never-performed exercises (FR-045, FR-046, PRD-§9/§10)

**Checkpoint**: Story 6 independently verifiable on top of the core loop.

---

## Phase 9: User Story 7 — Exercise History (Priority: P2)

**Goal**: Per-exercise history across workouts, newest first, efficient with large data.

**Independent Test**: Complete two workouts containing the same exercise on different
dates; both performances appear newest-first with all sets (spec US7).

### Tests for User Story 7 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T054 [P] [US7] Robolectric/Room test: exercise history returns all performances by date newest-first with all sets, paged via indexed exercise_id lookup in `app/src/test/java/com/gymora/data/ExerciseHistoryTest.kt` (FR-044, R-07)

### Implementation for User Story 7

- [ ] T055 [US7] Implement `getExerciseHistory` in `HistoryRepositoryImpl.kt` + supporting DAO query (filter by indexed `exercise_id`, ORDER BY session started_at DESC, LIMIT/OFFSET) (FR-044)
- [ ] T056 [US7] Implement exercise history UI `com/gymora/ui/history/ExerciseHistoryScreen.kt` + ViewModel (performances grouped by date with all sets), reachable from exercise library and workout detail (PRD-§17)

**Checkpoint**: Story 7 independently verifiable.

---

## Phase 10: User Story 8 — Rest Timer (Priority: P2)

**Goal**: Configurable rest countdown after set completion with Skip/+30s/Restart,
independent of the workout duration timer.

**Independent Test**: Complete a set, start rest timer, verify countdown, use
Skip/+30s/Restart, verify workout duration unaffected; change default in settings and
verify new default applies (spec US8).

### Tests for User Story 8 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T057 [P] [US8] Unit test for rest-timer state machine (start/skip/+30s/restart, default from settings, end-instant math) in `app/src/test/java/com/gymora/ui/workout/RestTimerTest.kt` (FR-031, FR-032)

### Implementation for User Story 8

- [ ] T058 [US8] Implement ephemeral rest-timer state in `ActiveWorkoutViewModel.kt` (countdown end-instant in memory, not persisted — R-06) with Skip/+30s/Restart actions and default from `SettingsRepository` (FR-031, FR-032)
- [ ] T059 [US8] Implement rest-timer UI component in `com/gymora/ui/workout/RestTimerBar.kt` shown during active workout without interfering with the duration ticker (FR-032)

**Checkpoint**: Story 8 independently verifiable.

---

## Phase 11: User Story 9 — Settings & Personalization (Priority: P2)

**Goal**: Settings screen for weight unit (kg/lb), default rest duration, and theme —
persisted, with lossless display conversion of all recorded weights on unit switch.

**Independent Test**: Change each setting, restart the app, verify persistence and effect
(unit shown everywhere, rest default, theme rendering) (spec US9).

### Tests for User Story 9 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T060 [P] [US9] Unit test: convertWeight is lossless/reversible (kg→lb→kg returns original typed value) using exact factor 0.45359237 in `app/src/test/java/com/gymora/domain/UnitConversionTest.kt` (FR-049, R-04)
- [ ] T061 [P] [US9] Robolectric test: settings persist across database reopen; unit switch never rewrites stored weights; display conversion applies to history/summary/previous-performance/records values in `app/src/test/java/com/gymora/data/SettingsPersistenceTest.kt` (FR-048..FR-052)

### Implementation for User Story 9

- [ ] T062 [US9] Implement settings UI `com/gymora/ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt`: weight unit selector (kg/lb), default rest duration (30s/60s/90s/2min/3min/custom), theme selector (System/Light/Dark) (FR-048, FR-050, FR-051, PRD-§39)
- [ ] T063 [US9] Apply display-unit conversion across all weight-rendering surfaces (active workout, summary, history detail, previous performance, records) via domain `convertWeight`, keeping stored values untouched (FR-049, FR-052, R-04)

**Checkpoint**: Story 9 independently verifiable.

---

## Phase 12: User Story 10 — Basic Personal Records & Statistics (Priority: P2)

**Goal**: Detect heaviest weight, highest reps, best estimated 1RM (Epley), and largest
workout volume from completed workouts, with exercise/date context.

**Independent Test**: Complete workouts with known values; verify recorded maxima match
expectations and update when surpassed (spec US10).

### Tests for User Story 10 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T064 [P] [US10] Unit test for Epley 1RM (weight × (1 + reps/30); reps ≥ 1; excludes REPS_ONLY/zero-weight sets) and records selection logic in `app/src/test/java/com/gymora/domain/PersonalRecordsTest.kt` (FR-047, R-09)
- [ ] T065 [P] [US10] Robolectric/Room test: records computed from completed sessions update when surpassed and carry exercise/date context in `app/src/test/java/com/gymora/data/RecordsRepositoryTest.kt` (FR-047)

### Implementation for User Story 10

- [ ] T066 [US10] Implement `estimatedOneRepMax` in `com/gymora/domain/calculator/WorkoutCalculators.kt` (Epley, R-09) and `RecordsRepository` interface in `com/gymora/domain/repository/RecordsRepository.kt` + implementation in `com/gymora/data/repository/RecordsRepositoryImpl.kt` (on-demand computation over completed sets; bind in `com/gymora/di/RepositoryModule.kt`) (contracts/repositories.md)
- [ ] T067 [US10] Implement records UI `com/gymora/ui/records/RecordsScreen.kt` + ViewModel showing the four records with exercise + date context, reachable from navigation (FR-047, PRD-§18)

**Checkpoint**: Story 10 independently verifiable.

---

## Phase 13: User Story 11 — Historical Workout Correction (Priority: P3)

**Goal**: Explicitly edit a completed workout to correct set values, add/remove exercises
within it, and edit workout notes — affecting only that workout.

**Independent Test**: Complete a workout, edit a set's weight/reps in history, add and
remove an exercise within that workout, verify corrections persist while all other
history, routines, and templates remain untouched (spec US11).

### Tests for User Story 11 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T068 [P] [US11] Robolectric/Room test: historical corrections write only to the target session's rows; routines/templates/other workouts unchanged; history stays read-only unless edit explicitly chosen in `app/src/test/java/com/gymora/data/HistoricalCorrectionTest.kt` (FR-042, BR-11)

### Implementation for User Story 11

- [ ] T069 [US11] Implement correction methods in `HistoryRepositoryImpl.kt` (correctSet, addExerciseToHistoricalWorkout with name snapshot at edit time, removeExerciseFromHistoricalWorkout, updateHistoricalWorkoutNotes — all scoped by session id) and `CorrectHistoricalWorkoutUseCase.kt` in `com/gymora/domain/usecase/CorrectHistoricalWorkoutUseCase.kt` (contracts/repositories.md)
- [ ] T070 [US11] Implement edit mode in `WorkoutDetailScreen.kt`: explicit Edit action unlocks editing of set values/completion/notes, add/remove exercise within the workout, workout-level notes; save persists via correction APIs; default remains read-only (FR-042, PRD-§16)

**Checkpoint**: All 11 user stories independently functional.

---

## Phase 14: Polish & Cross-Cutting Concerns

**Purpose**: Improvements affecting multiple stories; final validation against the spec.

- [ ] T071 [P] Accessibility pass (FR-061, PRD-§52): content descriptions, labeled controls, touch target sizes, readable contrast, set-completion indication not color-only — across `com/gymora/ui/` screens
- [ ] T072 [P] Large-history performance validation (SC-006, quickstart scenario 7): fixture generating 1,000+ completed workouts and `app/src/test/java/com/gymora/data/HistoryPagingTest.kt` verifying incremental loading and responsive scrolling
- [ ] T073 [P] Compose UI tests for critical journeys in `app/src/androidTest/java/com/gymora/ui/`: start→log→finish workout, recovery prompt flow, cancel/discard confirmation, empty states, and elapsed-timer accuracy after navigation/backgrounding (displayed elapsed matches timestamp-derived duration — SC-004) (Constitution VII end-to-end coverage)
- [ ] T074 Error-handling audit (FR-060, R-15): verify all repository failures map to user-friendly messages, no stack traces surface, recoverable UI state preserved — across ViewModels in `com/gymora/ui/`
- [ ] T075 Offline verification (SC-007): confirm zero network permissions/usage and full functionality with connectivity disabled; document result in `specs/001-workout-tracker/quickstart.md` validation notes
- [ ] T076 Run full quickstart.md validation (all 10 scenarios + definition-of-done checklist) in `specs/001-workout-tracker/quickstart.md` and fix any failures
- [ ] T077 Run detekt/lint and full test suites (`testDebugUnitTest`, `connectedDebugAndroidTest`); fix violations without disabling rules (Constitution X)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately (T001 first; T002–T004 parallel after/with it where files differ).
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories.
- **User Stories (Phases 3–13)**: All depend on Foundational completion.
  - P1 stories (US1–US5) deliver the core product; P2 (US6–US10) and P3 (US11) add on top.
  - Data-flow dependencies between stories (see below) constrain ordering for a single implementer.
- **Polish (Phase 14)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: After Foundational — no story dependencies.
- **US2 (P1)**: After US1 (routine exercises reference the exercise library; delete-exercise template cleanup needs routine tables).
- **US3 (P1)**: After US2 (start workout copies a routine into a session).
- **US4 (P1)**: After US3 (recovery operates on active sessions).
- **US5 (P1)**: After US3 (history lists completed sessions).
- **US6 (P2)**: After US3 + US5 (needs completed sessions to look up).
- **US7 (P2)**: After US5 (exercise history queries historical sets).
- **US8 (P2)**: After US3 (rest timer lives in the active workout; default from settings foundation).
- **US9 (P2)**: After US3 + US5 (unit conversion applies across workout/history surfaces; settings persistence exists from Foundational).
- **US10 (P2)**: After US5 (records computed from completed sessions).
- **US11 (P3)**: After US5 (correction edits historical workouts).

### Within Each User Story

- Tests MUST be written and FAIL before implementation.
- Entities/models before DAOs; DAOs before repositories; repositories before use cases; use cases before UI.
- Story complete (tests green) before moving to the next priority.

### Parallel Opportunities

- Setup: T002, T003, T004 parallel (after T001 scaffold).
- Foundational: T005, T006, T007 parallel; T008/T009/T010 sequential-ish (T010 needs T008).
- Within stories: all test tasks marked [P] can run in parallel; entity/model tasks marked [P] can run in parallel.
- Across stories (multi-developer): US6, US7, US8, US9, US10 can proceed in parallel once their listed prerequisites are done; US11 independent after US5.

---

## Parallel Example: User Story 1

```bash
# Launch all US1 tests together (write first, expect failures):
Task: "Unit test for exercise input validation in app/src/test/java/com/gymora/domain/ExerciseValidationTest.kt"
Task: "Robolectric/Room test: seeding in app/src/test/java/com/gymora/data/LibrarySeedingTest.kt"
Task: "Robolectric/Room test: ExerciseRepository in app/src/test/java/com/gymora/data/ExerciseRepositoryTest.kt"

# Launch US1 entity + domain model together:
Task: "Create Room entity com/gymora/data/local/entity/ExerciseEntity.kt"
Task: "Create domain model com/gymora/domain/model/Exercise.kt"
```

## Parallel Example: User Story 3

```bash
# Launch all US3 tests together:
Task: "Unit tests for calculators in app/src/test/java/com/gymora/domain/WorkoutCalculatorsTest.kt"
Task: "Robolectric/Room test: workout execution in app/src/test/java/com/gymora/data/WorkoutExecutionTest.kt"
Task: "Robolectric/Room test: finish/discard in app/src/test/java/com/gymora/data/WorkoutFinishDiscardTest.kt"

# Launch US3 entities + domain models together:
Task: "Create Room entities WorkoutSessionEntity/WorkoutExerciseEntity/WorkoutSetEntity"
Task: "Create domain models com/gymora/domain/model/Workout.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: seeded library browsable/searchable, custom exercises work
5. Demo if ready

### Core Product (recommended second milestone)

Continue Phases 4–7 (US2–US5) → the full plan→do→save→review loop with crash recovery and
protected history. This satisfies SC-001's end-to-end journey and the definition of done
(PRD-§56).

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → validate → MVP
3. US2 → validate (routines persist, duplicate/delete safe)
4. US3 → validate (core logging loop, immediate persistence)
5. US4 → validate (crash recovery — trust requirement)
6. US5 → validate (history immutability regression tests green)
7. US6–US10 (P2) in any dependency-respecting order → validate each
8. US11 (P3) → validate
9. Polish phase → full quickstart validation + suites green

### Parallel Team Strategy

With multiple developers after Foundational:

1. Developer A: US1 → US2 → US3 (critical path)
2. Developer B: joins at US4/US5 once US3 lands; then US7, US10
3. Developer C: US6, US8, US9 as prerequisites complete; US11 last

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks.
- [Story] label maps task to its user story for traceability.
- Tests are mandated by the spec (quality constraint) — write them first, watch them fail, then implement.
- Commit after each task or logical group; stop at any checkpoint to validate the story independently.
- History-protection regression tests (T024, T046) are NON-NEGOTIABLE per Constitution V and SC-005.
- Never configure destructive Room fallback (BR-19); migrations must be explicit and tested.
- Avoid: vague tasks, same-file conflicts, cross-story dependencies that break independence.
