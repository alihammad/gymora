# Quickstart: Validation Guide — Workout Tracker v1

**Branch**: `001-workout-tracker` | **Date**: 2026-09-02

Runnable validation scenarios proving the feature works end-to-end. This is a
validation/run guide only — implementation details live in `tasks.md` and the
implementation phase. Data shapes referenced: `data-model.md`; interfaces referenced:
`contracts/`.

## Prerequisites

- Android Studio (latest stable) with Android SDK; compile/target SDK latest stable,
  minSdk 26 (see plan.md Technical Context).
- JDK 17.
- An emulator (API 26+) or physical device for instrumented/UI tests; JVM tests run
  without a device.
- No network required — the app is offline-first (BR-18, SC-007).

## Build & run

```bash
# From repository root (after the Android project scaffold exists)
./gradlew :app:assembleDebug          # build the app
./gradlew :app:installDebug           # install on connected device/emulator
./gradlew :app:testDebugUnitTest      # JVM unit tests (domain logic, calculators)
./gradlew :app:testDebugUnitTest --tests "*MigrationTest*"   # Room migration tests
./gradlew :app:connectedDebugAndroidTest                     # instrumented/UI tests
```

## Validation scenarios

Each scenario maps to spec acceptance criteria. Run the app and verify manually, or via
the corresponding automated test named in parentheses.

### 1. First launch & seeded library (FR-005, FR-010, SC-008)

1. Install fresh (clear app data first if reinstalling).
2. Open the exercise library.
3. **Expect**: 30+ built-in exercises grouped into Chest, Back, Shoulders, Arms, Legs;
   zero routines on Home ("No workout routines yet." empty state).
   *(test: `LibrarySeedingTest`)*

### 2. Create routine → start → log → finish (SC-001 core loop)

1. Create routine "Chest Workout", add 3 exercises, 3 planned sets each
   (e.g., Bench Press 10×60, 8×70, 6×80).
2. Tap START. **Expect**: session created, timer running (00:00:00 format), planned sets shown.
3. Enter weight/reps for each set (include one decimal weight, e.g. 22.5) and mark complete.
   **Expect**: each set saves immediately; completion marked not by color alone (FR-025).
4. Try entering a negative weight → **Expect**: rejected (BR-16).
5. Tap FINISH WORKOUT. **Expect**: confirmation shows duration, exercise count, completed
   set count, total volume; after confirming, summary shows all FR-035 fields.
6. Verify total volume = Σ(weight × reps) of completed weighted sets (BR-12).
   *(tests: `WorkoutExecutionTest`, `WorkoutCalculatorsTest`)*

### 3. Crash recovery (FR-027, FR-038, FR-039, SC-003)

1. Start a workout, log 2 sets.
2. Force-stop the app (or kill the process).
3. Relaunch. **Expect**: recovery prompt "Workout in progress — <name>, started X ago"
   with RESUME.
4. Tap RESUME. **Expect**: all logged sets intact; elapsed time correct
   (derived from timestamps, BR-07).
   *(test: `WorkoutRecoveryTest`)*

### 4. History immutability (FR-043, BR-02/03/04, SC-005)

1. Complete a workout from "Chest Workout" including an exercise "Chest Fly".
2. Rename the routine; remove Chest Fly from it; then soft-delete Chest Fly from the library.
3. Open the history entry. **Expect**: still shows the original routine name and Chest Fly
   with all recorded sets — unchanged.
   *(tests: `HistoryProtectionTest` — mandatory regression coverage)*

### 5. Previous performance & pre-fill (FR-045, FR-046, SC-002)

1. Complete "Chest Workout" once.
2. Start it again. **Expect**: last performance shown beside each exercise; today's fields
   pre-filled and editable; completing a set identical to last time takes ≤ 1 action.
   *(test: `PreviousPerformanceTest`)*

### 6. Unit conversion (FR-048, FR-049, R-04)

1. Log sets in kg; switch Settings → weight unit to lb.
2. **Expect**: all weights (history, summaries, previous performance, records) display
   converted to lb; switching back to kg restores the originally typed values exactly
   (lossless).
   *(test: `UnitConversionTest`)*

### 7. Large-history responsiveness (FR-058, SC-006)

1. Seed/generate 1,000+ completed workouts (test fixture).
2. Open History and scroll. **Expect**: smooth scrolling; data loads incrementally
   (LIMIT/OFFSET paging), full history never in memory at once.
   *(test: `HistoryPagingTest`)*

### 8. Personal records (FR-047, R-09)

1. Complete workouts with known values.
2. Open Records. **Expect**: heaviest weight, highest reps, best estimated 1RM (Epley),
   largest volume — each with exercise + date; values update when surpassed.
   *(test: `PersonalRecordsTest`)*

### 9. Rest timer independence (FR-031, FR-032)

1. Complete a set, start the rest timer (default 90s).
2. Use Skip / +30s / Restart. **Expect**: behaves correctly; workout duration timer
   unaffected. Change default in Settings → next rest timer uses new default.
   *(test: `RestTimerTest`)*

### 10. Historical correction (FR-042, US11)

1. Open a completed workout, explicitly choose Edit.
2. Correct a set's weight/reps; add and remove an exercise within that workout; save.
3. **Expect**: only that workout changes; routines, templates, and all other workouts
   untouched; history read-only unless Edit is explicitly chosen.
   *(test: `HistoricalCorrectionTest`)*

## Definition-of-done check (Constitution XV)

Before declaring the feature complete, confirm:

- [x] All scenarios above pass (manually or via the named automated tests).
- [x] The spec's mandatory test list (quality constraint) is fully covered and green.
- [x] No lint/type checks disabled; no tests skipped or weakened.
- [x] Migrations (if any beyond v1) are explicit, tested, non-destructive.
- [x] No secrets, no sensitive data in logs, offline-only behavior verified (SC-007).

## Validation notes (T075/T076 — offline & final validation)

**Offline verification (SC-007, BR-18)**: `app/src/main/AndroidManifest.xml`
declares **zero** network permissions (no `INTERNET`, no `ACCESS_NETWORK_STATE`);
the app is fully local (Room/SQLite) with no network dependency in any
repository or use case. Confirmed by code audit: no HTTP/network libraries are
pulled in, and no permission grants network access.

**Automated test status** (run 2026-09-04, `:app:testDebugUnitTest`):

- 20 JVM/Robolectric suites green, including the mandatory history-protection
  regressions (`HistoryProtectionTest`, `TemplateDeletionHistoryProtectionTest`),
  recovery (`WorkoutRecoveryTest`), paging (`HistoryPagingTest`), and correction
  (`HistoricalCorrectionTest`).
- `:app:detekt` green (structural thresholds tuned for Compose/repository shapes;
  no rules disabled to pass).
- `:app:lintDebug` green.
- `:app:compileDebugAndroidTestKotlin` green (instrumented `SharedComponentUiTest`
  covers empty-state copy and confirmation dialogs; journey-level UI tests require
  a connected device/emulator).

**Emulator-only scenarios** (scenarios 2, 3, 7, 8, 9, 10 in the "tap-through" form)
require `:app:connectedDebugAndroidTest` on a device; their behavior is covered
by the named JVM/Robolectric tests above where the spec mandates automated
coverage.
